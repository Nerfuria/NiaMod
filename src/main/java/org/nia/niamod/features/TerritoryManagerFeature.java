package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.features.eco.ActionQueue;
import org.nia.niamod.features.eco.GameActions;
import org.nia.niamod.features.eco.GuiActions;
import org.nia.niamod.features.eco.Loadouts;
import org.nia.niamod.features.eco.UncommittedChanges;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.SchedulerManager;
import org.nia.niamod.models.api.TerritoryResponse;
import org.nia.niamod.models.eco.*;
import org.nia.niamod.models.eco.GameChange.Borders;
import org.nia.niamod.models.eco.GameChange.Headquarters;
import org.nia.niamod.models.eco.GameChange.Loadout;
import org.nia.niamod.models.eco.GameChange.Route;
import org.nia.niamod.models.eco.GameChange.Stat;
import org.nia.niamod.models.eco.GameChange.Tax;
import org.nia.niamod.gui.screen.TerritoryManagerScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class TerritoryManagerFeature extends Feature {
    private static final int GAME_ACTION_INTERVAL_TICKS = 10;

    private final GameActions gameActions;
    private final ActionQueue gameActionQueue = new ActionQueue();
    private final List<TerritoryLoadout> loadouts;
    private final UncommittedChanges uncommittedChanges = new UncommittedChanges();
    private final Map<String, EnumMap<TerritoryUpgrade, Integer>> territoryStats = new HashMap<>();
    private final Map<String, String> territoryLoadouts = new HashMap<>();
    private final Map<String, Integer> territoryTaxes = new HashMap<>();
    private final Map<String, Boolean> territoryBorders = new HashMap<>();
    private final Map<String, TerritoryRoute> territoryRoutes = new HashMap<>();
    private final Map<String, String> territoryNamesByKey = new HashMap<>();
    private Map<String, TerritoryResponse> territoryResponseCache = Map.of();
    private long territoryResponseCacheUpdatedAtMillis;
    private String headquartersTerritoryName;

    public TerritoryManagerFeature() {
        this(new GameActions());
    }

    public TerritoryManagerFeature(GameActions gameActions) {
        this.gameActions = gameActions == null ? new GameActions() : gameActions;
        this.loadouts = loadLoadouts(this.gameActions);
    }

    @Override
    public void init() {
        KeybindManager.registerKeybinding("Open Territory Manager", GLFW.GLFW_KEY_DOWN, safeRunnable("toggle_screen", this::toggleScreen));
        SchedulerManager.scheduleRepeating(
                safeRunnable("drain_game_action_queue", this::drainGameActionQueue),
                GAME_ACTION_INTERVAL_TICKS,
                GAME_ACTION_INTERVAL_TICKS,
                this::isDisabled
        );
    }

    private void toggleScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TerritoryManagerScreen screen) {
            screen.onClose();
            return;
        }
        minecraft.setScreen(new TerritoryManagerScreen(minecraft.screen, this));
    }

    private void drainGameActionQueue() {
        gameActionQueue.drainNext(gameActions);
    }

    private void queueGameChange(GameChange change) {
        gameActionQueue.enqueue(change);
    }

    private List<TerritoryLoadout> loadLoadouts(GameActions actions) {
        List<TerritoryLoadout> loaded = actions.loadLoadouts();
        return loaded == null || loaded.isEmpty() ? Loadouts.defaults() : List.copyOf(loaded);
    }

    public void requestStats(Collection<String> territoryNames) {
        forEachTerritory(territoryNames, key -> territoryStats.putIfAbsent(key, blankStats()));
    }

    public Map<TerritoryUpgrade, Integer> statsFor(String territoryName) {
        String key = keyFor(territoryName);
        return Collections.unmodifiableMap(new EnumMap<>(key.isEmpty() ? blankStats() : mutableStatsFor(key)));
    }

    public int statLevel(String territoryName, TerritoryUpgrade stat) {
        String key = keyFor(territoryName);
        return stat == null || key.isEmpty() ? 0 : mutableStatsFor(key).getOrDefault(stat, 0);
    }

    public void setStat(String territoryName, TerritoryUpgrade stat, int level) {
        if (stat == null) {
            return;
        }

        String key = keyFor(territoryName);
        if (!key.isEmpty()) {
            int nextLevel = clamp(level, 0, stat.maxLevel());
            EnumMap<TerritoryUpgrade, Integer> stats = mutableStatsFor(key);
            if (stats.getOrDefault(stat, 0) == nextLevel) {
                return;
            }
            stats.put(stat, nextLevel);
            territoryLoadouts.remove(key);
            uncommittedChanges.markStat(key, stat);
            queueGameChange(new Stat(territoryNameForKey(key), stat, nextLevel));
        }
    }

    public void adjustStat(String territoryName, TerritoryUpgrade stat, int delta) {
        setStat(territoryName, stat, statLevel(territoryName, stat) + delta);
    }

    public GuiActions actionsFor(String territoryName) {
        return new GuiActions(this, territoryName);
    }

    public List<TerritoryLoadout> loadouts() {
        return loadouts;
    }

    public Map<String, TerritoryResponse> cachedTerritoryResponse() {
        return territoryResponseCache;
    }

    public void cacheTerritoryResponse(Map<String, TerritoryResponse> response) {
        territoryResponseCache = response == null || response.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(response));
        territoryResponseCacheUpdatedAtMillis = System.currentTimeMillis();
    }

    public long territoryResponseCacheUpdatedAtMillis() {
        return territoryResponseCacheUpdatedAtMillis;
    }

    public String loadoutFor(String territoryName) {
        return territoryLoadouts.getOrDefault(keyFor(territoryName), "");
    }

    public boolean applyLoadout(String loadoutName, Collection<String> territoryNames) {
        TerritoryLoadout loadout = findLoadout(loadoutName);
        if (loadout == null || territoryNames == null || territoryNames.isEmpty()) {
            return false;
        }

        List<String> targets = territoryNames.stream()
                .map(this::cleanTerritoryName)
                .filter(territoryName -> !territoryName.isEmpty())
                .toList();
        if (targets.isEmpty()) {
            return false;
        }

        forEachTerritory(targets, key -> {
            EnumMap<TerritoryUpgrade, Integer> stats = blankStats();
            for (Map.Entry<TerritoryUpgrade, Integer> entry : loadout.levels().entrySet()) {
                TerritoryUpgrade upgrade = entry.getKey();
                if (upgrade != null) {
                    stats.put(upgrade, clamp(entry.getValue() == null ? 0 : entry.getValue(), 0, upgrade.maxLevel()));
                }
            }
            territoryStats.put(key, stats);
            territoryLoadouts.put(key, loadout.name());
            uncommittedChanges.markAllStats(key);
        });
        queueGameChange(new Loadout(loadout, targets));
        return true;
    }

    public void setHeadquarters(String territoryName) {
        String cleaned = cleanTerritoryName(territoryName);
        if (cleaned.isEmpty() || normalize(cleaned).equals(normalize(headquartersTerritoryName))) {
            return;
        }

        keyFor(cleaned);
        headquartersTerritoryName = cleaned;
        uncommittedChanges.markHeadquarters(normalize(cleaned));
        queueGameChange(new Headquarters(cleaned));
    }

    public boolean isHeadquarters(String territoryName) {
        String key = normalize(territoryName);
        return !key.isEmpty() && key.equals(normalize(headquartersTerritoryName));
    }

    public int tax(String territoryName) {
        return territoryTaxes.getOrDefault(keyFor(territoryName), 0);
    }

    public void setTax(String territoryName, int percent) {
        String key = keyFor(territoryName);
        if (!key.isEmpty()) {
            int nextTax = clamp(percent, 0, 70);
            if (territoryTaxes.getOrDefault(key, 0) == nextTax) {
                return;
            }
            territoryTaxes.put(key, nextTax);
            uncommittedChanges.markTax(key);
            queueGameChange(new Tax(territoryNameForKey(key), nextTax));
        }
    }

    public void adjustTax(String territoryName, int delta) {
        setTax(territoryName, tax(territoryName) + delta);
    }

    public void setGlobalTax(Collection<String> territoryNames, int percent) {
        List<String> targets = cleanTargets(territoryNames);
        int nextTax = clamp(percent, 0, 70);
        forEachTerritory(targets, key -> {
            territoryTaxes.put(key, nextTax);
            uncommittedChanges.markTax(key);
        });
        queueGameChange(Tax.global(nextTax));
    }

    public boolean bordersOpen(String territoryName) {
        return territoryBorders.getOrDefault(keyFor(territoryName), true);
    }

    public void setBordersOpen(String territoryName, boolean open) {
        String key = keyFor(territoryName);
        if (!key.isEmpty()) {
            if (territoryBorders.getOrDefault(key, false) == open) {
                return;
            }
            territoryBorders.put(key, open);
            uncommittedChanges.markBorders(key);
            queueGameChange(new Borders(territoryNameForKey(key), open));
        }
    }

    public void toggleBordersOpen(String territoryName) {
        setBordersOpen(territoryName, !bordersOpen(territoryName));
    }

    public void setGlobalBordersOpen(Collection<String> territoryNames, boolean open) {
        List<String> targets = cleanTargets(territoryNames);
        forEachTerritory(targets, key -> {
            territoryBorders.put(key, open);
            uncommittedChanges.markBorders(key);
        });
        queueGameChange(Borders.global(open));
    }

    public TerritoryRoute territoryRoute(String territoryName) {
        return territoryRoutes.getOrDefault(keyFor(territoryName), TerritoryRoute.CHEAPEST);
    }

    public void setTerritoryRoute(String territoryName, TerritoryRoute route) {
        String key = keyFor(territoryName);
        if (!key.isEmpty() && route != null) {
            if (territoryRoute(territoryName) == route) {
                return;
            }
            territoryRoutes.put(key, route);
            uncommittedChanges.markRoute(key);
            queueGameChange(new Route(territoryNameForKey(key), route));
        }
    }

    public void toggleTerritoryRoute(String territoryName) {
        setTerritoryRoute(territoryName, territoryRoute(territoryName).toggled());
    }

    public void setGlobalTerritoryRoute(Collection<String> territoryNames, TerritoryRoute route) {
        List<String> targets = cleanTargets(territoryNames);
        if (route == null) {
            return;
        }

        forEachTerritory(targets, key -> {
            territoryRoutes.put(key, route);
            uncommittedChanges.markRoute(key);
        });
        queueGameChange(Route.global(route));
    }

    public void applyTerritoryRoute(String territoryName) {
        applyTerritoryRoute(territoryName, territoryRoute(territoryName));
    }

    public void applyTerritoryRoute(String territoryName, TerritoryRoute route) {
        String key = keyFor(territoryName);
        if (key.isEmpty() || route == null) {
            return;
        }

        queueGameChange(new Route(territoryNameForKey(key), route));
    }

    public void receiveStatsFromGame(String territoryName, Map<TerritoryUpgrade, Integer> stats) {
        String key = keyFor(territoryName);
        if (key.isEmpty() || stats == null) {
            return;
        }

        EnumMap<TerritoryUpgrade, Integer> current = mutableStatsFor(key);
        for (Map.Entry<TerritoryUpgrade, Integer> entry : stats.entrySet()) {
            TerritoryUpgrade stat = entry.getKey();
            if (stat == null) {
                continue;
            }

            int level = clamp(entry.getValue() == null ? 0 : entry.getValue(), 0, stat.maxLevel());
            if (uncommittedChanges.hasStat(key, stat) || gameActionQueue.hasQueuedStat(key, stat)) {
                if (current.getOrDefault(stat, 0) == level) {
                    uncommittedChanges.clearStat(key, stat);
                }
                continue;
            }
            current.put(stat, level);
        }
    }

    public void receiveTaxFromGame(String territoryName, int percent) {
        String key = keyFor(territoryName);
        if (key.isEmpty()) {
            return;
        }

        int tax = clamp(percent, 0, 70);
        if (uncommittedChanges.hasTax(key) || gameActionQueue.hasQueuedTax(key)) {
            if (territoryTaxes.getOrDefault(key, 0) == tax) {
                uncommittedChanges.clearTax(key);
            }
            return;
        }
        territoryTaxes.put(key, tax);
    }

    public void receiveBordersFromGame(String territoryName, boolean open) {
        String key = keyFor(territoryName);
        if (key.isEmpty()) {
            return;
        }

        if (uncommittedChanges.hasBorders(key) || gameActionQueue.hasQueuedBorders(key)) {
            if (territoryBorders.getOrDefault(key, false) == open) {
                uncommittedChanges.clearBorders(key);
            }
            return;
        }
        territoryBorders.put(key, open);
    }

    public void receiveRouteFromGame(String territoryName, TerritoryRoute route) {
        String key = keyFor(territoryName);
        if (key.isEmpty() || route == null) {
            return;
        }

        if (uncommittedChanges.hasRoute(key) || gameActionQueue.hasQueuedRoute(key, route)) {
            if (territoryRoute(territoryName) == route) {
                uncommittedChanges.clearRoute(key);
            }
            return;
        }
        territoryRoutes.put(key, route);
    }

    public void receiveHeadquartersFromGame(String territoryName) {
        String cleaned = cleanTerritoryName(territoryName);
        String key = normalize(cleaned);
        keyFor(cleaned);
        if (uncommittedChanges.hasHeadquarters() || gameActionQueue.hasQueuedHeadquarters()) {
            if (uncommittedChanges.headquarters().equals(key)) {
                uncommittedChanges.clearHeadquarters();
            }
            return;
        }
        headquartersTerritoryName = cleaned.isEmpty() ? null : cleaned;
    }

    public void pushUncommittedChanges() {
        uncommittedChanges.stats().forEach((territoryKey, stats) -> {
            EnumMap<TerritoryUpgrade, Integer> current = mutableStatsFor(territoryKey);
            for (TerritoryUpgrade stat : stats) {
                queueGameChange(new Stat(territoryNameForKey(territoryKey), stat, current.getOrDefault(stat, 0)));
            }
        });
        for (String territoryKey : uncommittedChanges.taxes()) {
            queueGameChange(new Tax(territoryNameForKey(territoryKey), territoryTaxes.getOrDefault(territoryKey, 0)));
        }
        for (String territoryKey : uncommittedChanges.borders()) {
            queueGameChange(new Borders(territoryNameForKey(territoryKey), territoryBorders.getOrDefault(territoryKey, false)));
        }
        for (String territoryKey : uncommittedChanges.routes()) {
            queueGameChange(new Route(territoryNameForKey(territoryKey), territoryRoutes.getOrDefault(territoryKey, TerritoryRoute.CHEAPEST)));
        }
        if (uncommittedChanges.hasHeadquarters()) {
            queueGameChange(new Headquarters(territoryNameForKey(uncommittedChanges.headquarters())));
        }
    }

    private TerritoryLoadout findLoadout(String loadoutName) {
        String key = normalize(loadoutName);
        for (TerritoryLoadout loadout : loadouts) {
            if (normalize(loadout.name()).equals(key)) {
                return loadout;
            }
        }
        return null;
    }

    private void forEachTerritory(Collection<String> territoryNames, Consumer<String> action) {
        if (territoryNames == null) {
            return;
        }

        for (String territoryName : territoryNames) {
            String key = keyFor(territoryName);
            if (!key.isEmpty()) {
                action.accept(key);
            }
        }
    }

    private List<String> cleanTargets(Collection<String> territoryNames) {
        if (territoryNames == null) {
            return List.of();
        }

        List<String> targets = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String territoryName : territoryNames) {
            String key = keyFor(territoryName);
            if (!key.isEmpty() && seen.add(key)) {
                targets.add(territoryNameForKey(key));
            }
        }
        return targets;
    }

    private EnumMap<TerritoryUpgrade, Integer> mutableStatsFor(String territoryName) {
        return territoryStats.computeIfAbsent(normalize(territoryName), ignored -> blankStats());
    }

    private EnumMap<TerritoryUpgrade, Integer> blankStats() {
        EnumMap<TerritoryUpgrade, Integer> stats = new EnumMap<>(TerritoryUpgrade.class);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            stats.put(upgrade, 0);
        }
        return stats;
    }

    private int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private String normalize(String territoryName) {
        return territoryName == null ? "" : territoryName.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanTerritoryName(String territoryName) {
        return territoryName == null ? "" : territoryName.trim();
    }

    private String keyFor(String territoryName) {
        String cleaned = cleanTerritoryName(territoryName);
        String key = normalize(cleaned);
        if (!key.isEmpty()) {
            territoryNamesByKey.putIfAbsent(key, cleaned);
        }
        return key;
    }

    private String territoryNameForKey(String territoryKey) {
        String key = normalize(territoryKey);
        return territoryNamesByKey.getOrDefault(key, territoryKey);
    }
}

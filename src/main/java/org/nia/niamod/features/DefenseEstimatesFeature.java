package org.nia.niamod.features;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.TerritoryTooltipHeightEvent;
import org.nia.niamod.models.events.TerritoryTooltipRenderEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.util.TerritoryUtils;

import java.util.*;

public class DefenseEstimatesFeature extends Feature {

    private final static Map<Integer, List<EmeraldProdUpgrade>> EMERALD_MODIFIER_OPTIONS = getEmeraldModifierOptions();
    private final static Map<StoredResources, CachedEstimate> ESTIMATE_CACHE = new HashMap<>();

    private static Map<Integer, List<EmeraldProdUpgrade>> getEmeraldModifierOptions() {
        Map<Integer, List<EmeraldProdUpgrade>> result = new HashMap<>();
        for (int effLvl = 0; effLvl < TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels().length; effLvl++) {
            for (int rateLvl = 0; rateLvl < TerritoryUpgrade.EMERALD_RATE.getLevels().length; rateLvl++) {
                double eff = TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].bonus();
                double rate = TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].bonus();
                int modifier = (int) ((100 + eff) * (4.0 / rate));
                if (!result.containsKey(modifier))
                    result.put(modifier, new ArrayList<>());
                result.get(modifier).add(new EmeraldProdUpgrade(
                        effLvl,
                        rateLvl,
                        Math.toIntExact(TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].cost()),
                        Math.toIntExact(TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].cost())
                ));
            }
        }
        return Map.copyOf(result);
    }

    private static EmeraldProdUpgrade getMostLikelyEmProdUpgrade(String territoryName, TerritoryInfo territoryInfo) {
        double treasuryBonus = TerritoryUtils.getTreasuryBonus(territoryName);
        double modifier = TerritoryUtils.getProductionModifier(territoryName, GuildResource.EMERALDS);
        modifier = Math.round(modifier / (1.0 + treasuryBonus) * 100);
        List<EmeraldProdUpgrade> options = EMERALD_MODIFIER_OPTIONS.get((int) modifier);
        if (options == null)
            return null;

        int baseCrop = TerritoryUtils.getResCost(territoryInfo, GuildResource.CROPS);
        int baseOre = TerritoryUtils.getResCost(territoryInfo, GuildResource.ORE);

        int bestOption = -1;
        float bestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < options.size(); i++) {
            int restCrop = baseCrop - options.get(i).cropCost;
            int restOre = baseOre - options.get(i).oreCost;

            if (restCrop <= 0 || restOre <= 0)
                continue;

            int diff = Math.abs(restCrop - restOre);
            if (diff < bestDiff) {
                bestOption = i;
                bestDiff = diff;
            }
        }
        if (bestOption == -1)
            return null;
        return options.get(bestOption);
    }

    private static Pair<Integer, Integer> findUpgradeLevels(TerritoryUpgrade upgrade, TerritoryUpgrade bonus, int targetCost, boolean forceBonus) {
        Pair<Integer, Integer> best = new Pair<>(0, 0);
        long bestCost = 0;

        for (int bonusLevel = forceBonus ? 1 : 0; bonusLevel < bonus.getLevels().length; bonusLevel++) {
            long bonusCost = bonus.getLevels()[bonusLevel].cost();
            for (int upgradeLevel = 0; upgradeLevel < upgrade.getLevels().length; upgradeLevel++) {
                long upgradeCost = upgrade.getLevels()[upgradeLevel].cost();

                long totalCost = upgradeCost + bonusCost;
                if (totalCost <= targetCost + 99 && totalCost > bestCost) {
                    best = new Pair<>(upgradeLevel, bonusLevel);
                    bestCost = upgradeCost + bonusCost;
                } else if (totalCost > targetCost + 50)
                    break;
            }
        }
        return best;
    }

    @Safe
    public static Map<TerritoryUpgrade, Integer> estimateDefenses(String territoryName) {
        return estimate(territoryName).defenses();
    }

    @Safe
    public static List<String> estimateStats(String territoryName) {
        return estimate(territoryName).stats();
    }

    private static CachedEstimate estimate(String territoryName) {
        TerritoryInfo territoryInfo = territoryInfo(territoryName);
        if (territoryInfo == null) {
            return CachedEstimate.EMPTY;
        }

        StoredResources storedResources = StoredResources.from(territoryInfo);
        CachedEstimate cached = ESTIMATE_CACHE.get(storedResources);
        if (cached != null) {
            return cached;
        }

        Map<TerritoryUpgrade, Integer> defenses = estimateDefenses(territoryName, territoryInfo);
        List<String> stats = estimateStats(defenses, territoryInfo);
        CachedEstimate estimate = new CachedEstimate(Map.copyOf(defenses), List.copyOf(stats));
        ESTIMATE_CACHE.put(storedResources, estimate);
        return estimate;
    }

    private static Map<TerritoryUpgrade, Integer> estimateDefenses(String territoryName, TerritoryInfo territoryInfo) {
        Map<TerritoryUpgrade, Integer> result = new HashMap<>();

        if (territoryInfo.isHeadquarters()) {
            return result;  // TODO deal with HQs separately
        }

        boolean hasAuraVolley = territoryInfo.getDefences().getLevel() == 3;    // Force aura and volley if medium

        GuildResource[] resources = {GuildResource.ORE, GuildResource.CROPS, GuildResource.WOOD, GuildResource.FISH};
        TerritoryUpgrade[] upgrades = {TerritoryUpgrade.DAMAGE, TerritoryUpgrade.ATTACK, TerritoryUpgrade.HEALTH, TerritoryUpgrade.DEFENCE};
        TerritoryUpgrade[] bonuses = {TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.TOWER_MULTI_ATTACKS};

        int[] otherCosts = {0, 0, 0, 0};
        // Emerald storage
        int emStorageLvl = TerritoryUtils.getEmeraldStorageLevel(territoryInfo);
        if (emStorageLvl > 0)
            otherCosts[2] = TerritoryUtils.emeraldStorageLevelToCost(emStorageLvl);
        // Emerald prod
        EmeraldProdUpgrade emeraldProdUpgrade = getMostLikelyEmProdUpgrade(territoryName, territoryInfo);
        if (emeraldProdUpgrade != null) {
            otherCosts[0] = emeraldProdUpgrade.oreCost;
            otherCosts[1] = emeraldProdUpgrade.cropCost;
        }

        for (int i = 0; i < resources.length; i++) {
            int cost = TerritoryUtils.getResCost(territoryInfo, resources[i]);
            cost -= otherCosts[i];

            boolean forceBonus = hasAuraVolley && (i == 0 || i == 1);

            Pair<Integer, Integer> levels = findUpgradeLevels(upgrades[i], bonuses[i], cost, forceBonus);

            result.put(upgrades[i], levels.a());
            result.put(bonuses[i], levels.b());
        }

        return result;
    }

    @Subscribe
    @Safe
    public void renderTooltip(TerritoryTooltipRenderEvent event) {
        String territoryName = event.territoryPoi().getName();
        Map<TerritoryUpgrade, Integer> estimates = estimateDefenses(territoryName);

        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            int level = estimates.getOrDefault(upgrade, 0);
            if (level <= 0) continue;

            renderLine(
                    event.guiGraphics(),
                    event.xOffset(),
                    event.renderYOffset(),
                    ChatFormatting.GRAY + "Estimated " + upgrade.getName() + ": " + ChatFormatting.WHITE + level
            );
        }

        for (String stat : estimateStats(territoryName)) {
            renderLine(event.guiGraphics(), event.xOffset(), event.renderYOffset(), ChatFormatting.GRAY + stat);
        }
    }

    @Subscribe
    @Safe
    public void increaseTooltipHeight(TerritoryTooltipHeightEvent event) {
        String territoryName = event.getTerritoryPoi().getName();
        long defenseLines = estimateDefenses(territoryName).values().stream()
                .filter(level -> level != 0)
                .count();
        int statLines = estimateStats(territoryName).size();
        event.addHeight((defenseLines + statLines) * 10.0F);
    }

    private void renderLine(GuiGraphics guiGraphics, int xOffset, LocalFloatRef renderYOffset, String text) {
        renderYOffset.set(renderYOffset.get() + 10.0F);
        FontRenderer.getInstance()
                .renderText(
                        guiGraphics,
                        StyledText.fromString(text),
                        10 + xOffset,
                        10.0F + renderYOffset.get(),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);
    }

    private static List<String> estimateStats(Map<TerritoryUpgrade, Integer> upgrades, TerritoryInfo info) {
        int cons = ownedCons(info);
        int externalCons = info.isHeadquarters() ? ownedExt(info) : 0;
        boolean hq = info.isHeadquarters();
        double multiplier = territoryMultiplier(cons, externalCons, hq);

        double damageMultiplier = upgradeMultiplier(upgrades, TerritoryUpgrade.DAMAGE);
        double minDamage = 1000.0 * damageMultiplier * multiplier;
        double maxDamage = 1500.0 * damageMultiplier * multiplier;
        double attackSpeed = 0.5 * upgradeMultiplier(upgrades, TerritoryUpgrade.ATTACK);
        double health = 300000.0 * upgradeMultiplier(upgrades, TerritoryUpgrade.HEALTH) * multiplier;
        double defense = 0.1 * upgradeMultiplier(upgrades, TerritoryUpgrade.DEFENCE);

        return List.of(
                "Estimated Damage: " + formatStat(minDamage) + "-" + formatStat(maxDamage),
                "Estimated Attack Speed: " + formatStat(attackSpeed) + "x",
                "Estimated Health: " + formatStat(health),
                "Estimated Defense: " + formatPercent(defense)
        );
    }

    private static double territoryMultiplier(int directConnections, int externalConnections, boolean headquarters) {
        double connectionMultiplier = 1.0 + (0.3 * directConnections);
        if (!headquarters) {
            return connectionMultiplier;
        }

        return (1.5 + (0.25 * externalConnections)) * connectionMultiplier;
    }

    private static double upgradeMultiplier(Map<TerritoryUpgrade, Integer> upgrades, TerritoryUpgrade upgrade) {
        int level = upgrades == null ? 0 : upgrades.getOrDefault(upgrade, 0);
        TerritoryUpgrade.Level[] levels = upgrade.getLevels();
        int clampedLevel = Math.max(0, Math.min(level, levels.length - 1));
        return 1.0 + levels[clampedLevel].bonus() / 100.0;
    }

    private static int ownedCons(TerritoryInfo info) {
        String guild = info.getGuildName();
        if (guild == null) {
            return 0;
        }

        int count = 0;
        for (String connection : info.getTradingRoutes()) {
            TerritoryInfo connected = territoryInfo(connection);
            if (connected != null && guild.equals(connected.getGuildName())) {
                count++;
            }
        }
        return count;
    }

    private static int ownedExt(TerritoryInfo info) {
        String guild = info.getGuildName();
        if (guild == null) {
            return 0;
        }

        Set<TerritoryInfo> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<TerritoryInfoDepth> queue = new ArrayDeque<>();
        visited.add(info);
        queue.addLast(new TerritoryInfoDepth(info, 0));

        int count = 0;
        while (!queue.isEmpty()) {
            TerritoryInfoDepth current = queue.removeFirst();
            if (current.depth() >= 3) {
                continue;
            }

            for (String connection : current.info().getTradingRoutes()) {
                TerritoryInfo connected = territoryInfo(connection);
                if (connected == null || !guild.equals(connected.getGuildName()) || !visited.add(connected)) {
                    continue;
                }

                count++;
                queue.addLast(new TerritoryInfoDepth(connected, current.depth() + 1));
            }
        }

        return count;
    }

    private static TerritoryInfo territoryInfo(String territoryName) {
        var poi = Models.Territory.getTerritoryPoiFromAdvancement(territoryName);
        return poi == null ? null : poi.getTerritoryInfo();
    }

    private static String formatStat(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.format(Locale.ROOT, "%,d", Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPercent(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }

    @Safe
    public void init() {
        ESTIMATE_CACHE.clear();
        NiaEventBus.subscribe(this);
    }

    @Safe
    public static void clear_cache() {
        DEFENSE_CACHE.clear();
    }

    private record EmeraldProdUpgrade(int efficientEmeralds, int emeraldRate, int oreCost, int cropCost) {}

    private record CachedEstimate(Map<TerritoryUpgrade, Integer> defenses, List<String> stats) {
        private static final CachedEstimate EMPTY = new CachedEstimate(Map.of(), List.of());
    }

    private record TerritoryInfoDepth(TerritoryInfo info, int depth) {}

    private record StoredResources(
            int emeralds,
            int ore,
            int crops,
            int fish,
            int wood
    ) {
        public static StoredResources from(TerritoryInfo info) {
            return new StoredResources(
                    currentStorage(info, GuildResource.EMERALDS),
                    currentStorage(info, GuildResource.ORE),
                    currentStorage(info, GuildResource.CROPS),
                    currentStorage(info, GuildResource.FISH),
                    currentStorage(info, GuildResource.WOOD)
            );
        }

        private static int currentStorage(TerritoryInfo info, GuildResource resource) {
            CappedValue storage = info.getStorage(resource);
            return storage == null ? 0 : storage.current();
        }
    }
}

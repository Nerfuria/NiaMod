package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.fonts.WynnFont;
import com.wynntils.models.territories.TerritoryAttackTimer;
import com.wynntils.utils.colors.CustomColor;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.SchedulerManager;
import org.nia.niamod.models.records.NearbyTerritoryEntry;
import org.nia.niamod.models.records.QueuedTerritoryEntry;
import org.nia.niamod.models.records.Territory;
import org.nia.niamod.models.records.TerritoryPalette;
import org.nia.niamod.render.Render3D;
import org.nia.niamod.util.WynncraftAPI;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class WarTimersFeature extends Feature {
    private static final int RENDER_MIN_Y = 0;
    private static final int RENDER_MAX_Y = 512;
    private final Map<String, Territory> territories = new HashMap<>();
    private boolean showAll;

    @Override
    public void init() {
        loadTerritoriesAsync();
        WorldRenderEvents.AFTER_ENTITIES.register(context -> runSafe("render", () -> render(context)));
        KeybindManager.registerKeybinding(
                "Show unqueued territories", GLFW.GLFW_KEY_SEMICOLON, () -> showAll = !showAll);
        scheduleWarn();
    }

    private void loadTerritoriesAsync() {
        WynncraftAPI.territoryResponseAsync()
                .whenComplete(
                        (response, throwable) -> {
                            if (throwable != null) {
                                NiamodClient.LOGGER.warn("Failed to load territory data", throwable);
                                return;
                            }
                            Minecraft.getInstance().execute(() -> replaceTerritories(response));
                        });
    }

    private void replaceTerritories(
            Map<String, org.nia.niamod.models.api.TerritoryResponse> response) {
        territories.clear();
        if (response == null || response.isEmpty()) {
            return;
        }
        response.forEach(
                (name, territoryResponse) -> {
                    Territory territory = toTerritory(name, territoryResponse);
                    if (territory != null) {
                        territories.put(name, territory);
                    }
                });
    }

    private Territory toTerritory(String name, org.nia.niamod.models.api.TerritoryResponse response) {
        if (name == null || response == null || response.location() == null) {
            return null;
        }
        int[] start = response.location().start();
        int[] end = response.location().end();
        if (start == null || end == null || start.length < 2 || end.length < 2) {
            return null;
        }
        return new Territory(name, toTerritoryCorner(start), toTerritoryCorner(end));
    }

    private BlockPos toTerritoryCorner(int[] location) {
        return new BlockPos(location[0], RENDER_MIN_Y, location[1]);
    }

    private void scheduleWarn() {
        SchedulerManager.scheduleRepeating(
                this::warnUpcomingTerritories, () -> NyahConfig.getData().getWarnTime() * 20, () -> false);
    }

    private void warnUpcomingTerritories() {
        Minecraft mc = Minecraft.getInstance();
        if (isDisabled() || mc.player == null || NyahConfig.getData().getTerritoryWarningCount() <= 0) {
            return;
        }
        MutableComponent prefix =
                StyledText.fromString(
                                WynnFont.asBackgroundFont(
                                        "QUEUES",
                                        CustomColor.fromInt(0xFFEE6B6E),
                                        CustomColor.fromInt(0xFF3b1344),
                                        "NONE",
                                        "FLAG"))
                        .getComponent();
        long now = System.currentTimeMillis();
        long maxWarningWindowMillis = NyahConfig.getData().getMaxTimeTerr() * 60_000L;
        Models.GuildAttackTimer.getUpcomingTimers()
                .filter(timer -> isTimerWithinWarningWindow(timer, now, maxWarningWindowMillis))
                .sorted(Comparator.comparingLong(TerritoryAttackTimer::timerEnd))
                .limit(NyahConfig.getData().getTerritoryWarningCount())
                .map(this::createWarningLine)
                .forEach(line -> mc.player.displayClientMessage(prefix.copy().append(line), false));
    }

    private boolean isTimerWithinWarningWindow(
            TerritoryAttackTimer timer, long now, long maxWarningWindowMillis) {
        long remainingMillis = timer.timerEnd() - now;
        return remainingMillis >= 0 && remainingMillis < maxWarningWindowMillis;
    }

    private Component createWarningLine(TerritoryAttackTimer timer) {
        return Component.literal(
                        " " + timer.territoryName() + " - " + timeFromNow(timer.timerEnd()) + "\n")
                .withColor(0xFFEE6B6E);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String timeFromNow(long end) {
        long diff = Math.max(0L, end - System.currentTimeMillis());
        int minutes = Math.toIntExact(diff / 60_000L);
        int seconds = Math.toIntExact((diff / 1_000L) % 60L);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void render(WorldRenderContext ctx) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || territories.isEmpty()) {
            return;
        }
        BlockPos playerPosition = player.blockPosition().atY(RENDER_MIN_Y);
        List<TerritoryAttackTimer> upcomingTimers =
                Models.GuildAttackTimer.getUpcomingTimers().toList();
        int maximumDistanceSquared = getMaximumDistanceSquared();
        TerritoryPalette queuedPalette = queuedPalette();
        collectQueuedTerritories(upcomingTimers, playerPosition, maximumDistanceSquared)
                .forEach(entry -> renderTerritory(ctx, entry.territory(), playerPosition, queuedPalette));
        if (showAll) {
            Set<String> queuedTerritoryNames =
                    upcomingTimers.stream()
                            .map(TerritoryAttackTimer::territoryName)
                            .collect(Collectors.toSet());
            TerritoryPalette unqueuedPalette = unqueuedPalette();
            collectUnqueuedTerritories(queuedTerritoryNames, playerPosition, maximumDistanceSquared)
                    .forEach(
                            entry -> renderTerritory(ctx, entry.territory(), playerPosition, unqueuedPalette));
        }
    }

    private TerritoryPalette queuedPalette() {
        return new TerritoryPalette(
                NyahConfig.getData().getColor(), NyahConfig.getData().getColorInside());
    }

    private TerritoryPalette unqueuedPalette() {
        return new TerritoryPalette(
                NyahConfig.getData().getNotQColor(), NyahConfig.getData().getNotQInsideColor());
    }

    private int getMaximumDistanceSquared() {
        int maximumDistance = NyahConfig.getData().getMaximumDistance();
        return maximumDistance * maximumDistance;
    }

    private List<QueuedTerritoryEntry> collectQueuedTerritories(
            List<TerritoryAttackTimer> upcomingTimers,
            BlockPos playerPosition,
            int maximumDistanceSquared) {
        return upcomingTimers.stream()
                .map(timer -> toQueuedTerritoryEntry(timer, playerPosition))
                .filter(entry -> entry != null && entry.distanceSquared() <= maximumDistanceSquared)
                .sorted(
                        Comparator.comparingLong(QueuedTerritoryEntry::timerEnd)
                                .thenComparingInt(QueuedTerritoryEntry::distanceSquared))
                .limit(NyahConfig.getData().getMaximumTerritories())
                .toList();
    }

    private QueuedTerritoryEntry toQueuedTerritoryEntry(
            TerritoryAttackTimer timer, BlockPos playerPosition) {
        Territory territory = territories.get(timer.territoryName());
        if (territory == null) {
            return null;
        }
        return new QueuedTerritoryEntry(
                territory, territory.distanceSquaredTo(playerPosition), timer.timerEnd());
    }

    private List<NearbyTerritoryEntry> collectUnqueuedTerritories(
            Set<String> queuedTerritoryNames, BlockPos playerPosition, int maximumDistanceSquared) {
        return territories.values().stream()
                .filter(territory -> !queuedTerritoryNames.contains(territory.name()))
                .map(
                        territory ->
                                new NearbyTerritoryEntry(territory, territory.distanceSquaredTo(playerPosition)))
                .filter(entry -> entry.distanceSquared() <= maximumDistanceSquared)
                .sorted(Comparator.comparingInt(NearbyTerritoryEntry::distanceSquared))
                .limit(NyahConfig.getData().getMaximumTerritories())
                .toList();
    }

    private void renderTerritory(
            WorldRenderContext ctx,
            Territory territory,
            BlockPos playerPosition,
            TerritoryPalette palette) {
        int color = palette.colorFor(territory.contains(playerPosition));
        Render3D.box(
                ctx,
                territory.minCorner().atY(RENDER_MIN_Y),
                territory.maxCorner().atY(RENDER_MAX_Y),
                color);
    }
}

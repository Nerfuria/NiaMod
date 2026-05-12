package org.nia.niamod.overlays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.config.choices.RadianceOverlayMode;
import org.nia.niamod.features.radiance.CloudflareSync;
import org.nia.niamod.features.radiance.RadianceCooldownParser;
import org.nia.niamod.features.radiance.WarStateTracker;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.mixin.PlayerListHudAccessor;
import org.nia.niamod.models.radiance.CooldownOverlayLine;
import org.nia.niamod.models.radiance.RadianceSyncEntry;
import org.nia.niamod.models.render.TextOverlay;

import java.util.Locale;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class RadianceOverlaySync implements TextOverlay {
    private static final long OVERLAY_MAX_MS = 15000L;
    private static final long CONNECTION_NOTICE_TIMEOUT_MS = 6000L;

    private final WarStateTracker warStateTracker;
    private final CloudflareSync cloudflareSync = new CloudflareSync();

    private long timerStartMs;
    private double timerDurationSeconds;
    private double timerBufferedDurationSeconds;
    private boolean timerActive;
    private boolean pinged;
    private long castPromptUntilMs;

    private boolean wasOnCooldown;
    private boolean connectionNoticePending;
    private long connectionNoticeDeadlineMs;
    private boolean syncConnectionActive;
    @Getter
    private boolean manualConnectRequested;

    public void onJoin() {
        connectionNoticePending = false;
        syncConnectionActive = false;
        manualConnectRequested = false;
        warStateTracker.reset();
        cloudflareSync.reset();
    }

    public boolean isRequireWar() {
        return NyahConfig.getData().isRadianceSyncRequireWar();
    }

    public void setRequireWar(boolean requireWar) {
        if (NyahConfig.getData().isRadianceSyncRequireWar() == requireWar) {
            return;
        }
        NyahConfig.getData().setRadianceSyncRequireWar(requireWar);
        NyahConfig.save();
    }

    public void setManualConnectRequested(boolean manualConnectRequested) {
        if (this.manualConnectRequested == manualConnectRequested) {
            return;
        }
        this.manualConnectRequested = manualConnectRequested;
        if (!manualConnectRequested && !syncConnectionActive) {
            connectionNoticePending = false;
            cloudflareSync.reset();
        }
    }

    public int getSelfAspectTier() {
        return NyahConfig.getData().getRadianceSyncSelfTier();
    }

    public void setSelfAspectTier(int tier) {
        if (tier < 0 || tier > 3 || NyahConfig.getData().getRadianceSyncSelfTier() == tier) {
            return;
        }
        NyahConfig.getData().setRadianceSyncSelfTier(tier);
        NyahConfig.save();
    }

    public RadianceOverlayMode getOverlayMode() {
        RadianceOverlayMode mode = NyahConfig.getData().getRadianceSyncOverlayMode();
        return mode == null ? RadianceOverlayMode.CAST : mode;
    }

    public boolean isStatusOverlayMode() {
        return getOverlayMode() == RadianceOverlayMode.STATUS;
    }

    public String getGroupKey() {
        String key = NyahConfig.getData().getRadianceSyncGroupKey();
        return key == null ? "" : key;
    }

    public void setGroupKey(String key) {
        String normalised = key == null ? "" : key.trim();
        if (normalised.equals(getGroupKey())) {
            return;
        }
        NyahConfig.getData().setRadianceSyncGroupKey(normalised);
        NyahConfig.save();
        cloudflareSync.reset();
    }

    public void onClientTick(Minecraft client) {
        long now = System.currentTimeMillis();
        if (client == null || client.player == null) {
            return;
        }
        String key = getGroupKey();
        String playerName = client.player.getGameProfile().name();
        String playerUuid = client.player.getGameProfile().id() == null
                ? ""
                : client.player.getGameProfile().id().toString();
        boolean warActive = warStateTracker.isInWar();
        boolean connectAllowed = warActive || manualConnectRequested;
        updateSyncConnection(client, now, key, playerName, playerUuid, connectAllowed);

        if (NyahConfig.getData().isRadianceSyncRequireWar() && !warActive) {
            clearTimer();
            clearCastPrompt();
            wasOnCooldown = false;
            return;
        }
        boolean statusMode = isStatusOverlayMode();

        Double cooldownSecondsBoxed = getCooldownRemainingSeconds(client);
        boolean onCooldown = cooldownSecondsBoxed != null;
        if (cooldownSecondsBoxed != null) {
            if (!wasOnCooldown) {
                double cooldownSeconds = cooldownSecondsBoxed;
                double remainingDuration = getDetectedRemainingDurationSeconds(NyahConfig.getData().getRadianceSyncSelfTier(), cooldownSeconds);
                cloudflareSync.send(NyahConfig.getData().getRadianceSyncSelfTier(), toMillis(remainingDuration));
                if (statusMode) {
                    startTimerAt(now, remainingDuration, remainingDuration);
                }
            }
        }
        wasOnCooldown = onCooldown;

        RadianceSyncEntry cfEntry = cloudflareSync.consumePending();
        if (cfEntry != null) {
            int tier = cfEntry.tier();
            if (tier >= 0 && tier <= 3) {
                double remainingDuration = getRemoteRemainingDurationSeconds(now, cfEntry, tier);
                if (remainingDuration > 0.0) {
                    double buffered = statusMode
                            ? remainingDuration
                            : applyCastModePairBuffer(NyahConfig.getData().getRadianceSyncSelfTier(), tier, remainingDuration);
                    startTimerAt(now, remainingDuration, buffered);
                }
            }
        }

        if (timerActive) {
            double remaining = getRemainingBufferedSeconds(now);
            if (!pinged && remaining <= 0.0) {
                playPing(client);
                pinged = true;
                if (!statusMode) {
                    showCastPrompt(now);
                }
            }
            if (!statusMode && remaining <= 0.0 && pinged && !isCastPromptActive(now)) {
                clearTimer();
            }
        }

        if (timerActive && now - timerStartMs > OVERLAY_MAX_MS) {
            clearTimer();
        }
        if (!timerActive && !isCastPromptActive(now)) {
            clearCastPrompt();
        }
    }

    @Override
    public String defaultValue() {
        return isStatusOverlayMode() ? "RADIANCE: 8.7s" : "RADIANCE IN 3.0s";
    }

    @Override
    public int getXOffset() {
        return NyahConfig.getData().getRadianceSyncOverlayOffsetX();
    }

    @Override
    public void setXOffset(int xOffset) {
        NyahConfig.getData().setRadianceSyncOverlayOffsetX(xOffset);
        NyahConfig.save();
    }

    @Override
    public int getYOffset() {
        return NyahConfig.getData().getRadianceSyncOverlayOffsetY();
    }

    @Override
    public void setYOffset(int yOffset) {
        NyahConfig.getData().setRadianceSyncOverlayOffsetY(yOffset);
        NyahConfig.save();
    }

    @Override
    public float getScale() {
        return NyahConfig.getData().getRadianceSyncOverlayScale();
    }

    @Override
    public void setScale(float scale) {
        NyahConfig.getData().setRadianceSyncOverlayScale(scale);
        NyahConfig.save();
    }

    @Override
    public boolean isEnabled() {
        return NyahConfig.getData().isRadianceSyncEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        NyahConfig.getData().setRadianceSyncEnabled(enabled);
        NyahConfig.save();
        if (FeatureManager.getRadianceSyncFeature() != null) {
            FeatureManager.getRadianceSyncFeature().setEnabled(enabled);
        }
    }

    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (NyahConfig.getData().isRadianceSyncRequireWar() && !warStateTracker.isInWar()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean statusMode = isStatusOverlayMode();
        boolean castPromptActive = !statusMode && isCastPromptActive(now);

        int baseX = 0;
        int primaryLineY = 0;
        int cooldownLineY = 12;

        if (statusMode) {
            if (timerActive) {
                double remaining = Math.max(0.0, getRemainingSeconds(now));
                String text = String.format(Locale.ROOT, "RADIANCE: %.1fs", remaining);
                int color = remaining <= 0.0 ? 0xFFFF5555 : 0xFFFFFF55;
                drawCenteredText(drawContext, client, text, baseX, primaryLineY, color);
            }
            drawCooldownLine(drawContext, client, baseX, cooldownLineY);
        } else {
            if (timerActive) {
                double remaining = getRemainingBufferedSeconds(now);
                if (remaining > 0.0) {
                    String text = String.format(Locale.ROOT, "RADIANCE IN %.1fs", Math.max(0.0, remaining));
                    drawCenteredText(drawContext, client, text, baseX, primaryLineY, 0xFFFFFF55);
                } else if (castPromptActive) {
                    drawCenteredText(drawContext, client, "CAST RADIANCE", baseX, primaryLineY, 0xFFFF5555);
                }
            }
            drawCooldownLine(drawContext, client, baseX, cooldownLineY);
        }
    }

    private void updateSyncConnection(Minecraft client,
                                      long now,
                                      String key,
                                      String playerName,
                                      String playerUuid,
                                      boolean warActive) {
        boolean hasIdentity = playerName != null && !playerName.isBlank()
                && !playerUuid.isBlank();
        boolean shouldConnect = warActive && isValidKey(key) && hasIdentity;
        if (shouldConnect) {
            if (!syncConnectionActive) {
                connectionNoticePending = true;
                connectionNoticeDeadlineMs = now + CONNECTION_NOTICE_TIMEOUT_MS;
            }
            cloudflareSync.tick(key, playerName, playerUuid);
        } else {
            connectionNoticePending = false;
            if (syncConnectionActive) {
                cloudflareSync.reset();
            }
        }
        syncConnectionActive = shouldConnect;

        if (!connectionNoticePending || client.player == null) {
            return;
        }
        if (cloudflareSync.isConnected()) {
            connectionNoticePending = false;
            client.player.displayClientMessage(Component.literal("Connected to backend").withStyle(ChatFormatting.GREEN), false);
        } else if (now >= connectionNoticeDeadlineMs) {
            connectionNoticePending = false;
            client.player.displayClientMessage(Component.literal("Backend unreachable").withStyle(ChatFormatting.RED), false);
        }
    }

    private void startTimerAt(long startMs, double durationSeconds, double bufferedDurationSeconds) {
        timerActive = true;
        timerStartMs = startMs;
        timerDurationSeconds = durationSeconds;
        timerBufferedDurationSeconds = bufferedDurationSeconds;
        clearCastPrompt();
        pinged = false;
    }

    private void clearTimer() {
        timerActive = false;
        pinged = false;
    }

    private void showCastPrompt(long now) {
        long durationMs = Math.round(NyahConfig.getData().getRadianceSyncCastPromptSeconds() * 1000.0);
        castPromptUntilMs = now + Math.max(0L, durationMs);
    }

    private boolean isCastPromptActive(long now) {
        return now < castPromptUntilMs;
    }

    private void clearCastPrompt() {
        castPromptUntilMs = 0L;
    }

    private void drawCooldownLine(GuiGraphics drawContext, Minecraft client, int centerX, int y) {
        CooldownOverlayLine cooldownLine = getCooldownOverlayLine(client);
        drawCenteredText(drawContext, client, cooldownLine.text(), centerX, y, cooldownLine.color());
    }

    private CooldownOverlayLine getCooldownOverlayLine(Minecraft client) {
        Double cooldownSeconds = getCooldownRemainingSeconds(client);
        if (cooldownSeconds == null) {
            return new CooldownOverlayLine("RADIANCE READY", 0xFF55FF55);
        }
        String cooldownText = String.format(Locale.ROOT, "COOLDOWN: %.0fs", Math.max(0.0, cooldownSeconds));
        return new CooldownOverlayLine(cooldownText, 0xFF55FFFF);
    }


    private double getRemainingSeconds(long nowMs) {
        double durationMs = timerDurationSeconds * 1000.0;
        return (durationMs - (nowMs - timerStartMs)) / 1000.0;
    }

    private double getRemainingBufferedSeconds(long nowMs) {
        double durationMs = timerBufferedDurationSeconds * 1000.0;
        return (durationMs - (nowMs - timerStartMs)) / 1000.0;
    }

    private void playPing(Minecraft client) {
        if (!NyahConfig.getData().isRadianceSyncPingEnabled() || client.player == null) {
            return;
        }
        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(),
                NyahConfig.getData().getRadianceSyncPingVolume(),
                NyahConfig.getData().getRadianceSyncPingPitch());
    }

    private double getDurationForTier(int tier) {
        return switch (tier) {
            case 1 -> 9.0;
            case 2 -> 10.5;
            case 3 -> 11.5;
            default -> 7.0;
        };
    }

    private double applyCastModePairBuffer(int selfTier, int partnerTier, double durationSeconds) {
        double bufferSeconds = getCastModePairBufferSeconds(selfTier, partnerTier);
        return Math.max(0.0, durationSeconds - bufferSeconds);
    }

    private double getCastModePairBufferSeconds(int selfTier, int partnerTier) {
        if (selfTier == 3 && partnerTier == 3) {
            return 4.0;
        }
        if ((selfTier == 0 && partnerTier == 3) || (selfTier == 3 && partnerTier == 0)) {
            return 2.0;
        }
        double selfExtra = getTierExtraDuration(selfTier);
        double partnerExtra = getTierExtraDuration(partnerTier);
        return (selfExtra + partnerExtra) / 2.0;
    }

    private double getTierExtraDuration(int tier) {
        return switch (tier) {
            case 1 -> 2.0;
            case 2 -> 3.5;
            case 3 -> 4.5;
            default -> 0.0;
        };
    }

    private double getDetectedRemainingDurationSeconds(int tier, double cooldownSeconds) {
        return Math.clamp(cooldownSeconds, 0.0, getDurationForTier(tier));
    }

    private double getRemoteRemainingDurationSeconds(long now, RadianceSyncEntry entry, int tier) {
        if (entry.remainingMs() > 0L) {
            long remainingMs = entry.remainingMs();
            remainingMs -= cloudflareSync.getEstimatedOneWayLatencyMs();
            if (entry.receivedAtMs() > 0L && now > entry.receivedAtMs()) {
                remainingMs -= (now - entry.receivedAtMs());
            }
            if (remainingMs <= 0L) {
                return 0.0;
            }
            return remainingMs / 1000.0;
        }
        return getDurationForTier(tier);
    }

    private Double getCooldownRemainingSeconds(Minecraft client) {
        if (!(client.gui.getTabList() instanceof PlayerListHudAccessor accessor)) {
            return null;
        }
        Component footer = accessor.niamod$getFooter();
        if (footer == null) {
            return null;
        }
        return RadianceCooldownParser.remainingSeconds(footer.getString());
    }

    private long toMillis(double seconds) {
        return Math.max(0L, Math.round(seconds * 1000.0));
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank() && key.length() <= 64;
    }

}

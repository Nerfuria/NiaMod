package org.nia.niamod.config;

import lombok.Getter;
import lombok.Setter;
import org.nia.niamod.config.choices.GuiAnimationMode;
import org.nia.niamod.config.choices.RadianceOverlayMode;
import org.nia.niamod.config.choices.ShoutReplacement;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.gui.theme.FontOption;
import org.nia.niamod.gui.theme.ThemeOption;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NyahConfigData {
    private static final String DEFAULT_RADIANCE_SYNC_WORKER_URL = "https://radiancesync.wavelink.workers.dev";
    private String apiBase = "https://api.wynncraft.com/v3/";
    private String guildName = "Nerfuria";

    private String clickGuiTheme = ThemeOption.DEFAULT.getKey();
    private String clickGuiFont = FontOption.MINECRAFT_DEFAULT.getKey();
    private GuiAnimationMode clickGuiAnimation = GuiAnimationMode.NONE;
    private int animationTime = 1000;
    private float guiOpacity = 0.9f;
    private int customGuiBackground = 0x171A21;
    private int customGuiSecondary = 0x11141B;
    private int customGuiAccent = 0x4794FD;
    private int guiWidth = 500;
    private int guiHeight = 350;

    private boolean resourceTickFeatureEnabled = true;
    private boolean chatEncryptionFeatureEnabled = true;
    private boolean autoStreamFeatureEnabled = true;
    private boolean warTimersFeatureEnabled = true;
    private boolean warTowerEhpFeatureEnabled = true;
    private boolean consuTextFeatureEnabled = true;
    private boolean shoutFilterFeatureEnabled = true;
    private boolean viewModelFeatureEnabled = true;
    private boolean radianceSyncEnabled = true;

    private ShoutReplacement shoutFilterMode = ShoutReplacement.COLLAPSE;

    private String encryptionPrefix = "@";
    private String encryptionKey = "six seven";

    private int color = 0xFFFFFF;
    private int colorInside = 0xFFFFFF;
    private int notQColor = 0xFFFFFF;
    private int notQInsideColor = 0xFFFFFF;
    private int maximumDistance = 1000;
    private int maximumTerritories = 10;
    private int territoryWarningCount = 0;
    private int maxTimeTerr = 10;
    private int warnTime = 30;

    private float idScale = 0.7f;
    private int idXOffset = 1;
    private int idYOffset = 1;
    private String consumableTextureCategory = ConsuTextFeature.NO_TEXTURE_CATEGORY;

    private int streamCooldown = 5000;

    private int xOffset = 0;
    private int yOffset = 0;
    private int zOffset = 0;
    private int xRotation = 0;
    private int yRotation = 0;
    private int zRotation = 0;
    private float itemScale = 1.0f;
    private boolean disableHeldBobbing = true;

    private List<String> favouritePlayers = new ArrayList<>();
    private List<String> avoidedPlayers = new ArrayList<>();
    private List<String> ignoredPlayers = new ArrayList<>();

    private boolean radianceSyncRequireWar = true;
    private int radianceSyncSelfTier = 0;
    private float radianceSyncCastPromptSeconds = 2.0f;
    private RadianceOverlayMode radianceSyncOverlayMode = RadianceOverlayMode.CAST;
    private String radianceSyncWorkerUrl = DEFAULT_RADIANCE_SYNC_WORKER_URL;
    private boolean radianceSyncPingEnabled = false;
    private float radianceSyncPingVolume = 1.0f;
    private float radianceSyncPingPitch = 1.0f;
    private float radianceSyncOverlayScale = 1.0f;
    private int radianceSyncOverlayOffsetX = 0;
    private int radianceSyncOverlayOffsetY = -10;
    private String radianceSyncGroupKey = "";

    private int resTickOverlayOffsetX = 0;
    private int resTickOverlayOffsetY = 0;
    private float resTickOverlayScale = 1.0f;

    public void setEncryptionPrefix(String encryptionPrefix) {
        this.encryptionPrefix = encryptionPrefix == null || encryptionPrefix.isBlank() ? "@" : encryptionPrefix;
    }

    public void normalise() {
        if (apiBase == null || apiBase.isBlank()) {
            apiBase = "https://api.wynncraft.com/v3/";
        }
        if (guildName == null || guildName.isBlank()) {
            guildName = "Nerfuria";
        }

        if (favouritePlayers == null) {
            favouritePlayers = new ArrayList<>();
        }
        if (avoidedPlayers == null) {
            avoidedPlayers = new ArrayList<>();
        }
        if (ignoredPlayers == null) {
            ignoredPlayers = new ArrayList<>();
        }

        clickGuiTheme = ThemeOption.resolve(clickGuiTheme).getKey();
        clickGuiFont = FontOption.resolve(clickGuiFont).getKey();

        if (clickGuiAnimation == null) {
            clickGuiAnimation = GuiAnimationMode.NONE;
        }
        if (shoutFilterMode == null) {
            shoutFilterMode = ShoutReplacement.COLLAPSE;
        }
        if (encryptionPrefix == null || encryptionPrefix.isBlank()) {
            encryptionPrefix = "@";
        }
        if (encryptionKey == null) {
            encryptionKey = "";
        }

        animationTime = clamp(animationTime, 100, 2000);
        guiOpacity = clamp(guiOpacity, 0.1f, 1.0f);
        customGuiBackground &= 0xFFFFFF;
        customGuiSecondary &= 0xFFFFFF;
        customGuiAccent &= 0xFFFFFF;
        guiWidth = clamp(guiWidth, 320, 1000);
        guiHeight = clamp(guiHeight, 260, 800);

        color &= 0xFFFFFF;
        colorInside &= 0xFFFFFF;
        notQColor &= 0xFFFFFF;
        notQInsideColor &= 0xFFFFFF;
        maximumDistance = clamp(maximumDistance, 50, 5000);
        maximumTerritories = clamp(maximumTerritories, 1, 30);
        territoryWarningCount = clamp(territoryWarningCount, 0, 10);
        maxTimeTerr = clamp(maxTimeTerr, 1, 10);
        warnTime = clamp(warnTime, 10, 120);

        idScale = clamp(idScale, 0.25f, 2.5f);
        idXOffset = clamp(idXOffset, -16, 16);
        idYOffset = clamp(idYOffset, -16, 16);
        consumableTextureCategory = ConsuTextFeature.resolveTextureCategory(consumableTextureCategory);

        streamCooldown = clamp(streamCooldown, 100, 10000);

        xOffset = clamp(xOffset, -150, 150);
        yOffset = clamp(yOffset, -150, 150);
        zOffset = clamp(zOffset, -150, 50);
        xRotation = clamp(xRotation, -180, 180);
        yRotation = clamp(yRotation, -180, 180);
        zRotation = clamp(zRotation, -180, 180);
        itemScale = clamp(itemScale, 0.1f, 3.0f);

        if (radianceSyncOverlayMode == null) {
            radianceSyncOverlayMode = RadianceOverlayMode.CAST;
        }
        radianceSyncSelfTier = clamp(radianceSyncSelfTier, 0, 3);
        radianceSyncCastPromptSeconds = clamp(radianceSyncCastPromptSeconds, 0.5f, 10.0f);
        radianceSyncPingVolume = clamp(radianceSyncPingVolume, 0.1f, 2.0f);
        radianceSyncPingPitch = clamp(radianceSyncPingPitch, 0.5f, 2.0f);
        radianceSyncOverlayScale = clamp(radianceSyncOverlayScale, 0.5f, 3.0f);
        radianceSyncOverlayOffsetX = clamp(radianceSyncOverlayOffsetX, -2000, 2000);
        radianceSyncOverlayOffsetY = clamp(radianceSyncOverlayOffsetY, -2000, 2000);
        if (radianceSyncGroupKey == null) {
            radianceSyncGroupKey = "";
        }
        radianceSyncWorkerUrl = normaliseRadianceWorkerUrl(radianceSyncWorkerUrl);
        resTickOverlayScale = clamp(resTickOverlayScale, 0.5f, 3.0f);
        resTickOverlayOffsetX = clamp(resTickOverlayOffsetX, -2000, 2000);
        resTickOverlayOffsetY = clamp(resTickOverlayOffsetY, -2000, 2000);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String normaliseRadianceWorkerUrl(String workerUrl) {
        if (workerUrl == null || workerUrl.isBlank()) {
            return DEFAULT_RADIANCE_SYNC_WORKER_URL;
        }
        try {
            String trimmed = workerUrl.trim();
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host != null && ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme))) {
                return trimmed;
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_RADIANCE_SYNC_WORKER_URL;
    }
}

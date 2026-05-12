package org.nia.niamod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.overlays.RadianceOverlaySync;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@UtilityClass
public class RadianceSyncCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("radiancesync")
                .executes(ctx -> openConfig(ctx.getSource()))
                .then(literal("connect").executes(ctx -> setManualConnectRequested(ctx.getSource(), true)))
                .then(literal("disconnect").executes(ctx -> setManualConnectRequested(ctx.getSource(), false)))
                .then(literal("aspect")
                        .executes(ctx -> cycleAspectTier(ctx.getSource()))
                        .then(argument("tier", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        new String[]{"none", "t1", "t2", "t3"}, builder))
                                .executes(ctx -> setAspectTier(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "tier")))))
                .then(literal("requirewar")
                        .then(argument("value", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        new String[]{"on", "off"}, builder))
                                .executes(ctx -> setRequireWar(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "value")))))
                .then(literal("key")
                        .then(argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> setGroupKey(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "value")))));
    }

    private static int openConfig(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        client.submit(() -> client.setScreen(NyahConfig.getConfigScreen(client.screen)));
        return 1;
    }

    private static int cycleAspectTier(FabricClientCommandSource source) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        int next = (sync.getSelfAspectTier() + 1) % 4;
        sync.setSelfAspectTier(next);
        sendFeedback(source, "Your Radiance Tier = " + next);
        return 1;
    }

    private static int setAspectTier(FabricClientCommandSource source, String rawTier) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        Integer tier = parseAspectTier(rawTier);
        if (tier == null) {
            sendError(source, "tier expects none/t1/t2/t3.");
            return 0;
        }
        sync.setSelfAspectTier(tier);
        sendFeedback(source, "Your Radiance Tier = " + tier);
        return 1;
    }

    private static int setGroupKey(FabricClientCommandSource source, String rawKey) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isBlank()) {
            sendError(source, "key expects a non-empty value.");
            return 0;
        }
        if (key.length() > 64) {
            sendError(source, "key must be 64 characters or fewer.");
            return 0;
        }
        sync.setGroupKey(key);
        sendFeedback(source, "Radiance sync group key updated.");
        return 1;
    }

    private static int setRequireWar(FabricClientCommandSource source, String rawValue) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        String normalised = rawValue.trim().toLowerCase(Locale.ROOT);
        boolean next;
        switch (normalised) {
            case "on" -> next = true;
            case "off" -> next = false;
            default -> {
                sendError(source, "requirewar expects on/off.");
                return 0;
            }
        }
        sync.setRequireWar(next);
        sendFeedback(source, "Overlay requires being in war: " + next);
        return 1;
    }

    private static RadianceOverlaySync getSync(FabricClientCommandSource source) {
        RadianceOverlaySync sync = getSyncOrNull();
        if (sync == null) {
            sendError(source, "Radiance overlay not initialized.");
        }
        return sync;
    }

    private static int setManualConnectRequested(FabricClientCommandSource source, boolean enabled) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        if (enabled && sync.getGroupKey().isBlank()) {
            sendError(source, "Set a Group Key before forcing a connection.");
            return 0;
        }
        if (sync.isManualConnectRequested() == enabled) {
            sendFeedback(source, enabled
                    ? "Manual connection is already enabled."
                    : "Manual sync connection is already disabled.");
            return 1;
        }
        sync.setManualConnectRequested(enabled);
        sendFeedback(source, enabled
                ? "Manual sync connection enabled. Use /radiancesync disconnect to stop testing outside war."
                : "Manual sync connection disabled.");
        return 1;
    }

    private static RadianceOverlaySync getSyncOrNull() {
        var feature = FeatureManager.getRadianceSyncFeature();
        if (feature == null || feature.getOverlay() == null) {
            return null;
        }
        return feature.getOverlay();
    }

    private static Integer parseAspectTier(String rawValue) {
        String normalised = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalised) {
            case "none" -> 0;
            case "t1" -> 1;
            case "t2" -> 2;
            case "t3" -> 3;
            default -> null;
        };
    }

    private static void sendFeedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal(message));
    }

}

package org.nia.niamod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.OverlayManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@UtilityClass
public class NiamodCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("niamod")
                .then(literal("config").executes(ctx -> openConfig()))
                .then(literal("ignoremanager").executes(ctx -> openIgnore(ctx.getSource())))
                .then(literal("ignore").executes(ctx -> openIgnore(ctx.getSource())))
                .then(literal("overlays").executes(ctx -> openOverlays()))
                .then(literal("overlaymanager").executes(ctx -> openOverlays()));
    }

    private static int openConfig() {
        Minecraft client = Minecraft.getInstance();
        client.submit(() -> client.setScreen(NyahConfig.getConfigScreen(client.screen)));
        return 1;
    }

    private static int openIgnore(FabricClientCommandSource source) {
        IgnoreFeature ignoreFeature = FeatureManager.getIgnoreFeature();
        if (ignoreFeature == null) {
            source.sendError(Component.literal("Ignore manager is not initialized."));
            return 0;
        }
        Minecraft.getInstance().submit(ignoreFeature::openScreen);
        return 1;
    }

    private static int openOverlays() {
        Minecraft.getInstance().submit(() -> OverlayManager.openConfig());
        return 1;
    }
}

package org.nia.niamod.managers;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.nia.niamod.gui.screen.OverlayManagerScreen;
import org.nia.niamod.models.render.TextOverlay;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class OverlayManager {

    private static final List<TextOverlay> TEXT_OVERLAYS = new ArrayList<>();

    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("niamod", "overlays"),
                OverlayManager::onHudRender);
    }

    public static void openConfig() {
        openConfig(TEXT_OVERLAYS);
    }

    public static void openConfig(List<? extends TextOverlay> overlays) {
        Minecraft minecraft = Minecraft.getInstance();
        List<TextOverlay> selectedOverlays = new ArrayList<>(overlays);
        minecraft.setScreen(new OverlayManagerScreen(minecraft.screen, selectedOverlays));
    }

    public static void registerOverlay(TextOverlay overlay) {
        TEXT_OVERLAYS.add(overlay);
    }

    private static void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        if (Minecraft.getInstance().screen instanceof OverlayManagerScreen) {
            return;
        }
        int centreX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int centreY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        TEXT_OVERLAYS.forEach(overlay -> {
            if (!overlay.isEnabled()) return;
            drawContext.pose().pushMatrix();
            drawContext.pose().translate(centreX, centreY);
            drawContext.pose().translate(overlay.getXOffset(), overlay.getYOffset());
            drawContext.pose().scale(overlay.getScale(), overlay.getScale());
            overlay.onHudRender(drawContext, tickCounter);
            drawContext.pose().popMatrix();
        });
    }
}

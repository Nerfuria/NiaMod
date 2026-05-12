package org.nia.niamod.models.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public interface TextOverlay {

    String defaultValue();

    void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter);

    default void drawCenteredText(GuiGraphics drawContext, Minecraft client, String text, int x, int y, int color) {
        drawContext.drawString(client.font, text, x - client.font.width(text) / 2, y - client.font.lineHeight / 2, color, true);
    }

    default int getXOffset() {
        return 0;
    }

    default void setXOffset(int xOffset) {
    }

    default int getYOffset() {
        return 0;
    }

    default void setYOffset(int yOffset) {
    }

    default float getScale() {
        return 1.0f;
    }

    default void setScale(float scale) {
    }

    default boolean isEnabled() {
        return true;
    }

    default void setEnabled(boolean enabled) {
    }
}

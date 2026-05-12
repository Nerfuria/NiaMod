package org.nia.niamod.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.nia.niamod.gui.theme.GuiTheme;

import java.util.List;

public interface ConfigComponent {
    default List<EditBox> createEditBoxes(Font font, GuiTheme theme) {
        return List.of();
    }

    void setPosition(int x, int y, int width);

    void updateLabelLayout(Font font, int width);

    int getHeight();

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, GuiTheme theme, int opacity);

    default void updateClipVisibility(int clipTop, int clipBottom) {
    }

    default void hide() {
    }

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}

package org.nia.niamod.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.function.Consumer;

public class OverlayThemeButton extends AbstractButton {
    private final Consumer<InputWithModifiers> action;

    public OverlayThemeButton(int x, int y, int width, int height, Component message, Consumer<InputWithModifiers> action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress(@NotNull InputWithModifiers input) {
        action.accept(input);
    }

    @Override
    public void renderContents(@NotNull GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        GuiTheme theme = GuiStyle.configuredTheme();
        boolean hovered = isHoveredOrFocused();
        int fill = hovered ? Render2D.withAlpha(theme.secondary(), 242) : Render2D.withAlpha(theme.secondary(), 224);
        int border = hovered ? Render2D.withAlpha(0xFFFFFF, 56) : Render2D.withAlpha(0xFFFFFF, 26);
        Render2D.shaderRoundedSurface(context, getX(), getY(), getWidth(), getHeight(), 7, fill, border);

        Font font = Minecraft.getInstance().font;
        int textColor = hovered ? theme.accentColor() : theme.textColor();
        Component styledMessage = GuiStyle.styled(getMessage().getString());
        int textWidth = font.width(styledMessage);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - font.lineHeight) / 2;
        context.drawString(font, styledMessage, textX, textY, textColor, false);
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

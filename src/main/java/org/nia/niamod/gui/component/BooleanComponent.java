package org.nia.niamod.gui.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.BooleanSetting;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;

@RequiredArgsConstructor
public class BooleanComponent implements ConfigComponent {
    public static final int HEIGHT = 18;
    private static final int SWITCH_WIDTH = 24;
    private static final int SWITCH_HEIGHT = 12;
    private static final int SWITCH_RIGHT_PADDING = 4;
    private static final int LABEL_CONTROL_GAP = 8;

    private final BooleanSetting setting;
    private int x, y, width;
    @Getter
    private int height = HEIGHT;
    private float animAmount = -1;

    public static int switchWidth() {
        return SWITCH_WIDTH;
    }

    public static int switchHeight() {
        return SWITCH_HEIGHT;
    }

    public static int switchX(int x, int width) {
        return x + width - SWITCH_WIDTH - SWITCH_RIGHT_PADDING;
    }

    public static int switchY(int centerY) {
        return centerY - SWITCH_HEIGHT / 2;
    }

    public static boolean isOverSwitch(double mouseX, double mouseY, int switchX, int switchY) {
        return mouseX >= switchX
                && mouseX <= switchX + SWITCH_WIDTH
                && mouseY >= switchY
                && mouseY <= switchY + SWITCH_HEIGHT;
    }

    public static void renderToggle(GuiGraphics g, GuiTheme theme, int switchX, int switchY, float animAmount, int opacity) {
        int trackColor = Render2D.withAlpha(theme.secondary(), Math.min(240, opacity));
        int onColor = Render2D.withAlpha(theme.accentColor(), Math.min(255, opacity));
        int currentColor = Render2D.lerpColor(trackColor, onColor, animAmount);

        Render2D.shaderRoundedSurface(
                g,
                switchX,
                switchY,
                SWITCH_WIDTH,
                SWITCH_HEIGHT,
                SWITCH_HEIGHT / 2,
                currentColor,
                Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity))
        );

        int knobSize = SWITCH_HEIGHT - 2;
        int knobY = switchY + 1;
        float knobXStart = switchX + 1;
        float knobXEnd = switchX + SWITCH_WIDTH - 1 - knobSize;
        int knobX = Math.round(knobXStart + animAmount * (knobXEnd - knobXStart));

        Render2D.shaderRoundedRect(g, knobX, knobY, knobSize, knobSize, knobSize / 2, Render2D.withAlpha(0xFFFFFF, Math.min(255, opacity + 50)));
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void updateLabelLayout(Font font, int width) {
        this.width = width;
        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        height = WrappedText.rowHeight(font, lines, HEIGHT);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, GuiTheme theme, int opacity) {
        updateLabelLayout(font, width);
        int centerY = y + height / 2;
        boolean active = Boolean.TRUE.equals(setting.get());
        animAmount = animate(active);

        int textAlpha = Math.min(220, opacity);
        int textColor = active
                ? Render2D.withAlpha(theme.accentColor(), textAlpha)
                : Render2D.withAlpha(0xFFFFFF, textAlpha);
        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x, WrappedText.centeredY(y, height, WrappedText.height(font, lines)), textColor);

        renderToggle(g, theme, switchX(x, width), switchY(centerY), animAmount, opacity);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            setting.set(!Boolean.TRUE.equals(setting.get()));
            return true;
        }
        return false;
    }

    private float animate(boolean active) {
        if (animAmount < 0) {
            animAmount = active ? 1 : 0;
        }
        return animAmount = (animAmount * 4f + (active ? 1f : 0f)) / 5f;
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, width - SWITCH_WIDTH - SWITCH_RIGHT_PADDING - LABEL_CONTROL_GAP);
    }
}

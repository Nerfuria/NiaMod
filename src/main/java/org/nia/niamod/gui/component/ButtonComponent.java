package org.nia.niamod.gui.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.ButtonSetting;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;

@RequiredArgsConstructor
public class ButtonComponent implements ConfigComponent {
    public static final int HEIGHT = 18;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_RIGHT_PADDING = 4;
    private static final int LABEL_CONTROL_GAP = 8;

    private final ButtonSetting setting;
    private int x, y, width;
    @Getter
    private int height = HEIGHT;

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
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        int centerY = y + height / 2;

        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x, WrappedText.centeredY(y, height, WrappedText.height(font, lines)), textColor);

        int buttonX = x + width - BUTTON_WIDTH - BUTTON_RIGHT_PADDING;
        int buttonY = centerY - BUTTON_HEIGHT / 2;
        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

        int buttonFill = hovered ? Render2D.withAlpha(theme.secondary(), Math.min(242, opacity)) : Render2D.withAlpha(theme.secondary(), Math.min(224, opacity));
        int buttonBorder = hovered ? Render2D.withAlpha(0xFFFFFF, Math.min(56, opacity)) : Render2D.withAlpha(0xFFFFFF, Math.min(26, opacity));

        Render2D.shaderRoundedSurface(
                g,
                buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                4, buttonFill, buttonBorder
        );

        String btnText = setting.getButtonText();
        if (btnText != null) {
            int tw = font.width(GuiStyle.styled(btnText));
            g.drawString(font, GuiStyle.styled(btnText), buttonX + (BUTTON_WIDTH - tw) / 2, buttonY + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, textColor, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int centerY = y + height / 2;
            int buttonX = x + width - BUTTON_WIDTH - BUTTON_RIGHT_PADDING;
            int buttonY = centerY - BUTTON_HEIGHT / 2;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH && my >= buttonY && my <= buttonY + BUTTON_HEIGHT) {
                Runnable action = setting.get();
                if (action != null) {
                    action.run();
                }
                return true;
            }
        }
        return false;
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, width - BUTTON_WIDTH - BUTTON_RIGHT_PADDING - LABEL_CONTROL_GAP);
    }
}

package org.nia.niamod.gui.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.ChoiceSetting;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;

@RequiredArgsConstructor
public class ChoiceComponent implements ConfigComponent {
    public static final int HEIGHT = 24;
    private static final int ARROW_WIDTH = 16;
    private static final int MIN_CONTROL_WIDTH = 104;
    private static final int CONTROL_HEIGHT = 14;
    private static final int ROW_SIDE_PADDING = 4;
    private static final int LABEL_GAP = 10;

    private final ChoiceSetting setting;
    private int x;
    private int y;
    private int width;
    @Getter
    private int height = HEIGHT;
    private Font lastFont;

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
        lastFont = font;
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x + 1, WrappedText.centeredY(y, height, WrappedText.height(font, lines)), textColor);

        UiRect control = controlRect(font);
        boolean hovered = mouseX >= control.x() && mouseX <= control.right()
                && mouseY >= control.y() && mouseY <= control.bottom();

        int fillColor = hovered
                ? Render2D.withAlpha(theme.secondary(), Math.min(245, opacity + 24))
                : Render2D.withAlpha(theme.secondary(), Math.min(225, opacity));
        int borderColor = hovered
                ? Render2D.withAlpha(theme.accentColor(), Math.min(96, opacity + 18))
                : Render2D.withAlpha(0xFFFFFF, Math.min(28, textAlpha));
        Render2D.shaderRoundedSurface(g, control.x(), control.y(), control.width(), control.height(), 5, fillColor, borderColor);

        int dividerColor = Render2D.withAlpha(0xFFFFFF, Math.min(36, textAlpha));
        g.fill(control.x() + ARROW_WIDTH, control.y() + 2, control.x() + ARROW_WIDTH + 1, control.bottom() - 2, dividerColor);
        g.fill(control.right() - ARROW_WIDTH - 1, control.y() + 2, control.right() - ARROW_WIDTH, control.bottom() - 2, dividerColor);

        int arrowColor = hovered ? 0xFFFFFFFF : 0xC8FFFFFF;
        int controlTextY = control.y() + Math.max(0, (control.height() - font.lineHeight) / 2);
        g.drawString(font, GuiStyle.styled("<"), control.x() + 5, controlTextY, arrowColor, false);
        g.drawString(font, GuiStyle.styled(">"), control.right() - 9, controlTextY, arrowColor, false);

        String label = setting.displayValue(setting.get());
        int maxLabelWidth = control.width() - (ARROW_WIDTH * 2) - 4;

        if (GuiStyle.styledWidth(font, label) > maxLabelWidth) {
            while (!label.isEmpty() && GuiStyle.styledWidth(font, label + "...") > maxLabelWidth) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }

        int labelWidth = GuiStyle.styledWidth(font, label);
        int labelX = control.x() + (control.width() - labelWidth) / 2;
        g.drawString(font, GuiStyle.styled(label), labelX, controlTextY, textColor, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        UiRect control = controlRect(lastFont != null ? lastFont : Minecraft.getInstance().font);
        if (mouseX < control.x() || mouseX > control.right() || mouseY < control.y() || mouseY > control.bottom()) {
            return false;
        }

        if (mouseX <= control.x() + ARROW_WIDTH) {
            setting.previous();
        } else if (mouseX >= control.right() - ARROW_WIDTH) {
            setting.next();
        } else {
            setting.next();
        }
        return true;
    }

    private UiRect controlRect(Font font) {
        int controlX = x + (int) (width * 0.45f);
        int controlWidth = Math.max(MIN_CONTROL_WIDTH, x + width - ROW_SIDE_PADDING - controlX);
        int controlY = y + (height - CONTROL_HEIGHT) / 2;
        return new UiRect(controlX, controlY, controlWidth, CONTROL_HEIGHT);
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, (int) (width * 0.45f) - LABEL_GAP - 1);
    }
}

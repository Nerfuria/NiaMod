package org.nia.niamod.gui.component;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.gui.GuiStyle;

import java.util.List;

@UtilityClass
class WrappedText {
    static final int LINE_GAP = 2;
    static final int VERTICAL_PADDING = 4;

    static List<FormattedCharSequence> lines(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return font.split(GuiStyle.styled(text), Math.max(1, maxWidth));
    }

    static int height(Font font, List<FormattedCharSequence> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        return lines.size() * font.lineHeight + (lines.size() - 1) * LINE_GAP;
    }

    static int rowHeight(Font font, List<FormattedCharSequence> lines, int minimumHeight) {
        int textHeight = height(font, lines);
        if (textHeight == 0) {
            return minimumHeight;
        }
        return Math.max(minimumHeight, textHeight + VERTICAL_PADDING * 2);
    }

    static int centeredY(int y, int rowHeight, int textHeight) {
        return y + Math.max(0, (rowHeight - textHeight) / 2);
    }

    static void draw(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int x, int y, int color) {
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, lineY, color, false);
            lineY += font.lineHeight + LINE_GAP;
        }
    }
}

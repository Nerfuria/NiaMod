package org.nia.niamod.gui.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.StringSetting;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;

@RequiredArgsConstructor
public class StringInputComponent implements ConfigComponent {
    public static final int HEIGHT = 24;
    private static final int MIN_FIELD_WIDTH = 96;
    private static final int ROW_SIDE_PADDING = 4;
    private static final int LABEL_GAP = 10;

    private final StringSetting setting;
    @Getter
    private EditBox editBox;
    private int x, y, width;
    private int height = HEIGHT;

    public EditBox createEditBox(Font font, GuiTheme theme) {
        int inputHeight = Math.max(14, font.lineHeight + 3);
        editBox = new EditBox(font, 0, 0, 230, inputHeight, GuiStyle.styled(setting.getTitle()));
        editBox.setMaxLength(setting.getMaxLength());
        editBox.setBordered(false);
        editBox.setHeight(inputHeight);
        editBox.setTextColor(0xFFFFFFFF);
        editBox.setTextColorUneditable(0x82FFFFFF);
        GuiStyle.applyFont(editBox, setting.getTitle());
        editBox.setValue(setting.format());
        editBox.setResponder(setting::tryParseAndSet);
        return editBox;
    }

    @Override
    public List<EditBox> createEditBoxes(Font font, GuiTheme theme) {
        return List.of(createEditBox(font, theme));
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        int fieldHeight = editBox != null ? editBox.getHeight() : 14;
        return Math.max(height, fieldHeight + 8);
    }

    public void updateLabelLayout(Font font, int width) {
        this.width = width;
        int fieldHeight = editBox != null ? editBox.getHeight() : Math.max(14, font.lineHeight + 3);
        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        height = Math.max(WrappedText.rowHeight(font, lines, HEIGHT), fieldHeight + 8);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, GuiTheme theme, int opacity) {
        updateLabelLayout(font, width);
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        int rowHeight = getHeight();
        int centerY = y + rowHeight / 2;
        int fieldX = x + (int) (width * 0.45f);
        int fieldWidth = Math.max(MIN_FIELD_WIDTH, x + width - ROW_SIDE_PADDING - fieldX);

        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x + 1, WrappedText.centeredY(y, rowHeight, WrappedText.height(font, lines)), textColor);

        if (editBox != null) {
            int fieldHeight = editBox.getHeight();
            int fieldY = y + Math.max(0, (rowHeight - fieldHeight) / 2);
            int border = editBox.isFocused()
                    ? Render2D.withAlpha(theme.accentColor(), Math.min(120, opacity + 30))
                    : Render2D.withAlpha(0xFFFFFF, Math.min(34, opacity / 6 + 18));
            Render2D.shaderRoundedSurface(
                    g,
                    fieldX,
                    fieldY,
                    fieldWidth,
                    fieldHeight,
                    5,
                    Render2D.withAlpha(theme.secondary(), Math.min(210, opacity)),
                    border
            );

            boolean active = opacity > 10;
            editBox.visible = active;
            editBox.active = active;
            editBox.setEditable(active);
            if (!active && editBox.isFocused()) {
                editBox.setFocused(false);
            }
            if (!editBox.isFocused()) {
                editBox.setValue(setting.format());
                editBox.moveCursorToStart(false);
            }
            GuiStyle.layoutBorderlessEditBox(editBox, font, fieldX + 6, fieldY, fieldWidth - 12, fieldHeight);
        }
    }

    public void updateClipVisibility(int clipTop, int clipBottom) {
        if (editBox == null) return;
        int editY = y + Math.max(0, (getHeight() - editBox.getHeight()) / 2);
        int editBottom = editY + editBox.getHeight();
        boolean visible = editY >= clipTop && editBottom <= clipBottom;
        if (!visible) {
            hide();
        }
    }

    public void hide() {
        if (editBox != null) {
            editBox.setX(-300);
            editBox.setY(-300);
            editBox.setFocused(false);
            editBox.visible = false;
            editBox.active = false;
            editBox.setEditable(false);
        }
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, (int) (width * 0.45f) - LABEL_GAP - 1);
    }
}

package org.nia.niamod.gui.component;

import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.ConfigSetting;
import org.nia.niamod.config.setting.FloatSetting;
import org.nia.niamod.config.setting.IntSetting;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;
import java.util.Locale;

public class SliderComponent implements ConfigComponent {
    public static final int HEIGHT = 18;
    private static final int TRACK_HEIGHT = 2;
    private static final int GRABBER_SIZE = 5;

    private final ConfigSetting<?> setting;
    private final float min;
    private final float max;
    private final boolean isInt;

    private int x, y, width;
    private int trackX;
    private int trackWidth;
    @Getter
    private int height = HEIGHT;
    private boolean dragging;
    private float renderPercentage;
    private float dragSquish;
    private EditBox editBox;

    public SliderComponent(ConfigSetting<?> setting) {
        this.setting = setting;
        if (setting instanceof IntSetting intSetting) {
            this.min = intSetting.getMin();
            this.max = intSetting.getMax();
            this.isInt = true;
        } else if (setting instanceof FloatSetting floatSetting) {
            this.min = floatSetting.getMin();
            this.max = floatSetting.getMax();
            this.isInt = false;
        } else {
            throw new IllegalArgumentException("SliderComponent requires IntSetting or FloatSetting");
        }
        this.renderPercentage = getProgress();
    }

    public EditBox createEditBox(Font font, GuiTheme theme) {
        int inputHeight = Math.max(16, font.lineHeight + 4);
        editBox = new EditBox(font, 0, 0, 30, inputHeight, GuiStyle.styled(""));
        editBox.setBordered(false);
        editBox.setHeight(inputHeight);
        editBox.setTextColor(0xFFFFFFFF);
        editBox.setTextColorUneditable(0x82FFFFFF);
        GuiStyle.applyFont(editBox, "");
        editBox.setValue(formatValue());
        editBox.setResponder(this::onTextChanged);
        return editBox;
    }

    @Override
    public List<EditBox> createEditBoxes(Font font, GuiTheme theme) {
        return List.of(createEditBox(font, theme));
    }

    @SuppressWarnings("unchecked")
    private void onTextChanged(String text) {
        if (dragging) return;
        try {
            if (isInt) {
                int val = Integer.parseInt(text);
                val = Math.max((int) min, Math.min((int) max, val));
                ((ConfigSetting<Integer>) setting).set(val);
            } else {
                float val = Float.parseFloat(text);
                val = Math.max(min, Math.min(max, val));
                ((ConfigSetting<Float>) setting).set(val);
            }
            renderPercentage = getProgress();
        } catch (NumberFormatException ignored) {
        }
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
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        int centerY = y + height / 2;

        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x, WrappedText.centeredY(y, height, WrappedText.height(font, lines)), textColor);

        float targetPercentage = getProgress();
        renderPercentage = (renderPercentage * 29 + targetPercentage) / 30;
        dragSquish *= 0.82f;

        String valueText = formatValue();
        int minBoxWidth = Math.max(30, GuiStyle.styledWidth(font, valueText) + 8);
        int valueWidth = Math.max(minBoxWidth, (int) (width * 0.15f));
        int valueX = x + width - valueWidth;

        int controlX = x + (int) (width * 0.45f);
        trackWidth = Math.max(50, valueX - 8 - controlX);

        trackX = controlX;
        int trackY = centerY - TRACK_HEIGHT / 2;

        int bgColor = Render2D.withAlpha(theme.background(), Math.min(254, opacity));
        int filledColor = Render2D.withAlpha(theme.accentColor(), Math.min(180, opacity));
        g.fill(trackX, trackY, trackX + trackWidth, trackY + TRACK_HEIGHT, bgColor);
        int fillWidth = Math.round(renderPercentage * trackWidth);
        g.fill(trackX, trackY, trackX + fillWidth, trackY + TRACK_HEIGHT, filledColor);

        float grabberCenterX = trackX + renderPercentage * trackWidth;
        int knobWidth = Math.max(2, GRABBER_SIZE - Math.round(dragSquish));
        int knobHeight = GRABBER_SIZE + Math.round(dragSquish * 0.65f);
        int gx = Math.round(grabberCenterX - knobWidth / 2.0f);
        int gy = centerY - knobHeight / 2;
        int accent = Render2D.withAlpha(theme.accentColor(), Math.min(255, opacity));
        g.fill(gx, gy, gx + knobWidth, gy + knobHeight, accent);

        int boxHeight = editBox != null ? editBox.getHeight() : Math.max(18, font.lineHeight + 6);
        int boxY = centerY - boxHeight / 2;
        int boxBorder = editBox != null && editBox.isFocused()
                ? Render2D.withAlpha(theme.accentColor(), Math.min(120, opacity + 30))
                : Render2D.withAlpha(0xFFFFFF, Math.min(34, opacity / 6 + 18));
        Render2D.shaderRoundedSurface(
                g,
                valueX,
                boxY,
                valueWidth,
                boxHeight,
                5,
                Render2D.withAlpha(theme.secondary(), Math.min(200, opacity)),
                boxBorder
        );

        if (editBox != null) {
            boolean active = opacity > 10;
            editBox.visible = active;
            editBox.active = active;
            editBox.setEditable(active);
            if (!editBox.isFocused()) {
                editBox.setValue(valueText);
                editBox.moveCursorToStart(false);
            }
            GuiStyle.layoutBorderlessEditBox(editBox, font, valueX + 6, boxY, valueWidth - 12, boxHeight);
        } else {
            int textWidth = GuiStyle.styledWidth(font, valueText);
            g.drawString(font, GuiStyle.styled(valueText), valueX + (valueWidth - textWidth) / 2, centerY - font.lineHeight / 2 + 1, textColor, false);
        }
    }

    public void updateClipVisibility(int clipTop, int clipBottom) {
        if (editBox == null) return;
        int centerY = y + height / 2;
        int editY = centerY - editBox.getHeight() / 2;
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= trackX - 3 && mouseX <= trackX + trackWidth + 3
                && mouseY >= y && mouseY <= y + height) {
            dragging = true;
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            dragSquish = Math.min(3.0f, dragSquish + (float) Math.abs(deltaX) * 0.35f);
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private float getProgress() {
        if (max == min) return 0;
        Number value = (Number) setting.get();
        return Math.max(0, Math.min(1, (value.floatValue() - min) / (max - min)));
    }

    private String formatValue() {
        if (isInt) return Integer.toString(((Number) setting.get()).intValue());
        return String.format(Locale.ROOT, "%.2f", ((Number) setting.get()).floatValue());
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, (int) (width * 0.45f) - 10);
    }

    @SuppressWarnings("unchecked")
    private void updateValueFromMouse(double mouseX) {
        float percent = (float) ((mouseX - trackX) / trackWidth);
        percent = Math.max(0, Math.min(1, percent));

        if (isInt) {
            int value = Math.round(min + percent * (max - min));
            ((ConfigSetting<Integer>) setting).set(value);
        } else {
            float value = min + percent * (max - min);
            ((ConfigSetting<Float>) setting).set(value);
        }
    }
}

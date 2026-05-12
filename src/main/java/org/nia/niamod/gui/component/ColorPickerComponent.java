package org.nia.niamod.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.render.GradientQuad;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.List;

public class ColorPickerComponent implements ConfigComponent {
    public static final int CLOSED_HEIGHT = 18;
    public static final int OPENED_HEIGHT = 116;
    private static final int SWATCH_SIZE = 12;
    private static final int SWATCH_RIGHT_PADDING = 4;
    private static final int HEX_WIDTH = 50;
    private static final int HEX_GAP = 8;
    private static final int LABEL_CONTROL_GAP = 8;
    private static final int PICKER_PANEL_GAP = 2;
    private static final int PICKER_BOTTOM_PADDING = 4;

    private final ColorSetting setting;
    private boolean expanded;
    private float hue, saturation, value;
    private boolean draggingSV;
    private boolean draggingHue;
    private boolean syncingHexInput;

    private int x, y, width;
    private int closedHeight = CLOSED_HEIGHT;
    private EditBox hexInput;

    public ColorPickerComponent(ColorSetting setting) {
        this.setting = setting;
        float[] hsv = Render2D.rgbToHsv(setting.get() & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    public EditBox createEditBox(Font font, GuiTheme theme) {
        int inputHeight = Math.max(16, font.lineHeight + 4);
        hexInput = new EditBox(font, 0, 0, 46, inputHeight, GuiStyle.styled("Hex"));
        hexInput.setBordered(false);
        hexInput.setHeight(inputHeight);
        hexInput.setTextColor(0xFFFFFFFF);
        GuiStyle.applyFont(hexInput, "Hex");
        setHexInputValue(hex(setting.get() & 0xFFFFFF));
        hexInput.setResponder(this::onHexChanged);
        return hexInput;
    }

    @Override
    public List<EditBox> createEditBoxes(Font font, GuiTheme theme) {
        return List.of(createEditBox(font, theme));
    }

    private String getHex() {
        return hex(currentRgb());
    }

    private String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private void onHexChanged(String text) {
        if (syncingHexInput) return;
        if (draggingHue || draggingSV) return;
        if (text.startsWith("#")) text = text.substring(1);
        if (text.length() == 6) {
            try {
                int rgb = Integer.parseInt(text, 16);
                syncHsvFromRgb(rgb);
                commitColor(rgb);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        return expanded ? closedHeight + openedExtraHeight() : closedHeight;
    }

    public void updateLabelLayout(Font font, int width) {
        this.width = width;
        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        closedHeight = WrappedText.rowHeight(font, lines, CLOSED_HEIGHT);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, GuiTheme theme, int opacity) {
        syncFromSettingIfIdle();
        updateLabelLayout(font, width);
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        int centerY = y + closedHeight / 2;

        List<FormattedCharSequence> lines = WrappedText.lines(font, setting.getTitle(), labelMaxWidth(width));
        WrappedText.draw(g, font, lines, x, WrappedText.centeredY(y, closedHeight, WrappedText.height(font, lines)), textColor);

        int swatchX = swatchX();
        int swatchY = centerY - SWATCH_SIZE / 2;
        int currentColor = 0xFF000000 | Render2D.hsvToRgb(hue, saturation, value);

        Render2D.shaderRoundedSurface(
                g,
                swatchX - 1,
                swatchY - 1,
                SWATCH_SIZE + 2,
                SWATCH_SIZE + 2,
                4,
                Render2D.withAlpha(currentColor, opacity),
                Render2D.withAlpha(0xFFFFFF, Math.min(72, opacity))
        );

        int hexX = hexX();
        int hexHeight = hexInput != null ? hexInput.getHeight() : Math.max(18, font.lineHeight + 6);
        int hexY = centerY - hexHeight / 2;

        if (hexInput != null) {
            int hexBorder = hexInput.isFocused()
                    ? Render2D.withAlpha(theme.accentColor(), Math.min(120, opacity + 30))
                    : Render2D.withAlpha(0xFFFFFF, Math.min(34, opacity / 6 + 18));
            Render2D.shaderRoundedSurface(
                    g,
                    hexX,
                    hexY,
                    HEX_WIDTH,
                    hexHeight,
                    5,
                    Render2D.withAlpha(theme.secondary(), Math.min(200, opacity)),
                    hexBorder
            );

            boolean active = opacity > 10;
            hexInput.visible = active;
            hexInput.active = active;
            hexInput.setEditable(active);
            if (!hexInput.isFocused()) {
                setHexInputValue(getHex());
                hexInput.moveCursorToStart(false);
            }
            GuiStyle.layoutBorderlessEditBox(hexInput, font, hexX + 6, hexY, HEX_WIDTH - 12, hexHeight);
        }

        if (!expanded) {
            return;
        }

        int panelX = x + 4;
        int panelY = pickerPanelY();
        int panelW = width - 8;
        int panelH = pickerPanelHeight();

        Render2D.shaderRoundedSurface(
                g,
                panelX,
                panelY,
                panelW,
                panelH,
                6,
                Render2D.withAlpha(theme.secondary(), opacity),
                Render2D.withAlpha(0xFFFFFF, Math.min(28, opacity / 6 + 16))
        );

        int svX = panelX + 4;
        int svY = panelY + 4;
        int svW = panelW - 8;
        int svH = 70;
        renderSVField(g, svX, svY, svW, svH, opacity);
        Render2D.outline(g, new UiRect(svX - 1, svY - 1, svW + 2, svH + 2), Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity)));

        int crossX = svX + Math.round(saturation * svW);
        int crossY = svY + Math.round((1.0f - value) * svH);
        Render2D.circle(g, crossX, crossY, 6, Render2D.withAlpha(0xFFFFFF, opacity));
        Render2D.circle(g, crossX, crossY, 4, Render2D.withAlpha(currentColor, opacity));

        int hueX = panelX + 4;
        int hueY = svY + svH + 6;
        int hueW = panelW - 8;
        int hueH = 8;
        renderHueBar(g, hueX, hueY, hueW, hueH, opacity);
        Render2D.outline(g, new UiRect(hueX - 1, hueY - 1, hueW + 2, hueH + 2), Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity)));

        float huePointer = hue / 360.0f * hueW;
        int markerX = hueX + Math.round(huePointer);
        Render2D.shaderRoundedRect(g, markerX - 2, hueY - 1, 4, hueH + 2, 2, Render2D.withAlpha(0xFFFFFF, opacity));
    }

    private void renderSVField(GuiGraphics g, int fx, int fy, int fw, int fh, int opacity) {
        g.guiRenderState.submitGuiElement(new GradientQuad(
                g.pose(),
                fx,
                fx + fw,
                fy,
                fy + fh,
                g.scissorStack.peek(),
                Render2D.withAlpha(Render2D.hsvToRgb(hue, 1.0f, 1.0f), opacity)
        ));
    }

    private void renderHueBar(GuiGraphics g, int bx, int by, int bw, int bh, int opacity) {
        for (int i = 0; i < bw; i++) {
            float h = (float) i / bw * 360.0f;
            int color = Render2D.withAlpha(Render2D.hsvToRgb(h, 1.0f, 1.0f), opacity);
            g.fill(bx + i, by, bx + i + 1, by + bh, color);
        }
    }

    public void updateClipVisibility(int clipTop, int clipBottom) {
        if (hexInput == null) return;
        int centerY = y + closedHeight / 2;
        int editY = centerY - hexInput.getHeight() / 2;
        int editBottom = editY + hexInput.getHeight();
        boolean visible = editY >= clipTop && editBottom <= clipBottom;
        if (!visible) {
            hide();
        }
    }

    public void hide() {
        if (hexInput != null) {
            hexInput.setX(-300);
            hexInput.setY(-300);
            hexInput.setFocused(false);
            hexInput.visible = false;
            hexInput.active = false;
            hexInput.setEditable(false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        syncFromSettingIfIdle();

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + closedHeight) {
            if (mouseX >= hexX() && mouseX <= hexX() + HEX_WIDTH) {
                return false;
            }

            expanded = !expanded;
            return true;
        }

        if (!expanded) return false;

        int panelX = x + 4;
        int panelY = pickerPanelY();
        int panelW = width - 8;

        int svX = panelX + 4;
        int svY = panelY + 4;
        int svW = panelW - 8;
        int svH = 70;

        if (mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH) {
            draggingSV = true;
            updateSV(mouseX, mouseY, svX, svY, svW, svH);
            return true;
        }

        int hueX = panelX + 4;
        int hueY = svY + svH + 6;
        int hueW = panelW - 8;
        int hueH = 8;

        if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY - 2 && mouseY <= hueY + hueH + 2) {
            draggingHue = true;
            updateHue(mouseX, hueX, hueW);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSV) {
            int panelX = x + 4;
            int panelY = pickerPanelY();
            int panelW = width - 8;
            int svX = panelX + 4;
            int svY = panelY + 4;
            int svW = panelW - 8;
            int svH = 70;
            updateSV(mouseX, mouseY, svX, svY, svW, svH);
            return true;
        }
        if (draggingHue) {
            int panelX = x + 4;
            int panelW = width - 8;
            int hueX = panelX + 4;
            int hueW = panelW - 8;
            updateHue(mouseX, hueX, hueW);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSV || draggingHue) {
            draggingSV = false;
            draggingHue = false;
            return true;
        }
        return false;
    }

    private int swatchX() {
        return x + width - SWATCH_SIZE - SWATCH_RIGHT_PADDING;
    }

    private int hexX() {
        return swatchX() - HEX_WIDTH - HEX_GAP;
    }

    private int labelMaxWidth(int width) {
        return Math.max(1, width - SWATCH_SIZE - SWATCH_RIGHT_PADDING - HEX_WIDTH - HEX_GAP - LABEL_CONTROL_GAP);
    }

    private int pickerPanelY() {
        return y + closedHeight + PICKER_PANEL_GAP;
    }

    private int pickerPanelHeight() {
        return OPENED_HEIGHT - CLOSED_HEIGHT - PICKER_PANEL_GAP - PICKER_BOTTOM_PADDING;
    }

    private int openedExtraHeight() {
        return PICKER_PANEL_GAP + pickerPanelHeight() + PICKER_BOTTOM_PADDING;
    }

    private void setHexInputValue(String value) {
        syncingHexInput = true;
        try {
            hexInput.setValue(value);
        } finally {
            syncingHexInput = false;
        }
    }

    private void updateSV(double mouseX, double mouseY, int gradX, int gradY, int gradW, int gradH) {
        saturation = Math.max(0, Math.min(1, (float) (mouseX - gradX) / gradW));
        value = Math.max(0, Math.min(1, 1.0f - (float) (mouseY - gradY) / gradH));
        commitColor(currentRgb());
    }

    private void updateHue(double mouseX, int hueBarX, int hueBarW) {
        hue = Math.max(0, Math.min(359.9f, (float) (mouseX - hueBarX) / hueBarW * 360.0f));
        commitColor(currentRgb());
    }

    private void syncFromSettingIfIdle() {
        if (draggingHue || draggingSV || (hexInput != null && hexInput.isFocused())) {
            return;
        }

        int settingRgb = setting.get() & 0xFFFFFF;
        if (settingRgb != currentRgb()) {
            syncHsvFromRgb(settingRgb);
        }
    }

    private void syncHsvFromRgb(int rgb) {
        float[] hsv = Render2D.rgbToHsv(rgb & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    private int currentRgb() {
        return Render2D.hsvToRgb(hue, saturation, value) & 0xFFFFFF;
    }

    private void commitColor(int rgb) {
        int normalisedRgb = rgb & 0xFFFFFF;
        if ((setting.get() & 0xFFFFFF) != normalisedRgb) {
            setting.set(normalisedRgb);
        }
    }
}

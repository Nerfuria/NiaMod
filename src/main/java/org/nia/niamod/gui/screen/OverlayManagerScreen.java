package org.nia.niamod.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.component.OverlayThemeButton;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.models.render.TextOverlay;
import org.nia.niamod.render.Render2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverlayManagerScreen extends Screen {
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    private static final int MIN_OFFSET = -2000;
    private static final int MAX_OFFSET = 2000;
    private static final float SCALE_STEP = 0.05f;

    private final Screen parent;
    private final List<TextOverlay> overlays;
    private final Map<TextOverlay, OverlayState> originalStates = new HashMap<>();

    private TextOverlay draggingOverlay;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean closeHandled;
    private boolean resizingMode;

    public OverlayManagerScreen(Screen parent, List<TextOverlay> overlays) {
        super(Component.literal("Overlay Manager"));
        this.parent = parent;
        this.overlays = overlays;
        for (TextOverlay overlay : overlays) {
            originalStates.put(overlay, new OverlayState(overlay.getXOffset(), overlay.getYOffset(), overlay.getScale(), overlay.isEnabled()));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void init() {
        int buttonWidth = 98;
        int buttonY = this.height - 26;
        int centerX = this.width / 2;

        addRenderableWidget(new OverlayThemeButton(centerX - buttonWidth - 52, buttonY, buttonWidth, 20, Component.literal("Done"), input -> commitAndClose()));
        addRenderableWidget(new OverlayThemeButton(centerX - 49, buttonY, buttonWidth, 20, Component.literal("Reset All"), input -> {
            for (TextOverlay overlay : overlays) {
                overlay.setScale(1.0f);
                overlay.setXOffset(0);
                overlay.setYOffset(0);
                overlay.setEnabled(true);
            }
        }));
        addRenderableWidget(new OverlayThemeButton(centerX + 52, buttonY, buttonWidth, 20, Component.literal("Cancel"), input -> {
            restoreOriginalValues();
            closeHandled = true;
            onClose();
        }));
    }

    @Override
    public void onClose() {
        if (!closeHandled) {
            restoreOriginalValues();
        }
        draggingOverlay = null;
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        GuiTheme theme = theme();

        for (TextOverlay overlay : overlays) {
            renderPreview(context, overlay, mouseX, mouseY);
        }

        String hintA = "Left-click and drag an overlay to move it.";
        String hintB = "Hover and scroll wheel/drag left corner to resize.";
        String hintC = "Middle-click an overlay to toggle it on or off.";

        drawCenteredText(context, hintA, 8, theme.textColor());
        drawCenteredText(context, hintB, 20, theme.secondaryText());
        drawCenteredText(context, hintC, 32, theme.trinaryText());

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int button = click.input();
        if (button != 0 && button != 2) {
            return false;
        }
        double mouseX = click.x();
        double mouseY = click.y();

        for (int i = overlays.size() - 1; i >= 0; i--) {
            TextOverlay overlay = overlays.get(i);
            OverlayPreviewBounds bounds = getPreviewBounds(overlay);
            if (button == 2) {
                if (bounds.contains(mouseX, mouseY)) {
                    overlay.setEnabled(!overlay.isEnabled());
                    return true;
                }
            } else {
                if (bounds.containsCorner(mouseX, mouseY)) {
                    draggingOverlay = overlay;
                    resizingMode = true;
                    dragOffsetX = mouseX;
                    dragOffsetY = mouseY;
                    return true;
                } else if (bounds.contains(mouseX, mouseY)) {
                    draggingOverlay = overlay;
                    resizingMode = false;
                    dragOffsetX = mouseX - getAnchorScreenX(overlay);
                    dragOffsetY = mouseY - getAnchorScreenY(overlay);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent click, double deltaX, double deltaY) {
        if (draggingOverlay == null || click.input() != 0) {
            return super.mouseDragged(click, deltaX, deltaY);
        }

        if (resizingMode) {
            double delta = dragOffsetX - click.x();
            double deltaVertical = dragOffsetY - click.y();
            float scaleDelta = (float) ((delta + deltaVertical) / 100.0);
            adjustScale(draggingOverlay, scaleDelta);
            dragOffsetX = click.x();
            dragOffsetY = click.y();
            return true;
        }

        double targetAnchorX = click.x() - dragOffsetX;
        double targetAnchorY = click.y() - dragOffsetY;

        int offsetX = (int) Math.round(targetAnchorX - (this.width / 2.0));
        int offsetY = (int) Math.round(targetAnchorY - (this.height / 2.0));

        draggingOverlay.setXOffset(clamp(offsetX, MIN_OFFSET, MAX_OFFSET));
        draggingOverlay.setYOffset(clamp(offsetY, MIN_OFFSET, MAX_OFFSET));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.input() == 0) {
            draggingOverlay = null;
            resizingMode = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0.0) {
            for (int i = overlays.size() - 1; i >= 0; i--) {
                TextOverlay overlay = overlays.get(i);
                OverlayPreviewBounds bounds = getPreviewBounds(overlay);
                if (bounds.contains(mouseX, mouseY)) {
                    adjustScale(overlay, (float) (verticalAmount * SCALE_STEP));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderPreview(GuiGraphics context, TextOverlay overlay, int mouseX, int mouseY) {
        GuiTheme theme = theme();

        OverlayPreviewBounds bounds = getPreviewBounds(overlay);
        boolean hoveredCorner = bounds.containsCorner(mouseX, mouseY);
        boolean hovered = hoveredCorner || bounds.contains(mouseX, mouseY) || draggingOverlay == overlay;

        int outlineColor;
        int textColor;
        if (overlay.isEnabled()) {
            outlineColor = hovered ? Render2D.withAlpha(theme.accentColor(), 150) : Render2D.withAlpha(theme.textColor(), 100);
            textColor = theme.textColor();
        } else {
            outlineColor = hovered ? Render2D.withAlpha(theme.trinaryText(), 150) : Render2D.withAlpha(theme.trinaryText(), 100);
            textColor = theme.trinaryText();
        }

        drawOutline(context, bounds, outlineColor);

        int cornerColor = hoveredCorner ? (overlay.isEnabled() ? theme.accentColor() : theme.trinaryText())
                : (overlay.isEnabled() ? Render2D.withAlpha(theme.accentColor(), 150) : Render2D.withAlpha(theme.trinaryText(), 150));

        context.fill(bounds.left() - 2, bounds.top() - 2, bounds.left() + 4, bounds.top() + 4, cornerColor);

        float scale = clamp(overlay.getScale(), MIN_SCALE, MAX_SCALE);
        int centreX = this.width / 2;
        int centreY = this.height / 2;
        String text = previewText(overlay);

        context.pose().pushMatrix();
        context.pose().translate(centreX, centreY);
        context.pose().translate(overlay.getXOffset(), overlay.getYOffset());
        context.pose().scale(scale, scale);
        drawCenteredLine(context, text, 0, 0, textColor);
        context.pose().popMatrix();
    }

    private void drawCenteredLine(GuiGraphics context, String text, int centerX, int y, int color) {
        Component styledText = GuiStyle.styled(text);
        int width = this.font.width(styledText);
        context.drawString(this.font, styledText, centerX - width / 2, y - this.font.lineHeight / 2, color, false);
    }

    private void drawCenteredText(GuiGraphics context, String text, int y, int color) {
        Component styledText = GuiStyle.styled(text);
        int textWidth = this.font.width(styledText);
        context.drawString(this.font, styledText, (this.width - textWidth) / 2, y, color, false);
    }

    private OverlayPreviewBounds getPreviewBounds(TextOverlay overlay) {
        float scale = clamp(overlay.getScale(), MIN_SCALE, MAX_SCALE);
        String text = previewText(overlay);
        int textWidth = this.font.width(GuiStyle.styled(text));

        double halfWidth = (textWidth * scale) / 2.0;
        double halfHeight = (this.font.lineHeight * scale) / 2.0;

        int anchorX = getAnchorScreenX(overlay);
        int anchorY = getAnchorScreenY(overlay);

        int left = (int) Math.floor(anchorX - halfWidth) - 6;
        int top = (int) Math.floor(anchorY - halfHeight) - 4;
        int right = (int) Math.ceil(anchorX + halfWidth) + 6;
        int bottom = (int) Math.ceil(anchorY + halfHeight) + 4;
        return new OverlayPreviewBounds(left, top, right, bottom);
    }

    private int getAnchorScreenX(TextOverlay overlay) {
        return (int) Math.round(this.width / 2.0 + overlay.getXOffset());
    }

    private int getAnchorScreenY(TextOverlay overlay) {
        return (int) Math.round(this.height / 2.0 + overlay.getYOffset());
    }

    private GuiTheme theme() {
        return GuiStyle.configuredTheme();
    }

    private String previewText(TextOverlay overlay) {
        String text = overlay.defaultValue();
        return (text == null || text.isEmpty()) ? "Overlay" : text;
    }

    private void commitAndClose() {
        for (TextOverlay overlay : overlays) {
            overlay.setScale(clamp(overlay.getScale(), MIN_SCALE, MAX_SCALE));
            overlay.setXOffset(clamp(overlay.getXOffset(), MIN_OFFSET, MAX_OFFSET));
            overlay.setYOffset(clamp(overlay.getYOffset(), MIN_OFFSET, MAX_OFFSET));
        }
        NyahConfig.save();
        closeHandled = true;
        onClose();
    }

    private void restoreOriginalValues() {
        for (Map.Entry<TextOverlay, OverlayState> entry : originalStates.entrySet()) {
            TextOverlay overlay = entry.getKey();
            OverlayState state = entry.getValue();
            overlay.setXOffset(state.offsetX());
            overlay.setYOffset(state.offsetY());
            overlay.setScale(state.scale());
            overlay.setEnabled(state.enabled());
        }
    }

    private void adjustScale(TextOverlay overlay, float delta) {
        overlay.setScale(clamp(overlay.getScale() + delta, MIN_SCALE, MAX_SCALE));
    }

    private void drawOutline(GuiGraphics context, OverlayPreviewBounds bounds, int color) {
        context.fill(bounds.left(), bounds.top(), bounds.right(), bounds.top() + 1, color);
        context.fill(bounds.left(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
        context.fill(bounds.left(), bounds.top(), bounds.left() + 1, bounds.bottom(), color);
        context.fill(bounds.right() - 1, bounds.top(), bounds.right(), bounds.bottom(), color);
    }
}

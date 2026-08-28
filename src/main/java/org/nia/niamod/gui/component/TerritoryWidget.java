package org.nia.niamod.gui.component;

import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.eco.Resources;
import org.nia.niamod.models.eco.TerritoryNode;
import org.nia.niamod.models.eco.TerritoryResourceColors;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TerritoryWidget {
    public static final int CLIP_PADDING = 8;

    private static final int BORDER_DASH = 7;
    private static final int BORDER_GAP = 5;
    private static final int MAX_BORDER_DASHES_PER_SIDE = 6;
    private static final int LABEL_PADDING = 4;
    private static final int DISCONNECTED_COLOR = 0xFFFF3B30;
    private static final int DEFENSE_ALERT_COLOR = 0xFFFFB000;
    private static final TextureIcon CROP_ICON = texture("crop", 21, 21);
    private static final TextureIcon WOOD_ICON = texture("wood", 21, 21);
    private static final TextureIcon ORE_ICON = texture("ore", 21, 21);
    private static final TextureIcon FISH_ICON = texture("fish", 21, 21);
    private static final TextureIcon EMERALD_ICON = texture("emerald", 16, 16);
    private static final TextureIcon HQ_ICON = texture("hq", 16, 13);
    private static final double MARQUEE_PIXELS_PER_SECOND = 34.0;
    private static final long MARQUEE_PAUSE_MILLIS = 450L;

    private final TerritoryNode territory;
    private final boolean owned;
    private final Consumer<TerritoryWidget> selectionHandler;
    @Setter
    private UiRect bounds = new UiRect(0, 0, 1, 1);
    private int cachedNameMaxWidth = -1;
    private Component cachedName = Component.empty();
    private int cachedNameWidth;
    private Component fullName = Component.empty();
    private int fullNameWidth = -1;
    private boolean hoveredLastFrame;
    private long hoverStartedAt;

    public TerritoryWidget(TerritoryNode territory, boolean owned, Consumer<TerritoryWidget> selectionHandler) {
        this.territory = territory;
        this.owned = owned;
        this.selectionHandler = selectionHandler;
    }

    public static boolean intersects(UiRect rect, UiRect canvas, int padding) {
        return rect.right() >= canvas.x() - padding
                && rect.x() <= canvas.right() + padding
                && rect.bottom() >= canvas.y() - padding
                && rect.y() <= canvas.bottom() + padding;
    }

    public TerritoryNode territory() {
        return territory;
    }

    public UiRect bounds() {
        return bounds;
    }

    public boolean owned() {
        return owned;
    }

    private static TextureIcon texture(String path, int width, int height) {
        return new TextureIcon(Identifier.fromNamespaceAndPath("niamod", "textures/" + path + ".png"), width, height);
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, GuiTheme theme, long now, UiRect canvas, boolean headquarters, boolean selected, boolean disconnectedFromHeadquarters, boolean defenseAlert, boolean alwaysRenderTimers) {
        if (!intersects(bounds, canvas, CLIP_PADDING)) {
            return;
        }

        boolean hovered = contains(mouseX, mouseY);
        if (!owned) {
            drawForeignTerritory(g, font, theme, canvas);
            drawFreshHeldTime(g, font, theme, canvas, now, hovered, alwaysRenderTimers);
            return;
        }

        int fillAlpha = headquarters ? (hovered ? 92 : 74) : hovered ? 62 : 42;
        int borderAlpha = headquarters || territory.isRainbow() ? 255 : hovered ? 245 : 215;

        if (headquarters) {
            Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), fillAlpha));
        } else if (territory.isRainbow()) {
            drawRainbowFill(g, bounds, canvas, fillAlpha);
        } else {
            Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(territory.resourceColor(), fillAlpha));
        }

        boolean recentlyTaken = territory.heldUnderTenMinutes(now);
        if (!disconnectedFromHeadquarters && !selected && !recentlyTaken && !defenseAlert && headquarters) {
            drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), borderAlpha), 2);
        } else if (!disconnectedFromHeadquarters && !selected && !recentlyTaken && !defenseAlert && territory.isRainbow()) {
            drawRainbowBorder(g, bounds, canvas, borderAlpha, 2);
        } else if (!disconnectedFromHeadquarters && !selected && !recentlyTaken && !defenseAlert) {
            drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(territory.resourceColor(), borderAlpha), 2);
        }

        if (recentlyTaken) {
            drawRecentlyTakenMarkers(g, theme, canvas, now, headquarters);
        }
        if (defenseAlert) {
            drawDefenseAlert(g, canvas, now);
        }
        if (disconnectedFromHeadquarters) {
            drawDisconnectedWarning(g, canvas, now, selected);
        }
        if (selected) {
            drawSelectedBorder(g, theme, canvas, now);
        }

        if (hovered && !hoveredLastFrame) {
            hoverStartedAt = now;
        }
        hoveredLastFrame = hovered;
        drawLabels(g, font, theme, canvas, hovered, now, headquarters);
        if (headquarters) {
            drawHeadquartersMarker(g, canvas);
        }
        drawFreshHeldTime(g, font, theme, canvas, now, hovered, alwaysRenderTimers);
        if (territory.isCity()) {
            drawCityMarker(g, canvas);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (owned && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && contains(mouseX, mouseY)) {
            selectionHandler.accept(this);
            return true;
        }
        return false;
    }

    public double centerX() {
        return bounds.x() + bounds.width() / 2.0;
    }

    public double centerY() {
        return bounds.y() + bounds.height() / 2.0;
    }

    private void drawForeignTerritory(GuiGraphics g, Font font, GuiTheme theme, UiRect canvas) {
        Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(0x000000, 120));
        drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(0x000000, 235), 2);
        drawForeignResourceIcons(g, canvas);
    }

    private int disconnectedBorderAlpha(long now, boolean selected) {
        double pulse = (Math.sin(now / 135.0) + 1.0) / 2.0;
        int min = selected ? 205 : 120;
        int max = selected ? 255 : 245;
        return (int) Math.round(min + (max - min) * pulse);
    }

    private void drawSelectedBorder(GuiGraphics g, GuiTheme theme, UiRect canvas, long now) {
        int alpha = (int) Math.round(180 + Math.sin(now / 115.0) * 45);
        int color = Render2D.withAlpha(theme.textColor(), alpha);
        drawDashedBorder(g, bounds, canvas, now / 42.0, 2, (distance, length) -> color);
    }

    private void drawRecentlyTakenMarkers(GuiGraphics g, GuiTheme theme, UiRect canvas, long now, boolean headquarters) {
        int color = Render2D.withAlpha(stateColor(theme, headquarters), (int) Math.round(120 + Math.sin(now / 420.0) * 45));
        int inset = bounds.width() > 9 && bounds.height() > 9 ? 2 : 0;
        UiRect markerBounds = inset(bounds, inset);
        int corner = Math.max(4, Math.min(16, Math.min(markerBounds.width(), markerBounds.height()) / 3));
        int thickness = markerBounds.width() > 14 && markerBounds.height() > 14 ? 2 : 1;

        drawCornerMarker(g, canvas, markerBounds.x(), markerBounds.y(), corner, thickness, true, true, color);
        drawCornerMarker(g, canvas, markerBounds.right(), markerBounds.y(), corner, thickness, false, true, color);
        drawCornerMarker(g, canvas, markerBounds.x(), markerBounds.bottom(), corner, thickness, true, false, color);
        drawCornerMarker(g, canvas, markerBounds.right(), markerBounds.bottom(), corner, thickness, false, false, color);
    }

    private void drawDefenseAlert(GuiGraphics g, UiRect canvas, long now) {
        double pulse = (Math.sin(now / 65.0) + 1.0) / 2.0;
        int fillAlpha = (int) Math.round(38 + pulse * 82);
        int borderAlpha = (int) Math.round(168 + pulse * 87);
        Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(DEFENSE_ALERT_COLOR, fillAlpha));
        drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(DEFENSE_ALERT_COLOR, borderAlpha), bounds.width() > 18 && bounds.height() > 18 ? 3 : 2);
    }

    private void drawDisconnectedWarning(GuiGraphics g, UiRect canvas, long now, boolean selected) {
        int color = Render2D.withAlpha(DISCONNECTED_COLOR, disconnectedBorderAlpha(now, selected));
        drawSolidBorder(g, bounds, canvas, color, selected ? 3 : 2);
    }

    private int stateColor(GuiTheme theme, boolean headquarters) {
        if (headquarters) {
            return TerritoryResourceColors.headquarterColor();
        }
        if (territory.isRainbow()) {
            return theme.accentColor();
        }
        return territory.resourceColor();
    }

    private void drawCornerMarker(GuiGraphics g, UiRect canvas, int cornerX, int cornerY, int length, int thickness, boolean rightward, boolean downward, int color) {
        int horizontalLeft = rightward ? cornerX : cornerX - length;
        int horizontalRight = rightward ? cornerX + length : cornerX;
        int horizontalTop = downward ? cornerY : cornerY - thickness;
        int horizontalBottom = downward ? cornerY + thickness : cornerY;
        Render2D.clippedRect(g, horizontalLeft, horizontalTop, horizontalRight, horizontalBottom, canvas, color);

        int verticalLeft = rightward ? cornerX : cornerX - thickness;
        int verticalRight = rightward ? cornerX + thickness : cornerX;
        int verticalTop = downward ? cornerY : cornerY - length;
        int verticalBottom = downward ? cornerY + length : cornerY;
        Render2D.clippedRect(g, verticalLeft, verticalTop, verticalRight, verticalBottom, canvas, color);
    }

    private void drawForeignResourceIcons(GuiGraphics g, UiRect canvas) {
        UiRect clip = labelClip(canvas);
        if (clip == null) {
            return;
        }

        List<TextureIcon> icons = resourceIcons();
        int iconSize = iconSize(clip.width(), clip.height(), icons.size(), 16);
        if (iconSize <= 0) {
            return;
        }

        int totalW = iconRowWidth(icons.size(), iconSize);
        int iconX = clip.x() + (clip.width() - totalW) / 2;
        int iconY = clip.y() + (clip.height() - iconSize) / 2;
        drawIconRow(g, icons, iconX, iconY, iconSize, canvas);
    }

    private void drawLabels(GuiGraphics g, Font font, GuiTheme theme, UiRect canvas, boolean hovered, long now, boolean headquarters) {
        UiRect clip = labelClip(canvas);
        if (clip == null) {
            return;
        }

        int nameWidth = fullNameWidth(font);
        int labelW = clip.width();
        int labelX = clip.x();
        List<TextureIcon> icons = resourceIcons();
        int iconSize = iconSize(labelW, clip.height() - font.lineHeight - 2, icons.size(), 15);
        int blockH = font.lineHeight + (iconSize > 0 ? iconSize + 2 : 0);
        int nameY = bounds.centerY() - blockH / 2;

        if (hovered && nameWidth > labelW) {
            drawMarqueeName(g, font, labelX, nameY, labelW, nameWidth, theme.textColor(), now, clip);
        } else {
            Component nameText = nameText(font, labelW);
            int nameX = labelX + (labelW - cachedNameWidth) / 2;
            drawClippedText(g, font, nameText, nameX, nameY, theme.textColor(), clip);
        }

        if (iconSize > 0) {
            int totalW = iconRowWidth(icons.size(), iconSize);
            int iconX = labelX + (labelW - totalW) / 2;
            int iconY = nameY + font.lineHeight + 2;
            drawIconRow(g, icons, iconX, iconY, iconSize, canvas);
        }
    }

    private void drawHeadquartersMarker(GuiGraphics g, UiRect canvas) {
        UiRect clip = intersection(bounds, canvas);
        if (clip == null) {
            return;
        }

        int gold = TerritoryResourceColors.headquarterColor();
        if (bounds.width() < 16 || bounds.height() < 12) {
            drawHeadquartersDot(g, canvas, gold);
            return;
        }

        int maxIconW = Math.max(7, bounds.width() / 3);
        int maxIconH = Math.max(6, bounds.height() / 3);
        int iconW = Math.min(14, maxIconW);
        int iconH = Math.max(6, Math.round(iconW * HQ_ICON.height() / (float) HQ_ICON.width()));
        if (iconH > maxIconH) {
            iconH = maxIconH;
            iconW = Math.max(7, Math.round(iconH * HQ_ICON.width() / (float) HQ_ICON.height()));
        }

        int iconX = bounds.x() + 3;
        int iconY = bounds.y() + 3;
        drawTexture(g, HQ_ICON, iconX, iconY, iconW, iconH, canvas);
    }

    private void drawFreshHeldTime(GuiGraphics g, Font font, GuiTheme theme, UiRect canvas, long now, boolean hovered, boolean alwaysRenderTimers) {
        if (!territory.heldUnderTenMinutes(now) || (!alwaysRenderTimers && !hovered)) {
            return;
        }

        UiRect clip = intersection(bounds, canvas);
        if (clip == null || bounds.height() < 12 || bounds.width() < 18) {
            return;
        }

        int maxTextW = Math.max(1, bounds.width() - 12);
        String time = formatHeldTime(now);
        String label = timerLabel(font, maxTextW, time);
        Component text = GuiStyle.styled(label);
        int textWidth = font.width(text);
        int badgeW = Math.min(bounds.width() - 4, textWidth + 8);
        int badgeH = Math.min(bounds.height() - 4, font.lineHeight + 3);
        if (badgeW <= 4 || badgeH <= 4) {
            return;
        }

        int badgeX = bounds.x() + (bounds.width() - badgeW) / 2;
        int badgeY = bounds.bottom() - badgeH - 3;
        UiRect badge = new UiRect(badgeX, badgeY, badgeW, badgeH);
        Render2D.clippedRect(g, badge.x(), badge.y(), badge.right(), badge.bottom(), canvas, Render2D.withAlpha(0x000000, 185));
        drawSolidBorder(g, badge, canvas, Render2D.withAlpha(theme.accentColor(), 140), 1);

        int textX = badge.x() + (badge.width() - textWidth) / 2;
        int textY = badge.y() + (badge.height() - font.lineHeight) / 2 + 1;
        drawClippedText(g, font, text, textX, textY, theme.textColor(), clip);
    }

    private String formatHeldTime(long now) {
        long heldMillis = Math.max(0L, now - territory.acquiredMillis());
        long totalSeconds = heldMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
    }

    private String timerLabel(Font font, int maxWidth, String time) {
        String full = "Cooldown: " + time;
        if (font.width(GuiStyle.styled(full)) <= maxWidth) {
            return full;
        }

        String shortLabel = "CD: " + time;
        if (font.width(GuiStyle.styled(shortLabel)) <= maxWidth) {
            return shortLabel;
        }
        return time;
    }

    private void drawHeadquartersDot(GuiGraphics g, UiRect canvas, int color) {
        int size = Math.min(8, Math.max(3, Math.min(bounds.width(), bounds.height()) / 2));
        int x = bounds.centerX() - size / 2;
        int y = bounds.centerY() - size / 2;
        Render2D.clippedRect(g, x, y, x + size, y + size, canvas, Render2D.withAlpha(color, 245));
    }

    private void drawCityMarker(GuiGraphics g, UiRect canvas) {
        UiRect clip = intersection(bounds, canvas);
        if (clip == null) {
            return;
        }

        int size = Math.min(16, Math.max(7, Math.min(bounds.width(), bounds.height()) / 3));
        int iconX = bounds.width() >= 18 ? bounds.right() - size - 2 : bounds.centerX() - size / 2;
        int iconY = bounds.height() >= 18 ? bounds.y() + 2 : bounds.centerY() - size / 2;
        drawTexture(g, EMERALD_ICON, iconX, iconY, size, size, canvas);
    }

    private List<TextureIcon> resourceIcons() {
        Resources resources = territory.resources();
        List<TextureIcon> icons = new ArrayList<>(4);
        addResourceIcons(icons, ORE_ICON, resources.ore());
        addResourceIcons(icons, CROP_ICON, resources.crops());
        addResourceIcons(icons, WOOD_ICON, resources.wood());
        addResourceIcons(icons, FISH_ICON, resources.fish());
        return icons;
    }

    private void addResourceIcons(List<TextureIcon> icons, TextureIcon icon, long amount) {
        if (amount <= 0L) {
            return;
        }

        int count = Math.max(1, (int) Math.round(amount / 3600.0));
        for (int i = 0; i < count; i++) {
            icons.add(icon);
        }
    }

    private int iconSize(int availableWidth, int availableHeight, int iconCount, int preferredSize) {
        if (iconCount <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            return 0;
        }

        int maxByWidth = (availableWidth - Math.max(0, iconCount - 1) * 2) / iconCount;
        int size = Math.min(preferredSize, Math.min(availableHeight, maxByWidth));
        return size < 4 ? 0 : size;
    }

    private int iconRowWidth(int iconCount, int iconSize) {
        if (iconCount <= 0 || iconSize <= 0) {
            return 0;
        }
        return iconCount * iconSize + (iconCount - 1) * 2;
    }

    private void drawIconRow(GuiGraphics g, List<TextureIcon> icons, int x, int y, int iconSize, UiRect canvas) {
        for (int i = 0; i < icons.size(); i++) {
            drawTexture(g, icons.get(i), x + i * (iconSize + 2), y, iconSize, iconSize, canvas);
        }
    }

    private void drawTexture(GuiGraphics g, TextureIcon icon, int x, int y, int width, int height, UiRect canvas) {
        UiRect target = new UiRect(x, y, width, height);
        UiRect clip = intersection(target, canvas);
        if (clip == null || width <= 0 || height <= 0) {
            return;
        }

        g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
        g.pose().pushMatrix();
        try {
            g.pose().translate((float) x, (float) y);
            g.pose().scale(width / (float) icon.width(), height / (float) icon.height());
            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    icon.texture(),
                    0,
                    0,
                    0,
                    0,
                    icon.width(),
                    icon.height(),
                    icon.width(),
                    icon.height()
            );
        } finally {
            g.pose().popMatrix();
            g.disableScissor();
        }
    }

    private void drawMarqueeName(GuiGraphics g, Font font, int labelX, int nameY, int labelW, int nameWidth, int textColor, long now, UiRect clip) {
        double offset = calcOffset(labelW, nameWidth, now);

        int clipTop = Math.max(clip.y(), nameY - 1);
        int clipBottom = Math.min(clip.bottom(), nameY + font.lineHeight + 1);
        if (clipBottom <= clipTop) {
            return;
        }
        g.enableScissor(clip.x(), clipTop, clip.right(), clipBottom);
        try {
            g.drawString(font, fullNameText(font), labelX + (int) Math.round(offset), nameY, textColor, false);
        } finally {
            g.disableScissor();
        }
    }

    private double calcOffset(int labelW, int nameWidth, long now) {
        double scrollRange = Math.max(1.0, nameWidth - labelW);
        double scrollMillis = scrollRange / MARQUEE_PIXELS_PER_SECOND * 1000.0;
        double cycle = MARQUEE_PAUSE_MILLIS * 2.0 + scrollMillis;
        double elapsed = positiveModulo(now - hoverStartedAt, cycle);
        double offset;
        if (elapsed < MARQUEE_PAUSE_MILLIS) {
            offset = 0.0;
        } else if (elapsed < MARQUEE_PAUSE_MILLIS + scrollMillis) {
            offset = -scrollRange * ((elapsed - MARQUEE_PAUSE_MILLIS) / scrollMillis);
        } else {
            offset = -scrollRange;
        }
        return offset;
    }

    private void drawClippedText(GuiGraphics g, Font font, Component text, int x, int y, int color, UiRect clip) {
        int clipTop = Math.max(clip.y(), y - 1);
        int clipBottom = Math.min(clip.bottom(), y + font.lineHeight + 1);
        if (clipBottom <= clipTop) {
            return;
        }

        g.enableScissor(clip.x(), clipTop, clip.right(), clipBottom);
        try {
            g.drawString(font, text, x, y, color, false);
        } finally {
            g.disableScissor();
        }
    }

    private UiRect labelClip(UiRect canvas) {
        int inset = bounds.width() > LABEL_PADDING * 2 ? LABEL_PADDING : 0;
        int left = Math.max(bounds.x() + inset, canvas.x());
        int right = Math.min(bounds.right() - inset, canvas.right());
        int top = Math.max(bounds.y(), canvas.y());
        int bottom = Math.min(bounds.bottom(), canvas.bottom());
        if (right <= left || bottom <= top) {
            return null;
        }
        return new UiRect(left, top, right - left, bottom - top);
    }

    private Component nameText(Font font, int maxTextWidth) {
        if (maxTextWidth != cachedNameMaxWidth) {
            cachedName = GuiStyle.styled(fit(font, territory.name(), maxTextWidth));
            cachedNameWidth = font.width(cachedName);
            cachedNameMaxWidth = maxTextWidth;
        }
        return cachedName;
    }

    private Component fullNameText(Font font) {
        if (fullNameWidth < 0) {
            fullName = GuiStyle.styled(territory.name());
            fullNameWidth = font.width(fullName);
        }
        return fullName;
    }

    private int fullNameWidth(Font font) {
        if (fullNameWidth < 0) {
            fullNameText(font);
        }
        return fullNameWidth;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= bounds.x() && mouseX <= bounds.right() && mouseY >= bounds.y() && mouseY <= bounds.bottom();
    }

    private String fit(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.width(GuiStyle.styled(text)) <= maxWidth) {
            return text;
        }

        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(GuiStyle.styled(trimmed + "...")) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? "" : trimmed + "...";
    }

    private void drawSolidBorder(GuiGraphics g, UiRect bounds, UiRect canvas, int color, int thickness) {
        if (bounds.width() <= 0 || bounds.height() <= 0 || thickness <= 0) {
            return;
        }
        Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.y() + thickness, canvas, color);
        Render2D.clippedRect(g, bounds.x(), bounds.bottom() - thickness, bounds.right(), bounds.bottom(), canvas, color);
        Render2D.clippedRect(g, bounds.x(), bounds.y() + thickness, bounds.x() + thickness, bounds.bottom() - thickness, canvas, color);
        Render2D.clippedRect(g, bounds.right() - thickness, bounds.y() + thickness, bounds.right(), bounds.bottom() - thickness, canvas, color);
    }

    private UiRect inset(UiRect rect, int amount) {
        int inset = Math.max(0, amount);
        int width = Math.max(1, rect.width() - inset * 2);
        int height = Math.max(1, rect.height() - inset * 2);
        return new UiRect(rect.x() + inset, rect.y() + inset, width, height);
    }

    private void drawRainbowBorder(GuiGraphics g, UiRect bounds, UiRect canvas, int alpha, int thickness) {
        if (intersection(bounds, canvas) == null) {
            return;
        }

        drawRainbowRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.y() + thickness, bounds, canvas, alpha);
        drawRainbowRect(g, bounds.x(), bounds.bottom() - thickness, bounds.right(), bounds.bottom(), bounds, canvas, alpha);
        Render2D.clippedRect(g, bounds.x(), bounds.y() + thickness, bounds.x() + thickness, bounds.bottom() - thickness, canvas, TerritoryResourceColors.rainbowColor(0.0, alpha));
        Render2D.clippedRect(g, bounds.right() - thickness, bounds.y() + thickness, bounds.right(), bounds.bottom() - thickness, canvas, TerritoryResourceColors.rainbowColor(1.0, alpha));
    }

    private void drawDashedBorder(GuiGraphics g, UiRect bounds, UiRect canvas, double phase, int thickness, Render2D.DashColorProvider colorProvider) {
        Render2D.dashedLineClipped(g, bounds.x(), bounds.y(), bounds.right(), bounds.y(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.right(), bounds.y(), bounds.right(), bounds.bottom(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.right(), bounds.bottom(), bounds.x(), bounds.bottom(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.x(), bounds.bottom(), bounds.x(), bounds.y(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
    }

    private void drawRainbowFill(GuiGraphics g, UiRect bounds, UiRect canvas, int alpha) {
        drawRainbowRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), bounds, canvas, alpha);
    }

    private void drawRainbowRect(GuiGraphics g, int left, int top, int right, int bottom, UiRect gradientBounds, UiRect canvas, int alpha) {
        int x1 = Math.max(left, canvas.x());
        int y1 = Math.max(top, canvas.y());
        int x2 = Math.min(right, canvas.right());
        int y2 = Math.min(bottom, canvas.bottom());
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        Render2D.horizontalMultiGradient(
                g,
                x1,
                y1,
                x2,
                y2,
                gradientBounds.x(),
                Math.max(1, gradientBounds.width()),
                TerritoryResourceColors.rainbowStops(alpha)
        );
    }

    private UiRect intersection(UiRect a, UiRect b) {
        int x1 = Math.max(a.x(), b.x());
        int y1 = Math.max(a.y(), b.y());
        int x2 = Math.min(a.right(), b.right());
        int y2 = Math.min(a.bottom(), b.bottom());
        if (x2 <= x1 || y2 <= y1) {
            return null;
        }
        return new UiRect(x1, y1, x2 - x1, y2 - y1);
    }

    private double positiveModulo(double value, double modulo) {
        double result = value % modulo;
        return result < 0 ? result + modulo : result;
    }

    private record TextureIcon(Identifier texture, int width, int height) {
    }

}
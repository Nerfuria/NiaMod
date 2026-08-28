package org.nia.niamod.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.eco.ResourceKind;
import org.nia.niamod.models.eco.Resources;
import org.nia.niamod.models.eco.TerritoryDetails;
import org.nia.niamod.models.eco.TerritoryDetails.DamageRange;
import org.nia.niamod.models.eco.TerritoryNode;
import org.nia.niamod.models.eco.TerritoryResourceColors;
import org.nia.niamod.models.eco.TerritoryResourceStore;
import org.nia.niamod.models.eco.TerritoryUpgrade;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class TerritoryDetailPanel {
    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_WIDTH = 318;
    private static final int PANEL_MAX_HEIGHT = 560;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_SIZE = 16;
    private static final int SCROLL_STEP = 26;
    private static final Runnable NO_OP = () -> {
    };
    private static final BiConsumer<TerritoryUpgrade, Integer> NO_UPGRADE_ACTION = (upgrade, delta) -> {
    };
    private static final IntConsumer NO_INT_ACTION = value -> {
    };

    private final List<DetailButton> buttons = new ArrayList<>();
    private boolean dragging;
    private boolean positioned;
    private int x;
    private int y;
    private int scrollOffset;
    private double dragOffsetX;
    private double dragOffsetY;

    public void render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            GuiTheme theme,
            TerritoryDetails data,
            int screenWidth,
            int screenHeight,
            Actions actions
    ) {
        buttons.clear();
        if (data == null || data.territory() == null) {
            return;
        }
        actions = actions == null ? Actions.EMPTY : actions;

        TerritoryNode territory = data.territory();
        Map<TerritoryUpgrade, Integer> upgradeLevels = data.upgradeLevels();
        TerritoryResourceStore store = data.resourceStore();
        Resources perHourResources = data.producedResources();
        UiRect panel = panelBounds(screenWidth, screenHeight, perHourResources);
        if (panel.width() <= 0 || panel.height() <= 0) {
            return;
        }
        UiRect body = bodyBounds(panel);
        int bodyContentHeight = bodyContentHeight(perHourResources);
        int maxScroll = maxScroll(perHourResources, body.height());
        clampScroll(maxScroll);

        Render2D.dropShadow(g, panel, 5, 0x66000000, 8);
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 5, theme.background(), panelBorderColor(theme));
        Render2D.horizontalGradient(
                g,
                panel.x() + 1,
                panel.y() + 1,
                panel.width() - 2,
                HEADER_HEIGHT - 1,
                headerFillColor(theme),
                Render2D.withAlpha(theme.secondary(), 20)
        );

        int contentX = panel.x() + 10;
        int contentW = panel.width() - 20;
        int right = contentX + contentW;
        int rowY = panel.y() + 7;

        UiRect closeButton = new UiRect(panel.right() - 27, panel.y() + 5, 18, 18);
        drawPlainButton(g, font, closeButton, "X", closeButton.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(closeButton, actions.close(), false));

        drawFittedString(g, font, territory.name(), contentX, rowY + 2, Math.max(1, closeButton.x() - contentX - 8), theme.textColor());
        rowY = body.y() + 7 - scrollOffset;

        g.enableScissor(body.x(), body.y(), body.right(), body.bottom());
        rowY = drawResourceSummary(g, font, perHourResources, contentX, rowY, contentW, theme);
        rowY += 4;
        drawDivider(g, panel, rowY, theme);
        rowY += 8;

        int guildTagW = Math.min(contentW, Math.max(36, font.width(data.guildName()) + 8));
        g.fill(contentX, rowY - 2, contentX + guildTagW, rowY + font.lineHeight + 2, Render2D.withAlpha(theme.accentColor(), 230));
        drawFittedString(g, font, data.guildName(), contentX + 4, rowY, guildTagW - 8, theme.textColor());
        rowY += 18;

        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Time held: " + formatHeldTime(territory.acquiredMillis()), theme.secondaryText());
        rowY = drawStoredResources(g, font, contentX, rowY, contentW, store, theme);
        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Connections: " + data.ownedConnections() + "/" + data.totalConnections(), theme.secondaryText());
        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "External: " + data.externalConnections(), theme.secondaryText());
        if (!data.loadoutName().isBlank()) {
            rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Loadout: " + data.loadoutName(), theme.secondaryText());
        }
        rowY += 3;
        rowY = drawTaxRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, data.taxPercent(), actions.taxAdjusted(), actions.globalTaxSet());
        rowY = drawSplitButtons(
                g,
                font,
                contentX,
                right,
                rowY,
                mouseX,
                mouseY,
                theme,
                data.headquarters() ? "HQ Set" : "Set HQ",
                actions.headquartersSet(),
                data.bordersOpen() ? "Open" : "Closed",
                actions.bordersToggled(),
                actions.globalBordersToggled()
        );
        rowY = drawFullButton(g, font, contentX, right, rowY, mouseX, mouseY, theme, "Route: " + data.routeLabel(), actions.routeToggled(), actions.globalRouteToggled());
        rowY = drawFullButton(g, font, contentX, right, rowY, mouseX, mouseY, theme, "Loadouts", actions.loadoutsOpened());
        rowY += 2;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;

        rowY = drawSectionTitle(g, font, contentX, right, rowY, "Combat", theme);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.combat()) {
            rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, upgrade, combatLabel(upgrade, data), upgradeLevels, actions.upgradeAdjusted());
        }
        rowY += 8;

        rowY = drawSectionTitle(g, font, contentX, right, rowY, "Economy", theme);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.economy()) {
            rowY = drawUpgradeRow(
                    g,
                    font,
                    contentX,
                    right,
                    rowY,
                    mouseX,
                    mouseY,
                    theme,
                    upgrade,
                    upgrade.label(),
                    upgradeLevels,
                    actions.upgradeAdjusted()
            );
        }

        rowY += 8;
        rowY = drawSectionTitle(g, font, contentX, right, rowY, "Utility", theme);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.utility()) {
            rowY = drawUpgradeRow(
                    g,
                    font,
                    contentX,
                    right,
                    rowY,
                    mouseX,
                    mouseY,
                    theme,
                    upgrade,
                    upgrade.label(),
                    upgradeLevels,
                    actions.upgradeAdjusted()
            );
        }
        g.disableScissor();

        renderScrollbar(g, body, theme, bodyContentHeight, maxScroll);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight, Resources producedResources) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
        if (!panel.contains(mouseX, mouseY)) {
            return false;
        }
        UiRect body = bodyBounds(panel);

        for (DetailButton detailButton : buttons) {
            if (detailButton.clipToBody() && !body.contains(mouseX, mouseY)) {
                continue;
            }
            if (detailButton.bounds().contains(mouseX, mouseY)) {
                detailButton.onClick().run();
                return true;
            }
        }
        if (headerBounds(panel).contains(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - panel.x();
            dragOffsetY = mouseY - panel.y();
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount, int screenWidth, int screenHeight, Resources producedResources) {
        if (verticalAmount == 0.0) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
        if (!panel.contains(mouseX, mouseY)) {
            return false;
        }

        int maxScroll = maxScroll(resources, bodyBounds(panel).height());
        if (maxScroll <= 0) {
            return false;
        }

        int delta = (int) Math.round(-verticalAmount * SCROLL_STEP);
        if (delta == 0) {
            delta = verticalAmount < 0.0 ? SCROLL_STEP : -SCROLL_STEP;
        }

        int previous = scrollOffset;
        scrollOffset = clampInt(scrollOffset + delta, 0, maxScroll);
        return scrollOffset != previous;
    }

    public boolean contains(double mouseX, double mouseY, int screenWidth, int screenHeight, Resources producedResources) {
        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        return panelBounds(screenWidth, screenHeight, resources).contains(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight, Resources producedResources) {
        if (!dragging) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
        x = clampInt((int) Math.round(mouseX - dragOffsetX), PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - panel.width() - PANEL_MARGIN));
        y = clampInt((int) Math.round(mouseY - dragOffsetY), PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panel.height() - PANEL_MARGIN));
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    public void stopDragging() {
        dragging = false;
    }

    public void resetScroll() {
        scrollOffset = 0;
    }

    public void ensurePosition(int screenWidth, int screenHeight, Resources resources) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = panelHeight(screenHeight, resources);
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
    }

    private UiRect panelBounds(int screenWidth, int screenHeight, Resources resources) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = panelHeight(screenHeight, resources);
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
        return new UiRect(x, y, panelW, panelH);
    }

    private int panelHeight(int screenHeight, Resources resources) {
        int available = Math.max(HEADER_HEIGHT + 1, screenHeight - PANEL_MARGIN * 2);
        int maxBodyHeight = Math.max(1, Math.min(PANEL_MAX_HEIGHT, available) - HEADER_HEIGHT);
        return HEADER_HEIGHT + Math.min(bodyContentHeight(resources), maxBodyHeight);
    }

    private void ensurePosition(int screenWidth, int screenHeight, int panelW, int panelH) {
        if (!positioned) {
            x = screenWidth - panelW - PANEL_MARGIN;
            y = PANEL_MARGIN;
            positioned = true;
        }

        x = clampInt(x, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - panelW - PANEL_MARGIN));
        y = clampInt(y, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panelH - PANEL_MARGIN));
    }

    private UiRect headerBounds(UiRect panel) {
        return new UiRect(panel.x(), panel.y(), panel.width(), HEADER_HEIGHT);
    }

    private UiRect bodyBounds(UiRect panel) {
        return new UiRect(panel.x() + 1, panel.y() + HEADER_HEIGHT, Math.max(1, panel.width() - 2), Math.max(1, panel.height() - HEADER_HEIGHT));
    }

    private int bodyContentHeight(Resources resources) {
        return 7
                + resourceSummaryHeight(resources)
                + 4
                + 8
                + 18
                + 12
                + 60
                + 12
                + 12
                + 12
                + 3
                + ROW_HEIGHT * 4
                + 2
                + 9
                + 18
                + ROW_HEIGHT * TerritoryUpgrade.combat().size()
                + 8
                + 18
                + ROW_HEIGHT * TerritoryUpgrade.economy().size()
                + 8
                + 18
                + ROW_HEIGHT * TerritoryUpgrade.utility().size()
                + 10;
    }

    private int maxScroll(Resources resources, int bodyHeight) {
        return Math.max(0, bodyContentHeight(resources) - Math.max(1, bodyHeight));
    }

    private void clampScroll(int maxScroll) {
        scrollOffset = clampInt(scrollOffset, 0, maxScroll);
    }

    private int resourceSummaryHeight(Resources resources) {
        int rows = 0;
        if (resources != null) {
            for (ResourceKind resource : ResourceKind.DISPLAY_ORDER) {
                if (resources.amount(resource) > 0) {
                    rows++;
                }
            }
        }
        return Math.max(1, rows) * 12;
    }

    private int drawResourceSummary(GuiGraphics g, Font font, Resources resources, int x, int y, int maxWidth, GuiTheme theme) {
        int startY = y;
        int cardHeight = resourceSummaryHeight(resources) + 6;
        Render2D.shaderRoundedSurface(
                g,
                x - 4,
                y - 3,
                maxWidth + 8,
                cardHeight,
                4,
                Render2D.withAlpha(theme.secondary(), 70),
                Render2D.withAlpha(theme.accentColor(), 48)
        );
        for (ResourceKind resource : ResourceKind.DISPLAY_ORDER) {
            y = drawResourceRate(g, font, x, y, maxWidth, resourceColor(resource), resources.amount(resource), resource.label().toLowerCase(Locale.ROOT), theme);
        }
        if (y == startY) {
            drawFittedString(g, font, "No resource production", x, y, maxWidth, theme.trinaryText());
            y += 12;
        }
        return y;
    }

    private int resourceColor(ResourceKind resource) {
        return resource == ResourceKind.EMERALDS ? TerritoryResourceColors.cityColor() : TerritoryResourceColors.configuredColor(resource);
    }

    private int drawResourceRate(GuiGraphics g, Font font, int x, int y, int maxWidth, int color, long amount, String label, GuiTheme theme) {
        if (amount <= 0) {
            return y;
        }
        g.fill(x + 2, y + 3, x + 10, y + 9, color);
        drawFittedString(g, font, "+" + formatNumber(amount) + " " + label + " per hour", x + 15, y, Math.max(1, maxWidth - 15), theme.secondaryText());
        return y + 12;
    }

    private int drawInfoLine(GuiGraphics g, Font font, int x, int y, int maxWidth, String text, int color) {
        drawFittedString(g, font, text, x, y, maxWidth, color);
        return y + 12;
    }

    private int drawSectionTitle(GuiGraphics g, Font font, int x, int right, int y, String title, GuiTheme theme) {
        int titleW = Math.min(right - x, Math.max(58, font.width(GuiStyle.styled(title)) + 14));
        Render2D.shaderRoundedSurface(
                g,
                x - 3,
                y - 3,
                titleW,
                15,
                4,
                Render2D.withAlpha(theme.accentColor(), 42),
                Render2D.withAlpha(theme.accentColor(), 82)
        );
        drawFittedString(g, font, title, x + 4, y, titleW - 8, theme.textColor());
        return y + 18;
    }

    private int drawTaxRow(GuiGraphics g, Font font, int x, int right, int y, int mouseX, int mouseY, GuiTheme theme, int taxPercent, IntConsumer taxAdjusted, IntConsumer globalTaxSet) {
        drawFittedString(g, font, "Tax", x, y + 3, Math.max(1, right - x - 78), theme.textColor());

        int plusX = right - BUTTON_SIZE;
        int valueX = plusX - 31;
        int minusX = valueX - BUTTON_SIZE - 6;
        UiRect minus = new UiRect(minusX, y + 1, BUTTON_SIZE, BUTTON_SIZE);
        UiRect value = new UiRect(valueX, y + 1, 28, BUTTON_SIZE);
        UiRect plus = new UiRect(plusX, y + 1, BUTTON_SIZE, BUTTON_SIZE);

        drawPlainButton(g, font, minus, "-", minus.contains(mouseX, mouseY), theme);
        drawValuePill(g, font, value, Math.max(0, Math.min(70, taxPercent)) + "%", theme);
        drawPlainButton(g, font, plus, "+", plus.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(minus, () -> {
            if (shiftDown()) {
                globalTaxSet.accept(clampInt(taxPercent - 5, 0, 70));
            } else {
                taxAdjusted.accept(-5);
            }
        }, true));
        buttons.add(new DetailButton(plus, () -> {
            if (shiftDown()) {
                globalTaxSet.accept(clampInt(taxPercent + 5, 0, 70));
            } else {
                taxAdjusted.accept(5);
            }
        }, true));
        return y + ROW_HEIGHT;
    }

    private int drawSplitButtons(
            GuiGraphics g,
            Font font,
            int x,
            int right,
            int y,
            int mouseX,
            int mouseY,
            GuiTheme theme,
            String leftLabel,
            Runnable leftClick,
            String rightLabel,
            Runnable rightClick,
            Runnable rightShiftClick
    ) {
        int gap = 6;
        int width = Math.max(1, right - x);
        int buttonW = (width - gap) / 2;
        UiRect left = new UiRect(x, y + 2, buttonW, BUTTON_SIZE);
        UiRect rightButton = new UiRect(x + buttonW + gap, y + 2, width - buttonW - gap, BUTTON_SIZE);

        drawPlainButton(g, font, left, leftLabel, left.contains(mouseX, mouseY), theme);
        drawPlainButton(g, font, rightButton, rightLabel, rightButton.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(left, leftClick, true));
        buttons.add(new DetailButton(rightButton, () -> shiftAware(rightClick, rightShiftClick), true));
        return y + ROW_HEIGHT;
    }

    private int drawFullButton(GuiGraphics g, Font font, int x, int right, int y, int mouseX, int mouseY, GuiTheme theme, String label, Runnable onClick) {
        return drawFullButton(g, font, x, right, y, mouseX, mouseY, theme, label, onClick, NO_OP);
    }

    private int drawFullButton(GuiGraphics g, Font font, int x, int right, int y, int mouseX, int mouseY, GuiTheme theme, String label, Runnable onClick, Runnable shiftClick) {
        UiRect button = new UiRect(x, y + 2, Math.max(1, right - x), BUTTON_SIZE);
        drawPlainButton(g, font, button, label, button.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(button, () -> shiftAware(onClick, shiftClick), true));
        return y + ROW_HEIGHT;
    }

    private int drawUpgradeRow(
            GuiGraphics g,
            Font font,
            int x,
            int right,
            int y,
            int mouseX,
            int mouseY,
            GuiTheme theme,
            TerritoryUpgrade upgrade,
            String label,
            Map<TerritoryUpgrade, Integer> upgradeLevels,
            BiConsumer<TerritoryUpgrade, Integer> upgradeAdjusted
    ) {
        int controlsW = 84;
        int costW = 48;
        UiRect rowBounds = new UiRect(x - 4, y, Math.max(1, right - x + 8), ROW_HEIGHT - 1);
        boolean hovered = rowBounds.contains(mouseX, mouseY);
        long upgradeCost = upgrade.cost(upgradeLevel(upgradeLevels, upgrade));
        boolean showCost = upgradeCost > 0L;
        int labelW = showCost ? right - x - controlsW - costW - 12 : right - x - controlsW - 8;
        int resourceColor = TerritoryResourceColors.configuredColor(upgrade.costResource());
        if (hovered) {
            Render2D.shaderRoundedSurface(
                    g,
                    rowBounds.x(),
                    rowBounds.y(),
                    rowBounds.width(),
                    rowBounds.height(),
                    3,
                    Render2D.withAlpha(theme.secondary(), 92),
                    Render2D.withAlpha(resourceColor, 76)
            );
        }
        g.fill(x - 3, y + 2, x - 1, y + ROW_HEIGHT - 7, Render2D.withAlpha(resourceColor, hovered ? 230 : 150));
        drawFittedString(g, font, label, x, y + 3, Math.max(1, labelW), theme.textColor());

        int plusX = right - BUTTON_SIZE;
        int valueX = plusX - 37;
        int minusX = valueX - BUTTON_SIZE - 6;
        UiRect cost = new UiRect(minusX - costW - 6, y + 1, costW, BUTTON_SIZE);
        UiRect minus = new UiRect(minusX, y + 1, BUTTON_SIZE, BUTTON_SIZE);
        UiRect plus = new UiRect(plusX, y + 1, BUTTON_SIZE, BUTTON_SIZE);
        UiRect value = new UiRect(valueX, y + 1, 34, BUTTON_SIZE);
        if (showCost) {
            drawCostPill(g, font, cost, upgradeCost, upgrade.costResource(), theme);
        }
        drawPlainButton(g, font, minus, "-", minus.contains(mouseX, mouseY), theme);
        drawValuePill(g, font, value, upgradeLevel(upgradeLevels, upgrade) + "/" + upgrade.maxLevel(), theme);
        drawPlainButton(g, font, plus, "+", plus.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(minus, () -> upgradeAdjusted.accept(upgrade, -1), true));
        buttons.add(new DetailButton(plus, () -> upgradeAdjusted.accept(upgrade, 1), true));
        return y + ROW_HEIGHT;
    }

    private void drawCostPill(GuiGraphics g, Font font, UiRect rect, long cost, ResourceKind resource, GuiTheme theme) {
        int color = TerritoryResourceColors.configuredColor(resource);
        int textColor = contrastTextColor(color);
        int borderColor = contrastBorderColor(color);
        Render2D.shaderRoundedSurface(g, rect.x(), rect.y(), rect.width(), rect.height(), 3, costFillColor(color), borderColor);
        Component text = GuiStyle.styled(fit(font, formatShortNumber(cost) + shortResourceName(resource), Math.max(1, rect.width() - 4)));
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, textColor, false);
    }

    private void drawDivider(GuiGraphics g, UiRect panel, int y, GuiTheme theme) {
        g.fill(panel.x(), y, panel.right(), y + 1, dividerColor(theme));
    }

    private void drawPlainButton(GuiGraphics g, Font font, UiRect rect, String label, boolean hovered, GuiTheme theme) {
        Render2D.shaderRoundedSurface(g, rect.x(), rect.y(), rect.width(), rect.height(), 3, controlFillColor(theme, hovered), Render2D.withAlpha(theme.accentColor(), hovered ? 210 : 145));
        int color = hovered ? theme.textColor() : theme.secondaryText();
        Component text = GuiStyle.styled(label);
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, color, false);
    }

    private void drawValuePill(GuiGraphics g, Font font, UiRect rect, String value, GuiTheme theme) {
        Render2D.shaderRoundedSurface(g, rect.x(), rect.y(), rect.width(), rect.height(), 3, valueFillColor(theme), Render2D.withAlpha(theme.textColor(), 130));
        Component text = GuiStyle.styled(value);
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, theme.textColor(), false);
    }

    private void drawFittedString(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) {
            return;
        }
        g.drawString(font, GuiStyle.styled(fit(font, text, maxWidth)), x, y, color, false);
    }

    private int upgradeLevel(Map<TerritoryUpgrade, Integer> upgradeLevels, TerritoryUpgrade upgrade) {
        return upgradeLevels == null ? 0 : upgradeLevels.getOrDefault(upgrade, 0);
    }

    private String formatHeldTime(long acquiredMillis) {
        if (acquiredMillis <= 0) {
            return "Unknown";
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - acquiredMillis) / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    private int drawStoredResources(GuiGraphics g, Font font, int x, int y, int maxWidth, TerritoryResourceStore store, GuiTheme theme) {
        if (store == null || store.max().empty()) {
            return drawInfoLine(g, font, x, y, maxWidth, "Stored Resources: Unknown", theme.secondaryText());
        }

        Resources current = store.current();
        Resources max = store.max();
        for (ResourceKind resource : ResourceKind.DISPLAY_ORDER) {
            y = drawInfoLine(g, font, x, y, maxWidth, storedResourceLine(resource.label(), current.amount(resource), max.amount(resource)), theme.secondaryText());
        }
        return y;
    }

    private String storedResourceLine(String label, long current, long max) {
        return label + ": " + formatNumber(current) + " / " + formatNumber(max);
    }

    private String combatLabel(TerritoryUpgrade upgrade, TerritoryDetails data) {
        return switch (upgrade) {
            case DAMAGE -> "Damage: " + formatDamage(data.damage());
            case ATTACK -> "Attack Speed: " + formatStat(data.attackSpeed());
            case HEALTH -> "Health: " + formatStat(data.health());
            case DEFENSE -> "Defense: " + formatPercent(data.defense());
            default -> upgrade.label();
        };
    }

    private String formatDamage(DamageRange damage) {
        if (damage == null) {
            return "0 - 0";
        }
        return formatStat(damage.min()) + "-" + formatStat(damage.max());
    }

    private String formatNumber(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String formatShortNumber(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private String shortResourceName(ResourceKind resource) {
        return switch (resource) {
            case EMERALDS -> "e";
            case ORE -> "o";
            case CROPS -> "c";
            case FISH -> "f";
            case WOOD -> "w";
            case ALL -> "r";
            case NONE -> "-";
        };
    }

    private String formatStat(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return formatNumber(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPercent(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
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

    private int panelBorderColor(GuiTheme theme) {
        return Render2D.withAlpha(theme.accentColor(), 86);
    }

    private int headerFillColor(GuiTheme theme) {
        return Render2D.withAlpha(theme.accentColor(), 28);
    }

    private int dividerColor(GuiTheme theme) {
        return Render2D.withAlpha(theme.textColor(), 42);
    }

    private int controlFillColor(GuiTheme theme, boolean hovered) {
        return Render2D.withAlpha(hovered ? Render2D.lerpColor(theme.secondary(), theme.accentColor(), 0.24f) : theme.secondary(), 238);
    }

    private int valueFillColor(GuiTheme theme) {
        return Render2D.withAlpha(Render2D.lerpColor(theme.background(), theme.secondary(), 0.78f), 238);
    }

    private int costFillColor(int color) {
        int contrastBase = lightness(color) > 170 ? 0xFF000000 : 0xFFFFFFFF;
        return Render2D.withAlpha(Render2D.lerpColor(color, contrastBase, 0.14f), 245);
    }

    private int contrastBorderColor(int color) {
        return lightness(color) > 170 ? 0xFF1A1D23 : 0xFFEDEFF4;
    }

    private int contrastTextColor(int color) {
        return lightness(color) > 150 ? 0xFF101318 : 0xFFFFFFFF;
    }

    private int lightness(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }

    private void shiftAware(Runnable normalClick, Runnable shiftClick) {
        if (shiftDown()) {
            shiftClick.run();
        } else {
            normalClick.run();
        }
    }

    private boolean shiftDown() {
        Minecraft minecraft = Minecraft.getInstance();

        long window = minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private void renderScrollbar(GuiGraphics g, UiRect body, GuiTheme theme, int bodyContentHeight, int maxScroll) {
        if (maxScroll <= 0 || body.height() <= 0) {
            return;
        }

        int trackX = body.right() - 4;
        int trackTop = body.y() + 2;
        int trackBottom = body.bottom() - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int thumbHeight = Math.max(18, Math.round((body.height() / (float) Math.max(1, bodyContentHeight)) * body.height()));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackTop + Math.round((scrollOffset / (float) maxScroll) * thumbTravel);

        g.fill(trackX, trackTop, trackX + 2, trackBottom, Render2D.withAlpha(theme.secondaryText(), 55));
        g.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, Render2D.withAlpha(theme.accentColor(), 170));
    }

    private int clampInt(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public record Actions(
            BiConsumer<TerritoryUpgrade, Integer> upgradeAdjusted,
            IntConsumer taxAdjusted,
            IntConsumer globalTaxSet,
            Runnable headquartersSet,
            Runnable bordersToggled,
            Runnable globalBordersToggled,
            Runnable routeToggled,
            Runnable globalRouteToggled,
            Runnable loadoutsOpened,
            Runnable close
    ) {
        public static final Actions EMPTY = new Actions(NO_UPGRADE_ACTION, NO_INT_ACTION, NO_INT_ACTION, NO_OP, NO_OP, NO_OP, NO_OP, NO_OP, NO_OP, NO_OP);

        public Actions {
            upgradeAdjusted = upgradeAdjusted == null ? NO_UPGRADE_ACTION : upgradeAdjusted;
            taxAdjusted = taxAdjusted == null ? NO_INT_ACTION : taxAdjusted;
            globalTaxSet = globalTaxSet == null ? NO_INT_ACTION : globalTaxSet;
            headquartersSet = headquartersSet == null ? NO_OP : headquartersSet;
            bordersToggled = bordersToggled == null ? NO_OP : bordersToggled;
            globalBordersToggled = globalBordersToggled == null ? NO_OP : globalBordersToggled;
            routeToggled = routeToggled == null ? NO_OP : routeToggled;
            globalRouteToggled = globalRouteToggled == null ? NO_OP : globalRouteToggled;
            loadoutsOpened = loadoutsOpened == null ? NO_OP : loadoutsOpened;
            close = close == null ? NO_OP : close;
        }
    }

    private record DetailButton(UiRect bounds, Runnable onClick, boolean clipToBody) {
    }
}
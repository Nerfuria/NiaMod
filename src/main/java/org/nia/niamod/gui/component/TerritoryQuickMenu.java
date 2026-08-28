package org.nia.niamod.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.eco.TerritoryResourceColors;
import org.nia.niamod.models.eco.TerritoryUpgrade;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class TerritoryQuickMenu {
    private static final int TILE_W = 26;
    private static final int TILE_H = 26;
    private static final int STORAGE_TILE_W = 18;
    private static final int STORAGE_TILE_H = 34;
    private static final int UPGRADE_COLUMNS = 4;
    private static final int SIDE_ROWS = 4;
    private static final List<TerritoryUpgrade> STORAGE_UPGRADES = TerritoryUpgrade.storage();
    private static final List<TerritoryUpgrade> MAIN_UPGRADES = TerritoryUpgrade.quickMenu();
    private static final int UPGRADE_ROWS = Math.max(1, (MAIN_UPGRADES.size() + UPGRADE_COLUMNS - 1) / UPGRADE_COLUMNS);
    private static final int GAP = 2;
    private static final int GRID_H = TILE_H * UPGRADE_ROWS + GAP * (UPGRADE_ROWS - 1);
    private static final int HEIGHT = GRID_H;
    private static final int SIDE_W = 48;
    private static final int PANEL_GAP = 3;
    private static final int GRID_W = TILE_W * UPGRADE_COLUMNS + GAP * (UPGRADE_COLUMNS - 1);
    private static final int WIDTH = STORAGE_TILE_W + GRID_W + PANEL_GAP + SIDE_W;
    private static final int STORAGE_H = STORAGE_TILE_H * STORAGE_UPGRADES.size() + GAP * Math.max(0, STORAGE_UPGRADES.size() - 1);
    private static final int SIDE_CELL_H = 15;
    private static final int SIDE_H = SIDE_CELL_H * SIDE_ROWS + GAP * (SIDE_ROWS - 1);
    private static final Map<TerritoryUpgrade, ItemStack> ICONS = createIcons();
    private static final ItemStack TAX_ICON = new ItemStack(Items.EMERALD);
    private static final String CROWN = "\uD83D\uDC51";

    private final List<Button> buttons = new ArrayList<>();
    private int x;
    private int y;
    private boolean visible;
    private boolean editingTax;
    private boolean editingGlobalTax;
    private String taxInput = "";
    private int originalTax;

    private static Map<TerritoryUpgrade, ItemStack> createIcons() {
        EnumMap<TerritoryUpgrade, ItemStack> icons = new EnumMap<>(TerritoryUpgrade.class);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            icons.put(upgrade, new ItemStack(upgrade.iconItem()));
        }
        return icons;
    }

    public void showAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        moveTo(mouseX, mouseY, screenWidth, screenHeight);
        visible = true;
    }

    public void moveTo(int screenX, int screenY, int screenWidth, int screenHeight) {
        x = clamp(screenX, 4, Math.max(4, screenWidth - WIDTH - 4));
        y = clamp(screenY, 4, Math.max(4, screenHeight - HEIGHT - 4));
    }

    public void hide() {
        visible = false;
        editingTax = false;
        taxInput = "";
        buttons.clear();
    }

    public boolean visible() {
        return visible;
    }

    public boolean contains(double mouseX, double mouseY) {
        return visible && bounds().contains(mouseX, mouseY);
    }

    public void render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            GuiTheme theme,
            Map<TerritoryUpgrade, Integer> stats,
            int taxPercent,
            boolean headquarters,
            boolean bordersOpen,
            String routeLabel,
            BiConsumer<TerritoryUpgrade, Integer> statAdjusted,
            Runnable headquartersSet,
            Runnable bordersToggled,
            Runnable globalBordersToggled,
            Runnable routeToggled,
            Runnable globalRouteToggled
    ) {
        buttons.clear();
        if (!visible) {
            return;
        }

        int storageY = y + Math.max(0, (GRID_H - STORAGE_H) / 2);
        int gridX = x + STORAGE_TILE_W;
        int sideX = gridX + GRID_W + PANEL_GAP;
        int sideY = y + Math.max(0, (GRID_H - SIDE_H) / 2);
        UiRect panel = new UiRect(gridX, y, GRID_W + PANEL_GAP + SIDE_W, GRID_H);

        Render2D.dropShadow(g, panel, 4, 0x66000000, 7);
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 5, theme.background(), Render2D.withAlpha(theme.accentColor(), 100));

        for (int i = 0; i < STORAGE_UPGRADES.size(); i++) {
            TerritoryUpgrade upgrade = STORAGE_UPGRADES.get(i);
            UiRect tile = new UiRect(gridX - STORAGE_TILE_W, storageY + i * (STORAGE_TILE_H + GAP), STORAGE_TILE_W, STORAGE_TILE_H);
            drawUpgradeTile(g, font, tile, mouseX, mouseY, theme, upgrade, stats, true);
            buttons.add(new Button(tile, button -> statAdjusted.accept(upgrade, button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 1)));
        }

        for (int i = 0; i < MAIN_UPGRADES.size(); i++) {
            TerritoryUpgrade upgrade = MAIN_UPGRADES.get(i);
            int col = i % UPGRADE_COLUMNS;
            int row = i / UPGRADE_COLUMNS;
            UiRect tile = new UiRect(gridX + col * (TILE_W + GAP), y + row * (TILE_H + GAP), TILE_W, TILE_H);
            drawUpgradeTile(g, font, tile, mouseX, mouseY, theme, upgrade, stats, false);
            buttons.add(new Button(tile, button -> statAdjusted.accept(upgrade, button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 1)));
        }

        UiRect tax = sideButtonBounds(sideX, sideY, 0);
        UiRect hq = sideButtonBounds(sideX, sideY, 1);
        UiRect borders = sideButtonBounds(sideX, sideY, 2);
        UiRect route = sideButtonBounds(sideX, sideY, 3);
        drawTaxControl(g, font, tax, mouseX, mouseY, theme, taxPercent);
        drawCrownTile(g, font, hq, mouseX, mouseY, theme, headquarters);
        drawBordersTile(g, font, borders, mouseX, mouseY, theme, bordersOpen);
        drawRouteTile(g, font, route, mouseX, mouseY, theme, routeLabel);
        buttons.add(new Button(tax, ignored -> startTaxEdit(taxPercent, shiftDown())));
        buttons.add(new Button(hq, ignored -> headquartersSet.run()));
        buttons.add(new Button(borders, ignored -> shiftAware(bordersToggled, globalBordersToggled)));
        buttons.add(new Button(route, ignored -> shiftAware(routeToggled, globalRouteToggled)));
    }

    private UiRect sideButtonBounds(int sideX, int sideY, int row) {
        return new UiRect(sideX, sideY + row * (SIDE_CELL_H + GAP), SIDE_W, SIDE_CELL_H);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !bounds().contains(mouseX, mouseY)) {
            return false;
        }
        for (Button quickButton : buttons) {
            if (quickButton.bounds().contains(mouseX, mouseY)) {
                quickButton.onClick().accept(button);
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(KeyEvent event, IntConsumer taxSet, IntConsumer globalTaxSet) {
        if (!visible || !editingTax) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            commitTax(taxSet, globalTaxSet);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            cancelTaxEdit();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!taxInput.isEmpty()) {
                taxInput = taxInput.substring(0, taxInput.length() - 1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE) {
            taxInput = "";
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!visible || !editingTax || !event.isAllowedChatCharacter()) {
            return false;
        }

        String typed = event.codepointAsString();
        if (typed.length() == 1 && Character.isDigit(typed.charAt(0)) && taxInput.length() < 2) {
            taxInput += typed;
            return true;
        }
        return false;
    }

    private void drawUpgradeTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, GuiTheme theme, TerritoryUpgrade upgrade, Map<TerritoryUpgrade, Integer> stats, boolean addon) {
        boolean hovered = tile.contains(mouseX, mouseY);
        int color = TerritoryResourceColors.configuredColor(upgrade.costResource());
        Render2D.shaderRoundedSurface(
                g,
                tile.x(),
                tile.y(),
                tile.width(),
                tile.height(),
                4,
                hovered ? Render2D.withAlpha(theme.accentColor(), 230) : Render2D.withAlpha(theme.secondary(), 232),
                Render2D.withAlpha(color, hovered ? 255 : 220)
        );

        int itemX = addon ? tile.x() + 1 : tile.x() + (tile.width() - 16) / 2;
        int itemY = tile.y() + (tile.height() - 16) / 2;
        ItemStack stack = ICONS.getOrDefault(upgrade, ItemStack.EMPTY);
        g.renderItem(stack, itemX, itemY);
        g.renderItemDecorations(font, stack, itemX, itemY, Integer.toString(level(stats, upgrade)));
    }

    private void drawTaxControl(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, GuiTheme theme, int taxPercent) {
        boolean hovered = tile.contains(mouseX, mouseY);
        int border = editingTax
                ? Render2D.withAlpha(theme.accentColor(), 210)
                : Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90);
        Render2D.shaderRoundedSurface(
                g,
                tile.x(),
                tile.y(),
                tile.width(),
                tile.height(),
                4,
                hovered || editingTax ? Render2D.withAlpha(theme.accentColor(), 230) : Render2D.withAlpha(theme.secondary(), 232),
                border
        );

        int iconX = tile.x() + 2;
        int iconY = tile.y() + Math.max(0, (tile.height() - 16) / 2);
        g.renderItem(TAX_ICON, iconX, iconY);

        String value = editingTax ? taxInput + cursorText() : taxPercent + "%";
        drawCentered(g, font, value, tile.x() + 19, tile.y() + (tile.height() - font.lineHeight) / 2 + 1, tile.width() - 21, theme.textColor());
    }

    private void drawCrownTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, GuiTheme theme, boolean headquarters) {
        boolean hovered = tile.contains(mouseX, mouseY);
        Render2D.shaderRoundedSurface(
                g,
                tile.x(),
                tile.y(),
                tile.width(),
                tile.height(),
                4,
                hovered ? Render2D.withAlpha(theme.accentColor(), 230) : Render2D.withAlpha(theme.secondary(), 232),
                Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90)
        );
        drawCentered(g, font, CROWN, tile.x(), tile.y() + 4, tile.width(), headquarters ? 0xFFFFD75A : Render2D.withAlpha(0xFFFFFF, 92));
    }

    private void drawBordersTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, GuiTheme theme, boolean bordersOpen) {
        boolean hovered = tile.contains(mouseX, mouseY);
        Render2D.shaderRoundedSurface(
                g,
                tile.x(),
                tile.y(),
                tile.width(),
                tile.height(),
                4,
                hovered ? Render2D.withAlpha(theme.accentColor(), 230) : Render2D.withAlpha(theme.secondary(), 232),
                Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90)
        );
        drawCentered(g, font, bordersOpen ? "Open" : "Closed", tile.x() + 1, tile.y() + 4, tile.width() - 2, bordersOpen ? theme.textColor() : theme.secondaryText());
    }

    private void drawRouteTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, GuiTheme theme, String routeLabel) {
        boolean hovered = tile.contains(mouseX, mouseY);
        Render2D.shaderRoundedSurface(
                g,
                tile.x(),
                tile.y(),
                tile.width(),
                tile.height(),
                4,
                hovered ? Render2D.withAlpha(theme.accentColor(), 230) : Render2D.withAlpha(theme.secondary(), 232),
                Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90)
        );
        drawCentered(g, font, routeLabel == null || routeLabel.isBlank() ? "Route" : routeLabel, tile.x() + 1, tile.y() + 4, tile.width() - 2, hovered ? theme.textColor() : theme.secondaryText());
    }

    private void drawCentered(GuiGraphics g, Font font, String value, int x, int y, int width, int color) {
        String fitted = fit(font, value, width);
        Component text = GuiStyle.styled(fitted);
        g.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private int level(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return stats == null ? 0 : stats.getOrDefault(upgrade, 0);
    }

    private void startTaxEdit(int taxPercent, boolean global) {
        originalTax = taxPercent;
        taxInput = "";
        editingTax = true;
        editingGlobalTax = global;
    }

    private void commitTax(IntConsumer taxSet, IntConsumer globalTaxSet) {
        int value = originalTax;
        if (!taxInput.isBlank()) {
            try {
                value = Integer.parseInt(taxInput);
            } catch (NumberFormatException ignored) {
            }
        }
        IntConsumer target = editingGlobalTax ? globalTaxSet : taxSet;
        target.accept(Math.max(0, Math.min(70, value)));
        editingTax = false;
        editingGlobalTax = false;
    }

    private void cancelTaxEdit() {
        editingTax = false;
        editingGlobalTax = false;
        taxInput = "";
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

    private String cursorText() {
        return (System.currentTimeMillis() / 450L) % 2L == 0L ? "|" : "";
    }

    private String fit(Font font, String value, int width) {
        if (value == null) {
            return "";
        }
        if (font.width(GuiStyle.styled(value)) <= width) {
            return value;
        }
        String trimmed = value;
        while (!trimmed.isEmpty() && font.width(GuiStyle.styled(trimmed + "...")) > width) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? "" : trimmed + "...";
    }

    private UiRect bounds() {
        return new UiRect(x, y, WIDTH, HEIGHT);
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private record Button(UiRect bounds, java.util.function.IntConsumer onClick) {
    }
}
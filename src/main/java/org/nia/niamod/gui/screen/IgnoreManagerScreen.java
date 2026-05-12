package org.nia.niamod.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.models.ignore.IgnorePlayerEntry;
import org.nia.niamod.models.ignore.IgnorePlayerMode;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;

public class IgnoreManagerScreen extends Screen {
    private static final int FAVOURITE_COLOR = 0xFFFFD166;
    private static final int AVOID_COLOR = 0xFFFF6B6B;
    private static final int IGNORE_ON_COLOR = 0xFFFF7A7A;
    private static final int IGNORE_OFF_COLOR = 0xFF61D394;

    private final Screen parent;
    private final IgnoreFeature feature;
    private final List<IgnorePlayerEntry> players = new ArrayList<>();
    private EditBox searchBox;
    private IgnoreManagerLayout layout;
    private String searchQuery = "";
    private double scroll;
    private double scrollTarget;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;
    private int featureRevision = -1;

    public IgnoreManagerScreen(Screen parent, IgnoreFeature feature) {
        super(Component.literal("Ignore Manager"));
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    protected void init() {
        layout = IgnoreManagerLayout.fromScreen(width, height);
        searchBox = new EditBox(font, 0, 0, IgnoreManagerLayout.SEARCH_WIDTH, IgnoreManagerLayout.SEARCH_HEIGHT, GuiStyle.styled("Search"));
        searchBox.setBordered(false);
        searchBox.setHeight(IgnoreManagerLayout.SEARCH_HEIGHT);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0x82FFFFFF);
        searchBox.setCanLoseFocus(true);
        GuiStyle.applyFont(searchBox, "Search...");
        searchBox.setResponder(value -> {
            searchQuery = value;
            scroll = scrollTarget = 0;
            reloadPlayers();
        });
        searchBox.setValue(searchQuery);
        searchBox.setFocused(false);
        addRenderableWidget(searchBox);
        setFocused(null);
        reloadPlayers();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = GuiStyle.configuredTheme();
        reloadIfNeeded();
        updateScroll();

        Render2D.dropShadow(g, layout.panelBounds(), 7, theme.shadowColor(), IgnoreManagerLayout.PANEL_RADIUS);
        Render2D.shaderRoundedRect(g, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(), IgnoreManagerLayout.PANEL_RADIUS, theme.background());
        Render2D.shaderRoundedRect(g, layout.panelX(), layout.panelY(), layout.panelW(), IgnoreManagerLayout.HEADER_HEIGHT, IgnoreManagerLayout.PANEL_RADIUS, theme.secondary());
        g.nextStratum();

        drawTopBar(g, mouseX, mouseY, theme);
        drawPlayers(g, mouseX, mouseY, theme);
        drawScrollbar(g, theme);

        super.render(g, mouseX, mouseY, delta);
        drawStarTooltip(g, mouseX, mouseY, theme);
    }

    private void drawTopBar(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        float titleScale = 1.35f;
        Component title = GuiStyle.styled("Ignore Manager");
        int titleW = Math.round(font.width(title) * titleScale);
        int titleX = layout.panelX() + (layout.panelW() - titleW) / 2;
        int titleY = Math.round(layout.panelY() + (IgnoreManagerLayout.HEADER_HEIGHT - font.lineHeight * titleScale) / 2.0f);
        drawBig(g, title, titleX, titleY, titleScale, theme.textColor());

        drawUnignoreAll(g, mouseX, mouseY, theme);
        drawSearch(g, mouseX, mouseY, theme);
    }

    private void drawUnignoreAll(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        UiRect button = unignoreBounds();
        boolean active = feature.hasIgnoredPlayers();
        boolean hovered = active && inside(button, mouseX, mouseY);
        int stateColor = active ? IGNORE_ON_COLOR : theme.trinaryText();
        int fill = hovered ? Render2D.withAlpha(stateColor, 44) : Render2D.withAlpha(theme.secondary(), active ? 196 : 150);
        int border = Render2D.withAlpha(stateColor, active ? hovered ? 140 : 82 : 42);
        int textColor = active ? stateColor : Render2D.withAlpha(theme.trinaryText(), 150);

        Render2D.shaderRoundedSurface(g, button.x(), button.y(), button.width(), button.height(), 6, fill, border);
        String label = "Unignore All";
        int labelW = font.width(GuiStyle.styled(label));
        int labelY = button.y() + (button.height() - font.lineHeight) / 2 + 1;
        g.drawString(font, GuiStyle.styled(label), button.x() + (button.width() - labelW) / 2, labelY, textColor, false);
    }

    private void drawSearch(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        UiRect search = searchBounds();
        boolean hovered = inside(search, mouseX, mouseY);
        int fill = searchBox.isFocused()
                ? Render2D.withAlpha(theme.secondary(), 245)
                : hovered ? Render2D.withAlpha(theme.secondary(), 228) : Render2D.withAlpha(theme.secondary(), 214);
        int border = searchBox.isFocused() ? Render2D.withAlpha(theme.accentColor(), 105) : 0x20FFFFFF;
        Render2D.shaderRoundedSurface(g, search.x(), search.y(), search.width(), search.height(), 7, fill, border);
        Render2D.circle(g, search.x() + 11, search.y() + search.height() / 2, 4, searchBox.isFocused() ? Render2D.withAlpha(theme.accentColor(), 220) : 0x66FFFFFF);
        GuiStyle.layoutBorderlessEditBox(searchBox, font, search.x() + 21, search.y() + 1, search.width() - 28, search.height());
        searchBox.visible = true;
        searchBox.active = true;
        searchBox.setEditable(true);
    }

    private void drawPlayers(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        int top = listTop();
        int bottom = layout.listBottom();
        UiRect list = layout.listBounds();

        g.enableScissor(list.x(), top, list.right(), bottom);
        double rowY = top + scroll;
        for (IgnorePlayerEntry player : players) {
            UiRect bounds = new UiRect(list.x(), (int) Math.round(rowY), list.width(), IgnoreManagerLayout.ROW_HEIGHT);
            drawPlayer(g, player, bounds, mouseX, mouseY, theme);
            rowY += IgnoreManagerLayout.ROW_HEIGHT + IgnoreManagerLayout.ROW_GAP;
        }
        g.disableScissor();
    }

    private void drawPlayer(GuiGraphics g, IgnorePlayerEntry player, UiRect bounds, int mouseX, int mouseY, GuiTheme theme) {
        boolean hovered = inside(bounds, mouseX, mouseY);
        int modeColor = starColor(player.mode(), theme);
        int rowFill = hovered ? Render2D.withAlpha(theme.overlay(), 185) : theme.overlay();
        int rowBorder = Render2D.withAlpha(0xFFFFFF, hovered ? 34 : 16);
        Render2D.shaderRoundedSurface(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 7, rowFill, rowBorder);

        UiRect star = starBounds(bounds);
        int starFill = Render2D.withAlpha(modeColor, player.mode() == IgnorePlayerMode.NONE ? 20 : 38);
        int starBorder = Render2D.withAlpha(modeColor, player.mode() == IgnorePlayerMode.NONE ? 46 : 90);
        UiRect starButton = new UiRect(star.x() + 8, star.y() + 6, 20, 20);
        Render2D.shaderRoundedSurface(g, starButton.x(), starButton.y(), starButton.width(), starButton.height(), 6, starFill, starBorder);
        Component starText = GuiStyle.styled("★");
        int starWidth = font.width(starText);
        float starTextX = starButton.x() + starButton.width() / 2.0f - starWidth / 2.0f + 0.5f;
        float starTextY = starButton.y() + starButton.height() / 2.0f - font.lineHeight / 2.0f + 0.5f;
        drawStringAt(g, starText, starTextX, starTextY, modeColor);

        int nameX = bounds.x() + IgnoreManagerLayout.STAR_WIDTH + 6;
        int nameMaxW = bounds.width() - IgnoreManagerLayout.STAR_WIDTH - IgnoreManagerLayout.IGNORE_WIDTH - 22;
        String displayName = fit(player.playerName(), nameMaxW);
        g.drawString(font, GuiStyle.styled(displayName), nameX, bounds.y() + 11, theme.textColor(), false);

        UiRect ignore = ignoreBounds(bounds);
        boolean ignored = player.ignored();
        boolean buttonHovered = inside(ignore, mouseX, mouseY);
        int stateColor = ignored ? IGNORE_ON_COLOR : IGNORE_OFF_COLOR;
        int buttonFill = buttonHovered ? Render2D.withAlpha(stateColor, 44) : Render2D.withAlpha(theme.secondary(), 196);
        int buttonBorder = Render2D.withAlpha(stateColor, buttonHovered ? 150 : 92);
        Render2D.shaderRoundedSurface(g, ignore.x(), ignore.y(), ignore.width(), ignore.height(), 6, buttonFill, buttonBorder);
        String label = ignored ? "Ignored" : "Ignore";
        int labelW = font.width(GuiStyle.styled(label));
        int labelY = ignore.y() + (ignore.height() - font.lineHeight) / 2 + 1;
        g.drawString(font, GuiStyle.styled(label), ignore.x() + (ignore.width() - labelW) / 2, labelY, stateColor, false);
    }

    private void drawScrollbar(GuiGraphics g, GuiTheme theme) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        UiRect track = scrollTrack();
        UiRect thumb = scrollThumb(maxScroll);
        int thumbX = track.x() + (track.width() - IgnoreManagerLayout.SCROLLBAR_WIDTH) / 2;
        g.fill(thumbX, thumb.y(), thumbX + IgnoreManagerLayout.SCROLLBAR_WIDTH, thumb.bottom(), theme.scrollbarColor());
    }

    private void drawStarTooltip(GuiGraphics g, int mouseX, int mouseY, GuiTheme theme) {
        if (mouseY < listTop() || mouseY > layout.listBottom()) {
            return;
        }

        for (int i = 0; i < players.size(); i++) {
            IgnorePlayerEntry player = players.get(i);
            UiRect bounds = playerBounds(i);
            if (!inside(starBounds(bounds), mouseX, mouseY)) {
                continue;
            }

            String[] lines = player.modeEditable()
                    ? new String[]{
                    "Left click: Favourite",
                    "Middle click: Reset",
                    "Right click: Avoid"
            }
                    : new String[]{
                    "Left click: Favourite",
                    "Middle click: Reset",
                    "Right click: Avoid",
                    "Locked: chat-detected"
            };
            drawTooltip(g, lines, mouseX - 5, mouseY - 12, theme);
            return;
        }
    }

    private void drawTooltip(GuiGraphics g, String[] lines, int mouseX, int mouseY, GuiTheme theme) {
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.width(GuiStyle.styled(line)));
        }

        int lineGap = 2;
        int tooltipW = textWidth + 12;
        int tooltipH = lines.length * font.lineHeight + Math.max(0, lines.length - 1) * lineGap + 8;
        int x = Math.min(mouseX + 12, width - tooltipW - 4);
        int y = Math.min(mouseY + 12, height - tooltipH - 4);
        x = Math.max(4, x);
        y = Math.max(4, y);

        Render2D.dropShadow(g, new UiRect(x, y, tooltipW, tooltipH), 5, theme.shadowColor(), 6);
        Render2D.shaderRoundedSurface(g, x, y, tooltipW, tooltipH, 6, Render2D.withAlpha(theme.secondary(), 245), Render2D.withAlpha(0xFFFFFF, 42));
        for (int i = 0; i < lines.length; i++) {
            int lineY = y + 5 + i * (font.lineHeight + lineGap);
            g.drawString(font, GuiStyle.styled(lines[i]), x + 6, lineY, 0xE6FFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.input();
        boolean clickedSearch = inside(searchBounds(), mouseX, mouseY);
        if (!clickedSearch) {
            blurSearch();
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && grabScrollbar(mouseX, mouseY)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inside(unignoreBounds(), mouseX, mouseY)) {
            if (feature.hasIgnoredPlayers()) {
                feature.unignoreEveryone();
                reloadPlayers();
            }
            return true;
        }

        for (int i = 0; i < players.size(); i++) {
            IgnorePlayerEntry player = players.get(i);
            UiRect bounds = playerBounds(i);
            if (!inside(bounds, mouseX, mouseY)) {
                continue;
            }

            if (inside(starBounds(bounds), mouseX, mouseY)) {
                if (!player.modeEditable()) {
                    return true;
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    feature.setMode(player.playerName(), IgnorePlayerMode.FAVOURITE);
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    feature.setMode(player.playerName(), IgnorePlayerMode.AVOID);
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    feature.setMode(player.playerName(), IgnorePlayerMode.NONE);
                }
                reloadPlayers();
                return true;
            }

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inside(ignoreBounds(bounds), mouseX, mouseY)) {
                feature.toggleIgnored(player.playerName());
                reloadPlayers();
                return true;
            }
        }

        if (clickedSearch) {
            focusSearch();
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollbar) {
            dragScrollbarTo(event.y(), scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (layout.containsPanel(mouseX, mouseY)) {
            scrollTarget += verticalAmount * 28;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchBox != null && searchBox.isFocused() && !searchQuery.isEmpty()) {
                searchBox.setValue("");
                return true;
            }
            onClose();
            return true;
        }
        if (shouldFocusSearchForEditKey(event)) {
            focusSearch();
            return searchBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (searchBox == null) {
            return false;
        }
        if (!searchBox.isFocused()) {
            if (!event.isAllowedChatCharacter()) {
                return false;
            }
            focusSearch();
            return searchBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        draggingScrollbar = false;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void reloadPlayers() {
        players.clear();
        players.addAll(feature.getVisiblePlayers(searchQuery));
        featureRevision = feature.getRevision();
        if (maxScroll() <= 0) {
            draggingScrollbar = false;
        }
        clampScroll();
    }

    private void reloadIfNeeded() {
        if (featureRevision != feature.getRevision()) {
            reloadPlayers();
        }
    }

    private UiRect playerBounds(int index) {
        return layout.playerBounds(index, scroll);
    }

    private UiRect searchBounds() {
        return layout.searchBounds();
    }

    private UiRect unignoreBounds() {
        return layout.unignoreBounds();
    }

    private UiRect starBounds(UiRect row) {
        return layout.starBounds(row);
    }

    private UiRect ignoreBounds(UiRect row) {
        return layout.ignoreBounds(row);
    }

    private UiRect scrollTrack() {
        return layout.scrollTrack();
    }

    private UiRect scrollThumb(int maxScroll) {
        return layout.scrollThumb(maxScroll, scroll);
    }

    private int thumbHeight(int maxScroll) {
        return layout.thumbHeight(maxScroll);
    }

    private boolean grabScrollbar(double mouseX, double mouseY) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0 || !inside(scrollTrack(), mouseX, mouseY)) {
            return false;
        }

        UiRect thumb = scrollThumb(maxScroll);
        draggingScrollbar = true;
        scrollbarDragOffset = inside(thumb, mouseX, mouseY) ? mouseY - thumb.y() : thumb.height() / 2.0;
        dragScrollbarTo(mouseY, scrollbarDragOffset);
        return true;
    }

    private void dragScrollbarTo(double mouseY, double thumbOffset) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            scroll = scrollTarget = 0;
            draggingScrollbar = false;
            return;
        }

        UiRect track = scrollTrack();
        int height = thumbHeight(maxScroll);
        double travel = Math.max(1.0, track.height() - height);
        double thumbTop = mouseY - thumbOffset;
        double progress = (thumbTop - track.y()) / travel;
        progress = Math.max(0.0, Math.min(1.0, progress));
        scroll = scrollTarget = -maxScroll * progress;
    }

    private boolean inside(UiRect rect, double mouseX, double mouseY) {
        return mouseX >= rect.x() && mouseX <= rect.right() && mouseY >= rect.y() && mouseY <= rect.bottom();
    }

    private String fit(String text, int maxWidth) {
        if (font.width(GuiStyle.styled(text)) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(GuiStyle.styled(trimmed + "...")) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }

    private int starColor(IgnorePlayerMode mode, GuiTheme theme) {
        return switch (mode) {
            case FAVOURITE -> FAVOURITE_COLOR;
            case AVOID -> AVOID_COLOR;
            case NONE -> theme.trinaryText();
        };
    }

    private void drawBig(GuiGraphics g, Component text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private void drawStringAt(GuiGraphics g, Component text, float x, float y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private void focusSearch() {
        if (searchBox != null && !searchBox.isFocused()) {
            searchBox.setFocused(true);
            setFocused(searchBox);
        }
    }

    private boolean shouldFocusSearchForEditKey(KeyEvent event) {
        return searchBox != null
                && !searchBox.isFocused()
                && (event.isSelectAll() || event.key() == GLFW.GLFW_KEY_BACKSPACE);
    }

    private void blurSearch() {
        if (searchBox != null && searchBox.isFocused()) {
            searchBox.setFocused(false);
            setFocused(null);
        }
    }

    private void updateScroll() {
        scroll += (scrollTarget - scroll) * 0.35;
        if (Math.abs(scrollTarget - scroll) < 0.2) {
            scroll = scrollTarget;
        }
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = maxScroll();
        scrollTarget = Math.max(-maxScroll, Math.min(0, scrollTarget));
        scroll = Math.max(-maxScroll, Math.min(0, scroll));
    }

    private int maxScroll() {
        return layout.maxScroll(players.size());
    }

    private int listTop() {
        return layout.listTop();
    }
}

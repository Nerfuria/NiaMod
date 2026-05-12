package org.nia.niamod.gui.screen;

import org.nia.niamod.gui.render.UiRect;

record IgnoreManagerLayout(int panelX, int panelY, int panelW, int panelH) {
    public static final int PANEL_RADIUS = 12;
    public static final int HEADER_HEIGHT = 54;
    public static final int ROW_HEIGHT = 32;
    public static final int ROW_GAP = 5;
    public static final int STAR_WIDTH = 38;
    public static final int IGNORE_WIDTH = 96;
    public static final int SEARCH_WIDTH = 170;
    public static final int SEARCH_HEIGHT = 22;
    public static final int SCROLLBAR_WIDTH = 1;

    private static final int PANEL_MAX_WIDTH = 620;
    private static final int PANEL_MAX_HEIGHT = 430;
    private static final int PANEL_MARGIN = 24;
    private static final int UNIGNORE_WIDTH = 96;
    private static final int SCROLLBAR_HIT_WIDTH = 10;

    public static IgnoreManagerLayout fromScreen(int screenWidth, int screenHeight) {
        int availableWidth = Math.max(1, screenWidth - PANEL_MARGIN * 2);
        int availableHeight = Math.max(1, screenHeight - PANEL_MARGIN * 2);
        int panelWidth = Math.min(PANEL_MAX_WIDTH, availableWidth);
        int panelHeight = Math.min(PANEL_MAX_HEIGHT, availableHeight);
        return new IgnoreManagerLayout((screenWidth - panelWidth) / 2, (screenHeight - panelHeight) / 2, panelWidth, panelHeight);
    }

    public UiRect panelBounds() {
        return new UiRect(panelX, panelY, panelW, panelH);
    }

    public UiRect headerBounds() {
        return new UiRect(panelX, panelY, panelW, HEADER_HEIGHT);
    }

    public UiRect listBounds() {
        return new UiRect(panelX + 12, listTop(), panelW - 24, listHeight());
    }

    public UiRect playerBounds(int index, double scroll) {
        UiRect list = listBounds();
        int y = (int) Math.round(list.y() + scroll + index * (ROW_HEIGHT + ROW_GAP));
        return new UiRect(list.x(), y, list.width(), ROW_HEIGHT);
    }

    public UiRect searchBounds() {
        int width = Math.min(SEARCH_WIDTH, Math.max(96, panelW / 3 - 14));
        return new UiRect(panelX + 14, panelY + 16, width, SEARCH_HEIGHT);
    }

    public UiRect unignoreBounds() {
        return new UiRect(panelX + panelW - UNIGNORE_WIDTH - 18, panelY + 16, UNIGNORE_WIDTH, SEARCH_HEIGHT);
    }

    public UiRect starBounds(UiRect row) {
        return new UiRect(row.x(), row.y(), STAR_WIDTH, row.height());
    }

    public UiRect ignoreBounds(UiRect row) {
        return new UiRect(row.right() - IGNORE_WIDTH - 6, row.y() + 5, IGNORE_WIDTH, row.height() - 10);
    }

    public UiRect scrollTrack() {
        return new UiRect(panelX + panelW - 11, listTop(), SCROLLBAR_HIT_WIDTH, listHeight());
    }

    public UiRect scrollThumb(int maxScroll, double scroll) {
        UiRect track = scrollTrack();
        int height = thumbHeight(maxScroll);
        int y = track.y() + Math.round((track.height() - height) * (float) (-scroll / maxScroll));
        return new UiRect(track.x(), y, track.width(), height);
    }

    public int thumbHeight(int maxScroll) {
        int listHeight = listHeight();
        return Math.max(18, Math.round(listHeight * (float) listHeight / (listHeight + maxScroll)));
    }

    public int maxScroll(int playerCount) {
        int contentHeight = playerCount == 0 ? 0 : playerCount * ROW_HEIGHT + (playerCount - 1) * ROW_GAP;
        return Math.max(0, contentHeight - listHeight());
    }

    public boolean containsPanel(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    public int listTop() {
        return panelY + HEADER_HEIGHT + 10;
    }

    public int listBottom() {
        return panelY + panelH - 10;
    }

    public int listHeight() {
        return panelH - HEADER_HEIGHT - 20;
    }
}

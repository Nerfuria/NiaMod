package org.nia.niamod.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.models.eco.ResourceFlow;
import org.nia.niamod.models.eco.ResourceKind;
import org.nia.niamod.models.eco.Resources;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.Locale;

public class TerritoryResourceSummaryWidget {
    private static final int MARGIN = 8;
    private static final int WIDTH = 348;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 14;
    private static final int PAD = 8;

    public int height() {
        int rows = 7;
        return PAD + HEADER_HEIGHT + rows * ROW_HEIGHT + PAD - 2;
    }

    public void render(GuiGraphics g, Font font, GuiTheme theme, ResourceFlow state) {
        int height = height();
        UiRect panel = new UiRect(MARGIN, MARGIN, WIDTH, height);
        Render2D.dropShadow(g, panel, 4, 0x55000000, 7);
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 86));
        Render2D.horizontalGradient(
                g,
                panel.x() + 1,
                panel.y() + 1,
                panel.width() - 2,
                HEADER_HEIGHT + 1,
                Render2D.withAlpha(theme.accentColor(), 48),
                Render2D.withAlpha(theme.secondary(), 18)
        );

        int x = panel.x() + PAD;
        int y = panel.y() + PAD - 1;
        int right = panel.right() - PAD;
        g.drawString(font, GuiStyle.styled("Guild Economy"), x, y, theme.textColor(), false);
        y += HEADER_HEIGHT;

        Resources stored = state == null ? Resources.EMPTY : state.stored();
        Resources capacity = state == null ? Resources.EMPTY : state.capacity();
        Resources gained = state == null ? Resources.EMPTY : state.gainedPerHour();
        Resources used = state == null ? Resources.EMPTY : state.usedPerHour();
        Resources net = state == null ? Resources.EMPTY : state.netPerHour();

        y = drawHeaderRow(g, font, x, right, y, theme);
        for (ResourceKind resource : ResourceKind.DISPLAY_ORDER) {
            y = drawResourceRow(g, font, x, right, y, resource, stored, capacity, gained, used, net, theme);
        }
        drawRow(g, font, x, right, y, "Material Usage", formatPercent(state == null ? 0.0 : state.materialUsagePercent()), theme);
    }

    private int drawHeaderRow(GuiGraphics g, Font font, int x, int right, int y, GuiTheme theme) {
        int storeX = x + 72;
        int gainX = x + 172;
        int usedX = x + 226;
        int netX = right - 44;
        g.drawString(font, GuiStyle.styled("HQ Store"), storeX, y + 1, theme.trinaryText(), false);
        g.drawString(font, GuiStyle.styled("+/hr"), gainX, y + 1, theme.trinaryText(), false);
        g.drawString(font, GuiStyle.styled("-/hr"), usedX, y + 1, theme.trinaryText(), false);
        g.drawString(font, GuiStyle.styled("Net"), netX, y + 1, theme.trinaryText(), false);
        return y + ROW_HEIGHT;
    }

    private int drawResourceRow(
            GuiGraphics g,
            Font font,
            int x,
            int right,
            int y,
            ResourceKind resource,
            Resources stored,
            Resources capacity,
            Resources gained,
            Resources used,
            Resources net,
            GuiTheme theme
    ) {
        int storeX = x + 72;
        int gainX = x + 172;
        int usedX = x + 226;
        int netX = right - 44;
        g.drawString(font, GuiStyle.styled(resource.label()), x, y + 1, theme.secondaryText(), false);
        g.drawString(font, GuiStyle.styled(format(stored.amount(resource)) + "/" + formatShort(capacity.amount(resource))), storeX, y + 1, theme.textColor(), false);
        g.drawString(font, GuiStyle.styled(formatShort(gained.amount(resource))), gainX, y + 1, theme.textColor(), false);
        g.drawString(font, GuiStyle.styled(formatShort(used.amount(resource))), usedX, y + 1, theme.textColor(), false);
        g.drawString(font, GuiStyle.styled(formatShort(net.amount(resource))), netX, y + 1, theme.textColor(), false);
        return y + ROW_HEIGHT;
    }

    private int drawRow(GuiGraphics g, Font font, int x, int right, int y, String label, String value, GuiTheme theme) {
        g.drawString(font, GuiStyle.styled(label), x, y + 1, theme.secondaryText(), false);
        int valueWidth = font.width(GuiStyle.styled(value));
        g.drawString(font, GuiStyle.styled(value), right - valueWidth, y + 1, theme.textColor(), false);
        return y + ROW_HEIGHT;
    }

    private String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String formatShort(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return format(Math.round(value));
        }
        return String.format(Locale.ROOT, "%,.2f", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}
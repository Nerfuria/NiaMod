package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.*;
import org.nia.niamod.models.eco.GameChange.Borders;
import org.nia.niamod.models.eco.GameChange.Headquarters;
import org.nia.niamod.models.eco.GameChange.Route;
import org.nia.niamod.models.eco.GameChange.Stat;
import org.nia.niamod.models.eco.GameChange.Tax;

import java.util.ArrayDeque;
import java.util.Locale;

public final class ActionQueue {
    private final ArrayDeque<GameChange> queue = new ArrayDeque<>();

    public synchronized void enqueue(GameChange change) {
        if (change != null) {
            queue.addLast(change);
        }
    }

    public void drainNext(GameActions gameActions) {
        GameChange next;
        synchronized (this) {
            next = queue.pollFirst();
        }
        if (next != null && gameActions != null) {
            gameActions.apply(next);
        }
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean hasQueuedStat(String territoryKey, TerritoryUpgrade upgrade) {
        if (upgrade == null) {
            return false;
        }
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> change instanceof Stat stat
                && normalize(stat.territoryName()).equals(key)
                && stat.upgrade() == upgrade);
    }

    public synchronized boolean hasQueuedTax(String territoryKey) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> change instanceof Tax tax
                && (tax.global() || normalize(tax.territoryName()).equals(key)));
    }

    public synchronized boolean hasQueuedBorders(String territoryKey) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> change instanceof Borders borders
                && (borders.global() || normalize(borders.territoryName()).equals(key)));
    }

    public synchronized boolean hasQueuedRoute(String territoryKey, TerritoryRoute route) {
        String key = normalize(territoryKey);
        return queue.stream().anyMatch(change -> change instanceof Route routeChange
                && (routeChange.global()
                || (normalize(routeChange.territoryName()).equals(key)
                && (route == null || routeChange.route() == route))));
    }

    public synchronized boolean hasQueuedHeadquarters() {
        return queue.stream().anyMatch(change -> change instanceof Headquarters);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

}

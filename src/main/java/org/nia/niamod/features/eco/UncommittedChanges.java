package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.TerritoryUpgrade;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UncommittedChanges {
    private final Map<String, EnumSet<TerritoryUpgrade>> stats = new HashMap<>();
    private final Set<String> taxes = new HashSet<>();
    private final Set<String> borders = new HashSet<>();
    private final Set<String> routes = new HashSet<>();
    private String headquarters;

    public void markStat(String territoryKey, TerritoryUpgrade stat) {
        if (!territoryKey.isEmpty() && stat != null) {
            stats.computeIfAbsent(territoryKey, ignored -> EnumSet.noneOf(TerritoryUpgrade.class)).add(stat);
        }
    }

    public void markAllStats(String territoryKey) {
        if (!territoryKey.isEmpty()) {
            stats.put(territoryKey, EnumSet.allOf(TerritoryUpgrade.class));
        }
    }

    public void markTax(String territoryKey) {
        mark(taxes, territoryKey);
    }

    public void markBorders(String territoryKey) {
        mark(borders, territoryKey);
    }

    public void markRoute(String territoryKey) {
        mark(routes, territoryKey);
    }

    public void markHeadquarters(String territoryKey) {
        headquarters = territoryKey.isEmpty() ? null : territoryKey;
    }

    public boolean hasStat(String territoryKey, TerritoryUpgrade stat) {
        EnumSet<TerritoryUpgrade> pendingStats = stats.get(territoryKey);
        return pendingStats != null && pendingStats.contains(stat);
    }

    public boolean hasTax(String territoryKey) {
        return taxes.contains(territoryKey);
    }

    public boolean hasBorders(String territoryKey) {
        return borders.contains(territoryKey);
    }

    public boolean hasRoute(String territoryKey) {
        return routes.contains(territoryKey);
    }

    public boolean hasHeadquarters() {
        return headquarters != null;
    }

    public String headquarters() {
        return headquarters == null ? "" : headquarters;
    }

    public void clearStat(String territoryKey, TerritoryUpgrade stat) {
        EnumSet<TerritoryUpgrade> pendingStats = stats.get(territoryKey);
        if (pendingStats == null) {
            return;
        }
        pendingStats.remove(stat);
        if (pendingStats.isEmpty()) {
            stats.remove(territoryKey);
        }
    }

    public void clearTax(String territoryKey) {
        taxes.remove(territoryKey);
    }

    public void clearBorders(String territoryKey) {
        borders.remove(territoryKey);
    }

    public void clearRoute(String territoryKey) {
        routes.remove(territoryKey);
    }

    public void clearHeadquarters() {
        headquarters = null;
    }

    public Map<String, Set<TerritoryUpgrade>> stats() {
        Map<String, Set<TerritoryUpgrade>> copy = new HashMap<>();
        stats.forEach((territory, pendingStats) -> copy.put(territory, Collections.unmodifiableSet(EnumSet.copyOf(pendingStats))));
        return Collections.unmodifiableMap(copy);
    }

    public Set<String> taxes() {
        return Set.copyOf(taxes);
    }

    public Set<String> borders() {
        return Set.copyOf(borders);
    }

    public Set<String> routes() {
        return Set.copyOf(routes);
    }

    private void mark(Set<String> values, String territoryKey) {
        if (!territoryKey.isEmpty()) {
            values.add(territoryKey);
        }
    }
}

package org.nia.niamod.models.eco;

import java.util.Map;

public record TerritoryDetails(
        TerritoryNode territory,
        String guildName,
        Map<TerritoryUpgrade, Integer> upgradeLevels,
        TerritoryResourceStore resourceStore,
        Resources producedResources,
        DamageRange damage,
        double attackSpeed,
        double health,
        double defense,
        int ownedConnections,
        int totalConnections,
        int externalConnections,
        int taxPercent,
        boolean headquarters,
        boolean bordersOpen,
        String routeLabel,
        String loadoutName
) {
    public TerritoryDetails {
        guildName = guildName == null || guildName.isBlank() ? "Guild" : guildName;
        upgradeLevels = upgradeLevels == null ? Map.of() : Map.copyOf(upgradeLevels);
        resourceStore = resourceStore == null ? TerritoryResourceStore.EMPTY : resourceStore;
        producedResources = producedResources == null ? Resources.EMPTY : producedResources;
        damage = damage == null ? DamageRange.EMPTY : damage;
        routeLabel = routeLabel == null || routeLabel.isBlank() ? "Fastest" : routeLabel;
        loadoutName = loadoutName == null ? "" : loadoutName.trim();
    }

    public record DamageRange(double min, double max) {
        public static final DamageRange EMPTY = new DamageRange(0.0, 0.0);
    }
}

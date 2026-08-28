package org.nia.niamod.models.eco;

import java.util.EnumMap;

public record TerritoryLoadout(String name, EnumMap<TerritoryUpgrade, Integer> levels) {
    public TerritoryLoadout {
        name = name == null || name.isBlank() ? "Loadout" : name.trim();
        EnumMap<TerritoryUpgrade, Integer> copy = new EnumMap<>(TerritoryUpgrade.class);
        if (levels != null) {
            copy.putAll(levels);
        }
        levels = copy;
    }
}

package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.TerritoryLoadout;
import org.nia.niamod.models.eco.TerritoryUpgrade;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Loadouts {
    private static final List<TerritoryLoadout> DEFAULTS = List.of(
            loadout("MAX", Map.of(
                    TerritoryUpgrade.DAMAGE, 11,
                    TerritoryUpgrade.ATTACK, 11,
                    TerritoryUpgrade.HEALTH, 11,
                    TerritoryUpgrade.DEFENSE, 11,
                    TerritoryUpgrade.TOWER_AURA, 3,
                    TerritoryUpgrade.TOWER_VOLLEY, 3,
                    TerritoryUpgrade.STRONGER_MINIONS, 4,
                    TerritoryUpgrade.TOWER_MULTI_ATTACKS, 1
            ))
    );

    private Loadouts() {
    }

    public static List<TerritoryLoadout> defaults() {
        return DEFAULTS;
    }

    private static TerritoryLoadout loadout(String name, Map<TerritoryUpgrade, Integer> levels) {
        EnumMap<TerritoryUpgrade, Integer> copy = new EnumMap<>(TerritoryUpgrade.class);
        copy.putAll(levels);
        return new TerritoryLoadout(name, copy);
    }
}

package org.nia.niamod.models.defense;

import com.wynntils.models.territories.type.TerritoryUpgrade;

import java.util.List;
import java.util.Map;

public record CachedDefenseEstimate(Map<TerritoryUpgrade, Integer> defenses, List<String> stats) {
    public static final CachedDefenseEstimate EMPTY = new CachedDefenseEstimate(Map.of(), List.of());
}

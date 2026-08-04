package org.nia.niamod.models.defense;

import com.wynntils.models.territories.type.TerritoryUpgrade;

import java.util.List;
import java.util.Map;

public record DefenseEstimate(
        Map<TerritoryUpgrade, Integer> defenses,
        List<String> stats,
        int queueTime) {
    public static final DefenseEstimate EMPTY = new DefenseEstimate(
            Map.of(),
            List.of(),
            2
    );
}

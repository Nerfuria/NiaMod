package org.nia.niamod.models.records;

public record TerritoryCombatStats(
        double minDamage,
        double maxDamage,
        double attackSpeed,
        double health,
        double defence,
        double averageDps,
        double ehp
) {
}

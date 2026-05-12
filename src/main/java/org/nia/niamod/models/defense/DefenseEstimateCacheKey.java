package org.nia.niamod.models.defense;

public record DefenseEstimateCacheKey(
        int emeralds,
        int ore,
        int crops,
        int fish,
        int wood,
        int mapTick,
        String territoryName
) {
}

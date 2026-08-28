package org.nia.niamod.models.eco;

public record TerritoryResourceStore(Resources current, Resources max) {
    public static final TerritoryResourceStore EMPTY = new TerritoryResourceStore(Resources.EMPTY, Resources.EMPTY);

    public TerritoryResourceStore {
        current = current == null ? Resources.EMPTY : current;
        max = max == null ? Resources.EMPTY : max;
        current = new Resources(
                clamp(current.emeralds(), max.emeralds()),
                clamp(current.ore(), max.ore()),
                clamp(current.crops(), max.crops()),
                clamp(current.fish(), max.fish()),
                clamp(current.wood(), max.wood())
        );
    }

    private static long clamp(long current, long max) {
        current = Math.max(0L, current);
        max = Math.max(0L, max);
        if (max > 0L && current > max) {
            return max;
        }
        return current;
    }
}

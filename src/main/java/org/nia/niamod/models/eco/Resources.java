package org.nia.niamod.models.eco;

public record Resources(long emeralds, long ore, long crops, long fish, long wood) {
    public static final Resources EMPTY = new Resources(0L, 0L, 0L, 0L, 0L);

    public static Resources of(ResourceKind kind, long amount) {
        long safeAmount = Math.max(0L, amount);
        return switch (kind) {
            case EMERALDS -> new Resources(safeAmount, 0L, 0L, 0L, 0L);
            case ORE -> new Resources(0L, safeAmount, 0L, 0L, 0L);
            case CROPS -> new Resources(0L, 0L, safeAmount, 0L, 0L);
            case FISH -> new Resources(0L, 0L, 0L, safeAmount, 0L);
            case WOOD -> new Resources(0L, 0L, 0L, 0L, safeAmount);
            case ALL -> new Resources(safeAmount, safeAmount, safeAmount, safeAmount, safeAmount);
            case NONE -> EMPTY;
        };
    }

    public Resources plus(Resources other) {
        if (other == null) {
            return this;
        }
        return new Resources(
                emeralds + other.emeralds,
                ore + other.ore,
                crops + other.crops,
                fish + other.fish,
                wood + other.wood
        );
    }

    public Resources minus(Resources other) {
        if (other == null) {
            return this;
        }
        return new Resources(
                emeralds - other.emeralds,
                ore - other.ore,
                crops - other.crops,
                fish - other.fish,
                wood - other.wood
        );
    }

    public ResourceKind kind() {
        boolean hasCrops = crops > 0;
        boolean hasWood = wood > 0;
        boolean hasOre = ore > 0;
        boolean hasFish = fish > 0;
        if (hasCrops && hasWood && hasOre && hasFish) {
            return ResourceKind.ALL;
        }

        long max = Math.max(Math.max(ore, crops), Math.max(wood, fish));
        if (max <= 0) {
            return ResourceKind.NONE;
        }
        for (ResourceKind resource : ResourceKind.MATERIALS) {
            if (amount(resource) == max) {
                return resource;
            }
        }
        return ResourceKind.FISH;
    }

    public long amount(ResourceKind kind) {
        return switch (kind) {
            case EMERALDS -> emeralds;
            case ORE -> ore;
            case CROPS -> crops;
            case FISH -> fish;
            case WOOD -> wood;
            case ALL -> total();
            case NONE -> 0L;
        };
    }

    public long materialTotal() {
        return ore + crops + fish + wood;
    }

    public long total() {
        return emeralds + materialTotal();
    }

    public boolean empty() {
        return total() <= 0L;
    }
}

package org.nia.niamod.models.eco;

import java.util.List;

public record TerritoryNode(
        String name,
        int minX,
        int minZ,
        int maxX,
        int maxZ,
        long acquiredMillis,
        Resources resources,
        List<String> connections
) {
    private static final long FRESH_HOLD_MILLIS = 10L * 60_000L;

    public double centerX() {
        return (minX + maxX) / 2.0;
    }

    public double centerZ() {
        return (minZ + maxZ) / 2.0;
    }

    public int worldWidth() {
        return Math.max(1, maxX - minX);
    }

    public int worldHeight() {
        return Math.max(1, maxZ - minZ);
    }

    public boolean heldUnderTenMinutes(long now) {
        long heldMillis = now - acquiredMillis;
        return acquiredMillis > 0 && heldMillis >= 0 && heldMillis < FRESH_HOLD_MILLIS;
    }

    public long millisUntilAttackable(long now) {
        if (acquiredMillis <= 0) {
            return 0L;
        }
        return Math.max(0L, acquiredMillis + FRESH_HOLD_MILLIS - now);
    }

    public ResourceKind resourceKind() {
        return resources.kind();
    }

    public int resourceColor() {
        return TerritoryResourceColors.configuredColor(resourceKind());
    }

    public boolean isRainbow() {
        return resourceKind() == ResourceKind.ALL;
    }

    public boolean isCity() {
        return resources.emeralds() > 9000;
    }

    public String tag() {
        return resourceKind().label();
    }
}

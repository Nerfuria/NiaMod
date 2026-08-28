package org.nia.niamod.models.eco;

public record ResourceFlow(
        Resources stored,
        Resources capacity,
        Resources gainedPerHour,
        Resources usedPerHour,
        double materialUsagePercent
) {
    public static final ResourceFlow EMPTY = new ResourceFlow(
            Resources.EMPTY,
            Resources.EMPTY,
            Resources.EMPTY,
            Resources.EMPTY,
            0.0
    );

    public ResourceFlow {
        stored = stored == null ? Resources.EMPTY : stored;
        capacity = capacity == null ? Resources.EMPTY : capacity;
        gainedPerHour = gainedPerHour == null ? Resources.EMPTY : gainedPerHour;
        usedPerHour = usedPerHour == null ? Resources.EMPTY : usedPerHour;
    }

    public Resources netPerHour() {
        return gainedPerHour.minus(usedPerHour);
    }
}

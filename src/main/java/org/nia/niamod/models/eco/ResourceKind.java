package org.nia.niamod.models.eco;

import java.util.List;

public enum ResourceKind {
    EMERALDS("Emeralds"),
    ORE("Ore"),
    CROPS("Crops"),
    WOOD("Wood"),
    FISH("Fish"),
    ALL("All"),
    NONE("None");

    public static final List<ResourceKind> MATERIALS = List.of(ORE, CROPS, WOOD, FISH);
    public static final List<ResourceKind> DISPLAY_ORDER = List.of(EMERALDS, ORE, CROPS, WOOD, FISH);

    private final String label;

    ResourceKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

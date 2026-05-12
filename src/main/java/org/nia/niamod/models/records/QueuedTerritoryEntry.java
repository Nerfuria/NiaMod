package org.nia.niamod.models.records;

public record QueuedTerritoryEntry(Territory territory, int distanceSquared, long timerEnd) {
}

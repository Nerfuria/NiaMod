package org.nia.niamod.models.api;

public record TerritoryResponse(Guild guild, TerritoryLocation location, String acquired) {
    public record Guild(String name) {
    }
}
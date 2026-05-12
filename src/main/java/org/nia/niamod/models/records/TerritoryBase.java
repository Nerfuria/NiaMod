package org.nia.niamod.models.records;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.wynntils.models.territories.type.GuildResource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record TerritoryBase(
        Map<GuildResource, Integer> productions,
        List<String> connections
) {
    private static final Gson GSON = new Gson();

    public static TerritoryBase fromJson(JsonObject obj) {
        Map<GuildResource, Integer> resources = new EnumMap<>(GuildResource.class);
        JsonObject resJSON = obj.getAsJsonObject("resources");

        for (Map.Entry<String, JsonElement> e : resJSON.entrySet()) {
            resources.put(
                    GuildResource.fromName(e.getKey()),
                    e.getValue().getAsInt()
            );
        }

        List<String> connections = GSON.fromJson(
                obj.get("connections"),
                TypeToken.getParameterized(List.class, String.class).getType()
        );

        return new TerritoryBase(
                Map.copyOf(resources),
                List.copyOf(connections)
        );
    }

    public int getProduction(GuildResource resource) {
        return this.productions.getOrDefault(resource, 0);
    }
}

package org.nia.niamod.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.nia.niamod.models.records.TerritoryBase;
import org.nia.niamod.util.FileUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class TerritoryBaseManager {

    private static final String TERRITORY_FILE = "territory.json";
    private static final Gson GSON = new Gson();
    private static final Map<String, TerritoryBase> TERRITORIES = new HashMap<>();

    public static void init() {
        Type type = TypeToken.getParameterized(Map.class, String.class, JsonObject.class).getType();
        Map<String, JsonObject> root = GSON.fromJson(FileUtils.readFile(TERRITORY_FILE), type);
        TERRITORIES.clear();
        if (root == null) {
            return;
        }

        for (var entry : root.entrySet()) {
            if (!entry.getKey().equals("_meta")) {
                TERRITORIES.put(entry.getKey(), TerritoryBase.fromJson(entry.getValue()));
            }
        }
    }

    public static Map<String, TerritoryBase> getTerritories() {
        return java.util.Collections.unmodifiableMap(TERRITORIES);
    }

    public static TerritoryBase getTerritory(String name) {
        return TERRITORIES.get(name);
    }
}

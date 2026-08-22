package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.services.map.pois.TerritoryPoi;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.mixin.wynntils.TerritoryModelAccessor;
import org.nia.niamod.models.events.WynntilsTerritoryApiUpdateEvent;
import org.nia.niamod.util.TerritoryUtils;
import org.nia.niamod.util.WebUtils;

import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class TerritoryApiFeature extends Feature {
    private static final Gson gson = new Gson();
    private long territoryLastTick = 0;
    private int errors = 0;

    @Subscribe
    public void UpdateTerritories(WynntilsTerritoryApiUpdateEvent event) {
        if (this.enabled) {
            WebUtils.queryAPIAsyncH(NyahConfig.getData().getApiBase() + "guild/list/territory")
                    .thenApply(this::parseTerritoryApi)
                    .exceptionally(ex -> {
                        NiamodClient.LOGGER.error("Failed to fetch/parse territory API response", ex);
                        errors++;
                        if (errors >= 5) {
                            this.enabled = false;
                            NiamodClient.LOGGER.error("Too many exceptions in Territory Api Feature, disabling.");
                        }
                        return null;
                    });
        }
    }

    private HttpResponse<String> parseTerritoryApi(HttpResponse<String> data) {
        this.territoryLastTick = data.headers()
                .firstValue("territorylasttick")
                .map(value -> OffsetDateTime.parse(value.replace(' ', 'T')).toInstant().toEpochMilli())
                .orElseThrow(() -> new RuntimeException("Missing territorylasttick header"));

        JsonObject json = JsonParser.parseString(data.body()).getAsJsonObject();

        Map<String, TerritoryInfo> tempMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            tempMap.put(entry.getKey(), TerritoryUtils.territoryInfoFromJson(entry.getValue().getAsJsonObject()));
        }

        var territoryPoiMap = ((TerritoryModelAccessor) (Object) (Models.Territory)).getTerritoryPoiMap();
        for (Map.Entry<String, TerritoryInfo> entry : tempMap.entrySet()) {
            TerritoryProfile territoryProfile = Models.Territory.getTerritoryProfile(entry.getKey());
            if (territoryProfile != null) {
                territoryPoiMap.put(entry.getKey(), new TerritoryPoi(() -> Models.Territory.getTerritoryProfile(entry.getKey()), entry.getValue()));
            }
        }

        errors = 0;
        return data;
    }

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
    }
}

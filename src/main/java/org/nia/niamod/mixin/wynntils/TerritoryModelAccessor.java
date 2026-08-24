package org.nia.niamod.mixin.wynntils;

import com.google.gson.Gson;
import com.wynntils.models.territories.TerritoryModel;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.services.map.pois.TerritoryPoi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TerritoryModel.class)
public interface TerritoryModelAccessor {
    
    @Accessor("TERRITORY_PROFILE_GSON")
    Gson getTerritoryProfileGson();

    @Accessor("territoryPoiMap")
    Map<String, TerritoryPoi> getTerritoryPoiMap();

    @Accessor("territoryProfileMap")
    void setTerritoryProfileMap(Map<String, TerritoryProfile> territoryProfileMap);
}

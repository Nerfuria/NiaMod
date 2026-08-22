package org.nia.niamod.mixin.wynntils;

import com.wynntils.models.territories.TerritoryModel;
import com.wynntils.services.map.pois.TerritoryPoi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TerritoryModel.class)
public interface TerritoryModelAccessor {
    @Accessor("territoryPoiMap")
    Map<String, TerritoryPoi> getTerritoryPoiMap();
}

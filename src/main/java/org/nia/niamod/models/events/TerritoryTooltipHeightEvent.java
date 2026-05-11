package org.nia.niamod.models.events;

import com.wynntils.services.map.pois.TerritoryPoi;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public class TerritoryTooltipHeightEvent {
    private final TerritoryPoi territoryPoi;
    private float additionalHeight;

    public TerritoryTooltipHeightEvent(TerritoryPoi territoryPoi) {
        this.territoryPoi = territoryPoi;
    }

    public TerritoryPoi getTerritoryPoi() {
        return territoryPoi;
    }

    public float getAdditionalHeight() {
        return additionalHeight;
    }

    public void addHeight(float height) {
        additionalHeight += height;
    }
}

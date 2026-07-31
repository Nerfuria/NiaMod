package org.nia.niamod.models.events;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.wynntils.services.map.pois.TerritoryPoi;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public record HoveredTerritoryInfoRenderEvent(
        GuiGraphics guiGraphics,
        TerritoryPoi territoryPoi,
        float mapCenterX,
        float mapCenterZ,
        float centerX,
        float centerZ,
        float zoomRenderScale
) {
}

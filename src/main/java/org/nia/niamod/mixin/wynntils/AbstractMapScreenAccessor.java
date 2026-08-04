package org.nia.niamod.mixin.wynntils;

import com.wynntils.screens.maps.AbstractMapScreen;
import com.wynntils.services.map.pois.Poi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractMapScreen.class)
public interface AbstractMapScreenAccessor {

    @Accessor("mapCenterX")
    float getMapCenterX();

    @Accessor("mapCenterZ")
    float getMapCenterZ();

    @Accessor("centerX")
    float getCenterX();

    @Accessor("centerZ")
    float getCenterZ();

    @Accessor("zoomRenderScale")
    float getZoomRenderScale();

    @Accessor("hovered")
    Poi getHovered();
}

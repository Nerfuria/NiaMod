package org.nia.niamod.mixin.wynntils;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.services.map.pois.TerritoryPoi;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.HoveredTerritoryInfoRenderEvent;
import org.nia.niamod.models.events.TerritoryTooltipHeightEvent;
import org.nia.niamod.models.events.TerritoryTooltipRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuildMapScreen.class)
public class GuildMapScreenMixin {
    @Inject(method = "renderTerritoryTooltip", at = @At(ordinal = 4, value = "INVOKE", target = "Lcom/wynntils/utils/render/FontRenderer;renderText(Lnet/minecraft/client/gui/GuiGraphics;Lcom/wynntils/core/text/StyledText;FFLcom/wynntils/utils/colors/CustomColor;Lcom/wynntils/utils/render/type/HorizontalAlignment;Lcom/wynntils/utils/render/type/VerticalAlignment;Lcom/wynntils/utils/render/type/TextShadow;)V", shift = At.Shift.AFTER))
    private static void renderEstimatedDefenses(
            GuiGraphics guiGraphics,
            int xOffset,
            int yOffset,
            TerritoryPoi territoryPoi,
            CallbackInfo ci,
            @Local(name = "renderYOffset") LocalFloatRef renderYOffset) {
        NiaEventBus.dispatch(new TerritoryTooltipRenderEvent(guiGraphics, xOffset, yOffset, territoryPoi, renderYOffset));
    }

    @ModifyVariable(method = "renderTerritoryTooltip", at = @At("STORE"), name = "centerHeight")
    private static float increaseTooltipHeight(float centerHeight, @Local(argsOnly = true) TerritoryPoi territoryPoi) {
        TerritoryTooltipHeightEvent event = new TerritoryTooltipHeightEvent(territoryPoi);
        NiaEventBus.dispatch(event);
        return centerHeight + event.getAdditionalHeight();
    }

    @Inject(
            method = "doRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/screens/maps/GuildMapScreen;renderPois(Lnet/minecraft/client/gui/GuiGraphics;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void renderHoveredTerritoryPaths(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci) {
        AbstractMapScreenAccessor accessor = (AbstractMapScreenAccessor) this;
        if (!(accessor.getHovered() instanceof TerritoryPoi territoryPoi)) {
            return;
        }

        float mapCenterX = accessor.getMapCenterX();
        float mapCenterZ = accessor.getMapCenterZ();
        float centerX = accessor.getCenterX();
        float centerZ = accessor.getCenterZ();
        float zoomRenderScale = accessor.getZoomRenderScale();

        NiaEventBus.dispatch(new HoveredTerritoryInfoRenderEvent(
                guiGraphics,
                territoryPoi,
                mapCenterX,
                mapCenterZ,
                centerX,
                centerZ,
                zoomRenderScale
        ));
    }
}

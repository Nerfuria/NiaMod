package org.nia.niamod.mixin.wynntils;

import com.wynntils.models.territories.TerritoryModel;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.WynntilsTerritoryApiUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TerritoryModel.class)
public class TerritoryModelMixin {

    @Inject(method = "updateTerritoryProfileMap", at = @At("RETURN"))
    private void afterTerritoryProfileUpdate(CallbackInfo ci) {
        // Note the method runs an async download so this actually runs before...
        // Can't be bothered tho since wynntils will add an event for this soon.
        WynntilsTerritoryApiUpdateEvent event = new WynntilsTerritoryApiUpdateEvent();
        NiaEventBus.dispatch(event);
    }
}

package org.nia.niamod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.BossBarNameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BossEvent.class)
public class BossBarHudMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Component onInit(Component value) {
        BossBarNameEvent event = new BossBarNameEvent(value);
        NiaEventBus.dispatch(event);
        return event.getTitle();
    }

    @ModifyVariable(method = "setName", at = @At("HEAD"), argsOnly = true)
    private Component setName(Component value) {
        BossBarNameEvent event = new BossBarNameEvent(value);
        NiaEventBus.dispatch(event);
        return event.getTitle();
    }
}
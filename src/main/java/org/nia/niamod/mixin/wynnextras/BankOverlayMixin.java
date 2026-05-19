package org.nia.niamod.mixin.wynnextras;

import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BankOverlay2.class)
public class BankOverlayMixin {

    @Inject(method = "renderItemOverlays", at = @At("RETURN"))
    private static void renderOverlay(GuiGraphics context, ItemStack stack, int x, int y, CallbackInfo ci) {
        NiaEventBus.dispatch(new SlotRenderEvent(context, stack, x, y));
    }

    @Surrogate
    private static void renderOverlay(GuiGraphics context, ItemStack stack, int x, int y,
                                      Optional<?> item, CallbackInfo ci) {
        NiaEventBus.dispatch(new SlotRenderEvent(context, stack, x, y));
    }

}

package org.nia.niamod.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    private ItemStack draggingItem;

    @Inject(method = "renderSlot", at = @At("RETURN"))
    public void renderSlot(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        NiaEventBus.dispatch(new SlotRenderEvent(context, slot.getItem(), slot.x, slot.y));
    }

    @Inject(method = "renderCarriedItem", at = @At("HEAD"))
    public void refreshCarriedConsumable(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        ConsuTextFeature.updateConsumableMetadata(carriedStack());
    }

    @Inject(method = "renderCarriedItem", at = @At("RETURN"))
    public void renderCarriedConsumableLabel(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = carriedStack();
        int yOffset = draggingItem.isEmpty() ? 8 : 16;
        ConsuTextFeature.renderFloatingLabel(context, stack, mouseX - 8, mouseY - yOffset);
    }

    private ItemStack carriedStack() {
        return draggingItem.isEmpty() ? menu.getCarried() : draggingItem;
    }
}

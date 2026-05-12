package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.nia.niamod.gui.render.GuiRenderTargetOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @WrapOperation(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
            )
    )
    private RenderTarget niamod$overrideGuiRenderTarget(Minecraft instance, Operation<RenderTarget> original) {
        RenderTarget override = GuiRenderTargetOverride.get();
        return override != null ? override : original.call(instance);
    }
}

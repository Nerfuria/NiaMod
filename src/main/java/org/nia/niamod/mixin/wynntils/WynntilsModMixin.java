package org.nia.niamod.mixin.wynntils;

import com.wynntils.core.WynntilsMod;
import org.nia.niamod.NiamodClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WynntilsMod.class)
public class WynntilsModMixin {

    @Inject(
            method = "initFeatures",
            at = @At("HEAD")
    )
    private static void beforeInitFeatures(CallbackInfo ci) {
        NiamodClient.beforeWynntilsInitFeatures();
    }
}
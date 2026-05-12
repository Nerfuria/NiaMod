package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GuiRenderTargetOverride {
    private static final ThreadLocal<RenderTarget> CURRENT = new ThreadLocal<>();

    public static RenderTarget get() {
        return CURRENT.get();
    }

    public static GuiRenderTargetScope push(RenderTarget renderTarget) {
        RenderTarget previous = CURRENT.get();
        CURRENT.set(renderTarget);
        return new GuiRenderTargetScope(previous);
    }

    static void restore(RenderTarget previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}

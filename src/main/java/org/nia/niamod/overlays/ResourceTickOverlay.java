package org.nia.niamod.overlays;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.models.render.TextOverlay;

import java.util.function.IntSupplier;

@RequiredArgsConstructor
public class ResourceTickOverlay implements TextOverlay {
    private final IntSupplier timeUntilResourceTick;

    @Override
    public String defaultValue() {
        return "-1";
    }

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        drawCenteredText(drawContext, Minecraft.getInstance(), timeUntilResourceTick.getAsInt() + "s", 0, 0, 0xFFFFFFFF);
    }

    @Override
    public int getXOffset() {
        return NyahConfig.getData().getResTickOverlayOffsetX();
    }

    @Override
    public void setXOffset(int xOffset) {
        NyahConfig.getData().setResTickOverlayOffsetX(xOffset);
        NyahConfig.save();
    }

    @Override
    public int getYOffset() {
        return NyahConfig.getData().getResTickOverlayOffsetY();
    }

    @Override
    public void setYOffset(int yOffset) {
        NyahConfig.getData().setResTickOverlayOffsetY(yOffset);
        NyahConfig.save();
    }

    @Override
    public float getScale() {
        return NyahConfig.getData().getResTickOverlayScale();
    }

    @Override
    public void setScale(float scale) {
        NyahConfig.getData().setResTickOverlayScale(scale);
        NyahConfig.save();
    }

    @Override
    public boolean isEnabled() {
        return NyahConfig.getData().isResourceTickFeatureEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        NyahConfig.getData().setResourceTickFeatureEnabled(enabled);
        NyahConfig.save();

        if (FeatureManager.getResTickFeature() != null) {
            FeatureManager.getResTickFeature().setEnabled(enabled);
        }
    }
}

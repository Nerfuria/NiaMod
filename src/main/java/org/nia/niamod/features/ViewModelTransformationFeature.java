package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.HeldItemBobbingEvent;
import org.nia.niamod.models.events.HeldItemRenderEvent;

@SuppressWarnings("unused")
public class ViewModelTransformationFeature extends Feature {
    @Override
    public void init() {
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    public void modifyRender(HeldItemRenderEvent event) {
        if (event.hand() == InteractionHand.MAIN_HAND) {
            var config = NyahConfig.getData();
            event
                    .matrix()
                    .mulPose(
                            new Matrix4f()
                                    .translate(
                                            config.getXOffset() / 100f,
                                            config.getYOffset() / 100f,
                                            config.getZOffset() / 100f)
                                    .rotateX((float) Math.toRadians(config.getXRotation()))
                                    .rotateY((float) Math.toRadians(config.getYRotation()))
                                    .rotateZ((float) Math.toRadians(config.getZRotation()))
                                    .scale(config.getItemScale()));
        }
    }

    @Subscribe
    public void onHeldItemBobbing(HeldItemBobbingEvent event) {
        if (!NyahConfig.getData().isDisableHeldBobbing()) {
            return;
        }
        Minecraft mc = event.minecraft();
        if (mc == null || mc.player == null || mc.gameMode == null) {
            return;
        }
        if (!mc.options.bobView().get()) {
            return;
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (event.sleeping()) {
            return;
        }
        if (mc.options.hideGui) {
            return;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        int light =
                mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, event.tickProgress());
        event
                .itemInHandRenderer()
                .renderHandsWithItems(
                        event.tickProgress(),
                        event.matrixStack(),
                        mc.gameRenderer.getSubmitNodeStorage(),
                        mc.player,
                        light);
        event.cancel();
    }
}

package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record GradientQuad(Matrix3x2f pose, int left, int right, int top, int bottom, ScreenRectangle scissorArea,
                           int hueColor) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer vertices) {
        vertices.addVertexWith2DPose(pose, left, top).setColor(0xFFFFFFFF);
        vertices.addVertexWith2DPose(pose, left, bottom).setColor(0xFFFFFFFF);
        vertices.addVertexWith2DPose(pose, right, bottom).setColor(hueColor);
        vertices.addVertexWith2DPose(pose, right, top).setColor(hueColor);
        vertices.addVertexWith2DPose(pose, left, top).setColor(0x00000000);
        vertices.addVertexWith2DPose(pose, left, bottom).setColor(0xFF000000);
        vertices.addVertexWith2DPose(pose, right, bottom).setColor(0xFF000000);
        vertices.addVertexWith2DPose(pose, right, top).setColor(0x00000000);
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return net.minecraft.client.renderer.RenderPipelines.GUI;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return scissorArea().intersection(new ScreenRectangle(left(), top(), right() - left(), bottom() - top()));
    }
}

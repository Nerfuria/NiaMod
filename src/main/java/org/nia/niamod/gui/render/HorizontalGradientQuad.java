package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record HorizontalGradientQuad(
        Matrix3x2f pose,
        int left,
        int right,
        int top,
        int bottom,
        ScreenRectangle scissorArea,
        int leftColor,
        int rightColor
) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertices) {
        vertices.addVertexWith2DPose(pose, left, top).setColor(leftColor);
        vertices.addVertexWith2DPose(pose, left, bottom).setColor(leftColor);
        vertices.addVertexWith2DPose(pose, right, bottom).setColor(rightColor);
        vertices.addVertexWith2DPose(pose, right, top).setColor(rightColor);
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        ScreenRectangle rect = new ScreenRectangle(left, top, right - left, bottom - top);
        return scissorArea == null ? rect : scissorArea.intersection(rect);
    }
}
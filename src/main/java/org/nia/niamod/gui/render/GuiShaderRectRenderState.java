package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

public final class GuiShaderRectRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int color;
    private final int param0;
    private final int param1;
    private final int guiWidth;
    private final int guiHeight;
    private final ScreenRectangle bounds;
    private final ScreenRectangle scissorArea;

    public GuiShaderRectRenderState(
            RenderPipeline pipeline,
            Matrix3x2fc pose,
            int x,
            int y,
            int width,
            int height,
            int color,
            int param0,
            int param1,
            int guiWidth,
            int guiHeight,
            ScreenRectangle scissorArea
    ) {
        this.pipeline = pipeline;
        this.textureSetup = TextureSetup.noTexture();
        this.pose = new Matrix3x2f(pose);
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.color = color;
        this.param0 = param0;
        this.param1 = param1;
        this.guiWidth = Math.max(1, guiWidth);
        this.guiHeight = Math.max(1, guiHeight);
        this.bounds = new ScreenRectangle(x, y, this.width, this.height).transformAxisAligned(this.pose);
        this.scissorArea = scissorArea;
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer consumer) {
        submitVertex(consumer, x, y, 0.0f, 0.0f);
        submitVertex(consumer, x + width, y, width, 0.0f);
        submitVertex(consumer, x + width, y + height, width, height);
        submitVertex(consumer, x, y + height, 0.0f, height);
    }

    private void submitVertex(VertexConsumer consumer, float px, float py, float localX, float localY) {
        Vector2f transformed = pose.transformPosition(px, py, new Vector2f());
        float clipX = transformed.x() / guiWidth * 2.0f - 1.0f;
        float clipY = 1.0f - transformed.y() / guiHeight * 2.0f;

        consumer.addVertex(clipX, clipY, 0.0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .setUv(localX, localY)
                .setUv1(width, height)
                .setUv2(param0, param1)
                .setNormal(0.0f, 0.0f, 1.0f);
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }
}

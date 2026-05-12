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

public final class GuiPortalRectRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int sourceX;
    private final int sourceY;
    private final int sourceWidth;
    private final int sourceHeight;
    private final int color;
    private final float progress;
    private final float seedX;
    private final float seedY;
    private final int guiWidth;
    private final int guiHeight;
    private final ScreenRectangle bounds;
    private final ScreenRectangle scissorArea;

    public GuiPortalRectRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int color,
            float progress,
            float seedX,
            float seedY,
            int guiWidth,
            int guiHeight,
            ScreenRectangle scissorArea
    ) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = new Matrix3x2f(pose);
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceWidth = Math.max(1, sourceWidth);
        this.sourceHeight = Math.max(1, sourceHeight);
        this.color = color;
        this.progress = progress;
        this.seedX = seedX;
        this.seedY = seedY;
        this.guiWidth = Math.max(1, guiWidth);
        this.guiHeight = Math.max(1, guiHeight);
        this.bounds = new ScreenRectangle(x, y, this.width, this.height).transformAxisAligned(this.pose);
        this.scissorArea = scissorArea;
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer consumer) {
        submitVertex(consumer, x, y, 0.0f, 0.0f);
        submitVertex(consumer, x + width, y, 1.0f, 0.0f);
        submitVertex(consumer, x + width, y + height, 1.0f, 1.0f);
        submitVertex(consumer, x, y + height, 0.0f, 1.0f);
    }

    private void submitVertex(VertexConsumer consumer, float px, float py, float u, float v) {
        Vector2f transformed = pose.transformPosition(px, py, new Vector2f());
        float clipX = transformed.x() / guiWidth * 2.0f - 1.0f;
        float clipY = 1.0f - transformed.y() / guiHeight * 2.0f;

        consumer.addVertex(clipX, clipY, 0.0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .setUv(u, v)
                .setUv1(sourceWidth, sourceHeight)
                .setUv2(sourceX, sourceY)
                .setNormal(progress, seedX, seedY);
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

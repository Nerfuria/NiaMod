package org.nia.niamod.gui.render;

public final class RenderTarget extends com.mojang.blaze3d.pipeline.RenderTarget implements AutoCloseable {
    public RenderTarget(String label, int width, int height, boolean useDepth) {
        super(label, useDepth);
        createBuffers(width, height);
    }

    @Override
    public void close() {
        destroyBuffers();
    }
}

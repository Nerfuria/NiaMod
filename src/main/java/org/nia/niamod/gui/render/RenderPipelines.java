package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;

@UtilityClass
public class RenderPipelines {
    private static final String NAMESPACE = "niamod";
    public static final RenderPipeline GUI_ROUNDED_RECT = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_rounded_rect"))
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_shape"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_rounded_rect"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline GUI_PORTAL_OVERLAY = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_portal_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_shape"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_portal_overlay"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline GUI_PORTAL_CAPTURE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_portal_capture"))
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_portal"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_portal_capture"))
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline GUI_INCINERATE_CAPTURE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_incinerate_capture"))
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_portal"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_incinerate_capture"))
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline GUI_MUSHROOM_CAPTURE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_mushroom_capture"))
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_portal"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/gui_mushroom_capture"))
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline GUI_PORTAL_SNAPSHOT_BLIT = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/gui_portal_snapshot_blit"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "post/gui_portal_snapshot_blit"))
            .withSampler("InSampler")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

}

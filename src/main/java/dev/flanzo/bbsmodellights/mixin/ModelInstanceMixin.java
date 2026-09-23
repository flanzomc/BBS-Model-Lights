package dev.flanzo.bbsmodellights.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.flanzo.bbsmodellights.client.Effects;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * CML-style positive glow fallback for the render paths that do not use the
 * BBS model shader.
 *
 * Stock BBS FS intentionally renders CPU/non-VAO models with Minecraft's
 * entity_translucent shader. Iris/shader-pack world rendering also bypasses
 * BBSShaders.getModel(). A GlowingColor uniform therefore cannot cover those
 * paths by itself. CML solves this class of problem with full-bright additive
 * emission redraws; this mixin applies the same principle at ModelInstance,
 * which is the common FS path for both VAO and CPU models.
 */
@Mixin(value = ModelInstance.class, remap = false)
public abstract class ModelInstanceMixin {
    @Unique private static boolean bml$emittingGlow;

    @Inject(method = "render", at = @At("RETURN"))
    private void bml$renderPositiveGlow(
        MatrixStack stack,
        Supplier<ShaderProgram> program,
        Color color,
        int light,
        int overlay,
        StencilMap stencilMap,
        ShapeKeys shapeKeys,
        CallbackInfo ci
    ) {
        if (bml$emittingGlow || stencilMap != null || Effects.current == null || Effects.shader == null) {
            return;
        }

        float intensity = Effects.current.values().glow.intensity.get();

        if (intensity <= 0.001F) {
            return;
        }

        float alpha = color == null ? 1F : color.a;

        if (alpha <= 0.001F) {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean savedPolygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);

        /*
         * White here is intentional. The shader's emission-only branch uses
         * rawVertexColor.rgb, so per-bone/group color still tints the glow,
         * while the model texture contributes only its alpha silhouette.
         */
        Color emission = new Color();
        emission.r = 1F;
        emission.g = 1F;
        emission.b = 1F;
        emission.a = alpha;

        bml$emittingGlow = true;
        Effects.glowOverlay = true;

        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.depthMask(false);

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1F, -1F);

            Effects.glowOverlayDraws++;

            ((ModelInstance) (Object) this).render(
                stack,
                () -> Effects.shader,
                emission,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                overlay,
                null,
                shapeKeys
            );
        } finally {
            Effects.glowOverlay = false;
            bml$emittingGlow = false;

            GL11.glPolygonOffset(0F, 0F);

            if (!savedPolygonOffset) {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.defaultBlendFunc();

            if (savedBlend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
        }
    }
}

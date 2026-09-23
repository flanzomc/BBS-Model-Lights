package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.LightSettings;
import dev.flanzo.bbsmodellights.client.Effects;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Ports current CML's ModelForm Glow routing onto FS 1.20.4.
 *
 * CML does NOT redraw every positive glow additively. Normal VAO and
 * shape-key CPU models use model.fsh directly. Unsupported CPU models use a
 * vertex-color/full-light fallback. Iris world keeps the shader-pack base pass
 * and bakes unmasked Glow into that base color so pack bloom can see it.
 */
@Mixin(value = ModelInstance.class, remap = false)
public abstract class ModelInstanceMixin {
    @Unique
    private Matrix4f bml$previousRootInverse;

    @Unique
    private boolean bml$rootStateActive;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private Supplier<ShaderProgram> bml$selectGlowShader(Supplier<ShaderProgram> original) {
        if (Effects.current == null || Effects.shader == null || bml$isIrisWorld()) {
            return original;
        }

        ModelInstance self = (ModelInstance) (Object) this;

        /*
         * This is current CML's supportsBbsModelShaderEffects contract:
         * VAOs already arrive through BBSShaders#getModel; shape-key CPU geometry
         * must also use the model shader instead of vanilla entity_translucent.
         */
        if (!self.isVAORendered() && bml$hasShapeKeys(self)) {
            return () -> Effects.shader;
        }

        return original;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private Color bml$applyGlowFallback(Color original) {
        if (Effects.current == null || original == null || Effects.hasGlowTransform()) {
            return original;
        }

        LightSettings settings = Effects.current.values();
        float intensity = settings.glow.intensity.get();

        if (Math.abs(intensity) < 0.001F) {
            return original;
        }

        ModelInstance self = (ModelInstance) (Object) this;
        boolean shaderBacked = self.isVAORendered() || bml$hasShapeKeys(self);
        boolean irisWorld = bml$isIrisWorld();

        /*
         * CML fallback cases:
         *  1) unsupported CPU/no-GlowingColor path;
         *  2) Iris base pass, where positive/negative unmasked Glow must enter
         *     the shader-pack entity pass rather than a post-composite overlay.
         */
        if (!irisWorld && shaderBacked) {
            return original;
        }

        Color result = original.copy();
        Color glow = settings.glow.color.get();

        if (intensity > 0F) {
            result.r += MathUtils.clamp(glow.r, 0F, 1F) * intensity;
            result.g += MathUtils.clamp(glow.g, 0F, 1F) * intensity;
            result.b += MathUtils.clamp(glow.b, 0F, 1F) * intensity;
        } else {
            float factor = Math.max(0F, 1F + intensity);

            result.r *= factor;
            result.g *= factor;
            result.b *= factor;
        }

        Effects.glowFallbackApplications++;

        return result;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int bml$applyGlowFallbackLight(int original) {
        if (Effects.current == null || Effects.hasGlowTransform() || bml$isIrisWorld()) {
            return original;
        }

        ModelInstance self = (ModelInstance) (Object) this;

        if (self.isVAORendered() || bml$hasShapeKeys(self)) {
            return original;
        }

        float intensity = Effects.current.values().glow.intensity.get();

        if (Math.abs(intensity) < 0.001F) {
            return original;
        }

        float t = MathUtils.clamp(Math.abs(intensity), 0F, 1F);
        int block = original & 0xffff;
        int sky = original >> 16 & 0xffff;
        int boosted = (int) Lerps.lerp(block, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, t);

        return boosted | sky << 16;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void bml$beginModelSpace(
        MatrixStack stack,
        Supplier<ShaderProgram> program,
        Color color,
        int light,
        int overlay,
        StencilMap stencilMap,
        ShapeKeys shapeKeys,
        Function<String, Link> textureResolver,
        CallbackInfo ci
    ) {
        if (Effects.current == null || stencilMap != null) {
            return;
        }

        ModelInstance self = (ModelInstance) (Object) this;

        this.bml$previousRootInverse = new Matrix4f(Effects.renderRootInverse);
        this.bml$rootStateActive = true;

        if (self.isVAORendered()) {
            /* CML: VAO Position is already form/model space. */
            Effects.renderRootInverse.identity();
        } else if (bml$hasShapeKeys(self)) {
            /*
             * CML CPU shape-key renderer writes Position after applying stack.
             * Undo that stack for GlowEffect mask coordinates.
             */
            Effects.renderRootInverse.set(stack.peek().getPositionMatrix()).invert();
        } else {
            Effects.renderRootInverse.identity();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bml$endModelSpace(
        MatrixStack stack,
        Supplier<ShaderProgram> program,
        Color color,
        int light,
        int overlay,
        StencilMap stencilMap,
        ShapeKeys shapeKeys,
        Function<String, Link> textureResolver,
        CallbackInfo ci
    ) {
        if (!this.bml$rootStateActive) {
            return;
        }

        Effects.renderRootInverse.set(this.bml$previousRootInverse);
        this.bml$previousRootInverse = null;
        this.bml$rootStateActive = false;
    }

    @Unique
    private static boolean bml$hasShapeKeys(ModelInstance model) {
        return model != null
            && model.model != null
            && model.model.getShapeKeys() != null
            && !model.model.getShapeKeys().isEmpty();
    }

    @Unique
    private static boolean bml$isIrisWorld() {
        return BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
    }
}

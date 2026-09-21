package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.LightSettings;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.renderers.BlockFormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Breaking overlay adapted from CML BlockFormRenderer (see LICENSE). */
@Mixin(value=BlockFormRenderer.class, remap=false)
public abstract class BlockRendererMixin extends FormRenderer<BlockForm> {
    protected BlockRendererMixin(BlockForm form) { super(form); }
    @Inject(method="renderBlock", at=@At("RETURN"))
    private void bml$breaking(MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, boolean picking, CallbackInfo ci) {
        int stage = LightSettings.of(form).breaking.get();
        if (picking || stage < 1 || stage > 10 || form.blockState.get().getRenderType() != BlockRenderType.MODEL) return;
        var consumer = new OverlayVertexConsumer(consumers.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage - 1)), stack.peek(), 1F);
        var manager = MinecraftClient.getInstance().getBlockRenderManager();
        manager.getModelRenderer().render(stack.peek(), consumer, form.blockState.get(), manager.getModel(form.blockState.get()), 1F, 1F, 1F, light, overlay);
    }
}

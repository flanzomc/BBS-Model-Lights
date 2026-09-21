package dev.flanzo.bbsmodellights.mixin;
import dev.flanzo.bbsmodellights.client.Effects;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;
@Mixin(value=ModelFormRenderer.class, remap=false)
public abstract class ModelRendererMixin extends FormRenderer<ModelForm> {
    @Unique private Effects.Snapshot bml$previous;
    protected ModelRendererMixin(ModelForm form) { super(form); }
    @Group(name="bml$version", min=1, max=1)
    @Inject(method="renderModel(Lmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/function/Supplier;Lnet/minecraft/class_4587;Lmchorse/bbs_mod/cubic/ModelInstance;IILmchorse/bbs_mod/utils/colors/Color;Lmchorse/bbs_mod/utils/colors/Color;ZZLmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;FLnet/minecraft/class_4587;)V", at=@At("HEAD"), require=0)
    private void bml$252(IEntity target, Supplier<ShaderProgram> program, MatrixStack stack, ModelInstance model, int light, int overlay, Color cc, Color fc, boolean additive, boolean ui, StencilMap stencil, float transition, MatrixStack world, CallbackInfo ci) {
        bml$begin(stack, stencil);
    }
    @Group(name="bml$version", min=1, max=1)
    @Inject(method="renderModel(Lmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/function/Supplier;Lnet/minecraft/class_4587;Lmchorse/bbs_mod/cubic/ModelInstance;IILmchorse/bbs_mod/utils/colors/Color;Lmchorse/bbs_mod/utils/colors/Color;ZLmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;FLnet/minecraft/class_4587;)V", at=@At("HEAD"), require=0)
    private void bml$261(IEntity target, Supplier<ShaderProgram> program, MatrixStack stack, ModelInstance model, int light, int overlay, Color cc, Color fc, boolean ui, StencilMap stencil, float transition, MatrixStack world, CallbackInfo ci) {
        bml$begin(stack, stencil);
    }
    @Unique private void bml$begin(MatrixStack stack, StencilMap stencil) {
        bml$previous = Effects.current;
        Effects.current = stencil == null ? Effects.capture(form, stack) : null;
    }
    @Inject(method="renderModel", at=@At("RETURN"))
    private void bml$end(CallbackInfo ci) { Effects.current = bml$previous; }
}

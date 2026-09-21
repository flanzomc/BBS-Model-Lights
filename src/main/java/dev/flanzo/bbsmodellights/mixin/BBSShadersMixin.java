package dev.flanzo.bbsmodellights.mixin;
import dev.flanzo.bbsmodellights.client.Effects;
import mchorse.bbs_mod.client.BBSShaders;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BBSShaders.class, remap=false)
public abstract class BBSShadersMixin {
    @Inject(method="getModel", at=@At("HEAD"), cancellable=true)
    private static void bml$shader(CallbackInfoReturnable<ShaderProgram> ci) {
        if (Effects.shader != null) ci.setReturnValue(Effects.shader);
    }
}

package dev.flanzo.bbsmodellights.mixin;
import dev.flanzo.bbsmodellights.client.Effects;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ShaderProgram.class)
public abstract class ShaderMixin {
    @Inject(method="bind", at=@At("HEAD"))
    private void bml$uniforms(CallbackInfo ci) { Effects.upload((ShaderProgram)(Object)this); }
}

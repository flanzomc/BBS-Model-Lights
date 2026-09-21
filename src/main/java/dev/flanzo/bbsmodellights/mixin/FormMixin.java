package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.LightSettings;
import mchorse.bbs_mod.forms.forms.Form;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Form.class, remap = false)
public abstract class FormMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bml$register(CallbackInfo ci) {
        ((Form)(Object)this).add(new LightSettings());
    }
}

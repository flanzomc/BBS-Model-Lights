package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.LightSettings;
import mchorse.bbs_mod.forms.forms.Form;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Form.class, remap = false)
public abstract class FormMixin implements LightSettings.Access {
    @Unique private LightSettings bml$values;
    @Override public LightSettings bml$settings() { return bml$values; }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bml$register(CallbackInfo ci) {
        bml$values = new LightSettings();
        bml$values.attach((Form)(Object)this);
    }
}

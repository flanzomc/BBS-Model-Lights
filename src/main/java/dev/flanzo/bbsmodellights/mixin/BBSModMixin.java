package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.form.LightForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FS 2.5.2 has no RegisterFormsEvent and FS 2.6.1 does. Registering at the
 * end of BBS' common initializer is stable across both supported versions.
 *
 * Use CML's exact form id (bbs:light), so copied CML light forms remain
 * readable on FS when this add-on is installed.
 */
@Mixin(value = BBSMod.class, remap = false)
public abstract class BBSModMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"))
    private void bml$registerLightForm(CallbackInfo ci) {
        BBSMod.getForms().register(Link.bbs("light"), LightForm.class, null);
    }
}

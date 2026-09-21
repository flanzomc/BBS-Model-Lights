package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.ui.LightPanel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIForm.class, remap = false)
public abstract class FormEditorMixin {
    @Inject(method = "registerDefaultPanels", at = @At("RETURN"))
    private void bml$panel(CallbackInfo ci) {
        UIForm editor = (UIForm)(Object)this;
        editor.registerPanel(new LightPanel(editor), IKey.constant("Model Lights"), Icons.GEAR);
    }
}

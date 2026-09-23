package dev.flanzo.bbsmodellights.ui;

import dev.flanzo.bbsmodellights.form.LightForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

/**
 * Native BBS form editor shell. FormEditorMixin appends LightPanel to the
 * default panels; making the last panel default opens directly on light
 * controls instead of an unrelated material panel.
 */
public final class UILightForm extends UIForm<LightForm> {
    public UILightForm() {
        super();
        this.registerDefaultPanels();
        this.defaultPanel = this.panels.get(this.panels.size() - 1);
    }
}

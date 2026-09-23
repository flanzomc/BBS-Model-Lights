package dev.flanzo.bbsmodellights.form;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

/**
 * CML-compatible invisible world-light form.
 *
 * BBS FS 2.5.2-2.6.1 does not ship bbs:light, so the add-on supplies the
 * same two native values used by BBS-CML: enabled and level.
 */
public final class LightForm extends Form {
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueInt level = new ValueInt("level", 15, 0, 15);

    public LightForm() {
        super();
        this.add(this.enabled);
        this.add(this.level);
    }
}

package dev.flanzo.bbsmodellights.ui;

import dev.flanzo.bbsmodellights.LightSettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;

public final class LightPanel extends UIFormPanel<Form> {
    public LightPanel(UIForm editor) { super(editor); }

    @Override public void startEdit(Form form) {
        super.startEdit(form);
        options.removeAll();
        LightSettings s = LightSettings.of(form);
        effect("Glow", s.glow); effect("Paint", s.paint);
        effect("Brightness", s.brightness); effect("Contrast", s.contrast);
        effect("Saturation", s.saturation); effect("Hue (degrees)", s.hue);
        if (form instanceof BlockForm block) {
            UIToggle enabled = new UIToggle(IKey.constant("Emit block light"), t -> s.emission.set(t.getValue()));
            enabled.setValue(s.emission.get());
            UITrackpad level = new UITrackpad(v -> s.emissionIntensity.set(v.intValue())).limit(0, 15).integer();
            level.setValue(s.emissionIntensity.get());
            UITrackpad breaking = new UITrackpad(v -> s.breaking.set(v.intValue())).limit(0, 10).integer();
            breaking.setValue(s.breaking.get());
            options.add(enabled, UI.labelRow(IKey.constant("Light intensity"), level), UI.labelRow(IKey.constant("Breaking stage"), breaking));
        }
        options.resize();
    }

    private void effect(String name, LightSettings.Effect effect) {
        UISection section = new UISection(IKey.constant(name));
        section.fields.add(UI.labelRow(IKey.constant("Intensity"), number(effect.intensity)));
        if (effect.colored) {
            UIColor color = new UIColor(v -> effect.color.set(new Color().set(v)));
            color.setColor(effect.color.get().getARGBColor());
            section.fields.add(color);
        }
        LightSettings.Mask m = effect.transform;
        UISection transform = new UISection(IKey.constant("Effect transform"));
        transform.setExpanded(false);
        transform.fields.add(UI.labelRow(IKey.constant("Offset XYZ"), UI.row(number(m.x), number(m.y), number(m.z))));
        transform.fields.add(UI.labelRow(IKey.constant("Scale XYZ"), UI.row(number(m.sx), number(m.sy), number(m.sz))));
        transform.fields.add(UI.labelRow(IKey.constant("Rotation XYZ"), UI.row(number(m.rx), number(m.ry), number(m.rz))));
        transform.fields.add(UI.labelRow(IKey.constant("Pivot XYZ"), UI.row(number(m.px), number(m.py), number(m.pz))));
        UITrackpad shape = new UITrackpad(v -> m.shape.set(v.intValue())).limit(0, 2).integer();
        shape.setValue(m.shape.get());
        transform.fields.add(UI.labelRow(IKey.constant("Mask: 0 box / 1 circle / 2 triangle"), shape));
        section.fields.add(transform);
        options.add(section);
    }

    private UITrackpad number(ValueFloat value) {
        UITrackpad field = new UITrackpad(v -> value.set(v.floatValue()));
        field.setValue(value.get());
        return field;
    }
}

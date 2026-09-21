package dev.flanzo.bbsmodellights;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;

/** Form-owned values participate in FS persistence, copying, undo and track discovery. */
public final class LightSettings extends ValueGroup {
    public final Effect glow = new Effect("glow", true);
    public final Effect paint = new Effect("paint", true);
    public final Effect brightness = new Effect("brightness", false);
    public final Effect contrast = new Effect("contrast", false);
    public final Effect saturation = new Effect("saturation", false);
    public final Effect hue = new Effect("hue", false);
    public final ValueBoolean emission = new ValueBoolean("emit_light", false);
    public final ValueInt emissionIntensity = new ValueInt("light_intensity", 15, 0, 15);
    public final ValueInt breaking = new ValueInt("breaking", 0, 0, 10);

    public LightSettings() {
        super("bbs_model_lights");
        add(glow); add(paint); add(brightness); add(contrast); add(saturation); add(hue);
        add(emission); add(emissionIntensity); add(breaking);
    }

    public static LightSettings of(Form form) { return (LightSettings) form.get("bbs_model_lights"); }

    public static final class Effect extends ValueGroup {
        public final ValueFloat intensity = new ValueFloat("intensity", 0F);
        public final ValueColor color = new ValueColor("color", Color.white());
        public final Mask transform = new Mask();
        public final boolean colored;
        public Effect(String id, boolean colored) {
            super(id); this.colored = colored;
            add(intensity);
            if (colored) add(color);
            add(transform);
        }
    }

    /** Matches CML's effect-volume axes, degrees and default unmasked state. */
    public static final class Mask extends ValueGroup {
        public final ValueFloat x = new ValueFloat("offsetX", 0F);
        public final ValueFloat y = new ValueFloat("offsetY", 0F);
        public final ValueFloat z = new ValueFloat("offsetZ", 0F);
        public final ValueFloat sx = new ValueFloat("scaleX", 1F);
        public final ValueFloat sy = new ValueFloat("scaleY", 1F);
        public final ValueFloat sz = new ValueFloat("scaleZ", 1F);
        public final ValueFloat rx = new ValueFloat("rotateX", 0F);
        public final ValueFloat ry = new ValueFloat("rotateY", 0F);
        public final ValueFloat rz = new ValueFloat("rotateZ", 0F);
        public final ValueFloat px = new ValueFloat("pivotX", 0F);
        public final ValueFloat py = new ValueFloat("pivotY", 0F);
        public final ValueFloat pz = new ValueFloat("pivotZ", 0F);
        public final ValueInt shape = new ValueInt("shape", 0, 0, 2);
        public Mask() {
            super("transform");
            add(x); add(y); add(z); add(sx); add(sy); add(sz);
            add(rx); add(ry); add(rz); add(px); add(py); add(pz); add(shape);
        }
    }
}

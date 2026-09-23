package dev.flanzo.bbsmodellights;

import dev.flanzo.bbsmodellights.form.LightForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;

/** CML world-light rules for the missing LightForm plus BlockForm emission. */
public final class PlacedLight {
    public static final IntProperty LEVEL = IntProperty.of("bml_light", 0, 15);
    private static final IntProperty FS_LEVEL = IntProperty.of("light_level", 0, 15);

    public static int level(Form form) {
        if (form instanceof LightForm light) {
            return light.enabled.get() ? clamp(light.level.get()) : 0;
        }

        if (!(form instanceof BlockForm block)) return 0;

        LightSettings settings = LightSettings.of(form);
        if (settings == null || !settings.emission.get()) return 0;

        BlockState state = block.blockState.get();
        if (state == null) return 0;

        return clamp(Math.min(state.getLuminance(), settings.emissionIntensity.get()));
    }

    public static int luminance(BlockState state) {
        return Math.max(state.get(LEVEL), state.contains(FS_LEVEL) ? state.get(FS_LEVEL) : 0);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(15, value));
    }
}

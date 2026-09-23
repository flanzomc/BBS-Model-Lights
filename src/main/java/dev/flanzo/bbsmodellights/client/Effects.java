package dev.flanzo.bbsmodellights.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.flanzo.bbsmodellights.LightSettings;
import dev.flanzo.bbsmodellights.form.LightForm;
import dev.flanzo.bbsmodellights.ui.UILightForm;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class Effects implements ClientModInitializer {
    public static ShaderProgram shader;
    public static Snapshot current;

    /*
     * CML model.fsh evaluates effect masks in form/model space. VAO positions
     * are already model-local, therefore the normal value is identity.
     * CPU shape-key geometry is pre-transformed before upload; ModelInstanceMixin
     * temporarily supplies inverse(renderStack) for that path.
     */
    public static final Matrix4f renderRootInverse = new Matrix4f();

    /* Smoke/diagnostic counters. These are intentionally render-path counters,
       not gameplay behavior. */
    public static int glowShaderUploads;
    public static int glowFallbackApplications;

    private static final String[] PREFIX = {"Glow", "Paint", "GradeBrightness", "GradeContrast", "GradeSaturation", "GradeHue"};

    @Override
    public void onInitializeClient() {
        FormUtilsClient.register(LightForm.class, LightFormRenderer::new);
        UIFormEditor.register(LightForm.class, UILightForm::new);
        ProductionSmoke.install();

        CoreShaderRegistrationCallback.EVENT.register(context ->
            context.register(
                new Identifier("bbs_model_lights", "model"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                program -> shader = program
            )
        );
    }

    public static Snapshot capture(Form form) {
        LightSettings values = new LightSettings();
        values.fromData(LightSettings.of(form).toData());

        return new Snapshot(values);
    }

    public record Snapshot(LightSettings values) {}

    public static boolean hasGlowTransform() {
        Snapshot snapshot = current;

        if (snapshot == null) {
            return false;
        }

        LightSettings.Mask m = snapshot.values.glow.transform;

        return m.x.get() != 0F || m.y.get() != 0F || m.z.get() != 0F
            || m.rx.get() != 0F || m.ry.get() != 0F || m.rz.get() != 0F
            || m.px.get() != 0F || m.py.get() != 0F || m.pz.get() != 0F
            || m.sx.get() != 1F || m.sy.get() != 1F || m.sz.get() != 1F
            || m.shape.get() != 0;
    }

    public static void upload(ShaderProgram program) {
        if (program != shader) {
            return;
        }

        Snapshot snapshot = current;

        if (snapshot == null) {
            vec4(program, "GlowingColor", 1F, 1F, 1F, 0F);
            vec4(program, "PaintColor", 1F, 1F, 1F, 0F);
            vec4(program, "FormColorGrade", 0F, 0F, 0F, 0F);
            matrix(program, "FormRootInverse", new Matrix4f());
            clearEffect(program, "Glow");
            clearEffect(program, "Paint");
            clearEffect(program, "GradeBrightness");
            clearEffect(program, "GradeContrast");
            clearEffect(program, "GradeSaturation");
            clearEffect(program, "GradeHue");

            return;
        }

        LightSettings values = snapshot.values;

        matrix(program, "FormRootInverse", renderRootInverse);
        vec4(
            program,
            "GlowingColor",
            values.glow.color.get().r,
            values.glow.color.get().g,
            values.glow.color.get().b,
            values.glow.intensity.get()
        );

        if (Math.abs(values.glow.intensity.get()) > 0.001F) {
            glowShaderUploads++;
        }

        vec4(
            program,
            "PaintColor",
            values.paint.color.get().r,
            values.paint.color.get().g,
            values.paint.color.get().b,
            values.paint.intensity.get()
        );
        vec4(
            program,
            "FormColorGrade",
            values.brightness.intensity.get(),
            values.contrast.intensity.get(),
            values.hue.intensity.get(),
            values.saturation.intensity.get()
        );

        LightSettings.Effect[] effects = {
            values.glow,
            values.paint,
            values.brightness,
            values.contrast,
            values.saturation,
            values.hue
        };

        for (int i = 0; i < effects.length; i++) {
            uploadEffect(program, PREFIX[i], effects[i], i < 2);
        }
    }

    private static void uploadEffect(ShaderProgram program, String prefix, LightSettings.Effect effect, boolean modelEffect) {
        LightSettings.Mask m = effect.transform;
        String inverseName = modelEffect ? prefix + "EffectInverse" : prefix + "Inverse";
        String activeName = modelEffect ? prefix + "EffectActive" : prefix + "Active";
        String halfName = modelEffect ? prefix + "MaskHalf" : prefix + "Half";
        String bottomName = modelEffect ? prefix + "MaskBottomAnchored" : prefix + "BottomAnchored";
        String shapeName = modelEffect ? prefix + "MaskShape" : prefix + "Shape";

        boolean active = m.x.get() != 0F || m.y.get() != 0F || m.z.get() != 0F
            || m.rx.get() != 0F || m.ry.get() != 0F || m.rz.get() != 0F
            || m.px.get() != 0F || m.py.get() != 0F || m.pz.get() != 0F
            || m.sx.get() != 1F || m.sy.get() != 1F || m.sz.get() != 1F
            || m.shape.get() != 0;

        Matrix4f inverse = new Matrix4f()
            .translate(m.x.get(), m.y.get(), m.z.get())
            .translate(m.px.get(), m.py.get(), m.pz.get())
            .rotateXYZ(
                (float) Math.toRadians(m.rx.get()),
                (float) Math.toRadians(m.ry.get()),
                (float) Math.toRadians(m.rz.get())
            )
            .translate(-m.px.get(), -m.py.get(), -m.pz.get())
            .invert();

        matrix(program, inverseName, inverse);
        scalar(program, activeName, active ? 1F : 0F);
        scalar(program, bottomName, 1F);

        /*
         * Current CML EffectTransformMath.MODEL_MASK_HALF_BASE is 1.0.
         * Scale lives in the half extents, not in the inverse matrix.
         */
        vec3(
            program,
            halfName,
            m.sx.get() == 0F ? 0.001F : m.sx.get(),
            m.sy.get() == 0F ? 0.001F : m.sy.get(),
            m.sz.get() == 0F ? 0.001F : m.sz.get()
        );
        scalar(program, shapeName, m.shape.get());
    }

    private static void clearEffect(ShaderProgram program, String prefix) {
        boolean modelEffect = prefix.equals("Glow") || prefix.equals("Paint");
        String active = modelEffect ? prefix + "EffectActive" : prefix + "Active";

        scalar(program, active, 0F);
    }

    private static void matrix(ShaderProgram program, String name, Matrix4f value) {
        GlUniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void scalar(ShaderProgram program, String name, float value) {
        GlUniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void vec3(ShaderProgram program, String name, float x, float y, float z) {
        GlUniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    private static void vec4(ShaderProgram program, String name, float x, float y, float z, float w) {
        GlUniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }
}

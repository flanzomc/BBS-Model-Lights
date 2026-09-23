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
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class Effects implements ClientModInitializer {
    public static ShaderProgram shader;
    public static Snapshot current;
    private static final String[] PREFIX = {"Glow", "Paint", "GradeBrightness", "GradeContrast", "GradeSaturation", "GradeHue"};

    @Override public void onInitializeClient() {
        FormUtilsClient.register(LightForm.class, LightFormRenderer::new);
        UIFormEditor.register(LightForm.class, UILightForm::new);
        ProductionSmoke.install();
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(new Identifier("bbs_model_lights", "model"),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, program -> shader = program));
    }

    public static Snapshot capture(Form form, MatrixStack stack) {
        LightSettings values = new LightSettings();
        values.fromData(LightSettings.of(form).toData());
        return new Snapshot(values, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix()).invert());
    }

    public record Snapshot(LightSettings values, Matrix4f rootInverse) {}

    public static void upload(ShaderProgram program) {
        if (program != shader) return;
        Snapshot s = current;
        if (s == null) {
            vec4(program, "GlowingColor", 1, 1, 1, 0);
            vec4(program, "PaintColor", 1, 1, 1, 0);
            vec4(program, "FormColorGrade", 0, 0, 0, 0);
            return;
        }
        LightSettings v = s.values;
        program.getUniform("FormRootInverse").set(s.rootInverse);
        vec4(program, "GlowingColor", v.glow.color.get().r, v.glow.color.get().g, v.glow.color.get().b, v.glow.intensity.get());
        vec4(program, "PaintColor", v.paint.color.get().r, v.paint.color.get().g, v.paint.color.get().b, v.paint.intensity.get());
        vec4(program, "FormColorGrade", v.brightness.intensity.get(), v.contrast.intensity.get(), v.hue.intensity.get(), v.saturation.intensity.get());
        LightSettings.Effect[] effects = {v.glow, v.paint, v.brightness, v.contrast, v.saturation, v.hue};
        for (int i = 0; i < effects.length; i++) {
            LightSettings.Mask m = effects[i].transform;
            String p = PREFIX[i];
            String inv = i < 2 ? p + "EffectInverse" : p + "Inverse";
            String active = i < 2 ? p + "EffectActive" : p + "Active";
            String half = i < 2 ? p + "MaskHalf" : p + "Half";
            String shape = i < 2 ? p + "MaskShape" : p + "Shape";
            boolean isActive = m.x.get()!=0 || m.y.get()!=0 || m.z.get()!=0 || m.rx.get()!=0 || m.ry.get()!=0 || m.rz.get()!=0 || m.px.get()!=0 || m.py.get()!=0 || m.pz.get()!=0 || m.sx.get()!=1 || m.sy.get()!=1 || m.sz.get()!=1 || m.shape.get()!=0;
            Matrix4f inverse = new Matrix4f().translate(m.x.get(), m.y.get(), m.z.get())
                .translate(m.px.get(), m.py.get(), m.pz.get())
                .rotateXYZ((float)Math.toRadians(m.rx.get()), (float)Math.toRadians(m.ry.get()), (float)Math.toRadians(m.rz.get()))
                .translate(-m.px.get(), -m.py.get(), -m.pz.get()).invert();
            program.getUniform(inv).set(inverse);
            program.getUniform(active).set(isActive ? 1F : 0F);
            program.getUniform(i < 2 ? p + "MaskBottomAnchored" : p + "BottomAnchored").set(1F);
            program.getUniform(half).set(m.sx.get()==0 ? 0.001F : m.sx.get(), m.sy.get()==0 ? 0.001F : m.sy.get(), m.sz.get()==0 ? 0.001F : m.sz.get());
            program.getUniform(shape).set(m.shape.get().floatValue());
        }
    }
    private static void vec4(ShaderProgram p, String name, float x, float y, float z, float w) {
        GlUniform u = p.getUniform(name);
        if (u != null) u.set(x,y,z,w);
    }
}

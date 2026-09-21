package dev.flanzo.bbsmodellights.client;

import dev.flanzo.bbsmodellights.LightSettings;
import dev.flanzo.bbsmodellights.ui.LightPanel;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import java.nio.file.Files;

/** Opt-in production smoke test. Never active in an ordinary player launch. */
public final class ProductionSmoke {
    private static int phase, ticks;
    public static void install() {
        if (!Boolean.getBoolean("bml.smoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (phase == 0 && client.currentScreen instanceof TitleScreen) {
                    if (Effects.shader == null) throw new IllegalStateException("Effect shader not loaded");
                    ModelForm form = new ModelForm();
                    LightSettings.of(form).glow.intensity.set(0.75F);
                    LightSettings.of(form).hue.transform.rx.set(35F);
                    ModelForm loaded = new ModelForm();
                    loaded.fromData(form.toData());
                    if (LightSettings.of(loaded).glow.intensity.get()!=0.75F || LightSettings.of(loaded).hue.transform.rx.get()!=35F)
                        throw new IllegalStateException("Form settings roundtrip failed");
                    UIModelForm editor = new UIModelForm();
                    new LightPanel(editor).startEdit(loaded);
                    ProductionBehavior.checkKeyframes(loaded);
                    phase = 1;
                    CreateWorldScreen.create(client, client.currentScreen);
                } else if (phase == 1 && client.currentScreen instanceof CreateWorldScreen) {
                    for (var child : client.currentScreen.children()) {
                        if (child instanceof ButtonWidget b && b.active && b.getMessage().getString().equals(Text.translatable("selectWorld.create").getString())) {
                            phase = 2;
                            b.onPress();
                            break;
                        }
                    }
                } else if (phase == 2 && client.world != null && client.player != null && ++ticks >= 100) {
                    if (!ProductionBehavior.tick(client)) return;
                    ScreenshotRecorder.saveScreenshot(client.runDirectory, "world-smoke.png", client.getFramebuffer(), message -> {});
                    Files.writeString(client.runDirectory.toPath().resolve("bml-smoke-passed.txt"),
                        "PASS: production Fabric launch; shader load; form roundtrip; native editor construction; native keyframe interpolation; world entry; placed emission on/off; moving dynamic light; animated intensity; rendered model and breaking overlay.\nVisual screenshots require separate review.\n");
                    phase = 3;
                    ticks = 0;
                } else if (phase == 3 && ++ticks > 30) client.scheduleStop();
            } catch (Throwable e) {
                e.printStackTrace();
                throw new IllegalStateException("BML production smoke failure", e);
            }
        });
    }
}

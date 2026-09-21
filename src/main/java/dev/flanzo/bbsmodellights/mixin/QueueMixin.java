package dev.flanzo.bbsmodellights.mixin;
import dev.flanzo.bbsmodellights.client.Effects;
import mchorse.bbs_mod.forms.FormTranslucentQueue;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(value=FormTranslucentQueue.class, remap=false)
public abstract class QueueMixin {
    @ModifyVariable(method="add", at=@At("HEAD"), argsOnly=true)
    private static FormTranslucentQueue.DrawCommand bml$capture(FormTranslucentQueue.DrawCommand original) {
        Effects.Snapshot captured = Effects.current;
        if (captured == null) return original;
        return new FormTranslucentQueue.DrawCommand(new Vector3f(0,0,(float)Math.sqrt(original.distanceSq)), original.cull, original.depthWrite) {
            @Override public void draw() {
                Effects.Snapshot previous = Effects.current;
                Effects.current = captured;
                try { original.draw(); } finally { Effects.current = previous; }
            }
            @Override public void release() { original.release(); }
        };
    }
}

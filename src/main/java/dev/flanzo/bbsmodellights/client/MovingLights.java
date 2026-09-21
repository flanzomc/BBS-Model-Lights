package dev.flanzo.bbsmodellights.client;

import dev.flanzo.bbsmodellights.PlacedLight;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.entity.EntityType;

/** CML's entity light integration, adapted to the real 1.20.4 Lamb API. */
public final class MovingLights implements DynamicLightsInitializer {
    @Override public void onInitializeDynamicLights() {
        DynamicLightHandlers.registerDynamicLightHandler(BBSMod.ACTOR_ENTITY, actor -> PlacedLight.level(actor.getForm()));
        DynamicLightHandlers.registerDynamicLightHandler(BBSMod.GUN_PROJECTILE_ENTITY, projectile -> PlacedLight.level(projectile.getForm()));
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER, player -> {
            Morph morph = Morph.getMorph(player);
            return morph == null ? 0 : PlacedLight.level(morph.getForm());
        });
    }
}

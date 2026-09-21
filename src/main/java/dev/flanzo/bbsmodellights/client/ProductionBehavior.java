package dev.flanzo.bbsmodellights.client;

import dev.flanzo.bbsmodellights.LightSettings;
import dev.flanzo.bbsmodellights.PlacedLight;
import dev.lambdaurora.lambdynlights.LambDynLights;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import java.util.concurrent.CompletableFuture;

/** Runs only with -Dbml.smoke=true, in a newly-created disposable world. */
public final class ProductionBehavior {
    private static int stage, ticks;
    private static BlockPos origin;
    private static CompletableFuture<?> serverTask;
    private static ActorEntity moving, modelActor;
    private static BlockForm movingForm;
    private static ModelForm model;
    private static FormProperties animation;
    private static double initialLight;

    public static void checkKeyframes(ModelForm form) {
        FormProperties properties = new FormProperties("test");
        var value = LightSettings.of(form).glow.intensity;
        var channel = properties.create(value);
        require(channel != null, "Native keyframe channel");
        channel.insert(0, 0F); channel.insert(20, 1F);
        properties.applyProperties(form, 10);
        require(Math.abs(value.get() - .5F) < .01F, "Native keyframe midpoint: " + value.get());
        value.setRuntimeValue(null);
    }

    public static boolean tick(MinecraftClient client) {
        if (serverTask != null) {
            if (!serverTask.isDone()) return false;
            serverTask.join(); serverTask = null;
        }
        ticks++;
        if (stage == 0) {
            origin = client.player.getBlockPos().withY(200);
            serverTask = client.getServer().submit(() -> {
                var world = client.getServer().getOverworld();
                for (int x=-12;x<=12;x++) for(int z=-12;z<=12;z++)
                    world.setBlockState(origin.add(x,-1,z), Blocks.STONE.getDefaultState());
                world.setTimeOfDay(18000);
                var player = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                player.teleport(world, origin.getX()+.5, 200, origin.getZ()-6, 0, 0);
                world.setBlockState(origin, BBSMod.MODEL_BLOCK.getDefaultState());
                var entity = (ModelBlockEntity) world.getBlockEntity(origin);
                BlockForm form = new BlockForm();
                form.blockState.set(Blocks.GLOWSTONE.getDefaultState());
                LightSettings.of(form).emission.set(true);
                LightSettings.of(form).emissionIntensity.set(12);
                LightSettings.of(form).breaking.set(5);
                entity.getProperties().setForm(form);
                entity.markDirty();
                world.updateListeners(origin, world.getBlockState(origin), world.getBlockState(origin), 3);
            });
            stage=1; ticks=0;
        } else if (stage == 1 && ticks >= 80) {
            serverTask = client.getServer().submit(() -> {
                var world=client.getServer().getOverworld();
                require(world.getBlockState(origin).get(PlacedLight.LEVEL)==12, "Placed intensity property");
                require(world.getLightLevel(LightType.BLOCK, origin.east())>=11, "Placed light reaches neighbor");
                var form=((ModelBlockEntity)world.getBlockEntity(origin)).getProperties().getForm();
                LightSettings.of(form).emission.set(false);
            });
            stage=2; ticks=0;
        } else if (stage == 2 && ticks >= 60) {
            serverTask = client.getServer().submit(() -> {
                var world=client.getServer().getOverworld();
                require(world.getBlockState(origin).get(PlacedLight.LEVEL)==0, "Emission toggle clears state");
                require(world.getLightLevel(LightType.BLOCK,origin.east())==0, "Emission toggle clears world light");
            });
            model=new ModelForm(); model.model.set("player/steve");
            require(BBSModClient.getModels().loadModel("player/steve") != null, "Real BBS model loads");
            model.lighting.set(0F);
            modelActor=actor(client,model,origin.add(-2,0,0),-5000);
            movingForm=new BlockForm(); movingForm.blockState.set(Blocks.GLOWSTONE.getDefaultState());
            LightSettings.of(movingForm).emission.set(true);
            LightSettings.of(movingForm).breaking.set(5);
            moving=actor(client,movingForm,origin.add(2,0,0),-5001);
            animation=new FormProperties("animated light");
            var channel=animation.create(LightSettings.of(movingForm).emissionIntensity);
            require(channel!=null,"Emission keyframe track");
            channel.insert(0,15); channel.insert(20,3);
            stage=3; ticks=0;
        } else if (stage == 3 && ticks >= 60) {
            initialLight=LambDynLights.get().getDynamicLightLevel(moving.getBlockPos().up());
            require(initialLight>10,"Moving form illuminates world: "+initialLight);
            screenshot(client,"01-baseline.png");
            LightSettings.of(model).paint.color.set(new mchorse.bbs_mod.utils.colors.Color().set(0xffff0000));
            LightSettings.of(model).paint.intensity.set(1F);
            stage=4; ticks=0;
        } else if (stage == 4 && ticks>=30) {
            screenshot(client,"02-paint-red.png");
            var old=moving.getBlockPos();
            moving.setPosition(moving.getX()+10,moving.getY(),moving.getZ());
            require(LambDynLights.get().getDynamicLightLevel(old.up())<1,"Light follows moved actor");
            LightSettings.of(model).paint.intensity.set(0F);
            LightSettings.of(model).glow.color.set(new mchorse.bbs_mod.utils.colors.Color().set(0xff00ff00));
            LightSettings.of(model).glow.intensity.set(.8F);
            stage=5; ticks=0;
        } else if(stage==5 && ticks>=40) {
            require(LambDynLights.get().getDynamicLightLevel(moving.getBlockPos().up())>10,"Light reaches new actor position");
            animation.applyProperties(movingForm,20);
            screenshot(client,"03-glow-green.png");
            LightSettings.of(model).glow.intensity.set(0F);
            LightSettings.of(model).saturation.intensity.set(-1F);
            stage=6; ticks=0;
        } else if(stage==6 && ticks>=40) {
            double dim=LambDynLights.get().getDynamicLightLevel(moving.getBlockPos().up());
            require(dim>0 && dim<4,"Keyframed light intensity updates: "+dim);
            screenshot(client,"04-desaturated.png");
            LightSettings.of(movingForm).emission.set(false);
            LightSettings.of(model).saturation.intensity.set(0F);
            LightSettings.of(model).paint.intensity.set(1F);
            LightSettings.of(model).paint.transform.sy.set(.45F);
            stage=7; ticks=0;
        } else if(stage==7 && ticks>=40) {
            require(LambDynLights.get().getDynamicLightLevel(moving.getBlockPos().up())==0,"Moving emission off");
            screenshot(client,"05-transformed-paint.png");
            return true;
        }
        return false;
    }

    private static ActorEntity actor(MinecraftClient client,mchorse.bbs_mod.forms.forms.Form form,BlockPos pos,int id) {
        ActorEntity actor=new ActorEntity(BBSMod.ACTOR_ENTITY,client.world);
        actor.setId(id); actor.setForm(form); actor.setNoGravity(true);
        actor.setPosition(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        client.world.addEntity(actor);
        return actor;
    }
    private static void screenshot(MinecraftClient client,String name) {
        ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),message -> {});
    }
    private static void require(boolean condition,String message) {
        if(!condition) throw new IllegalStateException("BML behavior failed: "+message);
        System.out.println("BML CHECK: "+message);
    }
}

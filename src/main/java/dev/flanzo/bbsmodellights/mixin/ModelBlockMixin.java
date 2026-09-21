package dev.flanzo.bbsmodellights.mixin;

import dev.flanzo.bbsmodellights.PlacedLight;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ModelBlock.class, remap=false)
public abstract class ModelBlockMixin {
    @ModifyVariable(method="<init>", at=@At("HEAD"), argsOnly=true)
    private static AbstractBlock.Settings bml$luminance(AbstractBlock.Settings settings) {
        return settings.luminance(PlacedLight::luminance);
    }
    @Inject(method={"appendProperties", "method_9515"}, at=@At("TAIL"))
    private void bml$property(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(PlacedLight.LEVEL);
    }
    @Inject(method={"getTicker", "method_31645"}, at=@At("RETURN"), cancellable=true)
    private <T extends BlockEntity> void bml$ticker(World world, BlockState state, BlockEntityType<T> type, CallbackInfoReturnable<BlockEntityTicker<T>> ci) {
        if (world.isClient || type != BBSMod.MODEL_BLOCK_ENTITY) return;
        BlockEntityTicker<T> original = ci.getReturnValue();
        ci.setReturnValue((w, pos, blockState, entity) -> {
            if (original != null) original.tick(w, pos, blockState, entity);
            if (entity instanceof ModelBlockEntity model) {
                BlockState current = w.getBlockState(pos);
                if (!current.contains(PlacedLight.LEVEL)) return;
                int level = PlacedLight.level(model.getProperties().getForm());
                if (current.get(PlacedLight.LEVEL) != level) w.setBlockState(pos, current.with(PlacedLight.LEVEL, level), Block.NOTIFY_ALL);
            }
        });
    }
}

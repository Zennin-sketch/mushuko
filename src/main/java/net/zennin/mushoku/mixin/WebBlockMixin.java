package net.zennin.mushoku.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante la Rafaga las telaranas no te atrapan (Escape). */
@Mixin(WebBlock.class)
public abstract class WebBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void remushoku$escape(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (net.zennin.mushoku.magic.Escape.active(entity)) ci.cancel();
    }
}

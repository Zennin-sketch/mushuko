package net.zennin.mushokucombat.mixin;

import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushokucombat.Sheath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con el arma guardada, Better Combat no pone la pose de sostener el arma (servidor y cliente). */
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class PlayerAttackHelperMixin {
    @Inject(method = "poseForPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private static void mushokuCombat$noPoseWhenSheathed(Player player, CallbackInfoReturnable<Pose> cir) {
        if (Sheath.isSheathed(player)) cir.setReturnValue(new Pose("", ""));
    }
}

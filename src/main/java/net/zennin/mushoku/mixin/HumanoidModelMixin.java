package net.zennin.mushoku.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.client.ClientReadySpell;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Con un hechizo listo para lanzar, el brazo de la mano habil queda levantado apuntando a donde mira el jugador
 * (sigue a la cabeza, como al tensar un arco). Las mangas copian el brazo despues, en PlayerModel.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void remushoku$aimSpell(LivingEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player p) || p.isSwimming() || p.isSleeping() || p.isFallFlying() || p.isPassenger()) return;
        if (ClientReadySpell.get(p) == null) return;
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? rightArm : leftArm;
        arm.xRot = head.xRot - (float) Math.PI / 2f - 0.08f;
        arm.yRot = head.yRot + (right ? -0.06f : 0.06f);
        arm.zRot = 0;
    }
}

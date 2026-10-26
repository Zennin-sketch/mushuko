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
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    /**
     * Con la Capa Atrapallamas v2 puesta se camina "dentro de la capa": los brazos no se balancean (quedan quietos
     * bajo la capa) y los hombros suben y bajan con cada paso; los pasos son la mitad de largos. La capa sigue a los
     * hombros y a los pies (ArmorV2Renderer).
     */
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void remushoku$cloakWalk(LivingEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player p) || p.isSwimming() || p.isSleeping() || p.isFallFlying() || p.isPassenger()) return;
        if (!p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(net.zennin.mushoku.item.ArmorV2.FLAME_CLOAK)) return;
        float c = net.minecraft.util.Mth.cos(limbSwing * 0.6662f), s = net.minecraft.util.Mth.sin(limbSwing * 0.6662f);
        // quitar el balanceo de los brazos que suma vanilla (cos(f*0.6662 [+pi]) * 2 * g * 0.5)
        rightArm.xRot -= -c * limbAmount;
        leftArm.xRot -= c * limbAmount;
        // pasos a la mitad
        rightLeg.xRot -= c * 1.4f * limbAmount * 0.5f;
        leftLeg.xRot -= -c * 1.4f * limbAmount * 0.5f;
        // hombros: sube el del lado de la pierna que va atras (alternado) y los dos bajan un poco al pisar
        float bob = Math.min(1f, limbAmount) * 1.1f;
        rightArm.y += -s * bob + Math.abs(c) * bob * 0.35f;
        leftArm.y += s * bob + Math.abs(c) * bob * 0.35f;
    }

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

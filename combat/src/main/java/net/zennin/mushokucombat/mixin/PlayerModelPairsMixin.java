package net.zennin.mushokucombat.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.zennin.mushokucombat.client.compat.LayerBendConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guarda que capa externa va con que parte (manga -> brazo, pantalon -> pierna, chaqueta -> torso). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelPairsMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void mushokuCombat$pairs(ModelPart root, boolean slim, CallbackInfo ci) {
        PlayerModel<?> m = (PlayerModel<?>) (Object) this;
        LayerBendConsumer.pair(m.rightSleeve, m.rightArm);
        LayerBendConsumer.pair(m.leftSleeve, m.leftArm);
        LayerBendConsumer.pair(m.rightPants, m.rightLeg);
        LayerBendConsumer.pair(m.leftPants, m.leftLeg);
        LayerBendConsumer.pair(m.jacket, m.body);
    }
}

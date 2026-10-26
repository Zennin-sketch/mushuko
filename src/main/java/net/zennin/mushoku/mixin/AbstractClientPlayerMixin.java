package net.zennin.mushoku.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.zennin.mushoku.client.VisualTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Solo en la prueba visual: todos los jugadores usan la skin del escenario (sin internet no hay skins). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void remushoku$testSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (VisualTest.skin != null)
            cir.setReturnValue(new PlayerSkin(VisualTest.skin, null, null, null, PlayerSkin.Model.WIDE, true));
    }
}

package net.zennin.mushoku.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.zennin.mushoku.client.GustGlide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Rafaga: el cuerpo se inclina como al volar con elitros mientras va impulsado (solo la pose). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @ModifyExpressionValue(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"), require = 0)
    private boolean remushoku$gustFlying(boolean original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractClientPlayer player) {
        return original || GustGlide.active(player);
    }

    @ModifyExpressionValue(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getFallFlyingTicks()I"), require = 0)
    private int remushoku$gustTicks(int original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractClientPlayer player) {
        int g = GustGlide.ticks(player);
        return g > 0 ? Math.max(original, g) : original;
    }

    // por si el compilador de Minecraft llamo a los metodos sobre LivingEntity en vez de AbstractClientPlayer
    @ModifyExpressionValue(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"), require = 0)
    private boolean remushoku$gustFlying2(boolean original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractClientPlayer player) {
        return original || GustGlide.active(player);
    }

    @ModifyExpressionValue(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFallFlyingTicks()I"), require = 0)
    private int remushoku$gustTicks2(int original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractClientPlayer player) {
        int g = GustGlide.ticks(player);
        return g > 0 ? Math.max(original, g) : original;
    }
}

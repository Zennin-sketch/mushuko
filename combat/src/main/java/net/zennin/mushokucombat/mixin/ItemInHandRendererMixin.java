package net.zennin.mushokucombat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushokucombat.client.ClientSheath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Primera persona: con el arma guardada la mano principal se ve vacia. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void mushokuCombat$hideSheathed(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                            float swing, ItemStack stack, float equip, PoseStack pose, MultiBufferSource buffers,
                                            int light, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && !ClientSheath.handVisible(player)) ci.cancel();
    }
}

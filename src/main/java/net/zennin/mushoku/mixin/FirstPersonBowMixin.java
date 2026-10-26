package net.zennin.mushoku.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.client.render.BowFirstPerson;
import net.zennin.mushoku.client.render.BowRender;
import net.zennin.mushoku.item.MushokuBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Primera persona con un arco del mod: brazos y arco fijos a la camara (BowFirstPerson) en lugar de la mano vanilla. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonBowMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void remushoku$bowFirstPerson(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                                          ItemStack stack, float equip, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ItemStack bow = player.getMainHandItem();
        if (!(bow.getItem() instanceof MushokuBowItem) || BowRender.hidden.test(player)) return;
        ci.cancel();
        if (hand == InteractionHand.MAIN_HAND) BowFirstPerson.render(player, bow, partialTick, pose, buffers, light);
    }
}

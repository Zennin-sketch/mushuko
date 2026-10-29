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
        // golpe con animacion de primera persona hecha en el Taller
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            float now = mc.level.getGameTime() + partialTick;
            Object[] atk = net.zennin.mushoku.client.fp.FpAttack.current(now);
            if (atk != null) {
                ci.cancel();
                if (hand == InteractionHand.MAIN_HAND) {
                    net.zennin.mushoku.client.fp.FpAnim a = (net.zennin.mushoku.client.fp.FpAnim) atk[0];
                    float[] fp = net.zennin.mushoku.client.fp.FpRender.pose("attack", a, (Float) atk[1], now);
                    net.zennin.mushoku.client.fp.FpRender.arms(player, pose, buffers, light, fp);
                    net.zennin.mushoku.client.fp.FpRender.heldItem(player, pose, buffers, light, fp, player.getMainHandItem(), true);
                }
                return;
            }
        }
        ItemStack bow = player.getMainHandItem();
        if (!(bow.getItem() instanceof MushokuBowItem) || BowRender.hidden.test(player)) return;
        ci.cancel();
        if (hand == InteractionHand.MAIN_HAND) BowFirstPerson.render(player, bow, partialTick, pose, buffers, light);
    }
}

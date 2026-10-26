package net.zennin.mushoku.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.client.render.BowRender;
import net.zennin.mushoku.item.MushokuBowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arcos del mod: se agarran con la mano contraria a la habil (como en Unturned) y la habil tensa la cuerda. El arco
 * se dibuja en ese brazo (aunque este en la mano principal) y la mano principal queda libre.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
    public ItemInHandLayerMixin(RenderLayerParent<T, M> parent) { super(parent); }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void remushoku$bowInOtherHand(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, HumanoidArm arm,
                                          PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!(entity instanceof Player p)) return;
        ItemStack bow = p.getMainHandItem();
        if (!(bow.getItem() instanceof MushokuBowItem)) return;
        HumanoidArm bowArm = p.getMainArm().getOpposite();
        if (BowRender.hidden.test(p)) return;
        ci.cancel();
        if (arm != bowArm) return;
        BowRender.drawInHand(p, bow, getParentModel(), bowArm, pose, buffers, light, OverlayTexture.NO_OVERLAY);
    }
}

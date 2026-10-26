package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.item.QuiverItem;

/** El carcaj se ve colgado en diagonal en la espalda mientras lo llevas en el inventario. */
public class QuiverLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public QuiverLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack quiver = QuiverItem.find(player);
        if (quiver.isEmpty() || player.getMainHandItem() == quiver || player.getOffhandItem() == quiver) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Espalda (el modelo mira a -Z; +Y hacia abajo), en diagonal del hombro derecho a la cadera izquierda
        pose.translate(0, 0.35, 0.2);
        pose.mulPose(Axis.ZP.rotationDegrees(-35));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(0.75f, -0.75f, -0.75f);
        Minecraft.getInstance().getItemRenderer().renderStatic(quiver, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, player.level(), player.getId());
        pose.popPose();
    }
}

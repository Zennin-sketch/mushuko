package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.client.render.QuiverRender;
import net.zennin.mushoku.item.QuiverItem;

/**
 * El carcaj equipado en su ranura cuelga en diagonal en la espalda, con la boca sobre el hombro derecho (de donde
 * la mano saca las flechas al cargar el arco).
 */
public class QuiverLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public QuiverLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack quiver = QuiverItem.find(player);
        if (quiver.isEmpty()) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // espalda (+Z en el modelo; +Y hacia abajo): centro a media espalda, boca hacia el hombro derecho
        pose.translate(0.4f / 16f, 6.0f / 16f, 4.3f / 16f);
        pose.mulPose(Axis.ZP.rotationDegrees(148));
        pose.scale(0.82f, 0.82f, 0.82f);
        pose.translate(0, -QuiverRender.H / 2 / 16f, 0);
        QuiverRender.draw(pose, buffers, quiver, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}

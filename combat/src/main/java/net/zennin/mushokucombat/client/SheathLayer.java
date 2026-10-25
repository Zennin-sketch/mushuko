package net.zennin.mushokucombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Dibuja el arma guardada en la cintura o en la espalda. Usa la misma cadena de transformaciones que el arma en la
 * mano, como si la sostuviera un brazo invisible pegado al torso en la pose exacta del agarre.
 */
public class SheathLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ItemInHandRenderer items;

    public SheathLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ItemInHandRenderer items) {
        super(parent);
        this.items = items;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack stack = player.getMainHandItem();
        ClientSheath.Slot s = ClientSheath.slotOf(stack);
        if (s == null || ClientSheath.handVisible(player)) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(s.pos()[0] / 16f, s.pos()[1] / 16f, s.pos()[2] / 16f);
        pose.mulPose(new Quaternionf(s.rot()[0], s.rot()[1], s.rot()[2], s.rot()[3]));
        if (s.bend() != 0f) { // mismo doblez de codo que HeldItemMixin de Player Animator
            pose.translate(0, 0.25f, 0);
            float a = -s.axis();
            pose.mulPose(new Quaternionf().rotateAxis(s.bend(), (float) Math.cos(a), 0, (float) Math.sin(a)));
            pose.translate(0, -0.25f, 0);
        }
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.translate(1f / 16f, 0.125f, -0.625f);
        pose.mulPose(Axis.ZP.rotation(s.wrist()[2]));
        pose.mulPose(Axis.YP.rotation(s.wrist()[1]));
        pose.mulPose(Axis.XP.rotation(s.wrist()[0]));
        items.renderItem(player, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers, light);
        pose.popPose();
    }
}

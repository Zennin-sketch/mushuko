package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.zennin.mushoku.block.RefiningStoneBlockEntity;

/** La piedra apoyada flota y gira despacio sobre el piso, con un brillo. */
public class RefiningStoneRenderer implements BlockEntityRenderer<RefiningStoneBlockEntity> {
    public RefiningStoneRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RefiningStoneBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.stone().isEmpty() || be.getLevel() == null) return;
        float t = be.getLevel().getGameTime() + partial;
        float spin = be.rolling ? t * 18f : t * 2.5f;
        pose.pushPose();
        pose.translate(0.5, 0.3 + 0.05 * Math.sin(t * 0.12), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(be.stone(), ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY,
                pose, buffers, be.getLevel(), 0);
        pose.popPose();
        if (be.getLevel().random.nextInt(be.rolling ? 2 : 12) == 0)
            be.getLevel().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, be.getBlockPos().getX() + 0.5,
                    be.getBlockPos().getY() + 0.9, be.getBlockPos().getZ() + 0.5, (be.getLevel().random.nextDouble() - 0.5), -0.5,
                    (be.getLevel().random.nextDouble() - 0.5));
    }
}

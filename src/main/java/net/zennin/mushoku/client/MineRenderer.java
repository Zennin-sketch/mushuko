package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.entity.MineEntity;

/** Mina del Campo Minado (modelo voxel minefield_mine): gira en el aire y queda latiendo en el suelo. */
public class MineRenderer extends EntityRenderer<MineEntity> {
    public MineRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(MineEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial;
        pose.pushPose();
        if (!e.armed()) {
            pose.mulPose(Axis.YP.rotationDegrees(t * 25));
            pose.mulPose(Axis.XP.rotationDegrees(t * 17));
        } else {
            pose.translate(0, 0.12, 0);
        }
        float s = 0.8f / 16f;
        pose.scale(s, s, s);
        SpellModelRenderer.render(SpellModel.get("minefield_mine"), pose, buffers, 0xF000F0, t, 0);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MineEntity e) { return ResourceLocation.withDefaultNamespace("textures/misc/white.png"); }
}

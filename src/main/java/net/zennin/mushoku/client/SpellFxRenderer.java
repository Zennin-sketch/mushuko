package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.entity.SpellFxEntity;

/** Dibuja los efectos de hechizos (nubes, pinchos, cristales, escudos...) con su modelo voxel. */
public class SpellFxRenderer extends EntityRenderer<SpellFxEntity> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("remushoku", "textures/entity/spell/cloud.png");

    public SpellFxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(SpellFxEntity entity, Frustum frustum, double x, double y, double z) {
        // Los efectos son mucho mas grandes que su caja (una nube mide 18 bloques): no recortar por la caja
        return entity.shouldRenderAtSqrDistance(entity.distanceToSqr(x, y, z));
    }

    @Override
    public void render(SpellFxEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        SpellModel model = SpellModel.get(entity.model());
        float s = entity.scale() / 16f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        pose.scale(s, s, s);
        SpellModelRenderer.render(model, pose, buffers, light, entity.tickCount + partial, entity.life());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpellFxEntity entity) {
        return TEX;
    }
}

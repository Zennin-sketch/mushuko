package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;

/** Dibuja cada proyectil magico con su modelo voxel (bala de piedra, bola de fuego, etc.) orientado hacia donde vuela. */
public class MagicProjectileRenderer extends EntityRenderer<MagicProjectile> {
    private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath("remushoku", "textures/entity/spell/stone.png");

    public MagicProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    /** Modelo segun elemento y variante; null = invisible (golpes extra que solo existen para pegar). */
    public static String modelFor(MagicProjectile p) {
        if (p.variant() == MagicProjectile.VARIANT_HIDDEN) return null;
        return switch (p.kind()) {
            case MagicProjectile.ROCK -> "stone_bullet";
            case MagicProjectile.FIRE -> p.variant() == MagicProjectile.VARIANT_SLICE ? "fire_crescent" : "fireball";
            case MagicProjectile.ICE -> "icicle";
            case MagicProjectile.WIND -> p.variant() == MagicProjectile.VARIANT_DRILL ? "wind_drill" : "wind_blade";
            default -> p.variant() == MagicProjectile.VARIANT_ARROW ? "water_arrow" : "water_ball";
        };
    }

    @Override
    public void render(MagicProjectile entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        String name = modelFor(entity);
        if (name == null) return;
        SpellModel model = SpellModel.get(name);
        float t = entity.tickCount + partial;
        // Aparece en 3 ticks para no "saltar" delante de la camara
        float scale = entity.sizeScale() / 16f * Mth.clamp(t / 3f, 0.25f, 1f);

        pose.pushPose();
        pose.translate(0, entity.getBbHeight() / 2, 0);
        Vec3 v = entity.getDeltaMovement();
        if (v.lengthSqr() > 1.0e-6) {
            float ry = (float) Math.toDegrees(Math.atan2(v.x, v.z));
            float rx = (float) Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
            pose.mulPose(Axis.YP.rotationDegrees(ry));
            pose.mulPose(Axis.XP.rotationDegrees(-rx));
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partial, entity.yRotO, entity.getYRot())));
        }
        pose.scale(scale, scale, scale);
        SpellModelRenderer.render(model, pose, buffers, light, t, 0);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectile entity) {
        return FALLBACK;
    }
}

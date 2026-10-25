package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

    public static String modelFor(MagicProjectile p) {
        return switch (p.kind()) {
            case MagicProjectile.ROCK -> "stone_bullet";
            case MagicProjectile.FIRE -> "fireball";
            case MagicProjectile.ICE -> "icicle";
            case MagicProjectile.WIND -> p.variant() == 1 ? "wind_drill" : "wind_blade";
            default -> "water_ball";
        };
    }

    @Override
    public void render(MagicProjectile entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        SpellModel model = SpellModel.get(modelFor(entity));
        float t = entity.tickCount + partial;
        float scale = entity.sizeScale() / 16f;

        // Fade-in corto para que no "aparezca" de golpe delante de la camara
        float appear = Mth.clamp(t / 3f, 0.25f, 1f);

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
        pose.scale(scale * appear, scale * appear, scale * appear);

        int gi = 0;
        for (SpellModel.Group g : model.groups) {
            pose.pushPose();
            if (g.spin()[0] != 0 || g.spin()[1] != 0 || g.spin()[2] != 0) {
                pose.mulPose(Axis.ZP.rotationDegrees(g.spin()[2] * t));
                pose.mulPose(Axis.YP.rotationDegrees(g.spin()[1] * t));
                pose.mulPose(Axis.XP.rotationDegrees(g.spin()[0] * t));
            }
            if (g.pulseAmp() != 0) {
                float s = 1f + g.pulseAmp() * Mth.sin(t * Mth.TWO_PI / g.pulsePeriod());
                pose.scale(s, s, s);
            }
            float alphaMul = 1f;
            if (g.flicker() > 0) {
                alphaMul = 1f - g.flicker() * Math.abs(Mth.sin(t * 0.9f + gi * 1.7f) * Mth.sin(t * 0.37f + gi));
            }
            RenderType type = switch (g.render()) {
                case SOLID -> RenderType.entityCutoutNoCull(g.texture() != null ? g.texture() : FALLBACK);
                case EMISSIVE -> RenderType.entityTranslucentEmissive(g.texture());
                case TRANSLUCENT -> RenderType.entityTranslucent(g.texture());
            };
            int l = g.glow() ? LightTexture.FULL_BRIGHT : light;
            VertexConsumer vc = buffers.getBuffer(type);
            PoseStack.Pose last = pose.last();
            for (SpellModel.Face f : g.faces()) {
                int a = Mth.clamp(Math.round(f.alpha() * alphaMul * 255f), 0, 255);
                float[] p = f.xyz(), uv = f.uv();
                for (int i = 0; i < 4; i++) {
                    vc.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                            .setColor(f.r(), f.g(), f.b(), a)
                            .setUv(uv[i * 2], uv[i * 2 + 1])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(l)
                            .setNormal(last, f.nx(), f.ny(), f.nz());
                }
            }
            pose.popPose();
            gi++;
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectile entity) {
        return FALLBACK;
    }
}

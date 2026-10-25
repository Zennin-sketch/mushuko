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
            float[] tl = g.tilt();
            if (tl[0] != 0 || tl[1] != 0 || tl[2] != 0) {
                pose.mulPose(Axis.ZP.rotationDegrees(tl[2]));
                pose.mulPose(Axis.YP.rotationDegrees(tl[1]));
                pose.mulPose(Axis.XP.rotationDegrees(tl[0]));
            }
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
                case SOLID -> RenderType.entityCutoutNoCull(g.texture());
                case EMISSIVE -> RenderType.entityTranslucentEmissive(g.texture());
                case TRANSLUCENT -> RenderType.entityTranslucent(g.texture());
            };
            int l = g.glow() ? LightTexture.FULL_BRIGHT : light;
            VertexConsumer vc = buffers.getBuffer(type);
            draw(vc, pose.last(), g.faces(), l, alphaMul, -1);
            if (g.emitter() != null) drawEmitter(vc, pose, g.emitter(), t, l, g.render() == SpellModel.Render.SOLID);
            pose.popPose();
            gi++;
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** Dibuja caras; si 'rgb' >= 0 reemplaza el color de cada cara. */
    private static void draw(VertexConsumer vc, PoseStack.Pose last, java.util.List<SpellModel.Face> faces, int light, float alphaMul, int rgb) {
        for (SpellModel.Face f : faces) {
            int a = Mth.clamp(Math.round(f.alpha() * alphaMul * 255f), 0, 255);
            if (a == 0) continue;
            int r = rgb >= 0 ? (rgb >> 16) & 255 : f.r(), gr = rgb >= 0 ? (rgb >> 8) & 255 : f.g(), b = rgb >= 0 ? rgb & 255 : f.b();
            float[] p = f.xyz(), uv = f.uv();
            for (int i = 0; i < 4; i++) {
                vc.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                        .setColor(r, gr, b, a)
                        .setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(last, f.nx(), f.ny(), f.nz());
            }
        }
    }

    /** Cubitos que nacen, suben/se alejan, se achican y se apagan (fuego que sube, gotas, polvo, motas de viento). */
    private static void drawEmitter(VertexConsumer vc, PoseStack pose, SpellModel.Emitter e, float t, int light, boolean solid) {
        for (int i = 0; i < e.count(); i++) {
            float age = t / e.life() + SpellModel.hash(i, 4);
            int cycle = (int) Math.floor(age);
            age -= cycle;
            int seed = i * 31 + cycle * 7;  // cada vida nace en otro lugar
            float bx = e.radius()[0] * (2 * SpellModel.hash(seed, 1) - 1);
            float by = e.radius()[1] * (2 * SpellModel.hash(seed, 2) - 1);
            float bz = e.radius()[2] * (2 * SpellModel.hash(seed, 3) - 1);
            float len = Mth.sqrt(bx * bx + by * by + bz * bz) + 1.0e-3f;
            float ticks = age * e.life();
            float x = bx + e.outward() * ticks * bx / len;
            float y = by + e.outward() * ticks * by / len + e.rise() * ticks;
            float z = bz + e.outward() * ticks * bz / len - e.drift() * ticks;
            float size = Mth.lerp(age, e.size0(), e.size1());
            int c0 = e.color0(), c1 = e.color1();
            int rgb = (Math.round(Mth.lerp(age, (c0 >> 16) & 255, (c1 >> 16) & 255)) << 16)
                    | (Math.round(Mth.lerp(age, (c0 >> 8) & 255, (c1 >> 8) & 255)) << 8)
                    | Math.round(Mth.lerp(age, c0 & 255, c1 & 255));
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(SpellModel.hash(seed, 5) * 90));
            pose.mulPose(Axis.XP.rotationDegrees(SpellModel.hash(seed, 6) * 90));
            pose.scale(size, size, size);
            draw(vc, pose.last(), SpellModel.UNIT_CUBE, light, solid ? 1f : e.alpha() * (1 - age), rgb);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectile entity) {
        return FALLBACK;
    }
}

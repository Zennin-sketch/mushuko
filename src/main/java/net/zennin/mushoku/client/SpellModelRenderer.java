package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Dibuja un SpellModel en el tick 't' (con fraccion). La PoseStack ya tiene que estar en unidades de pixel del modelo.
 * Orden de cada grupo (igual que la pagina de preview): traslado (offset + rise) -> tilt -> espiral -> escala radial
 * -> giro -> pulso/crecer. 'life' > 0 activa el final del modelo (outro).
 */
public final class SpellModelRenderer {
    private SpellModelRenderer() {}

    private static float clamp01(float x) { return Mth.clamp(x, 0f, 1f); }
    private static float easeOut(float k) { float a = 1 - k; return 1 - a * a * a; }
    private static float progress(float[] w, float t) { return easeOut(clamp01((t - w[0]) / Math.max(1f, w[1] - w[0]))); }

    public static void render(SpellModel model, PoseStack pose, MultiBufferSource buffers, int light, float t, float life) {
        float outroAlpha = 1f;
        pose.pushPose();
        if (life > 0 && model.outro != null && t > life - model.outro[0]) {
            float o = clamp01((t - (life - model.outro[0])) / model.outro[0]);
            outroAlpha = 1 - o;
            pose.translate(0, -model.outro[1] * o, 0);
            float s = 1 - model.outro[2] * o;
            pose.scale(s, s, s);
        }
        if (life > 0 && t > life) { pose.popPose(); return; }
        int gi = 0;
        for (SpellModel.Group g : model.groups) {
            renderGroup(g, gi++, pose, buffers, light, t, outroAlpha);
        }
        pose.popPose();
    }

    private static float showFade(SpellModel.Group g, float t) {
        if (g.show == null) return 1f;
        float best = 0;
        for (float[] w : g.show)
            if (t >= w[0] && t <= w[1]) best = Math.max(best, clamp01(Math.min(t - w[0], w[1] - t) / 3f + 0.001f));
        return best;
    }

    private static void renderGroup(SpellModel.Group g, int gi, PoseStack pose, MultiBufferSource buffers, int light, float t, float outroAlpha) {
        float fade = showFade(g, t);
        if (fade <= 0) return;
        pose.pushPose();
        float riseY = 0;
        if (g.rise != null) riseY = -g.rise[2] * (1 - progress(g.rise, t));
        pose.translate(g.offset[0], g.offset[1] + riseY, g.offset[2]);
        if (g.tilt[0] != 0 || g.tilt[1] != 0 || g.tilt[2] != 0) {
            pose.mulPose(Axis.ZP.rotationDegrees(g.tilt[2]));
            pose.mulPose(Axis.YP.rotationDegrees(g.tilt[1]));
            pose.mulPose(Axis.XP.rotationDegrees(g.tilt[0]));
        }
        if (g.swirl != null) {
            float k = progress(g.swirl, t);
            pose.mulPose(Axis.YP.rotationDegrees(g.swirl[2] * (1 - k) * (1 - k)));
            float r = 1 + (g.swirl[3] - 1) * (1 - k);
            pose.scale(r, 1, r);
        }
        if (g.spin[0] != 0 || g.spin[1] != 0 || g.spin[2] != 0) {
            pose.mulPose(Axis.ZP.rotationDegrees(g.spin[2] * t));
            pose.mulPose(Axis.YP.rotationDegrees(g.spin[1] * t));
            pose.mulPose(Axis.XP.rotationDegrees(g.spin[0] * t));
        }
        float sc = g.pulseAmp != 0 ? 1f + g.pulseAmp * Mth.sin(t * Mth.TWO_PI / g.pulsePeriod) : 1f;
        if (g.grow != null) sc *= progress(g.grow, t);
        if (sc <= 1.0e-4f) { pose.popPose(); return; }
        pose.scale(sc, sc, sc);

        float alpha = fade * outroAlpha;
        if (g.flicker > 0) alpha *= 1f - g.flicker * Math.abs(Mth.sin(t * 0.9f + gi * 1.7f) * Mth.sin(t * 0.37f + gi));
        RenderType type = switch (g.render) {
            case SOLID -> RenderType.entityCutoutNoCull(g.texture);
            case EMISSIVE -> RenderType.entityTranslucentEmissive(g.texture);
            case TRANSLUCENT -> RenderType.entityTranslucent(g.texture);
        };
        int l = g.glow ? LightTexture.FULL_BRIGHT : light;
        VertexConsumer vc = buffers.getBuffer(type);
        float[] staggerK = null;
        if (g.stagger != null && g.boxes > 0) {
            staggerK = new float[g.boxes];
            for (int i = 0; i < g.boxes; i++) {
                float start = g.stagger[0] + (g.stagger[1] - g.stagger[0]) * i / g.boxes;
                staggerK[i] = easeOut(clamp01((t - start) / 4f));
            }
        }
        draw(vc, pose.last(), g.faces, l, g.render == SpellModel.Render.SOLID ? 1f : alpha, -1, staggerK);
        if (g.emitter != null) drawEmitter(vc, pose, g.emitter, t, l, g.render == SpellModel.Render.SOLID, fade * outroAlpha);
        pose.popPose();
    }

    /** Dibuja caras; si 'rgb' >= 0 reemplaza el color; 'staggerK' achica cada caja hacia su centro. */
    static void draw(VertexConsumer vc, PoseStack.Pose last, List<SpellModel.Face> faces, int light, float alphaMul, int rgb, float[] staggerK) {
        for (SpellModel.Face f : faces) {
            float k = staggerK != null ? staggerK[f.box()] : 1f;
            if (k <= 0) continue;
            int a = Mth.clamp(Math.round(f.alpha() * alphaMul * 255f), 0, 255);
            if (a == 0) continue;
            int r = rgb >= 0 ? (rgb >> 16) & 255 : f.r(), gr = rgb >= 0 ? (rgb >> 8) & 255 : f.g(), b = rgb >= 0 ? rgb & 255 : f.b();
            float[] p = f.xyz(), uv = f.uv();
            for (int i = 0; i < 4; i++) {
                float x = p[i * 3], y = p[i * 3 + 1], z = p[i * 3 + 2];
                if (k < 1f) {
                    x = f.cx() + (x - f.cx()) * k;
                    y = f.cy() + (y - f.cy()) * k;
                    z = f.cz() + (z - f.cz()) * k;
                }
                vc.addVertex(last, x, y, z)
                        .setColor(r, gr, b, a)
                        .setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(last, f.nx(), f.ny(), f.nz());
            }
        }
    }

    /** Cubitos que nacen, suben/se alejan, se achican y se apagan (fuego que sube, gotas, polvo, lluvia...). */
    private static void drawEmitter(VertexConsumer vc, PoseStack pose, SpellModel.Emitter e, float t, int light, boolean solid, float alphaMul) {
        boolean shaped = e.shape()[0] != 1 || e.shape()[1] != 1 || e.shape()[2] != 1;
        for (int i = 0; i < e.count(); i++) {
            float age;
            int seed;
            if (e.burst()) {
                age = t / e.life();
                seed = i * 31;
                if (age >= 1) continue;
            } else {
                age = t / e.life() + SpellModel.hash(i, 4);
                int cycle = (int) Math.floor(age);
                age -= cycle;
                seed = i * 31 + cycle * 7;  // cada vida nace en otro lugar
            }
            float bx = e.radius()[0] * (2 * SpellModel.hash(seed, 1) - 1);
            float by = e.radius()[1] * (2 * SpellModel.hash(seed, 2) - 1);
            float bz = e.radius()[2] * (2 * SpellModel.hash(seed, 3) - 1);
            float len = Mth.sqrt(bx * bx + by * by + bz * bz) + 1.0e-3f;
            float ticks = age * e.life();
            float x = e.center()[0] + bx + e.outward() * ticks * bx / len;
            float y = e.center()[1] + by + e.outward() * ticks * by / len + e.rise() * ticks;
            float z = e.center()[2] + bz + e.outward() * ticks * bz / len - e.drift() * ticks;
            float size = Mth.lerp(age, e.size0(), e.size1());
            int c0 = e.color0(), c1 = e.color1();
            int rgb = (Math.round(Mth.lerp(age, (c0 >> 16) & 255, (c1 >> 16) & 255)) << 16)
                    | (Math.round(Mth.lerp(age, (c0 >> 8) & 255, (c1 >> 8) & 255)) << 8)
                    | Math.round(Mth.lerp(age, c0 & 255, c1 & 255));
            pose.pushPose();
            pose.translate(x, y, z);
            if (!shaped) {
                pose.mulPose(Axis.YP.rotationDegrees(SpellModel.hash(seed, 5) * 90));
                pose.mulPose(Axis.XP.rotationDegrees(SpellModel.hash(seed, 6) * 90));
            }
            pose.scale(size * e.shape()[0], size * e.shape()[1], size * e.shape()[2]);
            draw(vc, pose.last(), SpellModel.UNIT_CUBE, light, solid ? 1f : e.alpha() * (1 - age) * alphaMul, rgb, null);
            pose.popPose();
        }
    }
}

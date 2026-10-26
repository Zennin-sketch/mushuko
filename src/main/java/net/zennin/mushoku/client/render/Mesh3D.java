package net.zennin.mushoku.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dibujo de piezas 3D lisas (arcos, carcaj, flechas) sin texturas pixeladas: cajas y prismas con color por vertice.
 * Todo en pixeles (1/16 de bloque) sobre la pose que se pase.
 */
public final class Mesh3D {
    public static final ResourceLocation WHITE = MushokuMod.id("textures/misc/white.png");

    private Mesh3D() {}

    public static VertexConsumer solid(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
    }

    public static VertexConsumer glow(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
    }

    /** Multiplica el color (sombras y vetas). */
    public static int shade(int argb, float k) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        r = Math.min(255, Math.round(r * k));
        g = Math.min(255, Math.round(g * k));
        b = Math.min(255, Math.round(b * k));
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Caja entre (x0,y0,z0) y (x1,y1,z1), en pixeles. */
    public static void box(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                           int color, int light, int overlay) {
        float s = 1 / 16f;
        x0 *= s; y0 *= s; z0 *= s; x1 *= s; y1 *= s; z1 *= s;
        PoseStack.Pose p = pose.last();
        Matrix4f m = p.pose();
        // caras: -x +x -y +y -z +z
        quad(vc, p, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, color, light, overlay);
        quad(vc, p, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, color, light, overlay);
        quad(vc, p, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, color, light, overlay);
        quad(vc, p, m, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, color, light, overlay);
        quad(vc, p, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, color, light, overlay);
        quad(vc, p, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, color, light, overlay);
    }

    /** Caja centrada en el origen. */
    public static void cbox(PoseStack pose, VertexConsumer vc, float w, float h, float d, int color, int light, int overlay) {
        box(pose, vc, -w / 2, -h / 2, -d / 2, w / 2, h / 2, d / 2, color, light, overlay);
    }

    /**
     * Barra de seccion rectangular de a a b (pixeles). 'w' es el ancho sobre 'side' (un eje perpendicular de
     * referencia) y 't' el grosor sobre el tercer eje.
     */
    public static void bar(PoseStack pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f side, float w, float t,
                           int color, int light, int overlay) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1e-4f) return;
        d.div(len);
        Vector3f x = new Vector3f(side).sub(new Vector3f(d).mul(side.dot(d)));
        if (x.lengthSquared() < 1e-6f) x.set(Math.abs(d.y) < 0.9f ? 0 : 1, Math.abs(d.y) < 0.9f ? 1 : 0, 0).cross(d);
        x.normalize();
        Vector3f z = new Vector3f(x).cross(d).normalize();
        pose.pushPose();
        pose.translate(a.x / 16f, a.y / 16f, a.z / 16f);
        Matrix4f r = new Matrix4f(x.x, x.y, x.z, 0, d.x, d.y, d.z, 0, z.x, z.y, z.z, 0, 0, 0, 0, 1);
        pose.mulPose(r);
        box(pose, vc, -w / 2, 0, -t / 2, w / 2, len, t / 2, color, light, overlay);
        pose.popPose();
    }

    /** Prisma de 'sides' lados a lo largo de Y (de y0 a y1), radio r, centrado en (cx, cz). */
    public static void prism(PoseStack pose, VertexConsumer vc, float cx, float cz, float r, float y0, float y1, int sides,
                             int color, int light, int overlay, boolean caps) {
        float s = 1 / 16f;
        PoseStack.Pose p = pose.last();
        Matrix4f m = p.pose();
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides, am = (a0 + a1) / 2;
            float x0 = (float) (cx + r * Math.cos(a0)) * s, z0 = (float) (cz + r * Math.sin(a0)) * s;
            float x1 = (float) (cx + r * Math.cos(a1)) * s, z1 = (float) (cz + r * Math.sin(a1)) * s;
            float nx = (float) Math.cos(am), nz = (float) Math.sin(am);
            int c = shade(color, 0.9f + 0.1f * (float) Math.sin(am * 3 + 1));
            quad(vc, p, m, x1, y0 * s, z1, x0, y0 * s, z0, x0, y1 * s, z0, x1, y1 * s, z1, nx, 0, nz, c, light, overlay);
            if (caps) {
                quad(vc, p, m, cx * s, y1 * s, cz * s, cx * s, y1 * s, cz * s, x0, y1 * s, z0, x1, y1 * s, z1, 0, 1, 0, shade(color, 0.8f), light, overlay);
                quad(vc, p, m, cx * s, y0 * s, cz * s, x1, y0 * s, z1, x0, y0 * s, z0, cx * s, y0 * s, cz * s, 0, -1, 0, shade(color, 0.7f), light, overlay);
            }
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, Matrix4f m,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float dx, float dy, float dz, float nx, float ny, float nz, int color, int light, int overlay) {
        vc.addVertex(m, ax, ay, az).setColor(color).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, bx, by, bz).setColor(color).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, cx, cy, cz).setColor(color).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, dx, dy, dz).setColor(color).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
    }

    @SuppressWarnings("unused")
    private static Matrix3f unused() { return null; }
}

package net.zennin.mushokucombat.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.IBendable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Tuple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Consumidor de vertices que dobla la capa 3D igual que la parte del cuerpo (bendy-lib).
 * <p>
 * Los vertices llegan ya transformados por la pose; se juntan de a 4 (un cuadrado), se pasan al marco de la parte y,
 * si el cuadrado cruza la zona del doblez, se corta en tiras (si no, una caja entera de 12 px de alto solo tiene
 * esquinas arriba y abajo y se deforma toda). Del lado de la "base" todo gira alrededor del eje del doblez; en la
 * franja del medio el giro pasa de 0 al total, sin saltos. Despues se vuelve a aplicar la pose.
 */
public final class LayerBendConsumer implements VertexConsumer {
    private static final boolean ACTIVE = FabricLoader.getInstance().isModLoaded("playeranimator")
            && !Boolean.getBoolean("mushoku.nolayercompat");
    /** Media franja (en bloques) donde el giro pasa de 0 al total. */
    private static final float BAND = 1.5f / 16f;
    /** Cortes dentro de la franja (en unidades de "mid"). */
    private static final float[] CUTS = {-BAND, -BAND / 2f, 0f, BAND / 2f, BAND};

    private final VertexConsumer out;
    private final Matrix4f pose, inv;
    private final Matrix3f normal, invNormal;
    private final float cx, cy, cz, value;
    private final Vector3f axis;
    private final IBendable.Plane base, other;

    // cuadrado en armado: posicion local (x,y,z), uv, color, overlay, luz, normal local
    private final float[][] pos = new float[4][3];
    private final float[][] uv = new float[4][2];
    private final int[] color = new int[4], ov = new int[4], light = new int[4];
    private final Vector3f quadNormal = new Vector3f();
    private int n;

    private LayerBendConsumer(VertexConsumer out, PoseStack.Pose p, BendableCuboid b, float mx, float my, float mz) {
        this.out = out;
        this.pose = new Matrix4f(p.pose());
        this.inv = new Matrix4f(p.pose()).invert();
        this.normal = new Matrix3f(p.normal());
        this.invNormal = new Matrix3f(p.normal()).invert();
        // el centro del doblez esta en el marco de la parte; la malla esta corrida (mx,my,mz) px respecto de la parte
        cx = (b.getBendX() - mx) / 16f;
        cy = (b.getBendY() - my) / 16f;
        cz = (b.getBendZ() - mz) / 16f;
        Vector3f shift = new Vector3f(-mx / 16f, -my / 16f, -mz / 16f);
        base = shifted(b.getBasePlane().scaled(1 / 16f), shift);
        other = shifted(b.getOtherSidePlane().scaled(1 / 16f), shift);
        value = b.getBend();
        axis = new Vector3f((float) Math.cos(b.getBendAxis()), 0, (float) Math.sin(b.getBendAxis()))
                .mul(new Matrix3f().set(b.getBendDirection().getRotation()));
    }

    /** El mismo plano, corrido: un punto p del marco nuevo es p - shift en el viejo. */
    private static IBendable.Plane shifted(IBendable.Plane pl, Vector3f shift) {
        // distancia(p) = n.p + d ; en el marco nuevo p' = p + shift -> n.p' + (d - n.shift)
        Vector3f nrm = new Vector3f(pl.normal);
        float d = pl.distanceTo(new Vector3f()); // = d
        float nd = d - nrm.dot(shift);
        // punto del plano: -nd * n
        return new IBendable.Plane(nrm, new Vector3f(nrm).mul(-nd));
    }

    /** Capa externa -> parte del cuerpo (la capa 3D se dibuja en la capa externa, pero el doblez vigente esta en la parte). */
    private static final java.util.Map<ModelPart, ModelPart> PAIRS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static void pair(ModelPart layer, ModelPart body) { PAIRS.put(layer, body); }

    private static Field FX, FY, FZ;
    private static boolean fieldsTried;

    private static float meshField(Object mesh, int i) {
        try {
            if (!fieldsTried) {
                fieldsTried = true;
                Class<?> c = Class.forName("dev.tr7zw.skinlayers.versionless.render.CustomModelPart");
                FX = c.getDeclaredField("x"); FX.setAccessible(true);
                FY = c.getDeclaredField("y"); FY.setAccessible(true);
                FZ = c.getDeclaredField("z"); FZ.setAccessible(true);
            }
            if (FX == null) return 0f;
            return (i == 0 ? FX : i == 1 ? FY : FZ).getFloat(mesh);
        } catch (Throwable t) {
            FX = null;
            return 0f;
        }
    }

    /** Devuelve el consumidor que dobla, o null si esa parte no esta doblada. */
    public static VertexConsumer wrap(Object mesh, ModelPart part, PoseStack.Pose pose, VertexConsumer vc) {
        if (!ACTIVE || part == null) return null;
        try {
            ModelPart src = PAIRS.getOrDefault(part, part);
            Optional<MutableCuboid> c = ModelPartAccessor.optionalGetCuboid(src, 0);
            if (c.isEmpty()) return null;
            Tuple<String, ?> m = c.get().getActiveMutator();
            if (m == null || !(m.getB() instanceof BendableCuboid b) || Math.abs(b.getBend()) < 1.0e-4f) return null;
            return new LayerBendConsumer(vc, pose, b, meshField(mesh, 0), meshField(mesh, 1), meshField(mesh, 2));
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ armado del cuadrado
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (n == 4) n = 0;
        Vector4f v = inv.transform(new Vector4f(x, y, z, 1f));
        pos[n][0] = v.x; pos[n][1] = v.y; pos[n][2] = v.z;
        color[n] = 0xFFFFFFFF; uv[n][0] = uv[n][1] = 0; ov[n] = 0; light[n] = 0;
        return this;
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a) { color[n] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255); return this; }
    @Override public VertexConsumer setUv(float u, float v) { uv[n][0] = u; uv[n][1] = v; return this; }
    @Override public VertexConsumer setUv1(int u, int v) { ov[n] = (u & 0xFFFF) | (v & 0xFFFF) << 16; return this; }
    @Override public VertexConsumer setUv2(int u, int v) { light[n] = (u & 0xFFFF) | (v & 0xFFFF) << 16; return this; }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        // la normal es el ultimo dato de cada vertice
        if (n == 0) invNormal.transform(quadNormal.set(x, y, z));
        n++;
        if (n == 4) flush();
        return this;
    }

    // ------------------------------------------------------------------ corte y doblez
    private float mid(float[] p) {
        Vector3f q = new Vector3f(p[0], p[1], p[2]);
        return (Math.abs(other.distanceTo(q)) - Math.abs(base.distanceTo(q))) / 2f;
    }

    /** Vertice de trabajo: x,y,z,u,v,mid */
    private void flush() {
        float[][] poly = new float[4][];
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float m = mid(pos[i]);
            poly[i] = new float[]{pos[i][0], pos[i][1], pos[i][2], uv[i][0], uv[i][1], m};
            lo = Math.min(lo, m);
            hi = Math.max(hi, m);
        }
        if (hi <= CUTS[0] || lo >= CUTS[CUTS.length - 1]) {
            emit(poly, poly.length);
            return;
        }
        // tiras entre cortes consecutivos (y los dos extremos abiertos)
        float prev = -Float.MAX_VALUE;
        for (int k = 0; k <= CUTS.length; k++) {
            float next = k < CUTS.length ? CUTS[k] : Float.MAX_VALUE;
            if (hi > prev && lo < next) {
                float[][] a = clip(poly, prev, true);
                float[][] s = clip(a, next, false);
                if (s.length >= 3) emit(s, s.length);
            }
            prev = next;
        }
    }

    /** Recorta el poligono a mid >= c (keepAbove) o mid <= c. */
    private static float[][] clip(float[][] in, float c, boolean keepAbove) {
        if (in.length == 0 || Float.isInfinite(c) || Math.abs(c) == Float.MAX_VALUE) return in;
        java.util.ArrayList<float[]> o = new java.util.ArrayList<>(in.length + 2);
        for (int i = 0; i < in.length; i++) {
            float[] A = in[i], B = in[(i + 1) % in.length];
            boolean ia = keepAbove ? A[5] >= c : A[5] <= c;
            boolean ib = keepAbove ? B[5] >= c : B[5] <= c;
            if (ia) o.add(A);
            if (ia != ib) {
                float t = (c - A[5]) / (B[5] - A[5]);
                float[] P = new float[6];
                for (int j = 0; j < 6; j++) P[j] = A[j] + (B[j] - A[j]) * t;
                P[5] = c;
                o.add(P);
            }
        }
        return o.toArray(new float[0][]);
    }

    /** Dibuja el poligono como abanico de cuadrados (un triangulo es un cuadrado con el ultimo vertice repetido). */
    private void emit(float[][] p, int len) {
        int c = color[0], o = ov[0], l = light[0];
        if (len == 4) {
            for (int i = 0; i < 4; i++) vertex(p[i], c, o, l);
            return;
        }
        for (int i = 1; i + 1 < len; i += 2) {
            vertex(p[0], c, o, l);
            vertex(p[i], c, o, l);
            vertex(p[i + 1], c, o, l);
            vertex(i + 2 < len ? p[i + 2] : p[i + 1], c, o, l);
        }
    }

    private void vertex(float[] p, int c, int o, int l) {
        float t = Math.max(0f, Math.min(1f, (p[5] + BAND) / (2 * BAND)));
        float w = t * t * (3 - 2 * t);
        Vector4f v = new Vector4f(p[0], p[1], p[2], 1f);
        Vector3f nr = new Vector3f(quadNormal);
        if (w > 0f) {
            float ang = value * w;
            new Matrix4f().translate(cx, cy, cz).rotate(ang, axis).translate(-cx, -cy, -cz).transform(v);
            new Matrix3f().rotate(ang, axis).transform(nr);
        }
        pose.transform(v);
        normal.transform(nr);
        out.addVertex(v.x, v.y, v.z);
        out.setColor(c);
        out.setUv(p[3], p[4]);
        out.setOverlay(o);
        out.setLight(l);
        out.setNormal(nr.x, nr.y, nr.z);
    }
}

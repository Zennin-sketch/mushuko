package net.zennin.mushoku.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Oreja articulada: tramos unidos (sin huecos, como los codos y rodillas con Bendy-lib) que se balancean y dan un tic
 * cada pocos segundos; la punta sigue con retraso. Mismo calculo que la pagina de preview (chainQuads).
 */
public final class CosmeticChain {
    /** Un cuadrilatero ya calculado: 4 puntos (pixeles del marco de la cabeza), normal y color. */
    public record Quad(Vector3f[] p, Vector3f n, int source, int rgb, float mul) {}

    private final float[] base, baseRot, widths, thick, shade;
    private final float[][] segs;
    private final int source, rgb;
    private final float colorMul;
    private final boolean hasInner, mirror;
    private final int innerSource, innerRgb, innerRings;
    private final float innerMul, innerWidth, twitch, sway, lag;

    CosmeticChain(JsonObject o) {
        base = f(o.getAsJsonArray("base"));
        baseRot = f(o.getAsJsonArray("base_rot"));
        JsonArray s = o.getAsJsonArray("segs");
        segs = new float[s.size()][];
        for (int i = 0; i < segs.length; i++) segs[i] = f(s.get(i).getAsJsonArray());
        widths = f(o.getAsJsonArray("widths"));
        thick = f(o.getAsJsonArray("thick"));
        shade = f(o.getAsJsonArray("shade"));
        Object[] c = CosmeticModel.parseColor(o.get("color").getAsString());
        source = (int) c[0]; rgb = (int) c[1]; colorMul = (float) c[2];
        hasInner = o.has("inner") && o.get("inner").isJsonObject();
        if (hasInner) {
            JsonObject in = o.getAsJsonObject("inner");
            Object[] ic = CosmeticModel.parseColor(in.get("color").getAsString());
            innerSource = (int) ic[0]; innerRgb = (int) ic[1]; innerMul = (float) ic[2];
            innerWidth = in.get("width").getAsFloat();
            innerRings = in.get("rings").getAsInt();
        } else {
            innerSource = 0; innerRgb = 0; innerMul = 1; innerWidth = 0; innerRings = 0;
        }
        twitch = o.get("twitch").getAsFloat();
        sway = o.get("sway").getAsFloat();
        lag = o.get("lag").getAsFloat();
        mirror = o.get("mirror").getAsBoolean();
    }

    private static float[] f(JsonArray a) {
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    private static float amp(boolean on, float x) {
        if (!on) return 0;
        if (x >= 0 && x < 8) return Mth.sin(Mth.PI * x / 8);
        if (x >= 8 && x < 14) return 0.4f * Mth.sin(Mth.PI * (x - 8) / 6);
        return 0;
    }

    private static final float D2R = Mth.DEG_TO_RAD;

    /** Cuadrilateros de ambas orejas en el tick t. hurt (0..1) = las orejas se echan para atras al recibir un golpe. */
    public List<Quad> quads(float t, float hurt) {
        List<Quad> out = new ArrayList<>();
        build(out, 0, t, hurt);
        if (mirror) build(out, 1, t, hurt);
        return out;
    }

    private void build(List<Quad> out, int side, float t, float hurt) {
        float T = t + side * 31;
        int w = Mth.floor(T / 70);
        float local = T - w * 70;
        float o = 10 + SpellModel.hash(w, 11 + side) * 40;
        boolean on = SpellModel.hash(w, 23 + side) < 0.65f;
        float d = local - o;
        // Igual que Euler 'ZYX' de three.js: primero X, despues Y, despues Z
        Quaternionf q = new Quaternionf().rotateZ(baseRot[2] * D2R).rotateY(baseRot[1] * D2R).rotateX(baseRot[0] * D2R);
        q.rotateX((sway * Mth.sin(t * 0.06f + side * 1.3f) - 35 * hurt) * D2R);
        int n = segs.length;
        Quaternionf[] frames = new Quaternionf[n];
        Vector3f[] P = new Vector3f[n + 1];
        P[0] = new Vector3f(base[0], base[1], base[2]);
        for (int k = 0; k < n; k++) {
            float a = amp(on, d - k * lag);
            q = new Quaternionf(q)
                    .rotateX((segs[k][1] + sway * 0.5f * Mth.sin(t * 0.06f + side * 1.3f - k * 0.5f)) * D2R)
                    .rotateZ((segs[k][2] - twitch * a * (k == 0 ? 1 : 0.25f)) * D2R);
            frames[k] = q;
            P[k + 1] = new Vector3f(0, segs[k][0], 0).rotate(q).add(P[k]);
        }
        Quaternionf[] R = new Quaternionf[n + 1];
        for (int i = 0; i <= n; i++)
            R[i] = i == 0 ? frames[0] : i == n ? frames[n - 1] : new Quaternionf(frames[i - 1]).slerp(frames[i], 0.5f);
        loft(out, side, P, R, n + 1, widths, thick, null, 0, source, rgb, colorMul, shade);
        if (hasInner) {
            int cnt = Math.min(n + 1, innerRings);
            float[] iw = new float[cnt], it = new float[cnt], off = new float[cnt], sh = new float[cnt];
            for (int i = 0; i < cnt; i++) { iw[i] = widths[i] * innerWidth; it[i] = 0.12f; off[i] = thick[i] / 2 + 0.06f; sh[i] = 1; }
            loft(out, side, P, R, cnt, iw, it, off, 0, innerSource, innerRgb, innerMul, sh);
        }
    }

    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

    private static void loft(List<Quad> out, int side, Vector3f[] P, Quaternionf[] R, int count, float[] w, float[] th, float[] off,
                             int unused, int source, int rgb, float mul, float[] shade) {
        Vector3f[][] rings = new Vector3f[count][4];
        for (int i = 0; i < count; i++) {
            for (int e = 0; e < 4; e++) {
                Vector3f v = new Vector3f(CORNERS[e][0] * w[i] / 2, 0, CORNERS[e][1] * th[i] / 2 + (off != null ? off[i] : 0)).rotate(R[i]).add(P[i]);
                if (side == 1) v.x = -v.x;
                rings[i][e] = v;
            }
        }
        Vector3f[] centers = new Vector3f[count];
        for (int i = 0; i < count; i++) centers[i] = new Vector3f(rings[i][0]).add(rings[i][1]).add(rings[i][2]).add(rings[i][3]).mul(0.25f);
        for (int i = 0; i < count - 1; i++) {
            Vector3f mid = new Vector3f(centers[i]).add(centers[i + 1]).mul(0.5f);
            for (int e = 0; e < 4; e++)
                add(out, new Vector3f[]{rings[i][e], rings[i][(e + 1) % 4], rings[i + 1][(e + 1) % 4], rings[i + 1][e]},
                        mid, source, rgb, mul * (shade[i] + shade[i + 1]) / 2);
        }
        Vector3f inside = count > 1 ? centers[1] : centers[0];
        add(out, new Vector3f[]{rings[0][3], rings[0][2], rings[0][1], rings[0][0]}, inside, source, rgb, mul * shade[0]);
        add(out, rings[count - 1], count > 1 ? centers[count - 2] : centers[0], source, rgb, mul * shade[count - 1]);
    }

    /** Agrega un cuadrilatero con la normal apuntando hacia afuera (lejos de 'inside'). */
    private static void add(List<Quad> out, Vector3f[] p, Vector3f inside, int source, int rgb, float mul) {
        Vector3f a = new Vector3f(p[2]).sub(p[0]), b = new Vector3f(p[3]).sub(p[1]);
        Vector3f n = a.cross(b);
        if (n.lengthSquared() < 1e-8f) n.set(0, 1, 0);
        else n.normalize();
        Vector3f c = new Vector3f(p[0]).add(p[1]).add(p[2]).add(p[3]).mul(0.25f).sub(inside);
        if (n.dot(c) < 0) n.negate();
        out.add(new Quad(p, n, source, rgb, mul));
    }
}

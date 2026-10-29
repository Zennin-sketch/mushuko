package net.zennin.mushoku.client.fp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Animacion de primera persona (se hace en el Taller de Combate, vista "1a persona"). Todo en el espacio de la
 * camara, en bloques: brazo derecho, brazo izquierdo (origen del modelo del brazo y giro) y el objeto, relativo al
 * brazo que lo sostiene. La curva entre keyframes es la misma que la del Taller (Hermite tipo Catmull-Rom; "frenar"
 * deja velocidad cero en ese keyframe).
 */
public final class FpAnim {
    /** Pose: rArm p(3) q(4), lArm p(3) q(4), item p(3) q(4). */
    public static final int SIZE = 21;
    public static final int R_ARM = 0, L_ARM = 7, ITEM = 14;
    private static final String[] PARTS = {"rArm", "lArm", "item"};

    public final String name;
    public final float length;
    public final boolean loop;
    private final float[] ticks;
    private final boolean[] stop;
    private final float[][] poses;

    private FpAnim(String name, float length, boolean loop, float[] ticks, boolean[] stop, float[][] poses) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.ticks = ticks;
        this.stop = stop;
        this.poses = poses;
    }

    public static FpAnim parse(String fallbackName, JsonObject j) {
        String name = j.has("name") ? j.get("name").getAsString() : fallbackName;
        float length = j.has("length") ? j.get("length").getAsFloat() : 20f;
        boolean loop = j.has("loop") && j.get("loop").getAsBoolean();
        List<JsonObject> keys = new ArrayList<>();
        for (JsonElement e : j.getAsJsonArray("keyframes")) keys.add(e.getAsJsonObject());
        keys.sort((a, b) -> Float.compare(a.get("tick").getAsFloat(), b.get("tick").getAsFloat()));
        int n = keys.size();
        float[] ticks = new float[n];
        boolean[] stop = new boolean[n];
        float[][] poses = new float[n][SIZE];
        for (int i = 0; i < n; i++) {
            JsonObject k = keys.get(i);
            ticks[i] = k.get("tick").getAsFloat();
            stop[i] = k.has("stop") && k.get("stop").getAsBoolean();
            JsonObject pose = k.getAsJsonObject("pose");
            for (int part = 0; part < 3; part++) {
                int o = part * 7;
                JsonObject pp = pose.has(PARTS[part]) ? pose.getAsJsonObject(PARTS[part]) : null;
                float[] def = {0, 0, 0, 0, 0, 0, 1};
                JsonArray p = pp != null && pp.has("p") ? pp.getAsJsonArray("p") : null;
                JsonArray q = pp != null && pp.has("q") ? pp.getAsJsonArray("q") : null;
                for (int c = 0; c < 3; c++) poses[i][o + c] = p != null ? p.get(c).getAsFloat() : def[c];
                for (int c = 0; c < 4; c++) poses[i][o + 3 + c] = q != null ? q.get(c).getAsFloat() : def[3 + c];
            }
        }
        // los cuaterniones de keyframes vecinos en el mismo hemisferio (si no, la curva da la vuelta larga)
        for (int i = 1; i < n; i++) {
            for (int part = 0; part < 3; part++) {
                int o = part * 7 + 3;
                float d = 0;
                for (int c = 0; c < 4; c++) d += poses[i][o + c] * poses[i - 1][o + c];
                if (d < 0) for (int c = 0; c < 4; c++) poses[i][o + c] = -poses[i][o + c];
            }
        }
        return new FpAnim(name, length, loop, ticks, stop, poses);
    }

    /** Pose en el tick t (con decimales). */
    public float[] eval(float t) {
        int n = ticks.length;
        float[] out = new float[SIZE];
        if (n == 0) return out;
        if (loop && length > 0) t = ((t % length) + length) % length;
        if (n == 1 || t <= ticks[0]) { System.arraycopy(poses[0], 0, out, 0, SIZE); return normalized(out); }
        if (t >= ticks[n - 1]) { System.arraycopy(poses[n - 1], 0, out, 0, SIZE); return normalized(out); }
        int i = 0;
        while (i < n - 2 && t > ticks[i + 1]) i++;
        float t0 = ticks[i], t1 = ticks[i + 1], dt = Math.max(1e-4f, t1 - t0);
        float u = (t - t0) / dt, u2 = u * u, u3 = u2 * u;
        float h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
        for (int c = 0; c < SIZE; c++) {
            float p0 = poses[i][c], p1 = poses[i + 1][c];
            float m0 = tangent(i, c) * dt, m1 = tangent(i + 1, c) * dt;
            out[c] = h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
        }
        return normalized(out);
    }

    private float tangent(int k, int c) {
        int n = ticks.length;
        if (stop[k] || k == 0 || k == n - 1) return 0f;
        return (poses[k + 1][c] - poses[k - 1][c]) / Math.max(1e-4f, ticks[k + 1] - ticks[k - 1]);
    }

    static float[] normalized(float[] pose) {
        for (int part = 0; part < 3; part++) {
            int o = part * 7 + 3;
            float l = (float) Math.sqrt(pose[o] * pose[o] + pose[o + 1] * pose[o + 1] + pose[o + 2] * pose[o + 2] + pose[o + 3] * pose[o + 3]);
            if (l < 1e-6f) { pose[o] = 0; pose[o + 1] = 0; pose[o + 2] = 0; pose[o + 3] = 1; continue; }
            for (int c = 0; c < 4; c++) pose[o + c] /= l;
        }
        return pose;
    }

    /** Mezcla dos poses (w = 0 -> a, 1 -> b). */
    public static float[] blend(float[] a, float[] b, float w) {
        float[] out = new float[SIZE];
        for (int part = 0; part < 3; part++) {
            int o = part * 7;
            for (int c = 0; c < 3; c++) out[o + c] = a[o + c] + (b[o + c] - a[o + c]) * w;
            float d = 0;
            for (int c = 3; c < 7; c++) d += a[o + c] * b[o + c];
            float s = d < 0 ? -1 : 1;
            for (int c = 3; c < 7; c++) out[o + c] = a[o + c] + (s * b[o + c] - a[o + c]) * w;
        }
        return normalized(out);
    }
}

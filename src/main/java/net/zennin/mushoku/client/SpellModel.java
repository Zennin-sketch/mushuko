package net.zennin.mushoku.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo voxel de un hechizo, leido de assets/remushoku/spell_models/<nombre>.json
 * (lo genera tools/spells/spell_models.py). Las caras de cada caja ya vienen giradas y con su UV.
 * Ver SpellModelRenderer para como se anima (giros, pulsos, espiral, aparicion, emisores).
 */
public final class SpellModel {
    public enum Render { SOLID, TRANSLUCENT, EMISSIVE }

    /** Una cara: 4 vertices (en pixeles), normal, UV, color y el centro/indice de su caja (para que aparezcan una por una). */
    public record Face(float[] xyz, float[] uv, float nx, float ny, float nz, int r, int g, int b, float alpha,
                       float cx, float cy, float cz, int box) {}

    /** Cubitos tipo particula: nacen al azar dentro de 'radius', suben/se alejan y se achican y apagan en 'life' ticks. */
    public record Emitter(int count, float life, float[] radius, float size0, float size1, int color0, int color1, float alpha,
                          float rise, float drift, float outward, float[] center, float[] shape, boolean burst) {}

    public static final class Group {
        public ResourceLocation texture;
        public Render render;
        public boolean glow;
        public float[] tilt = new float[3], spin = new float[3], offset = new float[3];
        public float pulseAmp, pulsePeriod = 1, flicker;
        public float[][] show;            // ventanas [a,b] o null = siempre
        public float[] grow, rise, swirl, stagger;
        public List<Face> faces = new ArrayList<>();
        public int boxes;
        public Emitter emitter;
    }

    public final List<Group> groups = new ArrayList<>();
    /** [ticks, hundirse_px, achicarse] o null. */
    public float[] outro;

    /** Cubo de 1x1x1 centrado (para los cubitos de los emisores). */
    public static final List<Face> UNIT_CUBE = new ArrayList<>();
    static { addBox(UNIT_CUBE, new float[]{0, 0, 0}, new float[]{1, 1, 1}, new float[]{0, 0, 0}, 0xffffff, 1f, 0); }

    /** Azar deterministico (identico al de la pagina de preview). */
    public static float hash(int i, int k) {
        double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    private static final Map<String, SpellModel> CACHE = new HashMap<>();

    public static void clearCache() { CACHE.clear(); }

    public static SpellModel get(String name) {
        return CACHE.computeIfAbsent(name, SpellModel::load);
    }

    private static SpellModel load(String name) {
        SpellModel m = new SpellModel();
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MushokuMod.MODID, "spell_models/" + name + ".json");
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) {
                MushokuMod.LOG.warn("Falta el modelo de hechizo {}", loc);
                return m;
            }
            try (Reader reader = res.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement ge : root.getAsJsonArray("groups")) m.groups.add(parseGroup(ge.getAsJsonObject()));
                if (root.has("outro")) m.outro = floats(root.getAsJsonArray("outro"));
            }
        } catch (Exception e) {
            MushokuMod.LOG.error("No se pudo leer el modelo de hechizo {}", loc, e);
        }
        return m;
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    private static float[] opt(JsonObject o, String key) {
        return o.has(key) ? floats(o.getAsJsonArray(key)) : null;
    }

    private static Group parseGroup(JsonObject o) {
        Group g = new Group();
        g.render = switch (o.get("render").getAsString()) {
            case "solid" -> Render.SOLID;
            case "emissive" -> Render.EMISSIVE;
            default -> Render.TRANSLUCENT;
        };
        g.texture = ResourceLocation.parse(o.get("texture").getAsString());
        g.glow = o.get("glow").getAsBoolean();
        if (o.has("tilt")) g.tilt = floats(o.getAsJsonArray("tilt"));
        if (o.has("offset")) g.offset = floats(o.getAsJsonArray("offset"));
        g.spin = floats(o.getAsJsonArray("spin"));
        float[] pulse = floats(o.getAsJsonArray("pulse"));
        g.pulseAmp = pulse[0];
        g.pulsePeriod = Math.max(1f, pulse[1]);
        g.flicker = o.get("flicker").getAsFloat();
        if (o.has("show")) {
            JsonArray s = o.getAsJsonArray("show");
            g.show = new float[s.size()][];
            for (int i = 0; i < s.size(); i++) g.show[i] = floats(s.get(i).getAsJsonArray());
        }
        g.grow = opt(o, "grow");
        g.rise = opt(o, "rise");
        g.swirl = opt(o, "swirl");
        g.stagger = opt(o, "stagger");
        int seed = 0;
        for (JsonElement be : o.getAsJsonArray("boxes")) {
            JsonObject b = be.getAsJsonObject();
            addBox(g.faces, floats(b.getAsJsonArray("at")), floats(b.getAsJsonArray("size")), floats(b.getAsJsonArray("rot")),
                    Integer.parseInt(b.get("color").getAsString(), 16), b.get("alpha").getAsFloat(), seed++);
        }
        g.boxes = seed;
        if (o.has("emit")) {
            JsonObject e = o.getAsJsonObject("emit");
            JsonArray size = e.getAsJsonArray("size"), colors = e.getAsJsonArray("colors");
            g.emitter = new Emitter(e.get("count").getAsInt(), Math.max(1f, e.get("life").getAsFloat()), floats(e.getAsJsonArray("radius")),
                    size.get(0).getAsFloat(), size.get(1).getAsFloat(),
                    Integer.parseInt(colors.get(0).getAsString(), 16), Integer.parseInt(colors.get(1).getAsString(), 16),
                    e.get("alpha").getAsFloat(), e.get("rise").getAsFloat(), e.get("drift").getAsFloat(), e.get("outward").getAsFloat(),
                    e.has("center") ? floats(e.getAsJsonArray("center")) : new float[3],
                    e.has("shape") ? floats(e.getAsJsonArray("shape")) : new float[]{1, 1, 1},
                    e.has("burst") && e.get("burst").getAsBoolean());
        }
        return g;
    }

    /** Giro X, luego Y, luego Z (igual que la pagina de preview). */
    static Matrix3f rotation(float rx, float ry, float rz) {
        return new Matrix3f().rotateZ((float) Math.toRadians(rz)).rotateY((float) Math.toRadians(ry)).rotateX((float) Math.toRadians(rx));
    }

    // Esquinas de cada cara (en unidades de -1..1), orden antihorario mirando desde afuera.
    private static final int[][][] FACES = {
            {{1, -1, 1}, {1, -1, -1}, {1, 1, -1}, {1, 1, 1}},     // +X
            {{-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}}, // -X
            {{-1, 1, 1}, {1, 1, 1}, {1, 1, -1}, {-1, 1, -1}},     // +Y
            {{-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1}}, // -Y
            {{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}},     // +Z
            {{1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}}, // -Z
    };
    private static final float[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    // Que ejes de la caja forman el ancho/alto de la textura en cada cara
    private static final int[][] FACE_AXES = {{2, 1}, {2, 1}, {0, 2}, {0, 2}, {0, 1}, {0, 1}};

    static void addBox(List<Face> out, float[] at, float[] size, float[] rot, int rgb, float alpha, int seed) {
        Matrix3f r = rotation(rot[0], rot[1], rot[2]);
        int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
        for (int f = 0; f < 6; f++) {
            float[] xyz = new float[12];
            for (int v = 0; v < 4; v++) {
                Vector3f p = new Vector3f(FACES[f][v][0] * size[0] / 2, FACES[f][v][1] * size[1] / 2, FACES[f][v][2] * size[2] / 2);
                r.transform(p);
                xyz[v * 3] = p.x + at[0];
                xyz[v * 3 + 1] = p.y + at[1];
                xyz[v * 3 + 2] = p.z + at[2];
            }
            Vector3f n = r.transform(new Vector3f(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2]));
            // Un texel por pixel del modelo, con un desplazamiento distinto por caja para que no se repita
            float w = Math.min(16f, Math.max(1f, size[FACE_AXES[f][0]]));
            float h = Math.min(16f, Math.max(1f, size[FACE_AXES[f][1]]));
            float u0 = ((seed * 5 + f * 3) % 16) % Math.max(1f, 17f - w);
            float v0 = ((seed * 3 + f * 7) % 16) % Math.max(1f, 17f - h);
            float[] uv = {u0 / 16f, (v0 + h) / 16f, (u0 + w) / 16f, (v0 + h) / 16f, (u0 + w) / 16f, v0 / 16f, u0 / 16f, v0 / 16f};
            out.add(new Face(xyz, uv, n.x, n.y, n.z, cr, cg, cb, alpha, at[0], at[1], at[2], seed));
        }
    }
}

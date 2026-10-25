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
 */
public final class SpellModel {
    public enum Render { SOLID, TRANSLUCENT, EMISSIVE }

    /** Una cara: 4 vertices (en pixeles), normal, UV y color. */
    public record Face(float[] xyz, float[] uv, float nx, float ny, float nz, int r, int g, int b, float alpha) {}

    public record Group(ResourceLocation texture, Render render, boolean glow, float[] spin, float pulseAmp, float pulsePeriod,
                        float flicker, List<Face> faces) {}

    public final List<Group> groups = new ArrayList<>();

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
            }
        } catch (Exception e) {
            MushokuMod.LOG.error("No se pudo leer el modelo de hechizo {}", loc, e);
        }
        return m;
    }

    private static float[] vec(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static Group parseGroup(JsonObject g) {
        Render render = switch (g.get("render").getAsString()) {
            case "solid" -> Render.SOLID;
            case "emissive" -> Render.EMISSIVE;
            default -> Render.TRANSLUCENT;
        };
        JsonArray pulse = g.getAsJsonArray("pulse");
        List<Face> faces = new ArrayList<>();
        int seed = 0;
        for (JsonElement be : g.getAsJsonArray("boxes")) {
            JsonObject b = be.getAsJsonObject();
            addBox(faces, vec(b.getAsJsonArray("at")), vec(b.getAsJsonArray("size")), vec(b.getAsJsonArray("rot")),
                    Integer.parseInt(b.get("color").getAsString(), 16), b.get("alpha").getAsFloat(), seed++);
        }
        return new Group(ResourceLocation.parse(g.get("texture").getAsString()), render, g.get("glow").getAsBoolean(),
                vec(g.getAsJsonArray("spin")), pulse.get(0).getAsFloat(), Math.max(1f, pulse.get(1).getAsFloat()),
                g.get("flicker").getAsFloat(), faces);
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

    private static void addBox(List<Face> out, float[] at, float[] size, float[] rot, int rgb, float alpha, int seed) {
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
            out.add(new Face(xyz, uv, n.x, n.y, n.z, cr, cg, cb, alpha));
        }
    }
}

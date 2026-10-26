package net.zennin.mushoku.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rasgo de raza (orejas, cuernos, gema) leido de assets/remushoku/cosmetics/<nombre>.json (tools/spells/cosmetics.py).
 * Cada caja tiene su color: fijo, o tomado de la skin del jugador (piel o pelo) con un multiplicador.
 */
public final class CosmeticModel {
    public static final int HEX = 0, SKIN = 1, HAIR = 2;

    public record Part(SpellModel.Face face, int source, float mul, boolean glow) {}

    public final List<Part> parts = new ArrayList<>();
    public final List<CosmeticChain> chains = new ArrayList<>();

    /** "skin:0.8" / "hair" / "e8a6b0" -> {fuente, rgb, multiplicador}. */
    static Object[] parseColor(String col) {
        String[] sp = col.split(":");
        int source = HEX, rgb = 0xffffff;
        if (sp[0].equals("skin")) source = SKIN;
        else if (sp[0].equals("hair")) source = HAIR;
        else rgb = Integer.parseInt(sp[0], 16);
        return new Object[]{source, rgb, sp.length > 1 ? Float.parseFloat(sp[1]) : 1f};
    }
    public float[] heights = {0};

    private static final Map<String, CosmeticModel> CACHE = new HashMap<>();

    public static void clearCache() { CACHE.clear(); }

    public static CosmeticModel get(String name) { return CACHE.computeIfAbsent(name, CosmeticModel::load); }

    private static float[] floats(com.google.gson.JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    private static CosmeticModel load(String name) {
        CosmeticModel m = new CosmeticModel();
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MushokuMod.MODID, "cosmetics/" + name + ".json");
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) return m;
            try (Reader reader = res.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                m.heights = floats(root.getAsJsonArray("heights"));
                if (root.has("chains"))
                    for (JsonElement ce : root.getAsJsonArray("chains")) m.chains.add(new CosmeticChain(ce.getAsJsonObject()));
                int seed = 0;
                for (JsonElement be : root.getAsJsonArray("boxes")) {
                    JsonObject b = be.getAsJsonObject();
                    String col = b.get("color").getAsString();
                    int source = HEX, rgb = 0xffffff;
                    float mul = 1f;
                    String[] sp = col.split(":");
                    if (sp[0].equals("skin")) source = SKIN;
                    else if (sp[0].equals("hair")) source = HAIR;
                    else rgb = Integer.parseInt(sp[0], 16);
                    if (sp.length > 1) mul = Float.parseFloat(sp[1]);
                    boolean glow = b.has("glow") && b.get("glow").getAsBoolean();
                    List<SpellModel.Face> faces = new ArrayList<>();
                    SpellModel.addBox(faces, floats(b.getAsJsonArray("at")), floats(b.getAsJsonArray("size")), floats(b.getAsJsonArray("rot")),
                            rgb, b.get("alpha").getAsFloat(), seed++);
                    for (SpellModel.Face f : faces) m.parts.add(new Part(f, source, mul, glow));
                }
            }
        } catch (Exception e) {
            MushokuMod.LOG.error("No se pudo leer el rasgo {}", loc, e);
        }
        return m;
    }
}

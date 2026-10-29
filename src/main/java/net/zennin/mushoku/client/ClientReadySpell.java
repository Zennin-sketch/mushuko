package net.zennin.mushoku.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.magic.Spell;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hechizos listos para lanzar de los jugadores que se ven: con eso se levanta la mano apuntando y el hechizo queda
 * flotando delante de ella. Al quedar listo primero se "forma" (modelo &lt;nombre&gt;_form si existe).
 */
public final class ClientReadySpell {
    public record Ready(String spell, String model, long start) {}

    private static final Map<Integer, Ready> READY = new HashMap<>();
    /** Proyectiles que pueden ir en la mano (los efectos grandes como nubes o muros no). */
    private static final String[] HAND_MODELS = {"minefield_ball", "fire_crescent", "water_arrow", "wind_drill", "icicle", "stone_bullet", "wind_blade",
            "fireball", "water_ball"};
    private static Map<String, String> bySpell;

    private ClientReadySpell() {}

    public static void set(int entityId, String spell) {
        if (spell == null || spell.isEmpty()) { READY.remove(entityId); return; }
        Ready old = READY.get(entityId);
        if (old != null && old.spell.equals(spell)) return;
        String model = modelFor(spell);
        if (model == null) { READY.remove(entityId); return; }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0;
        READY.put(entityId, new Ready(spell, model, now));
    }

    public static Ready get(Player p) { return READY.get(p.getId()); }

    public static void clear() { READY.clear(); }

    public static void reload() { bySpell = null; FORMS.clear(); }

    private static final Map<String, Boolean> FORMS = new HashMap<>();

    /** Edad en ticks desde que quedo listo. */
    public static float age(Ready r, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        return Math.max(0, mc.level.getGameTime() - r.start + partial);
    }

    private static String modelFor(String spell) {
        if (bySpell == null) {
            bySpell = new HashMap<>();
            for (String m : HAND_MODELS) {
                try {
                    Optional<net.minecraft.server.packs.resources.Resource> res = Minecraft.getInstance().getResourceManager()
                            .getResource(ResourceLocation.fromNamespaceAndPath(MushokuMod.MODID, "spell_models/" + m + ".json"));
                    if (res.isEmpty()) continue;
                    try (Reader r = res.get().openAsReader()) {
                        JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                        if (o.has("spells")) for (JsonElement e : o.getAsJsonArray("spells")) bySpell.putIfAbsent(e.getAsString(), m);
                    }
                } catch (Exception ignored) {}
            }
        }
        String m = bySpell.get(spell);
        if (m != null) return m;
        Spell s = Spell.byId(spell);
        if (s == null) return null;
        // Hechizos que crean estructuras (muros, pinchos, nubes, curaciones): solo un brillo del elemento
        return switch (s.element) {
            case FIRE -> "glow_fire";
            case WATER -> "glow_water";
            case EARTH -> "glow_earth";
            case WIND -> "glow_wind";
            default -> "glow_none";
        };
    }

    /** Hay modelo de formacion para este proyectil. */
    public static boolean hasForm(String model) {
        return FORMS.computeIfAbsent(model, m -> Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(MushokuMod.MODID, "spell_models/" + m + "_form.json")).isPresent());
    }
}

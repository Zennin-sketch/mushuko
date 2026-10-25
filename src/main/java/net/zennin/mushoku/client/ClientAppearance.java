package net.zennin.mushoku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.zennin.mushoku.race.Appearance;

import java.util.HashMap;
import java.util.Map;

/** Rasgos de raza de los jugadores que ve este cliente (y la vista previa mientras creas el personaje). */
public final class ClientAppearance {
    private static final Map<Integer, Appearance> BY_ID = new HashMap<>();
    /** Mientras se crea el personaje, tu jugador se dibuja con este rasgo (o null para usar el real). */
    public static Appearance preview = null;

    private ClientAppearance() {}

    public static void set(int id, Appearance a) { BY_ID.put(id, a); }

    public static void clear() { BY_ID.clear(); preview = null; }

    public static Appearance get(Entity e) {
        if (preview != null && e == Minecraft.getInstance().player) return preview;
        return BY_ID.get(e.getId());
    }
}

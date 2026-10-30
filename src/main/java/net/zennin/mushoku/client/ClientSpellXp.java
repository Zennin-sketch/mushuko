package net.zennin.mushoku.client;

import java.util.HashMap;
import java.util.Map;

/** Copia en el cliente de los lanzamientos de cada hechizo. */
public final class ClientSpellXp {
    private static Map<String, Integer> uses = new HashMap<>();
    private ClientSpellXp() {}

    public static int uses(String id) { return uses.getOrDefault(id, 0); }

    public static void set(Map<String, Integer> m) { uses = new HashMap<>(m); }

    private static java.util.Set<String> known = new java.util.HashSet<>();

    /** Hechizos que conoce el jugador (copia del servidor). */
    public static boolean knows(String id) { return known.contains(id); }

    public static void setKnown(java.util.List<String> l) { known = new java.util.HashSet<>(l); }
}

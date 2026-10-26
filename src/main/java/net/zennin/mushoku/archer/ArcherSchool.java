package net.zennin.mushoku.archer;

/** Las tres escuelas del arquero (como los tres estilos de espada). */
public enum ArcherSchool {
    HAWK("hawk"), VOLLEY("volley"), WIND("wind");

    public final String key;

    ArcherSchool(String key) { this.key = key; }

    public static ArcherSchool byKey(String key) {
        for (ArcherSchool s : values()) if (s.key.equals(key)) return s;
        return null;
    }
}

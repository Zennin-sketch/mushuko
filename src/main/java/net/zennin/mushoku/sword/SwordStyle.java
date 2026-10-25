package net.zennin.mushoku.sword;

/** Los tres estilos de espada del original. */
public enum SwordStyle {
    SWORD_GOD("sword_god"), WATER_GOD("water_god"), NORTH_GOD("north_god");

    public final String key;

    SwordStyle(String key) { this.key = key; }

    public static SwordStyle byKey(String key) {
        for (SwordStyle s : values()) if (s.key.equals(key)) return s;
        return null;
    }
}

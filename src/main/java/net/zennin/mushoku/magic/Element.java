package net.zennin.mushoku.magic;

/** Elemento elegido con la primera flecha: izquierda agua, derecha fuego, abajo tierra, arriba viento. */
public enum Element {
    WATER('l', "water"), FIRE('r', "fire"), EARTH('d', "earth"), WIND('u', "wind"), NONE(' ', "none");

    public final char arrow;
    public final String key;

    Element(char arrow, String key) {
        this.arrow = arrow;
        this.key = key;
    }

    public static Element fromArrow(char c) {
        for (Element e : values()) if (e.arrow == c) return e;
        return NONE;
    }

    public static Element byKey(String key) {
        for (Element e : values()) if (e.key.equals(key)) return e;
        return null;
    }
}

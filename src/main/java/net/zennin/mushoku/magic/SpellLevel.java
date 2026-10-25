package net.zennin.mushoku.magic;

/** Niveles de magia: costo de mana y mana maximo ganado, igual que en el original. */
public enum SpellLevel {
    ELEMENTARY(1, 30, 2), INTERMEDIATE(2, 90, 5), ADVANCED(3, 200, 7),
    SAINT(4, 500, 12), KING(5, 800, 15), IMPERIAL(6, 1000, 20);

    public final int tier;
    public final double cost;
    public final double maxManaGain;

    SpellLevel(int tier, double cost, double maxManaGain) {
        this.tier = tier;
        this.cost = cost;
        this.maxManaGain = maxManaGain;
    }

    /** 3 flechas = Principiante, 4 = Intermedio ... 8 = Imperial. */
    public static SpellLevel fromComboLength(int len) {
        int idx = Math.max(0, Math.min(values().length - 1, len - 3));
        return values()[idx];
    }
}

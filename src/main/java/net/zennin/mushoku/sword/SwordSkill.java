package net.zennin.mushoku.sword;

import java.util.ArrayList;
import java.util.List;

/**
 * Habilidades de espadachin con el mismo costo en puntos que el original.
 * tier: 1 Principiante, 2 Intermedio, 3 Avanzado, 4 Santo, 5 Rey, 6 Emperador.
 * Si el estilo no es tu afinidad, solo podes aprender hasta Avanzado.
 */
public enum SwordSkill {
    // Dios Espada (ataque)
    SG_STYLE("sg_style", SwordStyle.SWORD_GOD, 1, 50, Kind.STYLE, 0, 0),
    ARM_CHOP("arm_chop", SwordStyle.SWORD_GOD, 2, 25, Kind.ACTIVE, 25, 60),
    QUICK_STRIKES("quick_strikes", SwordStyle.SWORD_GOD, 2, 25, Kind.ACTIVE, 50, 80),
    LONGSWORD_SILENCE("longsword_silence", SwordStyle.SWORD_GOD, 3, 100, Kind.ACTIVE, 100, 100),
    LONGSWORD_LIGHT("longsword_light", SwordStyle.SWORD_GOD, 4, 200, Kind.ACTIVE, 200, 200),
    LIGHT_REVERSAL("light_reversal", SwordStyle.SWORD_GOD, 5, 300, Kind.PASSIVE, 0, 0),
    // Dios Agua (defensa)
    WG_STYLE("wg_style", SwordStyle.WATER_GOD, 1, 50, Kind.STYLE, 0, 0),
    DEFLECT("deflect", SwordStyle.WATER_GOD, 2, 50, Kind.TOGGLE, 0, 0),
    STEADFAST("steadfast", SwordStyle.WATER_GOD, 2, 25, Kind.ACTIVE, 25, 100),
    MAGIC_BLOCK("magic_block", SwordStyle.WATER_GOD, 3, 75, Kind.PASSIVE, 0, 0),
    TRUE_OBSERVATION("true_observation", SwordStyle.WATER_GOD, 4, 100, Kind.ACTIVE, 25, 300),
    FLOW("flow", SwordStyle.WATER_GOD, 4, 100, Kind.ACTIVE, 50, 100),
    AURA_REPULSION("aura_repulsion", SwordStyle.WATER_GOD, 5, 150, Kind.ACTIVE, 50, 400),
    AURA_MOTION("aura_motion", SwordStyle.WATER_GOD, 5, 200, Kind.ACTIVE, 200, 200),
    DEPRIVATION_KINGDOM("deprivation_kingdom", SwordStyle.WATER_GOD, 6, 250, Kind.ACTIVE, 50, 600),
    // Dios Norte (adaptabilidad)
    NG_STYLE("ng_style", SwordStyle.NORTH_GOD, 1, 50, Kind.STYLE, 0, 0),
    NG_MAGIC("ng_magic", SwordStyle.NORTH_GOD, 2, 50, Kind.PASSIVE, 0, 0),
    NG_ITEMS("ng_items", SwordStyle.NORTH_GOD, 2, 25, Kind.PASSIVE, 0, 0),
    DUEL_WIELDING("duel_wielding", SwordStyle.NORTH_GOD, 3, 100, Kind.TOGGLE, 0, 0),
    SWORD_THROW("sword_throw", SwordStyle.NORTH_GOD, 3, 100, Kind.ACTIVE, 100, 60),
    TOUKI_MANIPULATION("touki_manipulation", SwordStyle.NORTH_GOD, 4, 200, Kind.ACTIVE, 50, 900),
    ARSENAL_MASTERY("arsenal_mastery", SwordStyle.NORTH_GOD, 4, 150, Kind.TOGGLE, 0, 0),
    TOUKI_AMPLIFICATION("touki_amplification", SwordStyle.NORTH_GOD, 5, 300, Kind.ACTIVE, 50, 1200);

    public enum Kind { STYLE, ACTIVE, PASSIVE, TOGGLE }

    public final String id;
    public final SwordStyle style;
    public final int tier, cost;
    public final Kind kind;
    public final double mana;
    public final int cooldown;

    SwordSkill(String id, SwordStyle style, int tier, int cost, Kind kind, double mana, int cooldown) {
        this.id = id;
        this.style = style;
        this.tier = tier;
        this.cost = cost;
        this.kind = kind;
        this.mana = mana;
        this.cooldown = cooldown;
    }

    public String translationKey() { return "skill.remushoku." + id; }

    public static SwordSkill byId(String id) {
        for (SwordSkill s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    public static SwordSkill styleSkill(SwordStyle style) {
        return switch (style) {
            case SWORD_GOD -> SG_STYLE;
            case WATER_GOD -> WG_STYLE;
            case NORTH_GOD -> NG_STYLE;
        };
    }

    public static List<SwordSkill> of(SwordStyle style) {
        List<SwordSkill> list = new ArrayList<>();
        for (SwordSkill s : values()) if (s.style == style) list.add(s);
        return list;
    }
}

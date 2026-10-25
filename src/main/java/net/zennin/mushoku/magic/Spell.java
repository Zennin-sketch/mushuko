package net.zennin.mushoku.magic;

import java.util.HashMap;
import java.util.Map;

/**
 * Todos los hechizos del original con su combinacion de flechas (u=arriba, d=abajo, l=izquierda, r=derecha).
 * implemented = false: se puede formar pero todavia no hace nada (llega en la Fase 3).
 */
public enum Spell {
    // Agua
    WATER_BALL("water_ball", "lrl", Element.WATER, true),
    WATER_SHIELD("water_shield", "ldd", Element.WATER, true),
    WATER_ARROW("water_arrow", "lrr", Element.WATER, true),
    ICE_SMASH("ice_smash", "lru", Element.WATER, true),
    ICE_BLADE("ice_blade", "llr", Element.WATER, true),
    WATER_CANNON("water_cannon", "lrrl", Element.WATER, false),
    ICE_PILLAR("ice_pillar", "lldd", Element.WATER, false),
    FLOOD_FLUSH("flood_flush", "llll", Element.WATER, false),
    ICICLE_LANCE("icicle_lance", "lrrr", Element.WATER, false),
    BLIZZARD_STORM("blizzard_storm", "lrrrr", Element.WATER, false),
    ICE_FORTRESS("ice_fortress", "ldddd", Element.WATER, false),
    CUMULONIMBUS("cumulonimbus", "lldlul", Element.WATER, false),
    LIGHTNING("lightning", "lrruudl", Element.WATER, false),
    ABSOLUTE_ZERO("absolute_zero", "lldduurr", Element.WATER, false),
    // Fuego
    FIREBALL("fireball", "rrr", Element.FIRE, true),
    HEAT_HAND("heat_hand", "rll", Element.FIRE, true),
    BURNING_PLACE("burning_place", "rdd", Element.FIRE, true),
    EX_FLAME("ex_flame", "rrrr", Element.FIRE, false),
    EXPLOSION("explosion", "rrll", Element.FIRE, false),
    FLAME_SLICE("flame_slice", "rrlr", Element.FIRE, false),
    EXODUS_FLAME("exodus_flame", "rrrrr", Element.FIRE, false),
    MAGMA_GUSH("magma_gush", "rdldd", Element.FIRE, false),
    FLASH_OVER("flash_over", "rdlrdr", Element.FIRE, false),
    MINEFIELD("minefield", "rrllddr", Element.FIRE, false),
    NUCLEAR_STRIKE("nuclear_strike", "rrudlurr", Element.FIRE, false),
    // Tierra
    ROCK_BULLET("rock_bullet", "drr", Element.EARTH, true),
    SAND_CLOUD("sand_cloud", "dld", Element.EARTH, true),
    DIG("dig", "dll", Element.EARTH, true),
    STONE_CANNON("stone_cannon", "drrr", Element.EARTH, false),
    EARTH_PILLAR("earth_pillar", "dldd", Element.EARTH, false),
    EARTH_LANCE("earth_lance", "dulu", Element.EARTH, false),
    EARTH_WALL("earth_wall", "ddll", Element.EARTH, false),
    EARTH_FORTRESS("earth_fortress", "ddddd", Element.EARTH, false),
    DUST_STORM("dust_storm", "dlddl", Element.EARTH, false),
    SANDSTORM("sandstorm", "dldudd", Element.EARTH, false),
    EARTH_DRILL("earth_drill", "dldlddd", Element.EARTH, false),
    MOUNTAIN("mountain", "dldlurdl", Element.EARTH, false),
    // Viento
    BLAST("blast", "uuu", Element.WIND, true),
    SOFT_LANDING("soft_landing", "uul", Element.WIND, true),
    FLEET_FOOT("fleet_foot", "uud", Element.WIND, true),
    SONIC_BOOM("sonic_boom", "urll", Element.WIND, false),
    WIND_SLICE("wind_slice", "urrr", Element.WIND, false),
    GALE_FORCE("gale_force", "ullu", Element.WIND, false),
    WIND_GUST("wind_gust", "ulrr", Element.WIND, false),
    SONIC_BLAST("sonic_blast", "urrrr", Element.WIND, false),
    TORNADO_IMPACT("tornado_impact", "urrru", Element.WIND, false),
    VIOLENT_STORM("violent_storm", "udlrdd", Element.WIND, false),
    TORNADO("tornado", "uludrur", Element.WIND, false),
    EYE_OF_THE_STORM("eye_of_the_storm", "uuddrudd", Element.WIND, false),
    // Sin elemento (curacion y magia variada)
    HEALING("healing", "udl", Element.NONE, true),
    GROW("grow", "dlu", Element.NONE, true),
    MAGIC_SHIELD("magic_shield", "ddd", Element.NONE, true),
    X_HEALING("x_healing", "ldur", Element.NONE, false),
    FEAST("feast", "rudl", Element.NONE, false),
    SHRINE_HEALING("shrine_healing", "ldudl", Element.NONE, false),
    DISTURB_MAGIC("disturb_magic", "lrrddl", Element.NONE, false);

    public final String id;
    public final String combo;
    public final Element element;
    public final SpellLevel level;
    public final boolean implemented;

    private static final Map<String, Spell> ELEMENTAL = new HashMap<>();
    private static final Map<String, Spell> NO_ELEMENT = new HashMap<>();
    private static final Map<String, Spell> BY_ID = new HashMap<>();

    static {
        for (Spell s : values()) {
            (s.element == Element.NONE ? NO_ELEMENT : ELEMENTAL).put(s.combo, s);
            BY_ID.put(s.id, s);
        }
    }

    Spell(String id, String combo, Element element, boolean implemented) {
        this.id = id;
        this.combo = combo;
        this.element = element;
        this.level = SpellLevel.fromComboLength(combo.length());
        this.implemented = implemented;
    }

    public String translationKey() { return "spell.remushoku." + id; }

    public static Spell fromCombo(String combo, boolean noElementMode) {
        return (noElementMode ? NO_ELEMENT : ELEMENTAL).get(combo);
    }

    public static Spell byId(String id) { return BY_ID.get(id); }
}

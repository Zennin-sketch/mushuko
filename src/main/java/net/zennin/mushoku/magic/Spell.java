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
    WATER_CANNON("water_cannon", "lrrl", Element.WATER, true),
    ICE_PILLAR("ice_pillar", "lldd", Element.WATER, true),
    FLOOD_FLUSH("flood_flush", "llll", Element.WATER, true),
    ICICLE_LANCE("icicle_lance", "lrrr", Element.WATER, true),
    BLIZZARD_STORM("blizzard_storm", "lrrrr", Element.WATER, true),
    ICE_FORTRESS("ice_fortress", "ldddd", Element.WATER, true),
    CUMULONIMBUS("cumulonimbus", "lldlul", Element.WATER, true),
    LIGHTNING("lightning", "lrruudl", Element.WATER, true),
    ABSOLUTE_ZERO("absolute_zero", "lldduurr", Element.WATER, true),
    // Fuego
    FIREBALL("fireball", "rrr", Element.FIRE, true),
    HEAT_HAND("heat_hand", "rll", Element.FIRE, true),
    BURNING_PLACE("burning_place", "rdd", Element.FIRE, true),
    EX_FLAME("ex_flame", "rrrr", Element.FIRE, true),
    EXPLOSION("explosion", "rrll", Element.FIRE, true),
    FLAME_SLICE("flame_slice", "rrlr", Element.FIRE, true),
    EXODUS_FLAME("exodus_flame", "rrrrr", Element.FIRE, true),
    MAGMA_GUSH("magma_gush", "rdldd", Element.FIRE, true),
    FLASH_OVER("flash_over", "rdlrdr", Element.FIRE, true),
    MINEFIELD("minefield", "rrllddr", Element.FIRE, true),
    NUCLEAR_STRIKE("nuclear_strike", "rrudlurr", Element.FIRE, true),
    // Tierra
    ROCK_BULLET("rock_bullet", "drr", Element.EARTH, true),
    SAND_CLOUD("sand_cloud", "dld", Element.EARTH, true),
    DIG("dig", "dll", Element.EARTH, true),
    STONE_CANNON("stone_cannon", "drrr", Element.EARTH, true),
    EARTH_PILLAR("earth_pillar", "dldd", Element.EARTH, true),
    EARTH_LANCE("earth_lance", "dulu", Element.EARTH, true),
    EARTH_WALL("earth_wall", "ddll", Element.EARTH, true),
    EARTH_FORTRESS("earth_fortress", "ddddd", Element.EARTH, true),
    DUST_STORM("dust_storm", "dlddl", Element.EARTH, true),
    SANDSTORM("sandstorm", "dldudd", Element.EARTH, true),
    EARTH_DRILL("earth_drill", "dldlddd", Element.EARTH, true),
    MOUNTAIN("mountain", "dldlurdl", Element.EARTH, true),
    // Viento
    BLAST("blast", "uuu", Element.WIND, true),
    SOFT_LANDING("soft_landing", "uul", Element.WIND, true),
    FLEET_FOOT("fleet_foot", "uud", Element.WIND, true),
    SONIC_BOOM("sonic_boom", "urll", Element.WIND, true),
    WIND_SLICE("wind_slice", "urrr", Element.WIND, true),
    GALE_FORCE("gale_force", "ullu", Element.WIND, true),
    WIND_GUST("wind_gust", "ulrr", Element.WIND, true),
    SONIC_BLAST("sonic_blast", "urrrr", Element.WIND, true),
    TORNADO_IMPACT("tornado_impact", "urrru", Element.WIND, true),
    VIOLENT_STORM("violent_storm", "udlrdd", Element.WIND, true),
    TORNADO("tornado", "uludrur", Element.WIND, true),
    EYE_OF_THE_STORM("eye_of_the_storm", "uuddrudd", Element.WIND, true),
    // Sin elemento (curacion y magia variada)
    HEALING("healing", "udl", Element.NONE, true),
    GROW("grow", "dlu", Element.NONE, true),
    MAGIC_SHIELD("magic_shield", "ddd", Element.NONE, true),
    X_HEALING("x_healing", "ldur", Element.NONE, true),
    FEAST("feast", "rudl", Element.NONE, true),
    SHRINE_HEALING("shrine_healing", "ldudl", Element.NONE, true),
    DISTURB_MAGIC("disturb_magic", "lrrddl", Element.NONE, true);

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

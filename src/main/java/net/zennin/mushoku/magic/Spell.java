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
    DISTURB_MAGIC("disturb_magic", "lrrddl", Element.NONE, true),
    // --- Addon: magia variada (modo sin elemento) ---
    STRENGTH("strength", "rrr", Element.NONE, true),
    JUMP_BOOST("jump_boost", "uuu", Element.NONE, true),
    NIGHT_VISION("night_vision", "lll", Element.NONE, true),
    WATER_BREATHING("water_breathing", "ldl", Element.NONE, true),
    FIRE_RESISTANCE("fire_resistance", "rrdd", Element.NONE, true),
    INVISIBILITY("invisibility", "lluu", Element.NONE, true),
    REPAIR("repair", "dduu", Element.NONE, true),
    CLEANSE("cleanse", "uudd", Element.NONE, true),
    /** Infusion de Mana: la linea de tiza rojiza que miras pasa a azul y conduce redstone. */
    MANA_INFUSION("mana_infusion", "rlr", Element.NONE, true),
    // --- Addon: hechizos combinados (se forman con la tecla Combinar, no con flechas) ---
    FIRE_RAIN("fire_rain", "+fire_rain", Element.FIRE, true, SpellLevel.INTERMEDIATE),
    FREEZING_RAIN("freezing_rain", "+freezing_rain", Element.WATER, true, SpellLevel.ADVANCED),
    SPIKE_RAIN("spike_rain", "+spike_rain", Element.EARTH, true, SpellLevel.ADVANCED),
    DEEP_MIST("deep_mist", "+deep_mist", Element.WATER, true, SpellLevel.ADVANCED),
    POISON_MIST("poison_mist", "+poison_mist", Element.WATER, true, SpellLevel.ADVANCED),
    QUAGMIRE("quagmire", "+quagmire", Element.EARTH, true, SpellLevel.ADVANCED),
    STONE_BLADE("stone_blade", "+stone_blade", Element.EARTH, true, SpellLevel.ADVANCED),
    FROST_WIND("frost_wind", "+frost_wind", Element.WATER, true, SpellLevel.SAINT);

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
        this(id, combo, element, implemented, SpellLevel.fromComboLength(combo.length()));
    }

    Spell(String id, String combo, Element element, boolean implemented, SpellLevel level) {
        this.id = id;
        this.combo = combo;
        this.element = element;
        this.level = level;
        this.implemented = implemented;
    }

    /** Hechizo combinado (no se forma con flechas). */
    public boolean combined() { return combo.startsWith("+"); }

    /** Combinaciones del addon: primer hechizo + segundo hechizo (en cualquier orden). */
    public static Spell combine(Spell a, Spell b) {
        for (Spell[] c : new Spell[][]{
                {FIREBALL, FIREBALL, FIRE_RAIN}, {ICICLE_LANCE, ICICLE_LANCE, FREEZING_RAIN}, {STONE_CANNON, STONE_CANNON, SPIKE_RAIN},
                {WATER_SHIELD, BURNING_PLACE, DEEP_MIST}, {DEEP_MIST, WATER_BALL, POISON_MIST}, {FLOOD_FLUSH, SAND_CLOUD, QUAGMIRE},
                {ICE_BLADE, EARTH_PILLAR, STONE_BLADE}, {DEEP_MIST, ICE_FORTRESS, FROST_WIND}}) {
            if ((c[0] == a && c[1] == b) || (c[0] == b && c[1] == a)) return c[2];
        }
        return null;
    }

    public String translationKey() { return "spell.remushoku." + id; }

    public static Spell fromCombo(String combo, boolean noElementMode) {
        return (noElementMode ? NO_ELEMENT : ELEMENTAL).get(combo);
    }

    public static Spell byId(String id) { return BY_ID.get(id); }
}

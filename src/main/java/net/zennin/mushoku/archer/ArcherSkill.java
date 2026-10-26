package net.zennin.mushoku.archer;

import java.util.ArrayList;
import java.util.List;

/**
 * Habilidades del arquero, con el mismo esquema que las del espadachin.
 * tier: 1 Principiante, 2 Intermedio, 3 Avanzado, 4 Santo, 5 Rey. Fuera de tu escuela, solo hasta Avanzado.
 */
public enum ArcherSkill {
    // Ojo de Halcon (precision)
    HAWK_STYLE("hawk_style", ArcherSchool.HAWK, 1, 50, Kind.STYLE, 0, 0),
    POWER_SHOT("power_shot", ArcherSchool.HAWK, 2, 25, Kind.ACTIVE, 25, 80),
    MARKED_PREY("marked_prey", ArcherSchool.HAWK, 2, 25, Kind.PASSIVE, 0, 0),
    PIERCING_RAY("piercing_ray", ArcherSchool.HAWK, 3, 100, Kind.ACTIVE, 60, 120),
    DEADEYE("deadeye", ArcherSchool.HAWK, 4, 200, Kind.ACTIVE, 100, 300),
    PHANTOM_ARROW("phantom_arrow", ArcherSchool.HAWK, 5, 300, Kind.ACTIVE, 150, 400),
    // Lluvia de Acero (multidisparo)
    VOLLEY_STYLE("volley_style", ArcherSchool.VOLLEY, 1, 50, Kind.STYLE, 0, 0),
    TRIPLE_SHOT("triple_shot", ArcherSchool.VOLLEY, 2, 25, Kind.ACTIVE, 30, 100),
    QUICK_DRAW("quick_draw", ArcherSchool.VOLLEY, 2, 50, Kind.PASSIVE, 0, 0),
    ARROW_RAIN("arrow_rain", ArcherSchool.VOLLEY, 3, 100, Kind.ACTIVE, 80, 200),
    BARRAGE("barrage", ArcherSchool.VOLLEY, 4, 150, Kind.ACTIVE, 100, 240),
    STEEL_STORM("steel_storm", ArcherSchool.VOLLEY, 5, 300, Kind.ACTIVE, 200, 600),
    // Viento Errante (movilidad)
    WIND_STYLE("wind_style", ArcherSchool.WIND, 1, 50, Kind.STYLE, 0, 0),
    DISENGAGE("disengage", ArcherSchool.WIND, 2, 25, Kind.ACTIVE, 20, 60),
    GALE_ARROW("gale_arrow", ArcherSchool.WIND, 2, 50, Kind.ACTIVE, 30, 80),
    LIGHT_STEP("light_step", ArcherSchool.WIND, 3, 75, Kind.PASSIVE, 0, 0),
    TEMPEST_ARROW("tempest_arrow", ArcherSchool.WIND, 4, 200, Kind.ACTIVE, 120, 300),
    GALE_DANCE("gale_dance", ArcherSchool.WIND, 5, 300, Kind.ACTIVE, 150, 400);

    public enum Kind { STYLE, ACTIVE, PASSIVE }

    public final String id;
    public final ArcherSchool school;
    public final int tier, cost;
    public final Kind kind;
    public final double mana;
    public final int cooldown;

    ArcherSkill(String id, ArcherSchool school, int tier, int cost, Kind kind, double mana, int cooldown) {
        this.id = id;
        this.school = school;
        this.tier = tier;
        this.cost = cost;
        this.kind = kind;
        this.mana = mana;
        this.cooldown = cooldown;
    }

    public String translationKey() { return "skill.remushoku." + id; }

    public static ArcherSkill byId(String id) {
        for (ArcherSkill s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    public static ArcherSkill styleSkill(ArcherSchool school) {
        return switch (school) {
            case HAWK -> HAWK_STYLE;
            case VOLLEY -> VOLLEY_STYLE;
            case WIND -> WIND_STYLE;
        };
    }

    public static List<ArcherSkill> of(ArcherSchool school) {
        List<ArcherSkill> list = new ArrayList<>();
        for (ArcherSkill s : values()) if (s.school == school) list.add(s);
        return list;
    }
}

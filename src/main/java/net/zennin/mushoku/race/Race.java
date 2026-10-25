package net.zennin.mushoku.race;

/**
 * Las razas del addon (y Demonio, agregada para la creacion de personaje) con sus stats originales.
 * fire/water/earth/wind: multiplicador del daño de tus hechizos de ese elemento (afinidad magica).
 * scale: tamaño del jugador (en el addon dependia de Pehkui; aca se usa el atributo de escala de 1.21).
 */
public enum Race {
    HUMAN("human", 20, 0.0, 0, 0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0),
    ELF("elf", 26, 0.10, 0, 0, 0.4, 0.7, 1.2, 0.7, 1.3, 1.0),
    DWARF("dwarf", 24, -0.10, 2, 1, 0.0, 1.2, 0.7, 1.3, 0.7, 0.8),
    MIGURD("migurd", 20, 0.10, 0, 0, 0.0, 0.7, 1.8, 0.7, 0.7, 0.85),
    SUPERD("superd", 40, 0.20, 4, 2, 0.5, 0.8, 0.8, 0.8, 0.8, 1.05),
    OGRE("ogre", 40, -0.10, 2, 3, -0.2, 0.9, 0.9, 0.9, 0.9, 1.25),
    BEAST("beast", 30, 0.40, 0, 1, 0.6, 1.2, 1.2, 0.6, 1.2, 1.0),
    DEMON("demon", 24, 0.05, 1, 1, 0.0, 1.3, 0.8, 1.1, 0.8, 1.0);

    public final String key;
    public final double health, speed, armor, damage, attackSpeed, fire, water, earth, wind, scale;

    Race(String key, double health, double speed, double armor, double damage, double attackSpeed,
         double fire, double water, double earth, double wind, double scale) {
        this.key = key;
        this.health = health;
        this.speed = speed;
        this.armor = armor;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.fire = fire;
        this.water = water;
        this.earth = earth;
        this.wind = wind;
        this.scale = scale;
    }

    public static Race byKey(String key) {
        for (Race r : values()) if (r.key.equals(key)) return r;
        return null;
    }
}

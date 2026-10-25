package net.zennin.mushoku.race;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Raza elegida y habilidades del addon: Esquivar, Piel de Mana (encendibles) y Factor Laplace. */
public record RaceData(String race, boolean dodge, boolean dodgeOn, boolean manaSkinOn, boolean laplace, long skillCooldownUntil) {
    public static final RaceData DEFAULT = new RaceData("", false, true, true, false, 0);
    public static final Codec<RaceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("race", "").forGetter(RaceData::race),
            Codec.BOOL.optionalFieldOf("dodge", false).forGetter(RaceData::dodge),
            Codec.BOOL.optionalFieldOf("dodge_on", true).forGetter(RaceData::dodgeOn),
            Codec.BOOL.optionalFieldOf("mana_skin_on", true).forGetter(RaceData::manaSkinOn),
            Codec.BOOL.optionalFieldOf("laplace", false).forGetter(RaceData::laplace),
            Codec.LONG.optionalFieldOf("skill_cd", 0L).forGetter(RaceData::skillCooldownUntil)
    ).apply(i, RaceData::new));

    public Race raceOrNull() { return Race.byKey(race); }

    public RaceData withRace(String r) { return new RaceData(r, dodge, dodgeOn, manaSkinOn, laplace, skillCooldownUntil); }

    public RaceData withDodge(boolean d) { return new RaceData(race, d, dodgeOn, manaSkinOn, laplace, skillCooldownUntil); }

    public RaceData withDodgeOn(boolean d) { return new RaceData(race, dodge, d, manaSkinOn, laplace, skillCooldownUntil); }

    public RaceData withManaSkinOn(boolean m) { return new RaceData(race, dodge, dodgeOn, m, laplace, skillCooldownUntil); }

    public RaceData withLaplace(boolean l) { return new RaceData(race, dodge, dodgeOn, manaSkinOn, l, skillCooldownUntil); }

    public RaceData withCooldown(long t) { return new RaceData(race, dodge, dodgeOn, manaSkinOn, laplace, t); }

    /** Nivel de Piel de Mana segun el mana maximo (1000 / 10000 / 30000 / 50000, como el addon). */
    public static int manaSkinLevel(double maxMana) {
        if (maxMana >= 50000) return 4;
        if (maxMana >= 30000) return 3;
        if (maxMana >= 10000) return 2;
        if (maxMana >= 1000) return 1;
        return 0;
    }

    /** Regeneracion extra por tick del Factor Laplace segun el mana maximo (tramos del addon). */
    public static double laplaceRegen(double maxMana) {
        if (maxMana > 3999) return 2.0;
        if (maxMana > 2999) return 1.2;
        if (maxMana > 1499) return 0.8;
        if (maxMana > 999) return 0.6;
        if (maxMana > 599) return 0.4;
        if (maxMana > 299) return 0.2;
        return 0.1;
    }
}

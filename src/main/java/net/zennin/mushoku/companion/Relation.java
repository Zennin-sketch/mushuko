package net.zennin.mushoku.companion;

import net.minecraft.nbt.CompoundTag;

/** Relacion de un personaje con un jugador: corazones (-100 a 200) y cansancio de cada tipo de charla. */
public class Relation {
    public int hearts;
    /** Cuantas veces se repitio cada interaccion hace poco (baja la probabilidad de exito, como en MCA). */
    public final CompoundTag fatigue = new CompoundTag();
    public long lastFatigueDecay;
    /** Dia del juego de la ultima accion especial (leccion, entrenamiento...) y si Kishirika ya dio su ojo. */
    public long lastSpecialDay = -1;
    public boolean gotEye;

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Hearts", hearts);
        t.putLong("SpecialDay", lastSpecialDay);
        t.putBoolean("GotEye", gotEye);
        return t;
    }

    public static Relation load(CompoundTag t) {
        Relation r = new Relation();
        r.hearts = t.getInt("Hearts");
        r.lastSpecialDay = t.contains("SpecialDay") ? t.getLong("SpecialDay") : -1;
        r.gotEye = t.getBoolean("GotEye");
        return r;
    }

    public void addHearts(int n) { hearts = Math.max(-100, Math.min(200, hearts + n)); }

    public int fatigue(String key) { return fatigue.getInt(key); }

    public void tire(String key) { fatigue.putInt(key, fatigue.getInt(key) + 1); }

    /** Cada minuto se olvida un poco de la repeticion. */
    public void decay(long now) {
        if (now - lastFatigueDecay < 1200) return;
        lastFatigueDecay = now;
        for (String k : fatigue.getAllKeys()) fatigue.putInt(k, Math.max(0, fatigue.getInt(k) - 1));
    }
}

package net.zennin.mushoku.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.zennin.mushoku.MushokuMod;

/** Sonidos propios del mod. */
public final class ModSounds {
    /** Zumbido de oidos despues del Golpe Nuclear de nivel 20. */
    public static final SoundEvent NUCLEAR_RING = reg("spell.nuclear_ring");

    private ModSounds() {}

    private static SoundEvent reg(String id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, MushokuMod.id(id), SoundEvent.createVariableRangeEvent(MushokuMod.id(id)));
    }

    public static void init() {}
}

package net.zennin.mushoku.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Datos de magia del jugador. playerClass: 0 = sin elegir, 1 = Mago, 2 = Espadachin (igual que "classpick" del original).
 */
public record ManaData(double mana, double maxMana, int playerClass) {
    public static final ManaData DEFAULT = new ManaData(100, 100, 0);

    public static final Codec<ManaData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("mana").forGetter(ManaData::mana),
            Codec.DOUBLE.fieldOf("max_mana").forGetter(ManaData::maxMana),
            Codec.INT.optionalFieldOf("class", 0).forGetter(ManaData::playerClass)
    ).apply(i, ManaData::new));

    public ManaData withMana(double value) {
        return new ManaData(Math.max(0, Math.min(maxMana, value)), maxMana, playerClass);
    }

    public ManaData withMaxMana(double value) {
        return new ManaData(Math.min(mana, value), value, playerClass);
    }

    public ManaData withClass(int value) {
        return new ManaData(mana, maxMana, value);
    }
}

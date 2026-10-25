package net.zennin.mushoku.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.zennin.mushoku.magic.Element;

/**
 * Estado de magia del jugador.
 * queued: hechizo listo para lanzar ("" = ninguno). noElement: modo sin elemento.
 * water/fire/earth/wind: nivel aprendido de cada elemento (1 = Principiante ... 6 = Imperial).
 */
public record MagicData(String queued, boolean noElement, int water, int fire, int earth, int wind) {
    public static final MagicData DEFAULT = new MagicData("", false, 1, 1, 1, 1);

    public static final Codec<MagicData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("queued", "").forGetter(MagicData::queued),
            Codec.BOOL.optionalFieldOf("no_element", false).forGetter(MagicData::noElement),
            Codec.INT.optionalFieldOf("water", 1).forGetter(MagicData::water),
            Codec.INT.optionalFieldOf("fire", 1).forGetter(MagicData::fire),
            Codec.INT.optionalFieldOf("earth", 1).forGetter(MagicData::earth),
            Codec.INT.optionalFieldOf("wind", 1).forGetter(MagicData::wind)
    ).apply(i, MagicData::new));

    public int level(Element e) {
        return switch (e) {
            case WATER -> water;
            case FIRE -> fire;
            case EARTH -> earth;
            case WIND -> wind;
            case NONE -> Math.max(Math.max(water, fire), Math.max(earth, wind));
        };
    }

    public MagicData withLevel(Element e, int lvl) {
        return switch (e) {
            case WATER -> new MagicData(queued, noElement, lvl, fire, earth, wind);
            case FIRE -> new MagicData(queued, noElement, water, lvl, earth, wind);
            case EARTH -> new MagicData(queued, noElement, water, fire, lvl, wind);
            case WIND -> new MagicData(queued, noElement, water, fire, earth, lvl);
            case NONE -> this;
        };
    }

    public MagicData withQueued(String spell) { return new MagicData(spell, noElement, water, fire, earth, wind); }

    public MagicData withNoElement(boolean v) { return new MagicData(queued, v, water, fire, earth, wind); }
}

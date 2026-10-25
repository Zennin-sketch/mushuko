package net.zennin.mushoku.hitogami;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * created: ya creo su personaje con Hitogami. dreaming: esta en el sueno (y a donde volver al despertar).
 */
public record HitogamiData(boolean created, boolean dreaming, String retDim, double rx, double ry, double rz, float ryaw) {
    public static final HitogamiData DEFAULT = new HitogamiData(false, false, "", 0, 0, 0, 0);
    public static final Codec<HitogamiData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("created", false).forGetter(HitogamiData::created),
            Codec.BOOL.optionalFieldOf("dreaming", false).forGetter(HitogamiData::dreaming),
            Codec.STRING.optionalFieldOf("ret_dim", "").forGetter(HitogamiData::retDim),
            Codec.DOUBLE.optionalFieldOf("rx", 0.0).forGetter(HitogamiData::rx),
            Codec.DOUBLE.optionalFieldOf("ry", 0.0).forGetter(HitogamiData::ry),
            Codec.DOUBLE.optionalFieldOf("rz", 0.0).forGetter(HitogamiData::rz),
            Codec.FLOAT.optionalFieldOf("ryaw", 0f).forGetter(HitogamiData::ryaw)
    ).apply(i, HitogamiData::new));

    public HitogamiData withCreated() { return new HitogamiData(true, dreaming, retDim, rx, ry, rz, ryaw); }

    public HitogamiData dream(String dim, double x, double y, double z, float yaw) { return new HitogamiData(created, true, dim, x, y, z, yaw); }

    public HitogamiData awake() { return new HitogamiData(created, false, "", 0, 0, 0, 0); }
}

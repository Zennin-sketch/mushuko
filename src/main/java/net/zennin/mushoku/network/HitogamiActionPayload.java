package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: crear el personaje (raza, rasgo, clase, afinidad) o despertar del sueno. */
public record HitogamiActionPayload(int action, String race, int variant, int height, int cls, String affinity) implements CustomPacketPayload {
    public static final int CREATE = 0, WAKE = 1;
    public static final Type<HitogamiActionPayload> TYPE = new Type<>(MushokuMod.id("hitogami_action"));
    public static final StreamCodec<ByteBuf, HitogamiActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HitogamiActionPayload::action,
            ByteBufCodecs.STRING_UTF8, HitogamiActionPayload::race,
            ByteBufCodecs.VAR_INT, HitogamiActionPayload::variant,
            ByteBufCodecs.VAR_INT, HitogamiActionPayload::height,
            ByteBufCodecs.VAR_INT, HitogamiActionPayload::cls,
            ByteBufCodecs.STRING_UTF8, HitogamiActionPayload::affinity,
            HitogamiActionPayload::new);

    public static HitogamiActionPayload wake() { return new HitogamiActionPayload(WAKE, "", 0, 0, 0, ""); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

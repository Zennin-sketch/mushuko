package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: crear el personaje (raza, rasgo, color, clase, afinidad) o despertar del sueno. */
public record HitogamiActionPayload(int action, String race, int variant, int height, int color, int cls, String affinity) implements CustomPacketPayload {
    public static final int CREATE = 0, WAKE = 1;
    public static final Type<HitogamiActionPayload> TYPE = new Type<>(MushokuMod.id("hitogami_action"));
    public static final StreamCodec<ByteBuf, HitogamiActionPayload> CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.action());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.race());
        ByteBufCodecs.VAR_INT.encode(buf, p.variant());
        ByteBufCodecs.VAR_INT.encode(buf, p.height());
        ByteBufCodecs.INT.encode(buf, p.color());
        ByteBufCodecs.VAR_INT.encode(buf, p.cls());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.affinity());
    }, buf -> new HitogamiActionPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));

    public static HitogamiActionPayload wake() { return new HitogamiActionPayload(WAKE, "", 0, 0, -1, 0, ""); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

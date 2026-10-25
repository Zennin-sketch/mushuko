package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: abrir el dialogo de Hitogami. mode 0 = presentacion, 1 = oraculo (text = la profecia). */
public record HitogamiScreenPayload(int mode, String text) implements CustomPacketPayload {
    public static final Type<HitogamiScreenPayload> TYPE = new Type<>(MushokuMod.id("hitogami_screen"));
    public static final StreamCodec<ByteBuf, HitogamiScreenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HitogamiScreenPayload::mode,
            ByteBufCodecs.STRING_UTF8, HitogamiScreenPayload::text,
            HitogamiScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

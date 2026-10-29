package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: grabar este hechizo en un pergamino (Mesa de Investigacion abierta). */
public record EngravePayload(String spell) implements CustomPacketPayload {
    public static final Type<EngravePayload> TYPE = new Type<>(MushokuMod.id("engrave"));
    public static final StreamCodec<ByteBuf, EngravePayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, EngravePayload::spell, EngravePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

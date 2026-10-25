package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** S2C: reproducir una animacion del addon en otro jugador ("" = cortar la que tenga). */
public record AnimPayload(int entity, String anim) implements CustomPacketPayload {
    public static final Type<AnimPayload> TYPE = new Type<>(MushokuCombat.id("anim"));
    public static final StreamCodec<ByteBuf, AnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimPayload::entity, ByteBufCodecs.STRING_UTF8, AnimPayload::anim, AnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

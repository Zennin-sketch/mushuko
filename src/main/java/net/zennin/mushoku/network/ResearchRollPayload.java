package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: resultado de una investigacion de hechizo ("" = fallo) para la ruleta de la Mesa. */
public record ResearchRollPayload(String spell) implements CustomPacketPayload {
    public static final Type<ResearchRollPayload> TYPE = new Type<>(MushokuMod.id("research_roll"));
    public static final StreamCodec<ByteBuf, ResearchRollPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ResearchRollPayload::spell, ResearchRollPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

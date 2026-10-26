package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> clientes: hechizo listo para lanzar de un jugador ("" = ninguno). Lo ven todos los que lo tienen cerca. */
public record ReadySpellPayload(int entityId, String spell) implements CustomPacketPayload {
    public static final Type<ReadySpellPayload> TYPE = new Type<>(MushokuMod.id("ready_spell"));
    public static final StreamCodec<ByteBuf, ReadySpellPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReadySpellPayload::entityId,
            ByteBufCodecs.STRING_UTF8, ReadySpellPayload::spell,
            ReadySpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

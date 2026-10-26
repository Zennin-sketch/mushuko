package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/**
 * Servidor -> clientes: que un jugador reproduzca una animacion del addon de combate (cargar el arco, habilidades
 * del arquero). Si el addon no esta, el cliente no la recibe (no hay a quien mandarsela).
 */
public record PlayAnimPayload(int entityId, String anim) implements CustomPacketPayload {
    public static final Type<PlayAnimPayload> TYPE = new Type<>(MushokuMod.id("play_anim"));
    public static final StreamCodec<ByteBuf, PlayAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayAnimPayload::entityId,
            ByteBufCodecs.STRING_UTF8, PlayAnimPayload::anim,
            PlayAnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

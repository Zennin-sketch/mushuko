package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> clientes: este jugador salio impulsado por Rafaga (pose de planeo hasta que aterrice). */
public record GustGlidePayload(int entityId) implements CustomPacketPayload {
    public static final Type<GustGlidePayload> TYPE = new Type<>(MushokuMod.id("gust_glide"));
    public static final StreamCodec<ByteBuf, GustGlidePayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GustGlidePayload::entityId, GustGlidePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: hechizo en cola, modo sin elemento y niveles por elemento. */
public record MagicSyncPayload(String queued, boolean noElement, int water, int fire, int earth, int wind) implements CustomPacketPayload {
    public static final Type<MagicSyncPayload> TYPE = new Type<>(MushokuMod.id("magic_sync"));
    public static final StreamCodec<ByteBuf, MagicSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MagicSyncPayload::queued,
            ByteBufCodecs.BOOL, MagicSyncPayload::noElement,
            ByteBufCodecs.VAR_INT, MagicSyncPayload::water,
            ByteBufCodecs.VAR_INT, MagicSyncPayload::fire,
            ByteBufCodecs.VAR_INT, MagicSyncPayload::earth,
            ByteBufCodecs.VAR_INT, MagicSyncPayload::wind,
            MagicSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

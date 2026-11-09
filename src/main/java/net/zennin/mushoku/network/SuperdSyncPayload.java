package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: ticks que faltan para cada habilidad Superd (embestida, giro, lanzamiento, furia). */
public record SuperdSyncPayload(int lunge, int whirl, int thr, int fury) implements CustomPacketPayload {
    public static final Type<SuperdSyncPayload> TYPE = new Type<>(MushokuMod.id("superd_sync"));
    public static final StreamCodec<ByteBuf, SuperdSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SuperdSyncPayload::lunge, ByteBufCodecs.VAR_INT, SuperdSyncPayload::whirl,
            ByteBufCodecs.VAR_INT, SuperdSyncPayload::thr, ByteBufCodecs.VAR_INT, SuperdSyncPayload::fury, SuperdSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

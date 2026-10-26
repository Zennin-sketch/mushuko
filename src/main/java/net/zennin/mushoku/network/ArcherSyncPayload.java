package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ArcherData;

/** Servidor -> cliente: datos del arquero para el menu. */
public record ArcherSyncPayload(ArcherData data) implements CustomPacketPayload {
    public static final Type<ArcherSyncPayload> TYPE = new Type<>(MushokuMod.id("archer_sync"));
    public static final StreamCodec<ByteBuf, ArcherSyncPayload> CODEC =
            ByteBufCodecs.fromCodec(ArcherData.CODEC).map(ArcherSyncPayload::new, ArcherSyncPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

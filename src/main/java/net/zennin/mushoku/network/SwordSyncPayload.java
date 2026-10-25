package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.SwordData;

/** Servidor -> cliente: datos de espadachin para el menu y el HUD. */
public record SwordSyncPayload(SwordData data) implements CustomPacketPayload {
    public static final Type<SwordSyncPayload> TYPE = new Type<>(MushokuMod.id("sword_sync"));
    public static final StreamCodec<ByteBuf, SwordSyncPayload> CODEC =
            ByteBufCodecs.fromCodec(SwordData.CODEC).map(SwordSyncPayload::new, SwordSyncPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

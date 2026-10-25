package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ExtraData;

public record ExtraSyncPayload(ExtraData data) implements CustomPacketPayload {
    public static final Type<ExtraSyncPayload> TYPE = new Type<>(MushokuMod.id("extra_sync"));
    public static final StreamCodec<ByteBuf, ExtraSyncPayload> CODEC =
            ByteBufCodecs.fromCodec(ExtraData.CODEC).map(ExtraSyncPayload::new, ExtraSyncPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

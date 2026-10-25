package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.race.RaceData;

public record RaceSyncPayload(RaceData data) implements CustomPacketPayload {
    public static final Type<RaceSyncPayload> TYPE = new Type<>(MushokuMod.id("race_sync"));
    public static final StreamCodec<ByteBuf, RaceSyncPayload> CODEC =
            ByteBufCodecs.fromCodec(RaceData.CODEC).map(RaceSyncPayload::new, RaceSyncPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

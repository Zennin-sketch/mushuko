package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.guild.GuildData;

public record GuildSyncPayload(GuildData data) implements CustomPacketPayload {
    public static final Type<GuildSyncPayload> TYPE = new Type<>(MushokuMod.id("guild_sync"));
    public static final StreamCodec<ByteBuf, GuildSyncPayload> CODEC =
            ByteBufCodecs.fromCodec(GuildData.CODEC).map(GuildSyncPayload::new, GuildSyncPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

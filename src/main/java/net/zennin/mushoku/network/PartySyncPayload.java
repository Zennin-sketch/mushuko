package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

import java.util.List;

/** Servidor -> cliente: tu grupo (lider, miembros) y la invitacion pendiente. */
public record PartySyncPayload(String leader, List<String> members, String invite) implements CustomPacketPayload {
    public static final Type<PartySyncPayload> TYPE = new Type<>(MushokuMod.id("party_sync"));
    public static final StreamCodec<ByteBuf, PartySyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PartySyncPayload::leader,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PartySyncPayload::members,
            ByteBufCodecs.STRING_UTF8, PartySyncPayload::invite,
            PartySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

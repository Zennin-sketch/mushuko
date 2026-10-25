package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: interaccion elegida en el menu del personaje. */
public record CompanionActionPayload(int entityId, String action) implements CustomPacketPayload {
    public static final Type<CompanionActionPayload> TYPE = new Type<>(MushokuMod.id("companion_action"));
    public static final StreamCodec<ByteBuf, CompanionActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CompanionActionPayload::entityId,
            ByteBufCodecs.STRING_UTF8, CompanionActionPayload::action,
            CompanionActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

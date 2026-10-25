package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.companion.CompanionState;

/** Servidor -> cliente: abre o actualiza el menu de un personaje. */
public record CompanionStatePayload(CompanionState state) implements CustomPacketPayload {
    public static final Type<CompanionStatePayload> TYPE = new Type<>(MushokuMod.id("companion_state"));
    public static final StreamCodec<ByteBuf, CompanionStatePayload> CODEC =
            ByteBufCodecs.fromCodec(CompanionState.CODEC).map(CompanionStatePayload::new, CompanionStatePayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

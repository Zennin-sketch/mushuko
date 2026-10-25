package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** S2C: estado del arma de otro jugador. DRAWING / SHEATHING se ven con animacion. */
public record SheathStatePayload(int entity, byte state) implements CustomPacketPayload {
    public static final Type<SheathStatePayload> TYPE = new Type<>(MushokuCombat.id("sheath_state"));
    public static final StreamCodec<ByteBuf, SheathStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SheathStatePayload::entity, ByteBufCodecs.BYTE, SheathStatePayload::state, SheathStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

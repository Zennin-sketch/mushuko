package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** S2C: el jugador con esta id hizo una esquiva perfecta (brillo dorado). */
public record PerfectDodgePayload(int entity) implements CustomPacketPayload {
    public static final Type<PerfectDodgePayload> TYPE = new Type<>(MushokuCombat.id("perfect_dodge"));
    public static final StreamCodec<ByteBuf, PerfectDodgePayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PerfectDodgePayload::entity, PerfectDodgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

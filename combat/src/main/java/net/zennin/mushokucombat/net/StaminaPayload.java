package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** S2C: estamina actual del jugador. */
public record StaminaPayload(float value) implements CustomPacketPayload {
    public static final Type<StaminaPayload> TYPE = new Type<>(MushokuCombat.id("stamina"));
    public static final StreamCodec<ByteBuf, StaminaPayload> CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, StaminaPayload::value, StaminaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

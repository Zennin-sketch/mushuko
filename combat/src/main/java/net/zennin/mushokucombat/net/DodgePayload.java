package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** C2S: el jugador esquivo. dir: 0 adelante, 1 atras, 2 izquierda, 3 derecha. roll: true = roll, false = dash. */
public record DodgePayload(byte dir, boolean roll) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(MushokuCombat.id("dodge"));
    public static final StreamCodec<ByteBuf, DodgePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, DodgePayload::dir, ByteBufCodecs.BOOL, DodgePayload::roll, DodgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

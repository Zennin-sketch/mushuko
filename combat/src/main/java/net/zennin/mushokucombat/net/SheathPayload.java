package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** C2S: nuevo estado del arma (Sheath.SHEATHED / DRAWN / DRAWING / SHEATHING). */
public record SheathPayload(byte state) implements CustomPacketPayload {
    public static final Type<SheathPayload> TYPE = new Type<>(MushokuCombat.id("sheath"));
    public static final StreamCodec<ByteBuf, SheathPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BYTE, SheathPayload::state, SheathPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

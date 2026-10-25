package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: combinacion de flechas armada al soltar las teclas de conjuro. */
public record ComboPayload(String combo) implements CustomPacketPayload {
    public static final Type<ComboPayload> TYPE = new Type<>(MushokuMod.id("combo"));
    public static final StreamCodec<ByteBuf, ComboPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(ComboPayload::new, ComboPayload::combo);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

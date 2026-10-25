package net.zennin.mushokucombat.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushokucombat.MushokuCombat;

/** C2S: el jugador entra o sale de la pose de conjuro, o lanza el hechizo. */
public record PoseStatePayload(byte state) implements CustomPacketPayload {
    public static final Type<PoseStatePayload> TYPE = new Type<>(MushokuCombat.id("pose_state"));
    public static final StreamCodec<ByteBuf, PoseStatePayload> CODEC = StreamCodec.composite(ByteBufCodecs.BYTE, PoseStatePayload::state, PoseStatePayload::new);

    public static final byte NONE = 0, CAST_POSE = 1, CAST_RELEASE = 2;

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

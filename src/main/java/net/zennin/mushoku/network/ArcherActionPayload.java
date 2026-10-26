package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: elegir escuela, aprender, asignar y usar habilidades de arquero. */
public record ArcherActionPayload(int action, int slot, String arg) implements CustomPacketPayload {
    public static final int SCHOOL = 0, LEARN = 1, BIND = 2, USE_SLOT = 3;
    public static final Type<ArcherActionPayload> TYPE = new Type<>(MushokuMod.id("archer_action"));
    public static final StreamCodec<ByteBuf, ArcherActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArcherActionPayload::action,
            ByteBufCodecs.VAR_INT, ArcherActionPayload::slot,
            ByteBufCodecs.STRING_UTF8, ArcherActionPayload::arg,
            ArcherActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

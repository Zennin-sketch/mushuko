package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: acciones del menu de espadachin y teclas de habilidad. */
public record SwordActionPayload(int action, int slot, String arg) implements CustomPacketPayload {
    public static final int AFFINITY = 0, LEARN = 1, BIND = 2, TOGGLE = 3, TOUKI_LEVEL = 4, TOUKI_TOGGLE = 5, USE_SLOT = 6, USE_SILENT = 7;
    public static final Type<SwordActionPayload> TYPE = new Type<>(MushokuMod.id("sword_action"));
    public static final StreamCodec<ByteBuf, SwordActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SwordActionPayload::action,
            ByteBufCodecs.VAR_INT, SwordActionPayload::slot,
            ByteBufCodecs.STRING_UTF8, SwordActionPayload::arg,
            SwordActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

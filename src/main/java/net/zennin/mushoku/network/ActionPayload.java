package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/**
 * Acciones simples entre cliente y servidor.
 * Cliente -> servidor: CAST (tecla de activacion), CHOOSE_MAGE, CHOOSE_SWORDSMAN.
 * Servidor -> cliente: OPEN_CLASS_SCREEN.
 */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final int CAST = 0, CHOOSE_MAGE = 1, CHOOSE_SWORDSMAN = 2, OPEN_CLASS_SCREEN = 3, CHOOSE_ARCHER = 4, EYE_ONE = 10, EYE_TWO = 11, BOW_RELOAD = 12, ESCAPE = 13;
    public static final Type<ActionPayload> TYPE = new Type<>(MushokuMod.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

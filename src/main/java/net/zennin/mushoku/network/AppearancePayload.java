package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> clientes: rasgo de raza visible de un jugador (para dibujar orejas, cuernos, gema). */
public record AppearancePayload(int entityId, String kind, int variant, int height) implements CustomPacketPayload {
    public static final Type<AppearancePayload> TYPE = new Type<>(MushokuMod.id("appearance"));
    public static final StreamCodec<ByteBuf, AppearancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AppearancePayload::entityId,
            ByteBufCodecs.STRING_UTF8, AppearancePayload::kind,
            ByteBufCodecs.VAR_INT, AppearancePayload::variant,
            ByteBufCodecs.VAR_INT, AppearancePayload::height,
            AppearancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

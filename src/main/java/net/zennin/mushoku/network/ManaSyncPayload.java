package net.zennin.mushoku.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: mana actual y maximo, para la barra del HUD. */
public record ManaSyncPayload(double mana, double maxMana, int playerClass) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(MushokuMod.id("mana_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaSyncPayload::mana,
            ByteBufCodecs.DOUBLE, ManaSyncPayload::maxMana,
            ByteBufCodecs.VAR_INT, ManaSyncPayload::playerClass,
            ManaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

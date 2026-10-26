package net.zennin.mushoku.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> clientes: el carcaj que un jugador lleva en su ranura (para dibujarlo en la espalda con sus flechas). */
public record QuiverSyncPayload(int entityId, ItemStack quiver) implements CustomPacketPayload {
    public static final Type<QuiverSyncPayload> TYPE = new Type<>(MushokuMod.id("quiver_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuiverSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QuiverSyncPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC, QuiverSyncPayload::quiver,
            QuiverSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

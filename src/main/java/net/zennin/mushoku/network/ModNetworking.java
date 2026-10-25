package net.zennin.mushoku.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;

public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ManaSyncPayload.TYPE, ManaSyncPayload.CODEC);
    }

    public static void syncMana(ServerPlayer player) {
        ManaData d = ModAttachments.get(player);
        ServerPlayNetworking.send(player, new ManaSyncPayload(d.mana(), d.maxMana(), d.playerClass()));
    }
}

package net.zennin.mushoku.magic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;

/** Regeneracion de mana por tick, con la misma tabla que el mod original. */
public final class ManaSystem {
    private ManaSystem() {}

    public static double regenPerTick(double maxMana) {
        if (maxMana < 299) return 0.1;
        if (maxMana < 599) return 0.2;
        if (maxMana < 999) return 0.3;
        if (maxMana < 1499) return 0.4;
        if (maxMana < 2999) return 0.6;
        if (maxMana < 3499) return 0.8;
        return 1.0;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean syncTick = server.getTickCount() % 5 == 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ManaData d = ModAttachments.get(player);
                // En creativo el mana es infinito: siempre lleno
                if (player.getAbilities().instabuild && d.mana() < d.maxMana()) {
                    ModAttachments.set(player, d.withMana(d.maxMana()));
                } else if (d.mana() < d.maxMana()) {
                    ModAttachments.set(player, d.withMana(d.mana() + regenPerTick(d.maxMana())
                            + (ModAttachments.race(player).laplace() ? net.zennin.mushoku.race.RaceData.laplaceRegen(d.maxMana()) : 0)));
                }
                if (syncTick) ModNetworking.syncMana(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ModNetworking.syncMana(handler.getPlayer()));
    }
}

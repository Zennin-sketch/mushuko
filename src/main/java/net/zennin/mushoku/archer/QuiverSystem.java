package net.zennin.mushoku.archer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.QuiverSyncPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ranura del carcaj: se manda a los que te ven (para dibujarlo con sus flechas), se tira al morir (salvo
 * keepInventory) y se conserva al reaparecer.
 */
public final class QuiverSystem {
    private static final Map<UUID, ItemStack> LAST = new HashMap<>();

    private QuiverSystem() {}

    public static void broadcast(ServerPlayer p) {
        ItemStack q = ModAttachments.quiver(p).copy();
        LAST.put(p.getUUID(), q.copy());
        QuiverSyncPayload msg = new QuiverSyncPayload(p.getId(), q);
        if (ServerPlayNetworking.canSend(p, QuiverSyncPayload.TYPE)) ServerPlayNetworking.send(p, msg);
        for (ServerPlayer o : PlayerLookup.tracking(p))
            if (o != p && ServerPlayNetworking.canSend(o, QuiverSyncPayload.TYPE)) ServerPlayNetworking.send(o, msg);
    }

    public static void init() {
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof ServerPlayer target && ServerPlayNetworking.canSend(player, QuiverSyncPayload.TYPE))
                ServerPlayNetworking.send(player, new QuiverSyncPayload(target.getId(), ModAttachments.quiver(target).copy()));
        });
        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> broadcast(h.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> LAST.remove(h.getPlayer().getUUID()));
        // las flechas de adentro cambian sin pasar por la ranura (clic derecho, el arco que saca flechas)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ItemStack now = ModAttachments.quiver(p);
                ItemStack last = LAST.getOrDefault(p.getUUID(), ItemStack.EMPTY);
                if (!ItemStack.matches(now, last)) broadcast(p);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer p)) return;
            if (p.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
            ItemStack q = ModAttachments.quiver(p);
            if (q.isEmpty()) return;
            ModAttachments.setQuiver(p, ItemStack.EMPTY);
            p.drop(q, true, false);
        });
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            ItemStack q = ModAttachments.quiver(oldP);
            if (!q.isEmpty()) ModAttachments.setQuiver(newP, q.copy());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> broadcast(newP));
    }
}

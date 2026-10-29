package net.zennin.mushoku.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.zennin.mushoku.network.ActionPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escape: durante la Rafaga (el hechizo que te propulsa) no te atrapan ni las telaranas ni el lodo de Lodazal. Se
 * marca en el servidor y se avisa al cliente (el movimiento del jugador lo calcula su cliente).
 */
public final class Escape {
    public static final int TICKS = 50;
    private static final Map<UUID, Long> SERVER = new ConcurrentHashMap<>();
    /** Lado cliente: hasta que tick del mundo el jugador local se escapa. */
    public static long clientUntil = -1;

    private Escape() {}

    public static void grant(ServerPlayer p) {
        SERVER.put(p.getUUID(), p.level().getGameTime() + TICKS);
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, ActionPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new ActionPayload(ActionPayload.ESCAPE));
    }

    public static boolean active(Entity e) {
        long now = e.level().getGameTime();
        if (e.level().isClientSide) return clientUntil >= now && isLocal(e);
        Long until = SERVER.get(e.getUUID());
        if (until == null) return false;
        if (until < now) { SERVER.remove(e.getUUID()); return false; }
        return true;
    }

    private static boolean isLocal(Entity e) {
        try { return e == net.minecraft.client.Minecraft.getInstance().player; } catch (Throwable t) { return false; }
    }
}

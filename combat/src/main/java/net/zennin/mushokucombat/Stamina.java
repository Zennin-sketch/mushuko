package net.zennin.mushokucombat;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushokucombat.net.StaminaPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estamina del jugador (la decide el servidor).
 * Alcanza para 3 esquivas seguidas y recupera una esquiva cada 4.5 segundos.
 * Correr gasta muy poco (con la barra llena se puede correr 2 minutos seguidos) y mientras corres no se recarga.
 */
public final class Stamina {
    public static final float DODGE_COST = 10f;
    public static final float MAX = DODGE_COST * 3;
    public static final float REGEN_PER_TICK = DODGE_COST / 90f;   // 4.5 s = 90 ticks por esquiva
    public static final float SPRINT_PER_TICK = MAX / 2400f;       // barra llena = 2 minutos corriendo

    public static final AttachmentType<Float> STAMINA = AttachmentRegistry.<Float>builder()
            .initializer(() -> MAX)
            .buildAndRegister(MushokuCombat.id("stamina"));

    private static final Map<UUID, Float> SENT = new HashMap<>();

    private Stamina() {}

    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) tick(p);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SENT.remove(handler.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.player.getUUID()));
    }

    private static void tick(ServerPlayer p) {
        float s = get(p);
        if (p.isCreative() || p.isSpectator()) s = MAX;
        else if (p.isSprinting()) s = Math.max(0f, s - SPRINT_PER_TICK);
        else s = Math.min(MAX, s + REGEN_PER_TICK);
        set(p, s);
        Float last = SENT.get(p.getUUID());
        boolean edge = (s >= MAX || s <= 0f) && (last == null || last != s);
        if (last == null || edge || Math.abs(last - s) >= 0.4f) sync(p);
    }

    public static float get(Player p) {
        return p.getAttachedOrCreate(STAMINA);
    }

    public static void set(Player p, float value) {
        p.setAttached(STAMINA, Math.max(0f, Math.min(MAX, value)));
    }

    /** Gasta estamina si alcanza. */
    public static boolean consume(ServerPlayer p, float amount) {
        if (p.isCreative() || p.isSpectator()) return true;
        float s = get(p);
        if (s + 0.01f < amount) {
            sync(p);
            return false;
        }
        set(p, s - amount);
        sync(p);
        return true;
    }

    public static void sync(ServerPlayer p) {
        float s = get(p);
        SENT.put(p.getUUID(), s);
        if (ServerPlayNetworking.canSend(p, StaminaPayload.TYPE)) ServerPlayNetworking.send(p, new StaminaPayload(s));
    }
}

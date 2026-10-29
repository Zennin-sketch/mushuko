package net.zennin.mushokucombat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.zennin.mushokucombat.net.AnimPayload;
import net.zennin.mushokucombat.net.DodgePayload;
import net.zennin.mushokucombat.net.PerfectDodgePayload;
import net.zennin.mushokucombat.net.PoseStatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Roll (espadachin) y dash (mago). El movimiento lo hace el cliente; aca se cobra la estamina, se avisa a los demas
 * jugadores para que vean la animacion y se resuelve la esquiva perfecta: si un golpe llega en los primeros
 * 6 ticks (0.3 s) de la esquiva, se anula todo el dano, suena un golpe metalico, el personaje brilla dorado y queda
 * invulnerable 1.2 segundos.
 */
public final class Dodge {
    public static final int PERFECT_WINDOW = 6;
    /** Invulnerabilidad despues de una esquiva perfecta: 1.2 s (no la atraviesa ningun dano salvo el vacio o /kill). */
    public static final int INVULNERABLE_TICKS = 24;
    private static final Map<UUID, Long> STARTED = new HashMap<>();
    private static final Set<String> RELAYED = Set.of("cast_pose", "cast_release",
            "hip_draw", "hip_sheathe", "back_draw", "back_sheathe",
            "roll_forward", "roll_backward", "roll_left", "roll_right",
            "roll_forward_left", "roll_forward_right", "roll_backward_left", "roll_backward_right",
            "dash_forward", "dash_backward", "dash_left", "dash_right",
            "dash_forward_left", "dash_forward_right", "dash_backward_left", "dash_backward_right");
    /** Direcciones de la esquiva (el indice viaja en DodgePayload). */
    public static final String[] DIRS = {"forward", "backward", "left", "right",
            "forward_left", "forward_right", "backward_left", "backward_right"};

    private Dodge() {}

    static void init() {
        ServerPlayNetworking.registerGlobalReceiver(DodgePayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> onDodge(p, payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(PoseStatePayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                String anim = switch (payload.state()) {
                    case PoseStatePayload.CAST_POSE -> "cast_pose";
                    case PoseStatePayload.CAST_RELEASE -> "cast_release";
                    default -> "";
                };
                relay(p, anim);
            });
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer p) || !tryPerfect(p, source));
    }

    private static void onDodge(ServerPlayer p, DodgePayload payload) {
        if (!p.isAlive() || p.isSpectator()) return;
        if (!Stamina.consume(p, Stamina.DODGE_COST)) return;
        STARTED.put(p.getUUID(), p.serverLevel().getGameTime());
        String dir = DIRS[Math.max(0, Math.min(DIRS.length - 1, payload.dir()))];
        relay(p, (payload.roll() ? "roll_" : "dash_") + dir);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                payload.roll() ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS,
                payload.roll() ? 0.35f : 0.5f, payload.roll() ? 0.7f : 1.3f);
    }

    /** Manda la animacion a los que ven al jugador (el propio jugador ya la reproduce en su cliente). */
    private static void relay(ServerPlayer p, String anim) {
        if (!anim.isEmpty() && !RELAYED.contains(anim)) return;
        AnimPayload msg = new AnimPayload(p.getId(), anim);
        for (ServerPlayer other : PlayerLookup.tracking(p)) {
            if (other != p && ServerPlayNetworking.canSend(other, AnimPayload.TYPE)) ServerPlayNetworking.send(other, msg);
        }
    }

    private static final Map<UUID, Long> PERFECT_UNTIL = new HashMap<>();

    private static boolean tryPerfect(ServerPlayer p, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false; // vacio, /kill
        UUID id = p.getUUID();
        long now = p.serverLevel().getGameTime();
        Long until = PERFECT_UNTIL.get(id);
        if (until != null && now <= until) return true; // invulnerable despues de una esquiva perfecta
        boolean fromAttack = source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE);
        if (!fromAttack) return false;
        Long start = STARTED.get(id);
        if (start == null || now < start || now - start > PERFECT_WINDOW) return false;
        STARTED.remove(id);
        PERFECT_UNTIL.put(id, now + INVULNERABLE_TICKS);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45f, 1.9f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8f, 2.0f);
        PerfectDodgePayload msg = new PerfectDodgePayload(p.getId());
        if (ServerPlayNetworking.canSend(p, PerfectDodgePayload.TYPE)) ServerPlayNetworking.send(p, msg);
        for (ServerPlayer other : PlayerLookup.tracking(p)) {
            if (other != p && ServerPlayNetworking.canSend(other, PerfectDodgePayload.TYPE)) ServerPlayNetworking.send(other, msg);
        }
        return true;
    }
}

package net.zennin.mushokucombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.BowItem;
import net.zennin.mushoku.item.MushokuBowItem;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Animacion del arco para todos los jugadores que se ven (el "esta usando un item" ya viaja solo).
 * Arcos del mod (agarre tipo Unturned, arco en la mano izquierda): con el arco en la mano queda la pose de espera
 * (bow_idle), R carga la flecha (bow_load, llega del servidor), al tensar bow_draw y al soltar bow_release.
 * Arcos vanilla u otros: las versiones con el arco en la mano derecha (bow_draw_r / bow_release_r).
 * Las animaciones de una sola vez (cargar, habilidades) no se pisan hasta que terminan.
 */
public final class BowAnimClient {
    private static final Map<AbstractClientPlayer, String> STATE = new WeakHashMap<>();
    private static final Map<AbstractClientPlayer, Long> LOCK = new WeakHashMap<>();

    private BowAnimClient() {}

    /** Duracion (ticks) de las animaciones de una vez: mientras dura, la del arco no la reemplaza. */
    public static int oneShotTicks(String anim) {
        if (anim.equals("bow_load")) return 16;
        return switch (anim) {
            case "archer_barrage" -> 26;
            case "archer_arrow_rain", "archer_steel_storm", "archer_gale_dance", "archer_deadeye", "archer_phantom_arrow" -> 24;
            case "archer_disengage" -> 16;
            default -> anim.startsWith("archer_") ? 20 : 0;
        };
    }

    /** Llego una animacion de una vez (del servidor). */
    public static void oneShot(AbstractClientPlayer p, String anim) {
        if (p.level() == null) return;
        LOCK.put(p, p.level().getGameTime() + oneShotTicks(anim));
        STATE.put(p, anim);
        CombatAnimations.play(p, anim, 2);
    }

    static void tick(Minecraft mc) {
        if (mc.level == null) {
            STATE.clear();
            LOCK.clear();
            return;
        }
        long now = mc.level.getGameTime();
        for (AbstractClientPlayer p : mc.level.players()) {
            boolean mod = p.getMainHandItem().getItem() instanceof MushokuBowItem && ClientSheath.handVisible(p);
            boolean drawing = p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem;
            String was = STATE.getOrDefault(p, "");
            Long lock = LOCK.get(p);
            boolean locked = lock != null && lock > now;
            String want;
            if (drawing) {
                if (locked && was.equals("bow_load")) continue; // termina de colocar la flecha
                want = p.getUseItem().getItem() instanceof MushokuBowItem ? "bow_draw" : "bow_draw_r";
            } else if (was.equals("bow_draw") || was.equals("bow_draw_r")) {
                want = was.equals("bow_draw") ? "bow_release" : "bow_release_r";
                LOCK.put(p, now + 10);
            } else if (locked) {
                continue;
            } else if (mod) {
                // no pisar otra animacion del addon (esquivas, conjuros) mientras suena
                String cur = CombatAnimations.current(p);
                if (cur != null && !cur.isEmpty() && !cur.startsWith("bow_") && !cur.startsWith("archer_") && CombatAnimations.playing(p)) continue;
                want = "bow_idle";
            } else {
                if (was.isEmpty()) continue;
                want = "";
            }
            if (want.equals(was)) continue;
            int fade = want.startsWith("bow_draw") ? 3 : want.startsWith("bow_release") ? 1 : 5;
            CombatAnimations.play(p, want, fade);
            STATE.put(p, want);
        }
    }
}

package net.zennin.mushokucombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Brillo dorado de la esquiva perfecta (estilo AION): el reborde dura lo mismo que la invulnerabilidad (1.2 s) y se
 * desvanece en el ultimo tramo. El destello de pantalla del propio jugador sigue siendo corto.
 */
public final class GoldenGlow {
    /** Igual a Dodge.INVULNERABLE_TICKS. */
    public static final int DURATION = 24;
    /** Ticks del final en los que el reborde se apaga. */
    private static final int FADE = 8;
    /** Destello de pantalla al esquivar. */
    public static final int FLASH = 14;
    private static final Map<Integer, Integer> UNTIL = new HashMap<>();
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.3f), 1.1f);
    private static int ticks = 0;
    static int selfFlash = 0;

    private GoldenGlow() {}

    static void trigger(Minecraft mc, int entityId) {
        UNTIL.put(entityId, ticks + DURATION);
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e == null) return;
        if (e == mc.player) selfFlash = FLASH;
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            mc.level.addParticle(GOLD, e.getX() + Math.cos(a) * 0.7, e.getY() + 0.2 + (i % 4) * 0.45, e.getZ() + Math.sin(a) * 0.7,
                    Math.cos(a) * 0.05, 0.03, Math.sin(a) * 0.05);
        }
    }

    static void tick(Minecraft mc) {
        ticks++;
        if (selfFlash > 0) selfFlash--;
        UNTIL.values().removeIf(v -> v < ticks);
    }

    /** 1 mientras dura la invulnerabilidad, baja a 0 en los ultimos FADE ticks. */
    public static float strength(int entityId, float partialTick) {
        Integer until = UNTIL.get(entityId);
        if (until == null) return 0f;
        float left = until - ticks - partialTick;
        float t = Math.max(0f, Math.min(1f, left / FADE));
        return t * t * (3 - 2 * t);
    }
}

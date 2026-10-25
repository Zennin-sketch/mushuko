package net.zennin.mushokucombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Brillo dorado de la esquiva perfecta (estilo AION): dura 0.7 s y se apaga de a poco. */
public final class GoldenGlow {
    public static final int DURATION = 14;
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
        if (e == mc.player) selfFlash = DURATION;
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

    /** 1 recien esquivado, 0 apagado. */
    public static float strength(int entityId, float partialTick) {
        Integer until = UNTIL.get(entityId);
        if (until == null) return 0f;
        float left = until - ticks - partialTick;
        return Math.max(0f, Math.min(1f, left / DURATION));
    }
}

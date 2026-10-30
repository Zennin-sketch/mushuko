package net.zennin.mushoku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Rafaga: mientras el jugador va por el aire impulsado se dibuja con la forma del vuelo con elitros (solo la pose: ni
 * el sonido ni la velocidad de los elitros). Al tocar el piso o el agua vuelve a la normalidad. Si lleva elitros puestos
 * no se toca nada.
 */
public final class GustGlide {
    /** Jugador -> tick (del mundo) en que salio impulsado. */
    private static final Map<Integer, Long> START = new HashMap<>();

    /** Contador de veces que los mixins de pose preguntaron (para la prueba visual). */
    public static int HITS;

    private GustGlide() {}

    public static void start(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) START.put(entityId, mc.level.getGameTime());
    }

    /** Ticks de "vuelo" para la pose (0 = no esta planeando por Rafaga). */
    public static int ticks(LivingEntity e) {
        HITS++;
        Long s = START.get(e.getId());
        if (s == null || e.isFallFlying() || e.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        return (int) Math.max(1, mc.level.getGameTime() - s + 6);
    }

    public static boolean active(LivingEntity e) { return ticks(e) > 0; }

    /** Cada tick: termina al tocar el piso o el agua (con un margen al salir, que todavia esta en el piso). */
    public static void tick(Minecraft mc) {
        if (mc.level == null) { START.clear(); return; }
        long now = mc.level.getGameTime();
        START.entrySet().removeIf(en -> {
            var e = mc.level.getEntity(en.getKey());
            long age = now - en.getValue();
            if (!(e instanceof LivingEntity l) || !l.isAlive() || age > 400) return true;
            return age > 4 && (l.onGround() || l.isInWater() || l.onClimbable() || l.isPassenger());
        });
    }
}

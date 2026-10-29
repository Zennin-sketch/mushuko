package net.zennin.mushoku.client.fp;

import net.minecraft.client.Minecraft;

/**
 * Golpe en curso del jugador local (lo avisa el addon de combate cuando Better Combat arranca un ataque). Si hay una
 * animacion de primera persona con el mismo nombre que la del golpe, se dibuja esa en lugar de la mano vanilla.
 */
public final class FpAttack {
    private static String name;
    private static float start, length;

    private FpAttack() {}

    /** anim: nombre de la animacion del golpe (con o sin "mod:"); lengthTicks: lo que dura el golpe. */
    public static void start(String anim, float lengthTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || anim == null) return;
        int i = anim.indexOf(':');
        name = i >= 0 ? anim.substring(i + 1) : anim;
        start = mc.level.getGameTime();
        length = Math.max(1f, lengthTicks);
    }

    /** Animacion y tick del golpe en curso (o null). */
    public static Object[] current(float now) {
        if (name == null) return null;
        float t = (now - start) / length;
        if (t < 0 || t >= 1f) { name = null; return null; }
        FpAnim a = FpAnimations.get(name);
        if (a == null) return null;
        return new Object[]{a, t * a.length};
    }
}

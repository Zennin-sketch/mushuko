package net.zennin.mushoku.client;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * Codos y rodillas (doblez de Player Animator / bendy-lib) para la armadura y la ropa. Lo pone el addon de combate,
 * que conoce Player Animator; sin el addon no hay doblez.
 */
public final class LimbBend {
    private LimbBend() {}

    public interface Bender {
        /**
         * Dobla un punto como se dobla la pieza del jugador ('rightLeg', 'leftLeg', 'rightArm', 'leftArm'). El punto
         * esta en el marco propio de la pieza, en pixeles del modelo vanilla (y hacia abajo). Devuelve false si esa
         * pieza no esta doblada (el punto queda igual).
         */
        boolean bend(Entity e, String part, Vector3f q);
    }

    public static Bender BENDER = null;

    public static boolean bend(Entity e, String part, Vector3f q) {
        Bender b = BENDER;
        return b != null && b.bend(e, part, q);
    }
}

package net.zennin.mushokucombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.zennin.mushokucombat.Stamina;

/** Copia en el cliente de la estamina (la manda el servidor) y el freno de carrera cuando se agota. */
public final class ClientStamina {
    public static float value = Stamina.MAX;
    static int emptyFlash = 0;
    private static boolean sprintLocked = false;

    private ClientStamina() {}

    static void receive(float v) {
        value = v;
    }

    static void spend(float amount) {
        value = Math.max(0f, value - amount);
    }

    static void flashEmpty() {
        emptyFlash = 12;
    }

    static void tick(Minecraft mc) {
        if (emptyFlash > 0) emptyFlash--;
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (p.isCreative() || p.isSpectator()) {
            value = Stamina.MAX;
            sprintLocked = false;
            return;
        }
        // Prediccion local para que la barra se mueva suave entre mensajes del servidor
        if (p.isSprinting()) value = Math.max(0f, value - Stamina.SPRINT_PER_TICK);
        else value = Math.min(Stamina.MAX, value + Stamina.REGEN_PER_TICK);
        // Sin estamina no se puede correr hasta recuperar un poco
        if (value <= 0.05f) sprintLocked = true;
        else if (value >= Stamina.DODGE_COST * 0.3f) sprintLocked = false;
        if (sprintLocked && p.isSprinting()) {
            p.setSprinting(false);
            mc.options.keySprint.setDown(false);
        }
    }
}

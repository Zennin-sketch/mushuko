package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.zennin.mushoku.network.ActionPayload;
import net.zennin.mushoku.network.ComboPayload;

/**
 * Modo conjuro con bloqueo: tocas las dos teclas de conjuro una vez y queda activo (no hace falta mantenerlas).
 * Mientras esta activo, las flechas (WASD por defecto) arman la combinacion SIN dejar de moverte.
 * Para salir se vuelven a tocar las mismas dos teclas: la combinacion se prepara como siempre.
 * Atajo: Tab con el modo activo prepara la combinacion y la lanza de una.
 * Si la Tecla de Conjuro 2 esta sin asignar, basta con la tecla 1.
 */
public final class CastingClient {
    public static boolean casting = false;
    public static String combo = "";
    private static boolean upPrev, downPrev, leftPrev, rightPrev, shootPrev, toggleHeld;

    private CastingClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CastingClient::tick);
    }

    private static boolean toggleDown() {
        boolean k1 = ModKeys.isDown(ModKeys.CAST_1);
        if (ModKeys.CAST_2.isUnbound()) return k1;
        return k1 && ModKeys.isDown(ModKeys.CAST_2);
    }

    private static void finish(boolean shoot) {
        if (!combo.isEmpty()) ClientPlayNetworking.send(new ComboPayload(combo));
        if (shoot) ClientPlayNetworking.send(new ActionPayload(ActionPayload.CAST));
        casting = false;
        combo = "";
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null) { casting = false; combo = ""; return; }
        if (mc.screen != null) { toggleHeld = false; return; } // abrir el chat o el inventario no cancela el modo
        boolean isMage = net.zennin.mushoku.magic.SpellCasting.isMageClass(ClientManaState.playerClass)
                || (ClientManaState.playerClass == 2 && ClientSwordState.data.knows("ng_magic"));
        if (!isMage) { casting = false; combo = ""; }

        boolean toggle = toggleDown();
        boolean up = ModKeys.isDown(ModKeys.UP), down = ModKeys.isDown(ModKeys.DOWN),
                left = ModKeys.isDown(ModKeys.LEFT), right = ModKeys.isDown(ModKeys.RIGHT);
        boolean shoot = ModKeys.isDown(ModKeys.SHOOT);

        if (isMage && toggle && !toggleHeld) {
            if (casting) finish(false);
            else { casting = true; combo = ""; }
        }
        toggleHeld = toggle;

        if (casting && combo.length() < 8) {
            if (up && !upPrev) combo += 'u';
            if (down && !downPrev) combo += 'd';
            if (left && !leftPrev) combo += 'l';
            if (right && !rightPrev) combo += 'r';
        }
        upPrev = up; downPrev = down; leftPrev = left; rightPrev = right;

        if (shoot && !shootPrev && isMage) {
            if (casting) finish(true);
            else ClientPlayNetworking.send(new ActionPayload(ActionPayload.CAST));
        }
        shootPrev = shoot;
    }
}

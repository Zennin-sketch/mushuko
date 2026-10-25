package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.zennin.mushoku.network.ActionPayload;
import net.zennin.mushoku.network.ComboPayload;

/** Mientras mantenes las dos teclas de conjuro, WASD arman la combinacion de flechas. Al soltar se manda al servidor. */
public final class CastingClient {
    public static boolean casting = false;
    public static String combo = "";
    private static boolean upPrev, downPrev, leftPrev, rightPrev, shootPrev;

    private CastingClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CastingClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) {
            casting = false;
            combo = "";
            return;
        }
        boolean isMage = ClientManaState.playerClass == 1;
        boolean nowCasting = isMage && ModKeys.isDown(ModKeys.CAST_1) && ModKeys.isDown(ModKeys.CAST_2);
        boolean up = ModKeys.isDown(ModKeys.UP), down = ModKeys.isDown(ModKeys.DOWN),
                left = ModKeys.isDown(ModKeys.LEFT), right = ModKeys.isDown(ModKeys.RIGHT);

        if (nowCasting && !casting) combo = "";
        if (nowCasting && combo.length() < 8) {
            if (up && !upPrev) combo += 'u';
            if (down && !downPrev) combo += 'd';
            if (left && !leftPrev) combo += 'l';
            if (right && !rightPrev) combo += 'r';
        }
        if (!nowCasting && casting && !combo.isEmpty()) {
            ClientPlayNetworking.send(new ComboPayload(combo));
        }
        casting = nowCasting;
        upPrev = up; downPrev = down; leftPrev = left; rightPrev = right;

        boolean shoot = ModKeys.isDown(ModKeys.SHOOT);
        if (shoot && !shootPrev && isMage && !casting) ClientPlayNetworking.send(new ActionPayload(ActionPayload.CAST));
        shootPrev = shoot;
    }
}

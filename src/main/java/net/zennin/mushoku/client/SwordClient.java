package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.zennin.mushoku.network.SwordActionPayload;

/** Teclas del espadachin: V abre el menu, Z / X / C usan las habilidades de las ranuras 1-3. */
public final class SwordClient {
    private SwordClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(SwordClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null) return;
        boolean swordsman = ClientManaState.playerClass == 2;
        while (ModKeys.MENU.consumeClick()) {
            if (swordsman && mc.screen == null)
                mc.setScreen(ClientSwordState.data.style().isEmpty() ? new AffinityScreen() : new SwordsmanScreen());
        }
        KeyMapping[] keys = {ModKeys.SKILL_1, ModKeys.SKILL_2, ModKeys.SKILL_3};
        for (int i = 0; i < 3; i++) {
            while (keys[i].consumeClick()) {
                if (swordsman && mc.screen == null) ClientPlayNetworking.send(new SwordActionPayload(SwordActionPayload.USE_SLOT, i, ""));
            }
        }
    }
}

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
        boolean silentMage = ClientManaState.playerClass == 1 && ClientExtraState.data.silent();
        KeyMapping[] keys = {ModKeys.SKILL_1, ModKeys.SKILL_2, ModKeys.SKILL_3};
        for (int i = 0; i < 3; i++) {
            while (keys[i].consumeClick()) {
                if ((swordsman || silentMage) && mc.screen == null) ClientPlayNetworking.send(new SwordActionPayload(SwordActionPayload.USE_SLOT, i, ""));
            }
        }
        while (ModKeys.ADVENTURER.consumeClick()) if (mc.screen == null) mc.setScreen(new AdventurerScreen());
        while (ModKeys.RACE_SKILL.consumeClick())
            if (mc.screen == null) ClientPlayNetworking.send(new net.zennin.mushoku.network.GuildActionPayload(net.zennin.mushoku.network.GuildActionPayload.RACE_SKILL, 0, ""));
        while (ModKeys.EYE_1.consumeClick()) if (mc.screen == null) ClientPlayNetworking.send(new net.zennin.mushoku.network.ActionPayload(net.zennin.mushoku.network.ActionPayload.EYE_ONE));
        while (ModKeys.EYE_2.consumeClick()) if (mc.screen == null) ClientPlayNetworking.send(new net.zennin.mushoku.network.ActionPayload(net.zennin.mushoku.network.ActionPayload.EYE_TWO));

        // Garras de escalada: contra una pared subes (agachado te quedas quieto). Requiere "Objetos" del Dios Norte.
        var p = mc.player;
        boolean claws = net.zennin.mushoku.extra.ExtraTicks.holdingClaws(p)
                && (p.getAbilities().instabuild || ClientSwordState.data.knows("ng_items"));
        if (claws && p.horizontalCollision && !p.onGround()) {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, p.isShiftKeyDown() ? 0.0 : 0.2, v.z);
            p.fallDistance = 0;
        } else if (claws && p.horizontalCollision && mc.options.keyUp.isDown()) {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.2, v.z);
        }
    }
}

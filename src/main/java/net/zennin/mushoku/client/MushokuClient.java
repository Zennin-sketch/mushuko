package net.zennin.mushoku.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.zennin.mushoku.network.ManaSyncPayload;

public class MushokuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.TYPE, (payload, context) -> {
            ClientManaState.mana = payload.mana();
            ClientManaState.maxMana = payload.maxMana();
            ClientManaState.playerClass = payload.playerClass();
        });
        HudRenderCallback.EVENT.register(ManaBarHud::render);
    }
}

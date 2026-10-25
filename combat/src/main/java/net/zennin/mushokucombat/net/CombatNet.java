package net.zennin.mushokucombat.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class CombatNet {
    private CombatNet() {}

    public static void registerTypes() {
        PayloadTypeRegistry.playS2C().register(StaminaPayload.TYPE, StaminaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimPayload.TYPE, AnimPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PerfectDodgePayload.TYPE, PerfectDodgePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DodgePayload.TYPE, DodgePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PoseStatePayload.TYPE, PoseStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SheathPayload.TYPE, SheathPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SheathStatePayload.TYPE, SheathStatePayload.CODEC);
    }
}

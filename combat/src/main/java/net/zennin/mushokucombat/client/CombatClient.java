package net.zennin.mushokucombat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.zennin.mushokucombat.net.AnimPayload;
import net.zennin.mushokucombat.net.PerfectDodgePayload;
import net.zennin.mushokucombat.net.SheathStatePayload;
import net.zennin.mushokucombat.net.StaminaPayload;
import org.lwjgl.glfw.GLFW;

public final class CombatClient implements ClientModInitializer {
    /** Esquivar: roll si sos espadachin, dash si sos mago. Se cambia en Controles. */
    public static final KeyMapping DODGE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.mushoku_combat.dodge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.remushoku"));
    /** Modo combate: sacar / guardar el arma. */
    public static final KeyMapping COMBAT_MODE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.mushoku_combat.combat_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.remushoku"));

    @Override
    public void onInitializeClient() {
        CombatAnimations.init();
        ClientSheath.init();
        net.zennin.mushokucombat.client.compat.SkinLayersCompat.init();
        ClientPlayNetworking.registerGlobalReceiver(SheathStatePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> ClientSheath.receive(ctx.client(), payload.entity(), payload.state())));

        ClientPlayNetworking.registerGlobalReceiver(StaminaPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> ClientStamina.receive(payload.value())));
        ClientPlayNetworking.registerGlobalReceiver(AnimPayload.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().level == null) return;
            Entity e = ctx.client().level.getEntity(payload.entity());
            if (e instanceof AbstractClientPlayer p) CombatAnimations.play(p, payload.anim(), payload.anim().startsWith("cast") ? 4 : 2);
        }));
        ClientPlayNetworking.registerGlobalReceiver(PerfectDodgePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> GoldenGlow.trigger(ctx.client(), payload.entity())));

        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            DodgeClient.applyMotion(mc);
            ClientSheath.interceptAttack(mc);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientStamina.tick(mc);
            ClientSheath.tick(mc);
            DodgeClient.tick(mc);
            CastPoseClient.tick(mc);
            GoldenGlow.tick(mc);
        });
        HudRenderCallback.EVENT.register(StaminaHud::render);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer pr) {
                helper.register(new GoldenGlowLayer(pr));
                helper.register(new SheathLayer(pr, context.getItemInHandRenderer()));
            }
        });
    }
}

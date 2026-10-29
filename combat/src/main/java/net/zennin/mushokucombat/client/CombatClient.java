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
        // Caminar/correr con rodillas y codos, y la armadura/ropa del mod doblandose igual
        try { Locomotion.init(); } catch (Throwable t) { org.slf4j.LoggerFactory.getLogger("mushoku_combat").warn("Sin caminar propio: {}", t.toString()); }
        net.zennin.mushokucombat.client.compat.LimbBender.install();
        // Golpes de Better Combat: si el Taller tiene una animacion de primera persona con ese nombre, se usa esa
        try {
            net.bettercombat.api.client.BetterCombatClientEvents.ATTACK_START.register((player, hand) -> {
                String anim = hand.attack() == null ? null : hand.attack().animation();
                net.zennin.mushoku.client.fp.FpAttack.start(anim, player.getCurrentItemAttackStrengthDelay());
            });
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("mushoku_combat").warn("Sin primera persona de golpes (Better Combat cambio su API): {}", t.toString());
        }

        CombatAnimations.init();
        // con el arma guardada (modo combate apagado) el arco del mod tampoco se dibuja en la otra mano
        net.zennin.mushoku.client.render.BowRender.armBend = (p, arm, pose) -> {
            if (!(p instanceof dev.kosmx.playerAnim.impl.IAnimatedPlayer ap) || !ap.playerAnimator_getAnimation().isActive()) return;
            dev.kosmx.playerAnim.core.util.Vec3f d = ap.playerAnimator_getAnimation().get3DTransform(
                    arm == net.minecraft.world.entity.HumanoidArm.LEFT ? "leftArm" : "rightArm", dev.kosmx.playerAnim.api.TransformType.BEND,
                    new dev.kosmx.playerAnim.core.util.Vec3f(0f, 0f, 0f));
            float axisf = -d.getX(), bend = d.getY();
            pose.translate(0, 0.25f, 0);
            pose.mulPose(new org.joml.Quaternionf().rotateAxis(bend, new org.joml.Vector3f((float) Math.cos(axisf), 0, (float) Math.sin(axisf))));
            pose.translate(0, -0.25f, 0);
        };
        net.zennin.mushoku.client.render.BowRender.firstPerson = dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode::isFirstPersonPass;
        net.zennin.mushoku.client.render.BowRender.hidden = p -> p instanceof AbstractClientPlayer cp && !ClientSheath.handVisible(cp);
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
        // Animaciones que manda el mod principal (cargar el arco, habilidades del arquero)
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.PlayAnimPayload.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().level == null) return;
            Entity e = ctx.client().level.getEntity(payload.entityId());
            if (e instanceof AbstractClientPlayer p) {
                // conjuros con animacion propia (Campo Minado...): van directo a la capa del addon
                if (payload.anim().startsWith("magic_")) CombatAnimations.play(p, payload.anim(), 2);
                else BowAnimClient.oneShot(p, payload.anim());
            }
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
            BowAnimClient.tick(mc);
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

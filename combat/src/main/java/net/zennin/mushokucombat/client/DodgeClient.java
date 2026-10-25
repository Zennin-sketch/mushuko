package net.zennin.mushokucombat.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.client.ClientManaState;
import net.zennin.mushokucombat.Stamina;
import net.zennin.mushokucombat.net.DodgePayload;

/**
 * Roll (espadachin y jugadores sin clase) y dash (mago). Va hacia donde te estas moviendo con WASD;
 * si no te moves, hacia atras. El roll necesita estar en el piso; el dash se puede hacer una vez en el aire.
 */
public final class DodgeClient {
    private static final String[] DIRS = {"forward", "backward", "left", "right"};
    private static final double ROLL_SPEED = 0.46, DASH_SPEED = 0.8;
    private static final int ROLL_TICKS = 8, DASH_TICKS = 5;

    private static int cooldown = 0, motionTicks = 0;
    private static Vec3 motion = Vec3.ZERO;
    private static boolean dashing = false, airDashUsed = false;

    private DodgeClient() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            motionTicks = 0;
            return;
        }
        if (cooldown > 0) cooldown--;
        if (p.onGround()) airDashUsed = false;
        while (CombatClient.DODGE.consumeClick()) tryDodge(mc, p);
    }

    private static void tryDodge(Minecraft mc, LocalPlayer p) {
        if (mc.screen != null || cooldown > 0 || motionTicks > 0) return;
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || p.isSwimming()) return;
        boolean roll = ClientManaState.playerClass != 1;
        if (roll && !p.onGround()) return;
        if (!roll && !p.onGround() && airDashUsed) return;
        if (!p.isCreative() && ClientStamina.value + 0.01f < Stamina.DODGE_COST) {
            ClientStamina.flashEmpty();
            return;
        }
        float f = p.input.forwardImpulse, l = p.input.leftImpulse;
        if (Math.abs(f) < 0.1f && Math.abs(l) < 0.1f) f = -1f;
        byte dir = Math.abs(f) >= Math.abs(l) ? (byte) (f > 0 ? 0 : 1) : (byte) (l > 0 ? 2 : 3);
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        motion = forward.scale(f).add(left.scale(l)).normalize();
        dashing = !roll;
        motionTicks = roll ? ROLL_TICKS : DASH_TICKS;
        cooldown = roll ? 14 : 10;
        if (!roll && !p.onGround()) airDashUsed = true;
        if (!p.isCreative()) ClientStamina.spend(Stamina.DODGE_COST);
        CombatAnimations.play(p, (roll ? "roll_" : "dash_") + DIRS[dir], 2);
        ClientPlayNetworking.send(new DodgePayload(dir, roll));
        if (dashing && mc.level != null) {
            for (int i = 0; i < 10; i++) {
                mc.level.addParticle(ParticleTypes.CLOUD, p.getX() + (p.getRandom().nextDouble() - .5) * .6, p.getY() + p.getRandom().nextDouble() * 1.6,
                        p.getZ() + (p.getRandom().nextDouble() - .5) * .6, -motion.x * .15, 0.01, -motion.z * .15);
            }
        }
    }

    /** Al principio de cada tick: empuja al jugador mientras dura la esquiva. */
    static void applyMotion(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || motionTicks <= 0) return;
        motionTicks--;
        Vec3 v = p.getDeltaMovement();
        double speed = dashing ? DASH_SPEED : ROLL_SPEED;
        if (motionTicks == 0) speed *= 0.5; // frena al final
        p.setDeltaMovement(motion.x * speed, dashing ? Math.max(0.0, v.y * 0.2) : v.y, motion.z * speed);
        if (!dashing && mc.level != null && p.onGround() && motionTicks % 2 == 0) {
            mc.level.addParticle(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 0, 0.02, 0);
        }
    }
}

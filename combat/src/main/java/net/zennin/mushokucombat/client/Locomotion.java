package net.zennin.mushokucombat.client;

import dev.kosmx.playerAnim.api.layered.PlayerAnimationFrame;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

/**
 * Caminar y correr propios: en vez de levantar las piernas rectas (vanilla / Fresh Moves), las piernas se doblan en la
 * rodilla al pasar hacia adelante y los brazos van con el codo flexionado (mas al correr). Usa el doblez de Player
 * Animator, asi la armadura y la ropa del mod se doblan igual (LimbBend).
 * <p>
 * Prioridad baja (los ataques, esquivas y conjuros la tapan). Si hay un pack de recursos que anima al jugador
 * (Fresh Moves con EMF: optifine/cem/player.jem), esta capa no hace nada para no pelear con el.
 */
public final class Locomotion extends PlayerAnimationFrame {
    public static final int PRIORITY = 500;
    private static final ResourceLocation PACK_PLAYER = ResourceLocation.withDefaultNamespace("optifine/cem/player.jem");
    private static Boolean packAnimates;
    private static long packChecked;

    private final AbstractClientPlayer player;
    private boolean active;

    private Locomotion(AbstractClientPlayer player) { this.player = player; }

    static void init() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> stack.addAnimLayer(PRIORITY, new Locomotion(player)));
    }

    /** Un pack de recursos ya anima al jugador (se revisa cada tanto: el jugador puede cambiar los packs). */
    private static boolean packAnimates() {
        long now = System.currentTimeMillis();
        if (packAnimates == null || now - packChecked > 5000) {
            packChecked = now;
            packAnimates = Boolean.getBoolean("mushoku.nolocomotion")
                    || Minecraft.getInstance().getResourceManager().getResource(PACK_PLAYER).isPresent();
        }
        return packAnimates;
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void setupAnim(float partial) {
        resetPose();
        AbstractClientPlayer p = player;
        float amount = Math.min(1f, p.walkAnimation.speed(partial));
        active = amount > 0.02f && !packAnimates() && p.getPose() == Pose.STANDING && !p.isPassenger() && !p.isFallFlying()
                && !p.isSwimming() && !p.isSleeping() && !p.isShiftKeyDown() && !p.isUsingItem();
        if (!active) return;
        boolean run = p.isSprinting();
        // Capa Atrapallamas: brazos quietos bajo la capa y pasos a la mitad (como pidio el diseno de la capa)
        boolean cloak = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(net.zennin.mushoku.item.ArmorV2.FLAME_CLOAK);
        float phase = p.walkAnimation.position(partial) * 0.6662f;
        float a = amount;
        // ---- piernas: el muslo va y viene (menos que vanilla) y la rodilla se dobla al pasar hacia adelante
        float thigh = (run ? 0.85f : 0.72f) * (cloak ? 0.5f : 1f);
        float kneeMax = (run ? 1.35f : 0.75f) * (cloak ? 0.6f : 1f), kneeBase = run ? 0.22f : 0.06f;
        float rx = Mth.cos(phase) * thigh * a, lx = Mth.cos(phase + Mth.PI) * thigh * a;
        float rSwing = Math.max(0, Mth.sin(phase)), lSwing = Math.max(0, -Mth.sin(phase));
        float rKnee = a * (kneeBase + kneeMax * rSwing * rSwing), lKnee = a * (kneeBase + kneeMax * lSwing * lSwing);
        // con la rodilla doblada el muslo sube un poco mas adelante (asi el pie no raspa el piso)
        rx -= rKnee * 0.25f;
        lx -= lKnee * 0.25f;
        rightLeg.rot = new Vec3f(rx, 0, 0.0f);
        leftLeg.rot = new Vec3f(lx, 0, 0.0f);
        rightLeg.bend = new Pair<>(0f, rKnee);
        leftLeg.bend = new Pair<>(0f, lKnee);
        if (cloak) return;
        // ---- brazos: opuestos a las piernas, con el codo flexionado
        float arm = run ? 0.95f : 0.55f;
        float ra = Mth.cos(phase + Mth.PI) * arm * a, la = Mth.cos(phase) * arm * a;
        float elbow = run ? 1.25f : 0.3f;
        float rElbow = a * (elbow + (run ? 0.25f : 0.2f) * Math.max(0, -Mth.cos(phase + Mth.PI))),
              lElbow = a * (elbow + (run ? 0.25f : 0.2f) * Math.max(0, -Mth.cos(phase)));
        rightArm.rot = new Vec3f(ra - (run ? 0.15f : 0f) * a, 0, run ? 0.08f * a : 0.04f * a);
        leftArm.rot = new Vec3f(la - (run ? 0.15f : 0f) * a, 0, run ? -0.08f * a : -0.04f * a);
        rightArm.bend = new Pair<>(0f, -rElbow);
        leftArm.bend = new Pair<>(0f, -lElbow);
    }
}

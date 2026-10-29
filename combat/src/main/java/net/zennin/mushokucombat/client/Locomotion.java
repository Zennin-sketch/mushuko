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
    private static final ResourceLocation EYES_ONLY = ResourceLocation.withDefaultNamespace("optifine/cem/mushoku_eyes_only.txt");
    private static Boolean packAnimates;
    private static long packChecked;

    private final AbstractClientPlayer player;
    private boolean active;
    /** Inclinacion (doblez de la cintura) y giro del torso de este cuadro. */
    private float lean, twist;

    private Locomotion(AbstractClientPlayer player) { this.player = player; }

    static void init() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> stack.addAnimLayer(PRIORITY, new Locomotion(player)));
    }

    /** Un pack de recursos ya anima al jugador (se revisa cada tanto: el jugador puede cambiar los packs). */
    private static boolean packAnimates() {
        long now = System.currentTimeMillis();
        if (packAnimates == null || now - packChecked > 5000) {
            packChecked = now;
            var rm = Minecraft.getInstance().getResourceManager();
            // Fresh Moves "solo ojos" (tools/fresh) trae una marca: anima los ojos pero no el cuerpo
            packAnimates = Boolean.getBoolean("mushoku.nolocomotion")
                    || (rm.getResource(PACK_PLAYER).isPresent() && rm.getResource(EYES_ONLY).isEmpty());
        }
        return packAnimates;
    }

    @Override
    public boolean isActive() { return active; }

    /** El torso no esta en PlayerAnimationFrame: inclinacion con el doblez de la cintura y giro de hombros. */
    @Override
    public Vec3f get3DTransform(String part, dev.kosmx.playerAnim.api.TransformType type, float tickDelta, Vec3f value0) {
        if (active && "torso".equals(part)) {
            if (type == dev.kosmx.playerAnim.api.TransformType.BEND) return new Vec3f(0f, LEAN_SIGN * lean, 0f);
            if (type == dev.kosmx.playerAnim.api.TransformType.ROTATION) return new Vec3f(value0.getX(), twist, value0.getZ());
        }
        return super.get3DTransform(part, type, tickDelta, value0);
    }

    /** Signo del doblez de cintura que inclina hacia adelante. */
    private static final float LEAN_SIGN = Float.parseFloat(System.getProperty("mushoku.leansign", "1"));

    @Override
    public void setupAnim(float partial) {
        resetPose();
        lean = 0; twist = 0;
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
        if (run && !cloak) { sprint(phase, a); return; }
        // ---- caminar: el muslo va y viene (menos que vanilla) y la rodilla se dobla al pasar hacia adelante
        float thigh = (run ? 0.85f : 0.72f) * (cloak ? 0.5f : 1f);
        float kneeMax = (run ? 1.2f : 0.75f) * (cloak ? 0.6f : 1f), kneeBase = run ? 0.2f : 0.06f;
        float rx = Mth.cos(phase) * thigh * a, lx = Mth.cos(phase + Mth.PI) * thigh * a;
        float rSwing = Math.max(0, Mth.sin(phase)), lSwing = Math.max(0, -Mth.sin(phase));
        float rKnee = a * (kneeBase + kneeMax * rSwing * rSwing), lKnee = a * (kneeBase + kneeMax * lSwing * lSwing);
        rx -= rKnee * 0.25f;
        lx -= lKnee * 0.25f;
        rightLeg.rot = new Vec3f(rx, 0, 0.0f);
        leftLeg.rot = new Vec3f(lx, 0, 0.0f);
        rightLeg.bend = new Pair<>(0f, rKnee);
        leftLeg.bend = new Pair<>(0f, lKnee);
        lean = (run ? 0.2f : 0.04f) * a * (cloak ? 0.5f : 1f);
        twist = Mth.cos(phase) * 0.06f * a;
        if (cloak) return;
        // brazos: opuestos a las piernas, con el codo apenas doblado
        float ra = Mth.cos(phase + Mth.PI) * 0.55f * a, la = Mth.cos(phase) * 0.55f * a;
        float rElbow = a * (0.3f + 0.2f * Math.max(0, -Mth.cos(phase + Mth.PI))), lElbow = a * (0.3f + 0.2f * Math.max(0, -Mth.cos(phase)));
        rightArm.rot = new Vec3f(ra, 0, 0.04f * a);
        leftArm.rot = new Vec3f(la, 0, -0.04f * a);
        rightArm.bend = new Pair<>(0f, -rElbow);
        leftArm.bend = new Pair<>(0f, -lElbow);
    }

    /**
     * Correr al estilo Mo' Bends: cuerpo inclinado hacia adelante, rebote del cuerpo en cada paso, zancadas largas con
     * la rodilla de adelante bien alta y la de atras estirada, y brazos a 90 grados bombeando adelante y atras.
     */
    private void sprint(float phase, float a) {
        float s = Mth.sin(phase), c = Mth.cos(phase);
        // piernas: zancada larga; la que va adelante sube con la rodilla doblada, la de atras queda casi estirada
        float stride = 1.05f;
        float rx = c * stride * a, lx = -c * stride * a;
        float rKnee = a * (0.15f + 1.75f * (float) Math.pow(Math.max(0, s), 1.4)), lKnee = a * (0.15f + 1.75f * (float) Math.pow(Math.max(0, -s), 1.4));
        rx -= rKnee * 0.45f + 0.15f * a;
        lx -= lKnee * 0.45f + 0.15f * a;
        rightLeg.rot = new Vec3f(rx, 0, 0.03f * a);
        leftLeg.rot = new Vec3f(lx, 0, -0.03f * a);
        rightLeg.bend = new Pair<>(0f, rKnee);
        leftLeg.bend = new Pair<>(0f, lKnee);
        // cuerpo: inclinado y rebotando (baja cuando apoya, sube entre pasos)
        lean = 0.5f * a;
        twist = c * 0.2f * a;
        body.pos = new Vec3f(0f, -Math.abs(s) * 0.07f * a + 0.03f * a, 0f);
        // brazos: codo a 90 grados, bombean opuestos a las piernas; los hombros un poco abiertos
        float swing = 1.15f;
        float ra = -c * swing * a - 0.35f * a, la = c * swing * a - 0.35f * a;
        rightArm.rot = new Vec3f(ra, 0, 0.14f * a);
        leftArm.rot = new Vec3f(la, 0, -0.14f * a);
        rightArm.bend = new Pair<>(0f, -1.55f * a);
        leftArm.bend = new Pair<>(0f, -1.55f * a);
    }
}

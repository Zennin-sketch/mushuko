package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/**
 * El hechizo listo para lanzar, flotando delante de la mano levantada (tercera persona y otros jugadores).
 * Al quedar listo se ve como se forma; despues queda repitiendo su animacion hasta que se lanza.
 */
public class SpellHandLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Duracion de la formacion (igual que FORM en tools/spells/spell_fx.py). */
    public static final float FORM = 22;
    /** Tamano del hechizo en la mano (el proyectil completo seria muy grande). */
    private static final float SCALE = 0.5f / 16f;

    public SpellHandLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        ClientReadySpell.Ready r = ClientReadySpell.get(player);
        if (r == null || player.isInvisible()) return;
        HumanoidArm arm = player.getMainArm();
        pose.pushPose();
        getParentModel().translateToHand(arm, pose);
        // Del hombro a la palma (10 px) y un poco mas alla: el hechizo queda delante de la mano
        pose.translate((arm == HumanoidArm.LEFT ? -1 : 1) / 16f, 14.5f / 16f, 0);
        // El brazo apunta hacia donde mira: el +Z del modelo del hechizo pasa a ser el largo del brazo
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        draw(r, pose, buffers, light, partial);
        pose.popPose();
    }

    /** Dibuja la formacion o el hechizo en bucle segun la edad. */
    static void draw(ClientReadySpell.Ready r, PoseStack pose, MultiBufferSource buffers, int light, float partial) {
        float age = ClientReadySpell.age(r, partial);
        pose.scale(SCALE, SCALE, SCALE);
        if (age < FORM && ClientReadySpell.hasForm(r.model())) {
            SpellModelRenderer.render(SpellModel.get(r.model() + "_form"), pose, buffers, light, age, 0);
        } else {
            float grow = ClientReadySpell.hasForm(r.model()) ? 1f : Math.min(1f, 0.2f + age / 6f);
            pose.scale(grow, grow, grow);
            SpellModelRenderer.render(SpellModel.get(r.model()), pose, buffers, light, age, 0);
        }
    }

    /** Primera persona: el hechizo abajo a la derecha de la vista (o a la izquierda si sos zurdo). */
    public static void renderFirstPerson(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) return;
        ClientReadySpell.Ready r = ClientReadySpell.get(mc.player);
        if (r == null || ctx.consumers() == null) return;
        var cam = ctx.camera();
        float partial = ctx.tickCounter().getGameTimeDeltaPartialTick(true);
        Vec3 look = new Vec3(cam.getLookVector()), up = new Vec3(cam.getUpVector()), left = new Vec3(cam.getLeftVector());
        float side = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        Vec3 off = look.scale(0.85).add(up.scale(-0.26)).add(left.scale(-0.34 * side));
        PoseStack pose = ctx.matrixStack() != null ? ctx.matrixStack() : new PoseStack();
        pose.pushPose();
        pose.translate(off.x, off.y, off.z);
        float ry = (float) Math.toDegrees(Math.atan2(look.x, look.z));
        float rx = (float) Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.XP.rotationDegrees(-rx));
        pose.scale(0.8f, 0.8f, 0.8f);
        draw(r, pose, ctx.consumers(), net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, partial);
        pose.popPose();
    }
}

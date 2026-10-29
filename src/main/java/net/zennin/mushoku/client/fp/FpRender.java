package net.zennin.mushoku.client.fp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja una pose de primera persona (FpAnim) en el espacio de la camara: los dos brazos con la skin y el objeto.
 * Es la misma cuenta que hace el Taller de Combate en su vista "1a persona".
 */
public final class FpRender {
    private FpRender() {}

    /** Transicion entre animaciones: cuantos ticks tarda en pasar de una a otra. */
    public static final float BLEND_TICKS = 3f;

    private static final class Track {
        String name;
        float[] from, last;
        float since;
    }

    private static final Map<String, Track> TRACKS = new HashMap<>();

    /**
     * Pose de 'anim' en 'tick', mezclada con la anterior si recien cambio de animacion. 'channel' separa usos (arco,
     * golpes); 'now' es el tiempo de juego con decimales.
     */
    public static float[] pose(String channel, FpAnim anim, float tick, float now) {
        float[] p = anim.eval(tick);
        Track tr = TRACKS.computeIfAbsent(channel, k -> new Track());
        if (tr.name != null && !tr.name.equals(anim.name) && tr.last != null) {
            tr.from = tr.last;
            tr.since = now;
        }
        tr.name = anim.name;
        if (tr.from != null) {
            float w = (now - tr.since) / BLEND_TICKS;
            if (w >= 1 || w < 0) tr.from = null;
            else p = FpAnim.blend(tr.from, p, w * w * (3 - 2 * w));
        }
        tr.last = p;
        return p;
    }

    public static Vector3f pos(float[] pose, int part) { return new Vector3f(pose[part], pose[part + 1], pose[part + 2]); }

    public static Quaternionf rot(float[] pose, int part) { return new Quaternionf(pose[part + 3], pose[part + 4], pose[part + 5], pose[part + 6]); }

    /** Centro del puno del brazo (espacio de la camara). */
    public static Vector3f fist(float[] pose, int arm) {
        return rot(pose, arm).transform(new Vector3f(0, 8f / 16f, 0)).add(pos(pose, arm));
    }

    /** Lleva el PoseStack al marco del objeto (brazo que lo sostiene + su posicion relativa). */
    public static void toItem(PoseStack pose, float[] p, int arm) {
        Vector3f a = pos(p, arm);
        pose.translate(a.x, a.y, a.z);
        pose.mulPose(rot(p, arm));
        Vector3f i = pos(p, FpAnim.ITEM);
        pose.translate(i.x, i.y, i.z);
        pose.mulPose(rot(p, FpAnim.ITEM));
    }

    /** Los dos brazos del jugador (skin y mangas). */
    public static void arms(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light, float[] p) {
        arm(player, pose, buffers, light, true, p);
        arm(player, pose, buffers, light, false, p);
    }

    public static void arm(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light, boolean right, float[] p) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        int part = right ? FpAnim.R_ARM : FpAnim.L_ARM;
        Vector3f a = pos(p, part);
        pose.pushPose();
        pose.translate(a.x, a.y, a.z);
        pose.mulPose(rot(p, part));
        ResourceLocation skin = player.getSkin().texture();
        ModelPart arm = right ? m.rightArm : m.leftArm, sleeve = right ? m.rightSleeve : m.leftSleeve;
        // brazo fino (Alex): el cubo es de 3 px; se corre medio px para que el puno quede en el mismo lugar
        if (player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM) pose.translate((right ? -0.5f : 0.5f) / 16f, 0, 0);
        reset(arm);
        reset(sleeve);
        arm.render(pose, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        sleeve.render(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /**
     * Objeto en la mano (espada, baculo...): igual que en tercera persona (ItemInHandLayer) mas el giro/corrimiento del
     * objeto de la animacion.
     */
    public static void heldItem(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light, float[] p,
                                ItemStack stack, boolean rightHand) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        int arm = rightHand ? FpAnim.R_ARM : FpAnim.L_ARM;
        Vector3f a = pos(p, arm);
        pose.translate(a.x, a.y, a.z);
        pose.mulPose(rot(p, arm));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
        pose.translate((rightHand ? 1 : -1) / 16f, 2f / 16f, -10f / 16f);
        Vector3f i = pos(p, FpAnim.ITEM);
        pose.translate(i.x, i.y, i.z);
        pose.mulPose(rot(p, FpAnim.ITEM));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                rightHand ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, !rightHand,
                pose, buffers, light);
        pose.popPose();
    }

    private static void reset(ModelPart part) {
        part.x = 0; part.y = 0; part.z = 0;
        part.xRot = 0; part.yRot = 0; part.zRot = 0;
        part.visible = true;
    }
}

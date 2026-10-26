package net.zennin.mushoku.client.render;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.archer.BowSystem;
import net.zennin.mushoku.item.MushokuBowItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Primera persona de los arcos del mod, al estilo Unturned: los brazos y el arco quedan fijos a la camara (se ven
 * igual mires para donde mires). El arco en la izquierda, en diagonal; la derecha saca la flecha del carcaj por arriba
 * del hombro, la trae en la mano, la coloca y tensa hasta el menton. Todo en el espacio de la camara (bloques).
 */
public final class BowFirstPerson {
    /** Escala del arco en primera persona. */
    private static final float S = 0.55f;
    private static final Vector3f FWD = new Vector3f(0.13f, 0.05f, -1f).normalize();
    private static final Vector3f UP;
    static {
        // arriba del arco inclinado 50 grados hacia la derecha (en diagonal)
        Vector3f u = new Vector3f((float) Math.sin(Math.toRadians(50)), (float) Math.cos(Math.toRadians(50)), 0);
        UP = u.sub(new Vector3f(FWD).mul(u.dot(FWD))).normalize();
    }
    private static final Vector3f L_IDLE = new Vector3f(-0.13f, -0.27f, -0.72f), L_AIM = new Vector3f(-0.07f, -0.15f, -0.74f);
    private static final Vector3f R_IDLE = new Vector3f(0.19f, -0.42f, -0.5f), R_QUIVER = new Vector3f(0.42f, 0.16f, 0.25f);
    private static final Vector3f R_FLING = new Vector3f(0.34f, -0.16f, 0.02f);
    private static final Vector3f SH_L = new Vector3f(-0.38f, -0.9f, 0.05f), SH_R = new Vector3f(0.42f, -0.9f, 0.05f);

    private static boolean wasDrawing;
    private static float releaseAt = -1000;

    private BowFirstPerson() {}

    private static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(a).lerp(b, smooth(t));
    }

    public static void render(AbstractClientPlayer p, ItemStack bow, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float now = mc.level.getGameTime() + partial;
        boolean drawing = p.isUsingItem() && p.getUseItem() == bow;
        float speed = bow.getItem() instanceof MushokuBowItem b ? b.speed : 1f;
        float pull = drawing ? Math.min(1f, (p.getTicksUsingItem() + partial) * speed / 20f) : 0f;
        if (wasDrawing && !drawing) releaseAt = now;
        wasDrawing = drawing;
        Long ls = BowRender.LOAD_START.get(p.getId());
        float load = ls == null ? 2f : (now - ls) / BowSystem.LOAD_TICKS;
        boolean loading = load >= 0 && load < 1f;
        boolean loaded = BowSystem.isLoaded(bow) || (load >= 0.8f && load < 1.6f);
        float rel = (now - releaseAt) / 10f;

        // mano izquierda (arco) y orientacion del arco
        Vector3f L = lerp(L_IDLE, L_AIM, drawing ? Math.min(1, pull * 3) : (rel < 1 ? 1 - rel : 0));
        float back = BowRender.BRACE + BowRender.DRAW * pull;
        Vector3f nock = new Vector3f(FWD).mul(-back * S / 16f).add(L).add(new Vector3f(UP).mul(-0.02f));
        // mano derecha
        Vector3f R;
        boolean arrowInHand = false;
        if (drawing) {
            R = new Vector3f(nock);
        } else if (loading) {
            Vector3f rest = new Vector3f(FWD).mul(-BowRender.BRACE * S / 16f).add(L);
            if (load < 0.3f) R = lerp(R_IDLE, R_QUIVER, load / 0.3f);
            else if (load < 0.42f) R = new Vector3f(R_QUIVER);
            else if (load < 0.8f) { R = lerp(R_QUIVER, rest, (load - 0.42f) / 0.38f); arrowInHand = true; }
            else R = rest;
        } else if (rel < 1f) {
            R = rel < 0.25f ? lerp(nock, R_FLING, rel / 0.25f) : lerp(R_FLING, R_IDLE, (rel - 0.25f) / 0.75f);
        } else if (loaded) {
            R = lerp(R_IDLE, new Vector3f(FWD).mul(-BowRender.BRACE * S / 16f).add(L), 0.55f);
        } else {
            R = new Vector3f(R_IDLE);
        }
        // pequeno respiro en espera
        float breath = (float) Math.sin(now * 0.08f) * 0.006f;
        if (!drawing) { L.y += breath; R.y += breath * 0.7f; }

        arm(p, pose, buffers, light, false, L);
        arm(p, pose, buffers, light, true, R);

        // arco
        pose.pushPose();
        pose.translate(L.x, L.y, L.z);
        Vector3f z = new Vector3f(FWD).cross(UP).normalize();
        pose.mulPose(new Matrix4f(FWD.x, FWD.y, FWD.z, 0, UP.x, UP.y, UP.z, 0, z.x, z.y, z.z, 0, 0, 0, 0, 1));
        pose.scale(S, S, S);
        ItemStack ammo = p.getProjectile(bow);
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);
        boolean onString = drawing || (loaded && !arrowInHand && !(loading && load < 0.8f));
        BowRender.draw(pose, buffers, BowRender.style(bow), back, onString, ammo, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();

        // flecha en la mano derecha mientras la trae del carcaj
        if (arrowInHand) {
            float t = smooth((load - 0.42f) / 0.38f);
            Vector3f dir = new Vector3f(0.1f, 0.85f, 0.5f).normalize().lerp(FWD, t).normalize();
            int[] col = ArrowRender.colors(ammo);
            ArrowRender.draw(pose, Mesh3D.solid(buffers), new Vector3f(R).mul(16f).add(new Vector3f(dir).mul(-2f)), dir,
                    new Vector3f(0, 1, 0), 17f * S, col[0], col[1], light, OverlayTexture.NO_OVERLAY);
        }
    }

    /** Brazo del jugador (con su skin y manga) desde su hombro hasta la mano en 'hand'. */
    private static void arm(AbstractClientPlayer p, PoseStack pose, MultiBufferSource buffers, int light, boolean right, Vector3f hand) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        Vector3f sh = right ? SH_R : SH_L;
        Vector3f dir = new Vector3f(hand).sub(sh).normalize();
        Vector3f start = new Vector3f(hand).sub(new Vector3f(dir).mul(10f / 16f));
        Vector3f x = new Vector3f(-1, 0, 0);
        x.sub(new Vector3f(dir).mul(x.dot(dir))).normalize();
        Vector3f zz = new Vector3f(x).cross(dir).normalize();
        pose.pushPose();
        pose.translate(start.x, start.y, start.z);
        pose.mulPose(new Matrix4f(x.x, x.y, x.z, 0, dir.x, dir.y, dir.z, 0, zz.x, zz.y, zz.z, 0, 0, 0, 0, 1));
        // la mano queda en el centro del puno
        pose.translate((right ? 1 : -1) / 16f, 0, 0);
        ResourceLocation skin = p.getSkin().texture();
        ModelPart a = right ? m.rightArm : m.leftArm, s = right ? m.rightSleeve : m.leftSleeve;
        resetPart(a);
        resetPart(s);
        a.render(pose, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        s.render(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void resetPart(ModelPart part) {
        part.x = 0; part.y = 0; part.z = 0;
        part.xRot = 0; part.yRot = 0; part.zRot = 0;
        part.visible = true;
    }
}

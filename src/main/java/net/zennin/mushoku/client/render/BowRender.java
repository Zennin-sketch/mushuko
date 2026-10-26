package net.zennin.mushoku.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.archer.BowSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Arcos del mod dibujados por codigo: palas curvas hechas de muchos tramos (lisas), cuerda que va a la mano que tensa
 * y flecha cargada. Marco del arco (pixeles): +X hacia adonde sale la flecha, +Y arriba (palas), Z de costado; el
 * origen es el centro del agarre.
 */
public final class BowRender {
    public static final float BRACE = 3.2f, DRAW = 10.5f, ARROW = 17f;
    /** El addon de combate lo usa para no dibujar el arco mientras esta guardado. */
    public static java.util.function.Predicate<Player> hidden = p -> false;

    /** Forma y colores de cada arco. */
    public record Style(float riser, float limb, float w0, float w1, float t0, float t1, float base, float curve, float recurve,
                        float bend, int riserC, int limbC, int tipC, int gripC, int stringC, boolean glowString, int accent, String deco) {}

    public static Style style(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (id) {
            case "elven_bow" -> new Style(3.4f, 11.5f, 1.25f, 0.5f, 0.95f, 0.45f, 0.06f, 0.22f, 0.95f, 0.55f,
                    0xFFD9CFA8, 0xFFEDE6CC, 0xFFC9B67A, 0xFF3F6B3A, 0xFFF4F1E2, false, 0xFFD9B45A, "elven");
            case "beast_bow" -> new Style(3.6f, 9.0f, 1.9f, 0.8f, 1.45f, 0.7f, 0.24f, 0.5f, 1.15f, 0.5f,
                    0xFF6E5236, 0xFFE9DFC6, 0xFF8E7E62, 0xFF7A5A3C, 0xFFD8CBB0, false, 0xFF4A3A2A, "beast");
            case "mana_bow" -> new Style(3.4f, 10.5f, 1.4f, 0.55f, 1.0f, 0.5f, 0.1f, 0.3f, 0.7f, 0.55f,
                    0xFF2B3550, 0xFF5CB2F6, 0xFFB8E4FF, 0xFF1C2338, 0xFF9FE6FF, true, 0xFF7FD8FF, "mana");
            case "demon_bow" -> new Style(3.6f, 10.5f, 1.6f, 0.6f, 1.15f, 0.55f, 0.16f, 0.35f, 1.05f, 0.55f,
                    0xFF241A1E, 0xFF1E1418, 0xFF8E1B1B, 0xFF3A1414, 0xFFB02A2A, true, 0xFFD23A2A, "demon");
            default -> new Style(3.2f, 9.6f, 1.55f, 0.7f, 1.15f, 0.6f, 0.16f, 0.36f, 0.0f, 0.55f,
                    0xFF6B4A2B, 0xFF8C6239, 0xFF5E3F22, 0xFF3E2A1C, 0xFFE8E0C8, false, 0xFF2C1E14, "hunter");
        };
    }

    /**
     * Dibuja el arco en la mano del arco (la contraria a la habil, como en Unturned): la cuerda y la flecha van a la
     * mano que tensa. Se usa en tercera persona y en la primera persona animada.
     */
    public static void drawInHand(Player p, ItemStack bow, ArmedModel model, HumanoidArm bowArm, PoseStack pose,
                                  MultiBufferSource buffers, int light, int overlay) {
        HumanoidArm drawArm = bowArm.getOpposite();
        boolean drawing = p.isUsingItem() && p.getUseItem() == bow;
        Vector4f hand = null;
        if (drawing) {
            pose.pushPose();
            model.translateToHand(drawArm, pose);
            pose.translate((drawArm == HumanoidArm.LEFT ? 1 : -1) / 16f, 10.5f / 16f, 0);
            hand = pose.last().pose().transform(new Vector4f(0, 0, 0, 1));
            pose.popPose();
        }
        pose.pushPose();
        model.translateToHand(bowArm, pose);
        boolean left = bowArm == HumanoidArm.LEFT;
        pose.translate((left ? 1 : -1) / 16f, 10f / 16f, 0);
        // flecha a lo largo del brazo, palas "hacia arriba" del brazo estirado
        pose.mulPose(new Matrix4f(0, 1, 0, 0, 0, 0, -1, 0, -1, 0, 0, 0, 0, 0, 0, 1));
        // inclinado como en Unturned
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(left ? -16 : 16));
        float back = BRACE;
        if (hand != null) {
            Vector4f loc = new Matrix4f(pose.last().pose()).invert().transform(new Vector4f(hand));
            back = Math.max(BRACE, Math.min(BRACE + DRAW, -loc.x * 16f));
        }
        boolean arrow = drawing || BowSystem.isLoaded(bow);
        ItemStack ammo = arrow ? p.getProjectile(bow) : ItemStack.EMPTY;
        draw(pose, buffers, style(bow), back, arrow, ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo, light, overlay);
        pose.popPose();
    }

    /** back: cuanto esta atras la cuerda (px desde el agarre). */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Style st, float back, boolean arrow, ItemStack ammo,
                            int light, int overlay) {
        VertexConsumer vc = Mesh3D.solid(buffers);
        float pull = Math.max(0, Math.min(1, (back - BRACE) / DRAW));
        int glow = LightTexture.FULL_BRIGHT;
        Vector3f side = new Vector3f(0, 0, 1);
        // ---- cuerpo (empunadura)
        float rh = st.riser();
        Mesh3D.box(pose, vc, -0.8f, -rh, -0.75f, 1.0f, rh, 0.75f, st.riserC(), light, overlay);
        Mesh3D.box(pose, vc, -0.55f, -rh - 0.6f, -0.6f, 0.75f, -rh, 0.6f, Mesh3D.shade(st.riserC(), 0.85f), light, overlay);
        Mesh3D.box(pose, vc, -0.55f, rh, -0.6f, 0.75f, rh + 0.6f, 0.6f, Mesh3D.shade(st.riserC(), 0.85f), light, overlay);
        // agarre envuelto (tres vueltas)
        for (int i = 0; i < 3; i++) {
            float y0 = -1.9f + i * 1.3f;
            Mesh3D.box(pose, vc, -1.0f, y0, -0.95f, 1.2f, y0 + 1.15f, 0.95f, Mesh3D.shade(st.gripC(), i == 1 ? 1.08f : 0.96f), light, overlay);
        }
        // apoyo de la flecha
        Mesh3D.box(pose, vc, 0.9f, 1.95f, -0.45f, 1.5f, 2.35f, 0.45f, Mesh3D.shade(st.riserC(), 1.15f), light, overlay);
        // ---- palas
        List<Vector3f> top = limb(st, 1, rh, pull), bot = limb(st, -1, rh, pull);
        for (List<Vector3f> pts : List.of(top, bot)) {
            int n = pts.size() - 1;
            for (int i = 0; i < n; i++) {
                float t = (i + 0.5f) / n;
                float w = st.w0() + (st.w1() - st.w0()) * t, th = st.t0() + (st.t1() - st.t0()) * t;
                int c = Mesh3D.mix(st.limbC(), st.tipC(), (float) Math.pow(t, 1.6));
                c = Mesh3D.shade(c, i % 2 == 0 ? 1.0f : 0.94f);
                Mesh3D.bar(pose, vc, pts.get(i), pts.get(i + 1), side, w, th, c, light, overlay);
            }
            Vector3f tip = pts.get(n);
            Mesh3D.bar(pose, vc, tip, new Vector3f(tip).add(new Vector3f(tip).sub(pts.get(n - 1)).normalize().mul(0.8f)), side, 0.7f, 0.7f,
                    Mesh3D.shade(st.tipC(), 0.8f), light, overlay);
            deco(pose, vc, st, pts, light, overlay, glow);
        }
        // ---- cuerda
        Vector3f nock = new Vector3f(-back, 0, 0);
        Vector3f tT = top.get(top.size() - 1), tB = bot.get(bot.size() - 1);
        int sl = st.glowString() ? glow : light;
        Mesh3D.bar(pose, vc, tT, nock, side, 0.24f, 0.24f, st.stringC(), sl, overlay);
        Mesh3D.bar(pose, vc, nock, tB, side, 0.24f, 0.24f, st.stringC(), sl, overlay);
        // ---- adornos del cuerpo
        if (st.deco().equals("mana")) {
            pose.pushPose();
            pose.translate(1.9f / 16f, 0, 0);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45));
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(35));
            Mesh3D.cbox(pose, vc, 1.3f, 1.3f, 1.3f, 0xFFBFF0FF, glow, overlay);
            pose.popPose();
        } else if (st.deco().equals("demon")) {
            Mesh3D.bar(pose, vc, new Vector3f(1.0f, 0.6f, 0), new Vector3f(2.6f, 1.6f, 0), side, 0.6f, 0.5f, st.accent(), light, overlay);
            Mesh3D.bar(pose, vc, new Vector3f(1.0f, -0.6f, 0), new Vector3f(2.6f, -1.6f, 0), side, 0.6f, 0.5f, st.accent(), light, overlay);
            Mesh3D.box(pose, vc, 1.0f, -0.4f, -0.5f, 1.6f, 0.4f, 0.5f, 0xFFFF5A3A, glow, overlay);
        } else if (st.deco().equals("elven")) {
            Mesh3D.box(pose, vc, -0.9f, 2.0f, -0.85f, 1.1f, 2.3f, 0.85f, st.accent(), light, overlay);
            Mesh3D.box(pose, vc, -0.9f, -2.3f, -0.85f, 1.1f, -2.0f, 0.85f, st.accent(), light, overlay);
        }
        // ---- flecha cargada
        if (arrow) {
            int[] col = ArrowRender.colors(ammo);
            ArrowRender.draw(pose, vc, new Vector3f(-back, 0.0f, 0), new Vector3f(1, 0.012f, 0), new Vector3f(0, 1, 0), ARROW,
                    col[0], col[1], light, overlay);
        }
    }

    /** Puntos de una pala: arranca en el cuerpo y se curva hacia atras; las recurvas vuelven hacia adelante en la punta. */
    private static List<Vector3f> limb(Style st, int sgn, float rh, float pull) {
        int n = 12;
        List<Vector3f> pts = new ArrayList<>(n + 1);
        Vector3f p = new Vector3f(0.1f, sgn * (rh + 0.3f), 0);
        pts.add(new Vector3f(p));
        float seg = st.limb() / n;
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n;
            float th = st.base() + st.curve() * t + pull * st.bend() * (0.35f + 0.9f * t);
            float rc = 0.3f;
            if (st.recurve() > 0 && t > 1 - rc) th -= st.recurve() * (1 + pull * 0.3f) * ((t - (1 - rc)) / rc);
            p.add((float) -Math.sin(th) * seg, sgn * (float) Math.cos(th) * seg, 0);
            pts.add(new Vector3f(p));
        }
        return pts;
    }

    private static void deco(PoseStack pose, VertexConsumer vc, Style st, List<Vector3f> pts, int light, int overlay, int glow) {
        int n = pts.size() - 1;
        Vector3f tip = pts.get(n), prev = pts.get(n - 2);
        Vector3f d = new Vector3f(tip).sub(prev).normalize();
        Vector3f side = new Vector3f(0, 0, 1);
        switch (st.deco()) {
            case "elven" -> {
                // hojas doradas en las puntas, hacia adelante y afuera
                float sg = Math.signum(tip.y);
                Mesh3D.bar(pose, vc, tip, new Vector3f(tip).add(1.5f, 0.9f * sg, 0), side, 0.85f, 0.18f, st.accent(), light, overlay);
                Mesh3D.bar(pose, vc, new Vector3f(tip).add(-0.2f, -0.6f * sg, 0), new Vector3f(tip).add(1.1f, -0.2f * sg, 0), side, 0.6f, 0.16f,
                        Mesh3D.shade(st.accent(), 0.9f), light, overlay);
            }
            case "beast" -> {
                // pelo atado en la mitad de cada pala
                Vector3f m = pts.get(n / 3);
                for (int k = 0; k < 3; k++)
                    Mesh3D.bar(pose, vc, new Vector3f(m).add(0, k * 0.4f * Math.signum(m.y), 0),
                            new Vector3f(m).add(-1.4f - k * 0.3f, (k * 0.4f - 1.2f) * Math.signum(m.y), (k - 1) * 0.4f), side,
                            0.5f, 0.5f, Mesh3D.shade(st.accent(), 1f + k * 0.12f), light, overlay);
                Mesh3D.bar(pose, vc, new Vector3f(m).add(0, -0.5f * Math.signum(m.y), 0), new Vector3f(m).add(0, 0.5f * Math.signum(m.y), 0),
                        side, 2.1f, 1.8f, 0xFF7A5A3C, light, overlay);
            }
            case "mana" -> {
                for (int k = 2; k < n; k += 3)
                    Mesh3D.bar(pose, vc, pts.get(k), pts.get(k + 1), side, 0.3f, st.t0() + 0.15f, 0xFFDDF6FF, glow, overlay);
            }
            case "demon" -> {
                // espinas hacia afuera
                for (int k = 3; k < n; k += 3) {
                    Vector3f a = pts.get(k), b = pts.get(k + 1);
                    Vector3f dd = new Vector3f(b).sub(a).normalize();
                    Vector3f nrm = new Vector3f(dd.y, -dd.x, 0);
                    if (nrm.x < 0) nrm.mul(-1);
                    Mesh3D.bar(pose, vc, a, new Vector3f(a).add(new Vector3f(nrm).mul(1.4f)).add(new Vector3f(dd).mul(0.6f)), side,
                            0.45f, 0.45f, 0xFF3A2228, light, overlay);
                }
                for (int k = 1; k < n - 1; k++)
                    Mesh3D.bar(pose, vc, pts.get(k), pts.get(k + 1), side, 0.22f, st.t0() * 0.7f + 0.12f, st.accent(), glow, overlay);
            }
            default -> {}
        }
    }
}

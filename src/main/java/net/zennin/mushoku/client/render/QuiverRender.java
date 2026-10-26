package net.zennin.mushoku.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Carcaj 3D liso (tubo de cuero con boca reforzada, correas y hebilla) con las flechas que tiene adentro asomando
 * por la boca: plumas arriba, como se guardan. Marco: tubo a lo largo de +Y, de 0 a 13 px, centrado en X/Z.
 */
public final class QuiverRender {
    public static final float H = 13f, R = 2.7f;

    private QuiverRender() {}

    public static void draw(PoseStack pose, MultiBufferSource buffers, ItemStack quiver, int light, int overlay) {
        VertexConsumer vc = Mesh3D.solid(buffers);
        int leather = 0xFF7A4E2D, dark = 0xFF4E321E, rim = 0xFF9C6B42, strap = 0xFF3E2A1C;
        Mesh3D.prism(pose, vc, 0, 0, R, 1.0f, H - 1.2f, 14, leather, light, overlay, false);
        Mesh3D.prism(pose, vc, 0, 0, R + 0.25f, -0.2f, 1.2f, 14, dark, light, overlay, true);
        Mesh3D.prism(pose, vc, 0, 0, R + 0.35f, H - 1.4f, H, 14, rim, light, overlay, false);
        Mesh3D.prism(pose, vc, 0, 0, R - 0.1f, H - 0.6f, H - 0.4f, 14, 0xFF241810, light, overlay, true);
        for (float y : new float[]{4.0f, 8.2f}) Mesh3D.prism(pose, vc, 0, 0, R + 0.18f, y, y + 1.0f, 14, strap, light, overlay, false);
        // costura vertical y hebilla
        Mesh3D.box(pose, vc, -0.25f, 1.3f, R - 0.05f, 0.25f, H - 1.5f, R + 0.12f, Mesh3D.shade(leather, 0.75f), light, overlay);
        Mesh3D.box(pose, vc, -0.7f, 3.8f, R + 0.1f, 0.7f, 5.2f, R + 0.5f, 0xFFC9B37A, light, overlay);
        Mesh3D.box(pose, vc, -0.4f, 4.1f, R + 0.3f, 0.4f, 4.9f, R + 0.55f, strap, light, overlay);
        // flechas adentro
        List<ItemStack> arrows = new ArrayList<>();
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        int total = 0;
        if (c != null) for (ItemStack s : c.items()) { total += s.getCount(); arrows.add(s); }
        if (total <= 0) return;
        int shown = Math.min(9, (int) Math.ceil(total / 7.0));
        for (int i = 0; i < shown; i++) {
            ItemStack kind = pick(arrows, i, shown, total);
            int[] col = ArrowRender.colors(kind);
            double a = i * 2.39996; // angulo de oro: se reparten sin alinearse
            float r = (float) (0.35 + 1.25 * Math.sqrt((i + 0.5) / shown));
            float x = (float) (Math.cos(a) * r), z = (float) (Math.sin(a) * r);
            float out = 2.6f + ((i * 7) % 5) * 0.55f;
            Vector3f nock = new Vector3f(x * 1.25f, H + out, z * 1.25f);
            Vector3f dir = new Vector3f(-x * 0.06f, -1, -z * 0.06f).normalize();
            ArrowRender.draw(pose, vc, nock, dir, new Vector3f((float) Math.cos(a), 0, (float) Math.sin(a)), 16f, col[0], col[1], light, overlay);
        }
    }

    /** Que flecha mostrar en el lugar i (proporcional a cuantas hay de cada tipo). */
    private static ItemStack pick(List<ItemStack> arrows, int i, int shown, int total) {
        int target = (int) ((i + 0.5) * total / shown), acc = 0;
        for (ItemStack s : arrows) {
            acc += s.getCount();
            if (target < acc) return s;
        }
        return arrows.get(arrows.size() - 1);
    }
}

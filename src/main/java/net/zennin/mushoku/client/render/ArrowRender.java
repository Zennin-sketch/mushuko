package net.zennin.mushoku.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.joml.Vector3f;

/** Flecha 3D lisa con el estilo de las vanilla (asta de madera, punta de pedernal, tres plumas). */
public final class ArrowRender {
    private ArrowRender() {}

    /** Colores de una flecha segun su tipo: {plumas, punta}. */
    public static int[] colors(ItemStack arrow) {
        if (arrow.is(Items.SPECTRAL_ARROW)) return new int[]{0xFFF4D35E, 0xFFE8B23A};
        if (arrow.is(Items.TIPPED_ARROW)) {
            PotionContents pc = arrow.get(DataComponents.POTION_CONTENTS);
            int c = pc == null ? 0xFFB0B0B0 : 0xFF000000 | pc.getColor();
            return new int[]{Mesh3D.mix(0xFFF2F2F2, c, 0.75f), Mesh3D.mix(0xFF8E8E8E, c, 0.6f)};
        }
        return new int[]{0xFFF2F2F2, 0xFF8E8E8E};
    }

    /**
     * Flecha desde 'nock' (culatin) hacia 'dir' (unitario), en pixeles. 'up' orienta las plumas.
     */
    public static void draw(PoseStack pose, VertexConsumer vc, Vector3f nock, Vector3f dir, Vector3f up, float len,
                            int vane, int head, int light, int overlay) {
        Vector3f d = new Vector3f(dir).normalize();
        Vector3f side = new Vector3f(up).cross(d);
        if (side.lengthSquared() < 1e-5f) side.set(1, 0, 0);
        side.normalize();
        Vector3f u = new Vector3f(d).cross(side).normalize();
        // asta
        Mesh3D.bar(pose, vc, at(nock, d, 0.35f), at(nock, d, len - 2.4f), side, 0.42f, 0.42f, 0xFF9A7448, light, overlay);
        // culatin
        Mesh3D.bar(pose, vc, at(nock, d, 0f), at(nock, d, 0.45f), side, 0.6f, 0.6f, 0xFF4A3524, light, overlay);
        // punta: tres escalones que se afinan
        float[] w = {1.25f, 0.85f, 0.42f};
        for (int i = 0; i < 3; i++)
            Mesh3D.bar(pose, vc, at(nock, d, len - 2.6f + i * 0.85f), at(nock, d, len - 2.6f + (i + 1) * 0.85f), side, w[i], 0.32f,
                    Mesh3D.shade(head, 1.0f + 0.08f * i), light, overlay);
        // plumas a 120 grados
        for (int k = 0; k < 3; k++) {
            double a = Math.toRadians(90 + k * 120);
            Vector3f radial = new Vector3f(u).mul((float) Math.cos(a)).add(new Vector3f(side).mul((float) Math.sin(a)));
            Vector3f off = new Vector3f(radial).mul(0.62f);
            Vector3f a0 = at(nock, d, 0.7f).add(off), a1 = at(nock, d, 3.9f).add(off);
            Mesh3D.bar(pose, vc, a0, a1, radial, 1.05f, 0.12f, Mesh3D.shade(vane, k == 0 ? 0.92f : 1f), light, overlay);
        }
    }

    private static Vector3f at(Vector3f p, Vector3f d, float t) {
        return new Vector3f(d).mul(t).add(p);
    }
}

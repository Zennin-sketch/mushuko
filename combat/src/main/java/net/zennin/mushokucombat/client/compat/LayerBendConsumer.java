package net.zennin.mushokucombat.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.IBendable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Tuple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

/**
 * Consumidor de vertices que dobla la capa 3D igual que la parte del cuerpo (bendy-lib): los vertices llegan ya
 * transformados por la pose; se pasan al marco de la parte, se doblan y se vuelven a transformar. La mitad del lado
 * de la "base" gira entera alrededor del eje del doblez y cerca del medio el giro pasa de 0 al total (sin saltos).
 */
public final class LayerBendConsumer implements VertexConsumer {
    private static final boolean ACTIVE = FabricLoader.getInstance().isModLoaded("playeranimator")
            && !Boolean.getBoolean("mushoku.nolayercompat");
    /** Media franja (en bloques) donde el giro pasa de 0 al total. */
    private static final float BAND = 1.5f / 16f;

    private final VertexConsumer out;
    private final Matrix4f pose, inv;
    private final Matrix3f normal, invNormal;
    private final float cx, cy, cz, value;
    private final Vector3f axis;
    private final IBendable.Plane base, other;
    private final Matrix3f lastRot = new Matrix3f();

    private LayerBendConsumer(VertexConsumer out, PoseStack.Pose p, BendableCuboid b) {
        this.out = out;
        this.pose = new Matrix4f(p.pose());
        this.inv = new Matrix4f(p.pose()).invert();
        this.normal = new Matrix3f(p.normal());
        this.invNormal = new Matrix3f(p.normal()).invert();
        cx = b.getBendX() / 16f;
        cy = b.getBendY() / 16f;
        cz = b.getBendZ() / 16f;
        base = b.getBasePlane().scaled(1 / 16f);
        other = b.getOtherSidePlane().scaled(1 / 16f);
        value = b.getBend();
        axis = new Vector3f((float) Math.cos(b.getBendAxis()), 0, (float) Math.sin(b.getBendAxis()))
                .mul(new Matrix3f().set(b.getBendDirection().getRotation()));
    }

    /** Devuelve el consumidor que dobla, o null si esa parte no esta doblada. */
    public static VertexConsumer wrap(ModelPart part, PoseStack.Pose pose, VertexConsumer vc) {
        if (!ACTIVE || part == null) return null;
        try {
            Optional<MutableCuboid> c = ModelPartAccessor.optionalGetCuboid(part, 0);
            if (c.isEmpty()) return null;
            Tuple<String, ?> m = c.get().getActiveMutator();
            if (m == null || !(m.getB() instanceof BendableCuboid b) || Math.abs(b.getBend()) < 1.0e-4f) return null;
            return new LayerBendConsumer(vc, pose, b);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // al marco de la parte (bloques)
        Vector4f v = inv.transform(new Vector4f(x, y, z, 1f));
        Vector3f p = new Vector3f(v.x, v.y, v.z);
        float mid = (Math.abs(other.distanceTo(p)) - Math.abs(base.distanceTo(p))) / 2f;
        float t = Math.max(0f, Math.min(1f, (mid + BAND) / (2 * BAND)));
        float w = t * t * (3 - 2 * t);
        if (w > 0f) {
            float ang = value * w;
            Matrix4f r = new Matrix4f().translate(cx, cy, cz).rotate(ang, axis).translate(-cx, -cy, -cz);
            v = r.transform(new Vector4f(p, 1f));
            lastRot.identity().rotate(ang, axis);
        } else {
            v = new Vector4f(p, 1f);
            lastRot.identity();
        }
        Vector4f o = pose.transform(v);
        out.addVertex(o.x, o.y, o.z);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        Vector3f n = invNormal.transform(new Vector3f(x, y, z));
        lastRot.transform(n);
        normal.transform(n);
        out.setNormal(n.x, n.y, n.z);
        return this;
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a) { out.setColor(r, g, b, a); return this; }
    @Override public VertexConsumer setUv(float u, float v) { out.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { out.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { out.setUv2(u, v); return this; }
}

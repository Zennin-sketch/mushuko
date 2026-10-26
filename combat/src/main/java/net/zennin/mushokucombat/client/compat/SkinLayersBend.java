package net.zennin.mushokucombat.client.compat;

import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.IUpperPartHelper;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI;
import dev.tr7zw.skinlayers.api.MeshTransformer;
import dev.tr7zw.skinlayers.api.SkinLayersAPI;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.IBendable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

/** Lo que usa clases de 3D Skin Layers, Player Animator y bendy-lib: solo se carga si estan los tres. */
final class SkinLayersBend {
    private SkinLayersBend() {}

    private static final boolean DEBUG = Boolean.getBoolean("mushoku.debuglayers");
    private static int calls;

    static void install() {
        // 1) Cada malla 3D se dobla igual que la parte del cuerpo que cubre (mismo calculo que bendy-lib).
        SkinLayersAPI.setupMeshTransformerProvider(part -> {
            BendableCuboid b = activeBend(part);
            if (DEBUG && (++calls % 97) == 0) {
                Optional<MutableCuboid> c = part == null ? Optional.empty() : ModelPartAccessor.optionalGetCuboid(part, 0);
                org.slf4j.LoggerFactory.getLogger("mushoku_combat").info("[capas] parte={} cubo={} mutador={} doblez={}",
                        part == null ? "null" : Integer.toHexString(System.identityHashCode(part)),
                        c.map(x -> x.getClass().getSimpleName()).orElse("-"),
                        c.map(x -> String.valueOf(x.getActiveMutator() == null ? null : x.getActiveMutator().getA() + "/" + x.getActiveMutator().getB().getClass().getSimpleName())).orElse("-"),
                        b == null ? "-" : b.getBend());
            }
            if (b == null || Math.abs(b.getBend()) < 1.0e-4f) return Noop.INSTANCE;
            return new Bender(b);
        });
        // 2) Brazos (y mangas) acompanian la inclinacion del torso, como hace Player Animator con el modelo normal.
        LayerFeatureTransformerAPI.setLayerTransformer((player, pose, part) -> {
            if (!(player instanceof IAnimatedPlayer ap)) return;
            AnimationApplier anim = ap.playerAnimator_getAnimation();
            if (anim == null || !anim.isActive()) return;
            if (!((IUpperPartHelper) (Object) part).isUpperPart()) return;
            IBendHelper.rotateMatrixStack(pose, anim.getBend("body"));
        });
    }

    private static BendableCuboid activeBend(ModelPart part) {
        if (part == null) return null;
        Optional<MutableCuboid> c = ModelPartAccessor.optionalGetCuboid(part, 0);
        if (c.isEmpty()) return null;
        Tuple<String, ?> m = c.get().getActiveMutator();
        return m != null && m.getB() instanceof BendableCuboid b ? b : null;
    }

    private enum Noop implements MeshTransformer {
        INSTANCE;

        @Override public void transform(Vector3f normal, Vector4f[] vertices) {}

        @Override public void transform(ModelPart.Cube cube) {}
    }

    /** Aplica el doblez del cubo de bendy-lib a los vertices de la malla (que esta en bloques, no en pixeles). */
    private static final class Bender implements MeshTransformer, IBendable {
        private final Direction dir;
        private final float bx, by, bz, height, axis, value;
        private final Plane base, other;
        /** Media franja (en bloques) donde el giro pasa de 0 al total. */
        private static final float BAND = 1.5f / 16f;
        private final Vector3f axisVec;

        Bender(BendableCuboid b) {
            dir = b.getBendDirection();
            bx = b.getBendX() / 16f;
            by = b.getBendY() / 16f;
            bz = b.getBendZ() / 16f;
            base = b.getBasePlane().scaled(1 / 16f);
            other = b.getOtherSidePlane().scaled(1 / 16f);
            height = b.bendHeight() / 16f;
            axis = b.getBendAxis();
            value = b.getBend();
            // mismo eje que IBendable.applyBend
            axisVec = new Vector3f((float) Math.cos(axis), 0, (float) Math.sin(axis)).mul(new Matrix3f().set(dir.getRotation()));
        }

        /**
         * Doblez "suave" para vertices sueltos (los voxeles de la capa 3D): la mitad del lado de la base gira entera
         * alrededor del eje del doblez (igual que el cubo de bendy-lib) y cerca del medio el giro pasa de 0 al total
         * en una franja corta. La formula de bendy-lib solo sirve para los vertices de las caras del cubo: con
         * vertices intermedios deja un salto en el medio y la capa se estira en "abanicos".
         */
        @Override
        public void transform(Vector3f normal, Vector4f[] v) {
            int n = Math.min(4, v.length);
            for (int i = 0; i < n; i++) {
                Vector3f p = new Vector3f(v[i].x, v[i].y, v[i].z);
                float mid = (Math.abs(other.distanceTo(p)) - Math.abs(base.distanceTo(p))) / 2f;
                float t = Math.max(0f, Math.min(1f, (mid + BAND) / (2 * BAND)));
                float w = t * t * (3 - 2 * t);
                if (w <= 0f) continue;
                Vector4f q = new Vector4f(p, 1f).mul(new Matrix4f().translate(bx, by, bz).rotate(value * w, axisVec)
                        .translate(-bx, -by, -bz));
                v[i].set(q.x, q.y, q.z, 1f);
            }
            if (n >= 4) {
                Vector3f a = new Vector3f(v[0].x - v[2].x, v[0].y - v[2].y, v[0].z - v[2].z);
                Vector3f c = new Vector3f(v[1].x - v[3].x, v[1].y - v[3].y, v[1].z - v[3].z);
                Vector3f nn = a.cross(c);
                if (nn.lengthSquared() > 1.0e-12f) {
                    nn.normalize();
                    if (nn.dot(normal) < 0) nn.negate();
                    normal.set(nn);
                }
            }
        }

        @Override public void transform(ModelPart.Cube cube) {}

        @Override public Direction getBendDirection() { return dir; }
        @Override public float getBendX() { return bx; }
        @Override public float getBendY() { return by; }
        @Override public float getBendZ() { return bz; }
        @Override public Plane getBasePlane() { return base; }
        @Override public Plane getOtherSidePlane() { return other; }
        @Override public float bendHeight() { return height; }
    }
}

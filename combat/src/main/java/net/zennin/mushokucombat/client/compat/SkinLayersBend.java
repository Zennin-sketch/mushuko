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
import io.github.kosmx.bendylib.impl.RememberingPos;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

/** Lo que usa clases de 3D Skin Layers, Player Animator y bendy-lib: solo se carga si estan los tres. */
final class SkinLayersBend {
    private SkinLayersBend() {}

    static void install() {
        // 1) Cada malla 3D se dobla igual que la parte del cuerpo que cubre (mismo calculo que bendy-lib).
        SkinLayersAPI.setupMeshTransformerProvider(part -> {
            BendableCuboid b = activeBend(part);
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
        private final RememberingPos[] pos = new RememberingPos[4];

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
        }

        @Override
        public void transform(Vector3f normal, Vector4f[] v) {
            for (int i = 0; i < v.length && i < 4; i++) pos[i] = new RememberingPos(v[i].x, v[i].y, v[i].z);
            applyBend(axis, value, consumer -> { for (int i = 0; i < v.length && i < 4; i++) consumer.accept(pos[i]); });
            for (int i = 0; i < v.length && i < 4; i++) {
                Vector3f p = pos[i].getPos();
                if (p != null) v[i].set(p.x, p.y, p.z, 1f);
            }
            if (v.length >= 4) {
                Vector3f a = new Vector3f(v[0].x - v[2].x, v[0].y - v[2].y, v[0].z - v[2].z);
                Vector3f c = new Vector3f(v[1].x - v[3].x, v[1].y - v[3].y, v[1].z - v[3].z);
                Vector3f n = a.cross(c);
                if (n.lengthSquared() > 1.0e-12f) {
                    n.normalize();
                    if (n.dot(normal) < 0) n.negate();
                    normal.set(n);
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

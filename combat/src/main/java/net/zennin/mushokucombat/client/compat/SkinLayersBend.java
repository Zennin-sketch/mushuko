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
    /** Parte que se esta dibujando: 1 brazo, 2 pierna, 3 torso. */
    static int kind;
    /** Signo y mitad (0 = la del lado de la base, 1 = la otra) por parte; se pueden probar con -Dmushoku.bend.arm=1,0 etc. */
    private static final int[][] MODE = {{1, 0}, mode("arm", "1,0"), mode("leg", "1,0"), mode("body", "1,0")};

    private static int[] mode(String k, String def) {
        String[] v = System.getProperty("mushoku.bend." + k, def).split(",");
        return new int[]{Integer.parseInt(v[0].trim()), Integer.parseInt(v[1].trim())};
    }
    private static int calls, vcalls, bcalls, dcalls;

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
            // que parte se va a dibujar (brazo, pierna o torso): cada una dobla con su propio signo/mitad
            kind = 0;
            if (net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)
                    instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                var m = pr.getModel();
                kind = part == m.rightLeg || part == m.leftLeg ? 2 : part == m.body ? 3 : 1;
            }
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
        private final boolean otherHalf;

        Bender(BendableCuboid b) {
            dir = b.getBendDirection();
            bx = b.getBendX() / 16f;
            by = b.getBendY() / 16f;
            bz = b.getBendZ() / 16f;
            base = b.getBasePlane().scaled(1 / 16f);
            other = b.getOtherSidePlane().scaled(1 / 16f);
            height = b.bendHeight() / 16f;
            axis = b.getBendAxis();
            int[] md = MODE[Math.max(0, Math.min(3, kind))];
            value = b.getBend() * md[0];
            otherHalf = md[1] == 1;
            if (DEBUG && (++bcalls % 53) == 0) org.slf4j.LoggerFactory.getLogger("mushoku_combat").info(
                    "[capas] bender parte={} centro=({},{},{}) base(y0)={} base(y12)={} otro(y0)={} otro(y12)={} alto={} doblez={} ejeAng={}",
                    kind, bx, by, bz, base.distanceTo(new Vector3f(0, 0, 0)), base.distanceTo(new Vector3f(0, 0.75f, 0)),
                    other.distanceTo(new Vector3f(0, 0, 0)), other.distanceTo(new Vector3f(0, 0.75f, 0)), height, value, axis);
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
            boolean dbg = DEBUG && (++vcalls % 4001) == 0;
            float ox = v[0].x, oy = v[0].y, oz = v[0].z;
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
            if (dbg) org.slf4j.LoggerFactory.getLogger("mushoku_combat").info(
                    "[capas] v0 ({},{},{}) -> ({},{},{}) centro=({},{},{}) eje={} doblez={} base={} otro={}", ox, oy, oz, v[0].x, v[0].y, v[0].z,
                    bx, by, bz, axisVec, value, base.distanceTo(new Vector3f(ox, oy, oz)), other.distanceTo(new Vector3f(ox, oy, oz)));
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

        /**
         * La capa 3D de esta version arma cada voxel como un cubo comun (no pasa por transform(normal, vertices)):
         * a cada cubo se le pone un "mutador" de bendy-lib que lo dibuja doblado con este mismo calculo.
         */
        @Override
        public void transform(ModelPart.Cube cube) {
            MutableCuboid mc = (MutableCuboid) (Object) cube;
            if (!mc.hasMutator(VoxelCuboid.NAME)) mc.registerMutator(VoxelCuboid.NAME, VoxelCuboid::new);
            if (mc.getAndActivateMutator(VoxelCuboid.NAME) instanceof VoxelCuboid v) v.bender = this;
        }

        /** Posicion (en bloques, marco de la parte) ya doblada. El signo va al reves que en bendy-lib: comprobado en la
         *  prueba visual (con +doblez la pierna de la capa se iba para atras). */
        Vector3f bend(Vector3f p) {
            float mid = (Math.abs(other.distanceTo(p)) - Math.abs(base.distanceTo(p))) / 2f;
            if (otherHalf) mid = -mid;
            float t = Math.max(0f, Math.min(1f, (mid + BAND) / (2 * BAND)));
            float w = t * t * (3 - 2 * t);
            if (w <= 0f) return p;
            Vector4f q = new Vector4f(p, 1f).mul(new Matrix4f().translate(bx, by, bz).rotate(value * w, axisVec).translate(-bx, -by, -bz));
            return new Vector3f(q.x, q.y, q.z);
        }

        @Override public Direction getBendDirection() { return dir; }
        @Override public float getBendX() { return bx; }
        @Override public float getBendY() { return by; }
        @Override public float getBendZ() { return bz; }
        @Override public Plane getBasePlane() { return base; }
        @Override public Plane getOtherSidePlane() { return other; }
        @Override public float bendHeight() { return height; }
    }

    /** Un voxel de la capa 3D dibujado doblado (mismas caras y UV que ModelPart.Cube). */
    private static final class VoxelCuboid implements io.github.kosmx.bendylib.impl.ICuboid {
        static final String NAME = "mushoku_layer_bend";
        private final float[][] quads;   // por cara: 4 vertices (x, y, z, u, v)
        private final float[][] normals;
        Bender bender;

        VoxelCuboid(io.github.kosmx.bendylib.ICuboidBuilder.Data d) {
            if (DEBUG && (++dcalls % 211) == 0) org.slf4j.LoggerFactory.getLogger("mushoku_combat").info(
                    "[capas] voxel pos=({},{},{}) tam=({},{},{}) extra=({},{},{}) uv=({},{}) tex={}x{}", d.x, d.y, d.z, d.sizeX, d.sizeY, d.sizeZ,
                    d.extraX, d.extraY, d.extraZ, d.u, d.v, d.textureWidth, d.textureHeight);
            float x0 = d.x - d.extraX, y0 = d.y - d.extraY, z0 = d.z - d.extraZ;
            float x1 = d.x + d.sizeX + d.extraX, y1 = d.y + d.sizeY + d.extraY, z1 = d.z + d.sizeZ + d.extraZ;
            if (d.mirror) { float t = x0; x0 = x1; x1 = t; }
            float[] v1 = {x0, y0, z0}, v2 = {x1, y0, z0}, v3 = {x1, y1, z0}, v4 = {x0, y1, z0};
            float[] v5 = {x0, y0, z1}, v6 = {x1, y0, z1}, v7 = {x1, y1, z1}, v8 = {x0, y1, z1};
            float j = d.u, k = d.u + d.sizeZ, l = d.u + d.sizeZ + d.sizeX, m = d.u + d.sizeZ + d.sizeX + d.sizeX;
            float n = d.u + d.sizeZ + d.sizeX + d.sizeZ, o = d.u + d.sizeZ + d.sizeX + d.sizeZ + d.sizeX;
            float pp = d.v, q = d.v + d.sizeZ, r = d.v + d.sizeZ + d.sizeY;
            float tw = d.textureWidth, th = d.textureHeight;
            float[][][] faces = {
                    {v6, v5, v1, v2}, {v3, v4, v8, v7}, {v1, v5, v8, v4}, {v2, v1, v4, v3}, {v6, v2, v3, v7}, {v5, v6, v7, v8}};
            float[][] uv = {{k, pp, l, q}, {l, q, m, pp}, {j, q, k, r}, {k, q, l, r}, {l, q, n, r}, {n, q, o, r}};
            float[][] nn = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};
            quads = new float[6][];
            normals = new float[6][];
            for (int f = 0; f < 6; f++) {
                float u1 = uv[f][0], va = uv[f][1], u2 = uv[f][2], vb = uv[f][3];
                float[][] vs = faces[f];
                float[][] tuv = {{u2, va}, {u1, va}, {u1, vb}, {u2, vb}};
                float[] qd = new float[20];
                for (int i = 0; i < 4; i++) {
                    qd[i * 5] = vs[i][0] / 16f;
                    qd[i * 5 + 1] = vs[i][1] / 16f;
                    qd[i * 5 + 2] = vs[i][2] / 16f;
                    qd[i * 5 + 3] = tuv[i][0] / tw;
                    qd[i * 5 + 4] = tuv[i][1] / th;
                }
                quads[f] = qd;
                normals[f] = d.mirror && nn[f][0] != 0 ? new float[]{-nn[f][0], 0, 0} : nn[f];
            }
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay, int color) {
            Matrix4f m4 = pose.pose();
            Matrix3f m3 = pose.normal();
            for (int f = 0; f < 6; f++) {
                float[] qd = quads[f];
                Vector3f[] ps = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    Vector3f p = new Vector3f(qd[i * 5], qd[i * 5 + 1], qd[i * 5 + 2]);
                    ps[i] = bender != null ? bender.bend(p) : p;
                }
                // normal de la cara ya doblada
                Vector3f nrm = new Vector3f(ps[2]).sub(ps[0]).cross(new Vector3f(ps[3]).sub(ps[1]));
                Vector3f ref = new Vector3f(normals[f][0], normals[f][1], normals[f][2]);
                if (nrm.lengthSquared() < 1.0e-14f) nrm.set(ref);
                else { nrm.normalize(); if (nrm.dot(ref) < 0 && bender == null) nrm.negate(); }
                if (bender != null) {
                    // con el giro, la normal original girada es mas confiable que la del cuadrilatero para el signo
                    Vector3f c = new Vector3f(ps[0]).add(ps[1]).add(ps[2]).add(ps[3]).mul(0.25f);
                    Vector3f rc = bender.bend(new Vector3f((qd[0] + qd[5] + qd[10] + qd[15]) / 4f + ref.x * 0.01f,
                            (qd[1] + qd[6] + qd[11] + qd[16]) / 4f + ref.y * 0.01f, (qd[2] + qd[7] + qd[12] + qd[17]) / 4f + ref.z * 0.01f));
                    if (nrm.dot(new Vector3f(rc).sub(c)) < 0) nrm.negate();
                }
                m3.transform(nrm);
                for (int i = 0; i < 4; i++) {
                    Vector4f w = m4.transform(new Vector4f(ps[i], 1f));
                    vc.addVertex(w.x, w.y, w.z, color, qd[i * 5 + 3], qd[i * 5 + 4], overlay, light, nrm.x, nrm.y, nrm.z);
                }
            }
        }

        @Override
        public void copyState(io.github.kosmx.bendylib.impl.ICuboid other) {
            if (other instanceof VoxelCuboid v) bender = v.bender;
        }
    }
}

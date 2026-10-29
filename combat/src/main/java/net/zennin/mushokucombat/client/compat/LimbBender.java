package net.zennin.mushokucombat.client.compat;

import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.IBendable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.zennin.mushoku.client.LimbBend;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Doblez de codos y rodillas para la armadura y la ropa del mod principal: la misma cuenta que bendy-lib le hace a la
 * pieza del jugador (y que LayerBendConsumer le hace a la capa 3D), asi la armadura se dobla igual que la piel.
 */
public final class LimbBender implements LimbBend.Bender {
    private LimbBender() {}

    public static void install() {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("playeranimator")) LimbBend.BENDER = new LimbBender();
    }

    @Override
    public boolean bend(Entity e, String part, Vector3f q) {
        if (!(e instanceof AbstractClientPlayer p)) return false;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return false;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        ModelPart mp = switch (part) {
            case "rightLeg" -> m.rightLeg;
            case "leftLeg" -> m.leftLeg;
            case "rightArm" -> m.rightArm;
            case "leftArm" -> m.leftArm;
            default -> null;
        };
        if (mp == null) return false;
        try {
            Optional<MutableCuboid> c = ModelPartAccessor.optionalGetCuboid(mp, 0);
            if (c.isEmpty()) return false;
            Tuple<String, ?> mut = c.get().getActiveMutator();
            if (mut == null || !(mut.getB() instanceof BendableCuboid b)) return false;
            float value = b.getBend();
            if (Math.abs(value) < 1e-4f) return false;
            apply(b, value, q);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** bendy-lib: estira cada mitad hasta el plano del inglete y gira la mitad de la base alrededor del centro. */
    private static void apply(BendableCuboid b, float value, Vector3f q) {
        Vector3f axis = new Vector3f((float) Math.cos(b.getBendAxis()), 0, (float) Math.sin(b.getBendAxis()))
                .mul(new Matrix3f().set(b.getBendDirection().getRotation()));
        Vector3f dir = b.getBendDirection().step();
        float cx = b.getBendX(), cy = b.getBendY(), cz = b.getBendZ();
        IBendable.Plane base = b.getBasePlane(), other = b.getOtherSidePlane();
        IBendable.Plane bendPlane = new IBendable.Plane(new Vector3f(dir).cross(axis), new Vector3f(cx, cy, cz));
        float half = b.bendHeight() / 2f;
        float dB = b.isBendInverted() ? -bendPlane.distanceTo(q) : bendPlane.distanceTo(q);
        float dBase = base.distanceTo(q), dOther = other.distanceTo(q);
        double sh = Math.tan(value / 2) * dB;
        boolean rot = Math.abs(dBase) < Math.abs(dOther);
        q.add(new Vector3f(dir).mul((float) ((rot ? -dBase : -dOther) / half * sh)));
        if (rot) {
            org.joml.Vector4f v = new org.joml.Vector4f(q, 1f);
            new Matrix4f().translate(cx, cy, cz).rotate(value, axis).translate(-cx, -cy, -cz).transform(v);
            q.set(v.x, v.y, v.z);
        }
    }
}

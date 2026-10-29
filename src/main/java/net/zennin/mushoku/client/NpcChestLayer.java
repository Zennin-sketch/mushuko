package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;

/**
 * Pecho femenino con fisicas para algunos NPC (como Wildfire's Female Gender Mod, pero fijo por personaje): dos
 * volumenes sobre el pecho que toman el color de la skin y rebotan con un resorte segun como se mueve el NPC
 * (pasos, saltos, caidas, giros).
 */
public class NpcChestLayer<T extends Mob> extends RenderLayer<T, PlayerModel<T>> {
    /** Tamano por personaje (textura de la skin): cuanto sobresale y cuanto rebota. */
    public record Shape(float size, float bounce) {}

    public static final Map<String, Shape> SHAPES = Map.of(
            "roxy_migurdia", new Shape(0.55f, 0.6f),
            "sylphiette_greyrat", new Shape(0.8f, 0.8f),
            "eris_greyrat", new Shape(1.0f, 1.0f),
            "ghislaine", new Shape(1.35f, 1.15f));

    private final ResourceLocation texture;
    private final Shape shape;
    private final ModelPart right, left, rightOver, leftOver;
    private static final Map<Integer, float[]> springs = new HashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Shape> BY_TYPE = new HashMap<>();

    /** Lo llama MushokuClient al registrar cada companero: asi la ropa del mod (ArmorV2) sabe a quien darle pecho. */
    public static void registerType(net.minecraft.world.entity.EntityType<?> type, String texture) {
        Shape sh = SHAPES.get(texture);
        if (sh != null) BY_TYPE.put(type, sh);
    }

    /** Forma del pecho de esta entidad (null si no tiene: jugadores y demas NPC). */
    public static Shape shapeOf(net.minecraft.world.entity.Entity e) {
        return e == null ? null : BY_TYPE.get(e.getType());
    }

    public NpcChestLayer(RenderLayerParent<T, PlayerModel<T>> parent, ResourceLocation texture, Shape shape) {
        super(parent);
        this.texture = texture;
        this.shape = shape;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // la cara de adelante de cada lado toma el pecho de la skin (torso: u 20..28, v 20..32; chaqueta: v 36..48)
        root.addOrReplaceChild("r", CubeListBuilder.create().texOffs(18, 20).addBox(-4, 0, -2, 4, 4, 2), PartPose.ZERO);
        root.addOrReplaceChild("l", CubeListBuilder.create().texOffs(22, 20).addBox(0, 0, -2, 4, 4, 2), PartPose.ZERO);
        root.addOrReplaceChild("ro", CubeListBuilder.create().texOffs(18, 36).addBox(-4, 0, -2, 4, 4, 2, new CubeDeformation(0.25f)), PartPose.ZERO);
        root.addOrReplaceChild("lo", CubeListBuilder.create().texOffs(22, 36).addBox(0, 0, -2, 4, 4, 2, new CubeDeformation(0.25f)), PartPose.ZERO);
        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        right = baked.getChild("r");
        left = baked.getChild("l");
        rightOver = baked.getChild("ro");
        leftOver = baked.getChild("lo");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbAmount, float partial,
                       float age, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        float[] sp = spring(e, shape, partial);
        PlayerModel<T> m = getParentModel();
        pose.pushPose();
        m.body.translateAndRotate(pose);
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0f);
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            // arriba del pecho (2 px bajo el cuello, cara de adelante del torso): ahi cuelga y rebota
            float px = side == 0 ? -2f : 2f;
            pose.translate(px / 16f, 2f / 16f, -2f / 16f);
            float s = shape.size();
            // rebote: sube/baja, se inclina y se mueve un poco al costado (cada lado algo distinto)
            float lag = side == 0 ? 1f : 0.92f;
            pose.translate(sp[2] * 0.02f * lag, sp[0] * 0.06f * lag, 0);
            pose.mulPose(Axis.XP.rotation(-0.32f - 0.05f * s + sp[0] * 0.12f * lag));
            pose.mulPose(Axis.ZP.rotation(sp[2] * 0.06f * lag));
            pose.scale(0.92f + 0.08f * s, 0.85f + 0.15f * s, 0.25f + 0.55f * s);
            pose.translate(-px / 16f, 0, 0);
            var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            (side == 0 ? right : left).render(pose, vc, light, overlay);
            var vo = buffers.getBuffer(RenderType.entityTranslucent(texture));
            (side == 0 ? rightOver : leftOver).render(pose, vo, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * Resorte por NPC: {posicion vertical, velocidad vertical, posicion lateral, velocidad lateral, ultimo ms, ultima y,
     * ultimo giro}. Lo empujan los pasos (dos por ciclo), los saltos/caidas y los giros del cuerpo.
     */
    public static float[] spring(net.minecraft.world.entity.LivingEntity e, Shape shape, float partial) {
        float[] s = springs.computeIfAbsent(e.getId(), k -> new float[]{0, 0, 0, 0, net.minecraft.Util.getMillis(), (float) e.getY(), e.yBodyRot});
        float now = net.minecraft.Util.getMillis();
        float dt = Math.min(0.05f, Math.max(0f, (now - s[4]) / 1000f));
        s[4] = now;
        float y = (float) Mth.lerp(partial, e.yo, e.getY());
        float vy = dt > 0 ? (y - s[5]) / dt : 0;
        s[5] = y;
        float yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float dyaw = dt > 0 ? Mth.wrapDegrees(yaw - s[6]) / dt : 0;
        s[6] = yaw;
        float b = shape.bounce();
        // pasos: un golpe hacia abajo en cada pisada
        float walk = e.walkAnimation.speed(partial);
        float phase = e.walkAnimation.position(partial) * 0.6662f;
        float step = -Math.abs(Mth.sin(phase)) * walk * 9f * b;
        // fuerzas: resorte, amortiguacion, el cuerpo que sube/baja (la inercia lo deja atras) y los pasos
        float accY = -140f * s[0] - 9f * s[1] - vy * 6f * b + step;
        float accX = -110f * s[2] - 8f * s[3] - dyaw * 0.02f * b;
        int n = 4;
        for (int i = 0; i < n; i++) {
            float h = dt / n;
            s[1] += accY * h; s[0] += s[1] * h;
            s[3] += accX * h; s[2] += s[3] * h;
        }
        s[0] = Mth.clamp(s[0], -1.2f, 1.2f);
        s[2] = Mth.clamp(s[2], -1f, 1f);
        if (springs.size() > 128) springs.clear();
        return s;
    }

}

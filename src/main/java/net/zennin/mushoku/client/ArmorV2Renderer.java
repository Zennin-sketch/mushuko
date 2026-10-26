package net.zennin.mushoku.client;

import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.item.ArmorV2Item;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer de las armaduras v2: geo/armor_v2/<modelo>.geo.json y textures/armor_v2/<modelo>.png (+ brillo). */
public class ArmorV2Renderer extends GeoArmorRenderer<ArmorV2Item> {
    private final boolean fullBody;
    private final boolean cloak;

    public ArmorV2Renderer(String model, boolean glows, boolean fullBody) {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(ArmorV2Item a) { return MushokuMod.id("geo/armor_v2/" + model + ".geo.json"); }

            @Override
            public ResourceLocation getTextureResource(ArmorV2Item a) { return MushokuMod.id("textures/armor_v2/" + model + ".png"); }

            @Override
            public ResourceLocation getAnimationResource(ArmorV2Item a) { return MushokuMod.id("animations/mk2model.animation.json"); }
        });
        this.fullBody = fullBody;
        this.cloak = model.contains("cloak");
        if (glows) addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** La capa con capucha se pone en el torso pero tambien muestra la capucha (hueso de la cabeza). */
    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        if (fullBody && slot == net.minecraft.world.entity.EquipmentSlot.CHEST) setBoneVisible(this.head, true);
    }

    /**
     * Ropa que acompania a las piernas: los panos de faldas, tunicas y capas van en huesos de tela (hijos del torso)
     * que se abren hacia donde va cada pierna (adelante, atras o al costado), asi no las atraviesan al caminar.
     */
    @Override
    protected void applyBaseTransformations(net.minecraft.client.model.HumanoidModel<?> base) {
        super.applyBaseTransformations(base);
        var model = getGeoModel();
        // Giro de cada pierna respecto del torso (al agacharse el torso se inclina y las piernas quedan "atras")
        float r = base.rightLeg.xRot - base.body.xRot, l = base.leftLeg.xRot - base.body.xRot;
        float fwdR = Math.max(0, -r), fwdL = Math.max(0, -l), backR = Math.max(0, r), backL = Math.max(0, l);
        cloth(model, "clothF", Math.max(fwdR, fwdL), 1, 0);
        cloth(model, "clothFR", fwdR, 1, 0);
        cloth(model, "clothFL", fwdL, 1, 0);
        cloth(model, "clothB", Math.max(backR, backL), -1, 0);
        // Tela por zonas (ring_*_ANG): cada pano sigue a la pierna de su lado
        rings(model, -r, -l, base.rightLeg.zRot, base.leftLeg.zRot);
        // Costados: van sobre la pierna de su lado, asi que se balancean adelante y atras con ella (misma tela)
        side(model, "clothR", r, base.rightLeg.zRot);
        side(model, "clothL", l, base.leftLeg.zRot);
        // Hombros (Capa Atrapallamas): la capa sube y baja con cada hombro
        float dyR = cloak ? base.rightArm.y - base.body.y - 2f : 0, dyL = cloak ? base.leftArm.y - base.body.y - 2f : 0;
        shoulder(model, "clothR", dyR); shoulder(model, "clothFR", dyR);
        shoulder(model, "clothL", dyL); shoulder(model, "clothFL", dyL);
        shoulder(model, "clothF", (dyR + dyL) / 2); shoulder(model, "clothB", (dyR + dyL) / 2);
    }

    private final java.util.Map<String, Float> ringAngle = new java.util.HashMap<>();

    /**
     * Panos por zona alrededor de la cintura (0 adelante, 90 derecha, 180 atras): la pierna que va adelante empuja la
     * tela de adelante de su lado, la que va atras la de atras, y los costados acompanan a su pierna en los dos
     * sentidos. Cada pierna pesa mas en su lado y la mitad en el centro, asi los panos vecinos se mueven parecido y la
     * tela se ve continua. sR/sL: angulo de cada pierna (positivo = adelante).
     */
    private void rings(GeoModel<ArmorV2Item> model, float sR, float sL, float zR, float zL) {
        for (software.bernie.geckolib.cache.object.GeoBone b : model.getAnimationProcessor().getRegisteredBones()) {
            String n = b.getName();
            if (!n.startsWith("ring_")) continue;
            Float ang = ringAngle.get(n);
            if (ang == null) {
                try { ang = (float) Math.toRadians(Integer.parseInt(n.substring(n.lastIndexOf('_') + 1))); }
                catch (NumberFormatException e) { ang = 0f; }
                ringAngle.put(n, ang);
            }
            float side = (float) Math.sin(ang), front = (float) Math.cos(ang);
            float wR = Math.max(0, Math.min(1, 0.7f + 0.9f * side)), wL = Math.max(0, Math.min(1, 0.7f - 0.9f * side));
            float pushR = push(sR, front) * wR, pushL = push(sL, front) * wL;
            float fwd = Math.max(0, Math.max(pushR, pushL)), bwd = Math.min(0, Math.min(pushR, pushL));
            float d = fwd + bwd;
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float k = Math.min(Math.abs(d), 1.4f);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(k) / span)) * Math.signum(d);
            b.setRotX(a);
            b.setRotY(0);
            b.setRotZ(Math.abs(side) > 0.5f ? (side > 0 ? zR : zL) * Math.abs(side) : 0);
        }
    }

    /** Cuanto mueve una pierna (angulo s, + adelante) la tela que esta en 'front' (1 adelante, -1 atras). */
    private static float push(float s, float front) {
        if (s > 0) return s * (front > -0.35f ? 1f : 0.15f);
        return s * (front < 0.35f ? 1f : 0.15f);
    }

    /** Pano del costado: gira sobre X siguiendo el angulo de la pierna (con signo: adelante o atras). */
    private static void side(GeoModel<ArmorV2Item> model, String name, float legRel, float z) {
        model.getBone(name).ifPresent(b -> {
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float k = Math.min(Math.abs(legRel), 1.4f);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(k) / span)) * Math.signum(legRel);
            b.setRotX(-a);
            b.setRotY(0);
            b.setRotZ(z);
        });
    }

    private static void shoulder(GeoModel<ArmorV2Item> model, String name, float dy) {
        model.getBone(name).ifPresent(b -> b.setPosY(Math.abs(dy) < 1e-3f ? 0 : -dy));
    }

    /**
     * Angulo del pano para seguir a la pierna: la rodilla (6 px bajo la cadera) se corre 6*sen(a); el pano, que gira
     * desde su borde de arriba (cintura u hombros), tiene que correrse lo mismo a esa altura.
     */
    private static void cloth(GeoModel<ArmorV2Item> model, String name, float legAngle, int dir, float z) {
        model.getBone(name).ifPresent(b -> {
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(Math.min(legAngle, 1.4f)) / span)) * 1.1f;
            b.setRotX(dir * a);
            b.setRotY(0);
            b.setRotZ(z);
        });
    }

    public static Object create(String model, boolean glows, boolean fullBody) { return new ArmorV2Renderer(model, glows, fullBody); }
}

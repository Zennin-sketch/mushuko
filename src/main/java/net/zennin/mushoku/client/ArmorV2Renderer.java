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
        cloth(model, "clothR", 0, 0, base.rightLeg.zRot);
        cloth(model, "clothL", 0, 0, base.leftLeg.zRot);
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

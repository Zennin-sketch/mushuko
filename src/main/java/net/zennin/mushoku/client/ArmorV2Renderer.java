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
        if (fullBody && slot == net.minecraft.world.entity.EquipmentSlot.CHEST) setBoneVisible(getHeadBone(), true);
    }

    public static Object create(String model, boolean glows, boolean fullBody) { return new ArmorV2Renderer(model, glows, fullBody); }
}

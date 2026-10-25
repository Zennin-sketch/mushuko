package net.zennin.mushoku.client;

import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.item.Mk2ArmorItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Renderer de la armadura Mk 2 con geo/mk2model.geo.json y la textura original mk2txt2. */
public class Mk2ArmorRenderer extends GeoArmorRenderer<Mk2ArmorItem> {
    public Mk2ArmorRenderer() {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(Mk2ArmorItem a) { return MushokuMod.id("geo/mk2model.geo.json"); }

            @Override
            public ResourceLocation getTextureResource(Mk2ArmorItem a) { return MushokuMod.id("textures/item/mk2txt2.png"); }

            @Override
            public ResourceLocation getAnimationResource(Mk2ArmorItem a) { return MushokuMod.id("animations/mk2model.animation.json"); }
        });
    }

    public static Object create() { return new Mk2ArmorRenderer(); }
}

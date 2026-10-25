package net.zennin.mushoku.client;

import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.addon.SuperdSpearItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Modelo 3D de la Lanza Superd (geo/ruijerdspear). */
public class SpearRenderer extends GeoItemRenderer<SuperdSpearItem> {
    public SpearRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SuperdSpearItem a) { return MushokuMod.id("geo/ruijerdspear.geo.json"); }
            @Override public ResourceLocation getTextureResource(SuperdSpearItem a) { return MushokuMod.id("textures/item/addon_ruijerd_spear.png"); }
            @Override public ResourceLocation getAnimationResource(SuperdSpearItem a) { return MushokuMod.id("animations/ruijerdspear.animation.json"); }
        });
    }

    public static Object create() { return new SpearRenderer(); }
}

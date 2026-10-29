package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.zennin.mushoku.MushokuMod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer GeckoLib con los modelos, animaciones y texturas del original (assets/remushoku/geo, animations). */
public class AnimatedRenderer<T extends net.minecraft.world.entity.Entity & software.bernie.geckolib.animatable.GeoAnimatable> extends GeoEntityRenderer<T> {
    public AnimatedRenderer(EntityRendererProvider.Context ctx, String geo, String texture, float shadow) {
        this(ctx, geo, texture, shadow, 1f);
    }

    /** scale = tamano del modelo; si existe la textura <textura>_glowmask, esas partes brillan en la oscuridad. */
    public AnimatedRenderer(EntityRendererProvider.Context ctx, String geo, String texture, float shadow, float scale) {
        super(ctx, new Model<>(geo, texture));
        this.shadowRadius = shadow;
        if (scale != 1f) withScale(scale);
        if (ctx.getResourceManager().getResource(MushokuMod.id("textures/entities/" + texture + "_glowmask.png")).isPresent())
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
    }

    public static <T extends net.minecraft.world.entity.Entity & software.bernie.geckolib.animatable.GeoAnimatable> void register(EntityType<T> type, String geo, String texture, float shadow, float scale) {
        EntityRendererRegistry.register(type, ctx -> new AnimatedRenderer<>(ctx, geo, texture, shadow, scale));
    }

    public static <T extends net.minecraft.world.entity.Entity & software.bernie.geckolib.animatable.GeoAnimatable> void register(EntityType<T> type, String geo, String texture, float shadow) {
        EntityRendererRegistry.register(type, ctx -> new AnimatedRenderer<>(ctx, geo, texture, shadow));
    }

    private static class Model<T extends software.bernie.geckolib.animatable.GeoAnimatable> extends GeoModel<T> {
        private final ResourceLocation model, anim, tex;

        Model(String geo, String texture) {
            this.model = MushokuMod.id("geo/" + geo + ".geo.json");
            this.anim = MushokuMod.id("animations/" + geo + ".animation.json");
            this.tex = MushokuMod.id("textures/entities/" + texture + ".png");
        }

        @Override
        public ResourceLocation getModelResource(T animatable) { return model; }

        @Override
        public ResourceLocation getTextureResource(T animatable) { return tex; }

        @Override
        public ResourceLocation getAnimationResource(T animatable) { return anim; }
    }
}

package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.entity.creature.AnimatedMonster;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer GeckoLib con los modelos, animaciones y texturas del original (assets/remushoku/geo, animations). */
public class AnimatedRenderer<T extends AnimatedMonster> extends GeoEntityRenderer<T> {
    public AnimatedRenderer(EntityRendererProvider.Context ctx, String geo, String texture, float shadow) {
        super(ctx, new Model<>(geo, texture));
        this.shadowRadius = shadow;
    }

    public static <T extends AnimatedMonster> void register(EntityType<T> type, String geo, String texture, float shadow) {
        EntityRendererRegistry.register(type, ctx -> new AnimatedRenderer<>(ctx, geo, texture, shadow));
    }

    private static class Model<T extends AnimatedMonster> extends GeoModel<T> {
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

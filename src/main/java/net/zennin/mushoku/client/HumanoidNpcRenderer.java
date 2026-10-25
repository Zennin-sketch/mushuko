package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.zennin.mushoku.MushokuMod;

/** NPCs con modelo de jugador y su skin del original (igual que los renderers de MCreator). */
public class HumanoidNpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public HumanoidNpcRenderer(EntityRendererProvider.Context ctx, String texture) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.texture = MushokuMod.id("textures/entities/" + texture + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return texture; }

    public static <T extends Mob> void register(EntityType<T> type, String texture) {
        EntityRendererRegistry.register(type, ctx -> new HumanoidNpcRenderer<>(ctx, texture));
    }
}

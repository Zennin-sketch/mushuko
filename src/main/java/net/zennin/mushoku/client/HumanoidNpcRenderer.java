package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.zennin.mushoku.MushokuMod;

/**
 * NPCs con modelo de jugador completo (ancho o slim): se ve la capa exterior de la skin (pelo, ropa)
 * y, con 3D Skin Layers instalado, esa capa sale en 3D.
 */
public class HumanoidNpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    private final boolean slim;
    private boolean layersApplied = false;

    public HumanoidNpcRenderer(EntityRendererProvider.Context ctx, String texture, boolean slim) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
        this.texture = MushokuMod.id("textures/entities/" + texture + ".png");
        this.slim = slim;
        NpcChestLayer.attach(this, texture, this.texture);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!layersApplied) {
            layersApplied = true;
            SkinLayersCompat.apply(getModel(), texture, slim);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return texture; }

    public static <T extends Mob> void register(EntityType<T> type, String texture) { register(type, texture, false); }

    public static <T extends Mob> void register(EntityType<T> type, String texture, boolean slim) {
        EntityRendererRegistry.register(type, ctx -> new HumanoidNpcRenderer<>(ctx, texture, slim));
    }
}

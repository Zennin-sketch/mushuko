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
    /** Rasgo sobre la cabeza (orejas de bestia: beast_bear / beast_dog / beast_cat), o null. */
    private String trait;

    public HumanoidNpcRenderer(EntityRendererProvider.Context ctx, String texture, boolean slim) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
        this.texture = MushokuMod.id("textures/entities/" + texture + ".png");
        this.slim = slim;
        NpcChestLayer.Shape chest = NpcChestLayer.SHAPES.get(texture);
        if (chest != null) addLayer(new NpcChestLayer<>(this, this.texture, chest));
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        // brazos segun lo que tiene en la mano: arco tensado al apuntar, objeto adelante
        PlayerModel<T> m = getModel();
        var main = entity.getMainHandItem();
        m.rightArmPose = m.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        if (main.getItem() instanceof net.minecraft.world.item.BowItem) {
            if (entity.isAggressive()) m.rightArmPose = m.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else if (!main.isEmpty()) m.rightArmPose = HumanoidModel.ArmPose.ITEM;
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

    /** NPC con un rasgo en la cabeza (orejas de bestia, cuernos...): 'trait' es el modelo de assets/remushoku/cosmetics. */
    public static <T extends Mob> void register(EntityType<T> type, String texture, boolean slim, String trait) {
        EntityRendererRegistry.register(type, ctx -> {
            HumanoidNpcRenderer<T> r = new HumanoidNpcRenderer<>(ctx, texture, slim);
            r.trait = trait;
            r.addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<T, PlayerModel<T>>(r) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float ls, float la, float pt, float age, float hy, float hp) {
                    if (e.isInvisible() || r.trait == null) return;
                    float hurt = e.hurtTime > 0 ? Math.max(0f, (e.hurtTime - pt) / 10f) : 0f;
                    CosmeticLayer.renderTrait(pose, buffers, light, getParentModel().head, r.trait, 0, -1, r.texture, age, hurt);
                }
            });
            return r;
        });
    }
}

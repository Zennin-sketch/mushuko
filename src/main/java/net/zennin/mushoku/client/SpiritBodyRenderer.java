package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.entity.SpiritBodyEntity;

/** El cuerpo de la Clarividencia se dibuja con la skin (ancha o slim) de su dueno. */
public class SpiritBodyRenderer extends HumanoidMobRenderer<SpiritBodyEntity, PlayerModel<SpiritBodyEntity>> {
    private final PlayerModel<SpiritBodyEntity> wide, slim;

    public SpiritBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    private static PlayerSkin skin(SpiritBodyEntity e) {
        return e.owner().map(id -> {
            var conn = Minecraft.getInstance().getConnection();
            PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
            return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
        }).orElse(DefaultPlayerSkin.getDefaultSkin());
    }

    @Override
    public void render(SpiritBodyEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model = skin(e).model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritBodyEntity e) { return skin(e).texture(); }
}

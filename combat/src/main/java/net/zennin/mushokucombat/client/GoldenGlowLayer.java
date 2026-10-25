package net.zennin.mushokucombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.zennin.mushokucombat.MushokuCombat;

/** Aura dorada sobre el modelo del jugador (mismo truco que el brillo del creeper cargado). */
public class GoldenGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = MushokuCombat.id("textures/entity/golden_glow.png");

    public GoldenGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float s = GoldenGlow.strength(player.getId(), partialTick);
        if (s <= 0f) return;
        float t = ageInTicks * 0.03f;
        VertexConsumer vc = buffers.getBuffer(RenderType.energySwirl(TEXTURE, t % 1f, (t * 0.6f) % 1f));
        pose.pushPose();
        pose.scale(1.08f, 1.04f, 1.08f);
        pose.translate(0, -0.03, 0);
        int color = FastColor.ARGB32.color(255, (int) (255 * s), (int) (196 * s), (int) (70 * s));
        getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }
}

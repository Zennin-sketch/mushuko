package net.zennin.mushoku.client;

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
import net.minecraft.util.Mth;
import net.zennin.mushoku.race.Appearance;

/**
 * Dibuja el rasgo de raza sobre la cabeza del jugador: orejas de elfo, cuernos de demonio, orejas de bestia o la gema
 * de los Superd. Las partes "de piel" y "de pelo" toman el color de un pixel de la skin (mejilla / arriba de la cabeza).
 */
public class CosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation PLAIN = ResourceLocation.fromNamespaceAndPath("remushoku", "textures/entity/spell/cloud.png");
    // Pixeles de muestra en una skin de 64x64: mejilla (cara, fila de abajo) y parte de arriba de la cabeza
    private static final float SKIN_U = 9.5f / 64f, SKIN_V = 14.5f / 64f, HAIR_U = 12.5f / 64f, HAIR_V = 3.5f / 64f;

    public CosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        Appearance a = ClientAppearance.get(player);
        if (a == null || a.model() == null) return;
        CosmeticModel m = CosmeticModel.get(a.model());
        if (m.parts.isEmpty()) return;
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        // Marco del rasgo: +X derecha, +Y arriba, +Z adelante (el modelo del jugador tiene los tres ejes al reves)
        pose.scale(-1f / 16f, -1f / 16f, -1f / 16f);
        pose.translate(0, m.heights[Mth.clamp(a.height(), 0, m.heights.length - 1)], 0);
        PoseStack.Pose last = pose.last();
        ResourceLocation skin = player.getSkin().texture();
        VertexConsumer skinVc = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
        for (CosmeticModel.Part p : m.parts) {
            if (p.source() == CosmeticModel.HEX) continue;
            emit(skinVc, last, p, light, p.source() == CosmeticModel.SKIN ? SKIN_U : HAIR_U, p.source() == CosmeticModel.SKIN ? SKIN_V : HAIR_V);
        }
        VertexConsumer plainVc = buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN));
        for (CosmeticModel.Part p : m.parts) {
            if (p.source() != CosmeticModel.HEX) continue;
            emit(plainVc, last, p, p.glow() ? LightTexture.FULL_BRIGHT : light, -1, -1);
        }
        pose.popPose();
    }

    /** u/v >= 0: toda la cara usa ese pixel de la skin; si no, la UV propia de la caja. */
    private static void emit(VertexConsumer vc, PoseStack.Pose last, CosmeticModel.Part part, int light, float u, float v) {
        SpellModel.Face f = part.face();
        int r = Mth.clamp(Math.round(f.r() * part.mul()), 0, 255);
        int g = Mth.clamp(Math.round(f.g() * part.mul()), 0, 255);
        int b = Mth.clamp(Math.round(f.b() * part.mul()), 0, 255);
        float[] p = f.xyz(), uv = f.uv();
        for (int i = 0; i < 4; i++) {
            vc.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                    .setColor(r, g, b, 255)
                    .setUv(u >= 0 ? u : uv[i * 2], v >= 0 ? v : uv[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(last, f.nx(), f.ny(), f.nz());
        }
    }
}

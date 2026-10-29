package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.block.ShelfBlock;
import net.zennin.mushoku.block.ShelfBlockEntity;

/**
 * Muestra lo guardado en el estante: dos objetos en cada uno de los cuatro huecos (arriba/abajo, izquierda/derecha
 * del separador) y el noveno acostado arriba del estante. Los bloques se ven como bloquecitos y los items parados.
 */
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    /** Posicion (en px del bloque mirando al norte) de cada espacio: x, y del piso del hueco. */
    private static final float[][] SPOTS = {
            {2.2f, 10}, {5.0f, 10}, {11.0f, 10}, {13.8f, 10},
            {2.2f, 2}, {5.0f, 2}, {11.0f, 2}, {13.8f, 2},
            {8f, 16}};

    public ShelfRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ShelfBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        Direction facing = be.getBlockState().hasProperty(ShelfBlock.FACING) ? be.getBlockState().getValue(ShelfBlock.FACING) : Direction.NORTH;
        int lightHere = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        var items = be.shown();
        for (int i = 0; i < Math.min(items.size(), SPOTS.length); i++) {
            ItemStack st = items.get(i);
            if (st.isEmpty()) continue;
            boolean block = st.getItem() instanceof BlockItem;
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
            pose.translate(-0.5, 0, -0.5);
            float x = SPOTS[i][0] / 16f, y = SPOTS[i][1] / 16f;
            if (i == 8) {
                // acostado arriba del estante
                pose.translate(x, y + 0.01, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees(15));
                pose.scale(0.42f, 0.42f, 0.42f);
            } else if (block) {
                pose.translate(x, y + 0.11, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(i % 2 == 0 ? 8 : -6));
                pose.scale(0.26f, 0.26f, 0.26f);
            } else {
                pose.translate(x, y + 0.17, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(i % 2 == 0 ? 10 : -10));
                pose.scale(0.34f, 0.34f, 0.34f);
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, lightHere, OverlayTexture.NO_OVERLAY,
                    pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}

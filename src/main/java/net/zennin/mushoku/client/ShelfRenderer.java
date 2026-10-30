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
 * Muestra lo guardado en el estante. El estante se mira de costado (con la tabla del medio de fondo): de cada lado hay
 * dos objetos en la repisa de arriba y dos en la de abajo (8 en total). Los bloques se ven como bloquecitos y los
 * items parados mirando hacia afuera.
 */
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    /** x, y del piso de la repisa, z (px del modelo mirando al norte) y hacia que lado mira (-1 = oeste, 1 = este). */
    private static final float[][] SPOTS = {
            {3.6f, 10, 4.2f, -1}, {3.6f, 10, 11.8f, -1}, {3.6f, 2, 4.2f, -1}, {3.6f, 2, 11.8f, -1},
            {12.4f, 10, 4.2f, 1}, {12.4f, 10, 11.8f, 1}, {12.4f, 2, 4.2f, 1}, {12.4f, 2, 11.8f, 1}};

    public ShelfRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ShelfBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        Direction facing = be.getBlockState().hasProperty(ShelfBlock.FACING) ? be.getBlockState().getValue(ShelfBlock.FACING) : Direction.NORTH;
        int lightHere = Math.max(light, LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above()));
        var items = be.shown();
        for (int i = 0; i < Math.min(items.size(), SPOTS.length); i++) {
            ItemStack st = items.get(i);
            if (st.isEmpty()) continue;
            boolean block = st.getItem() instanceof BlockItem;
            float[] sp = SPOTS[i];
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
            pose.translate(-0.5, 0, -0.5);
            pose.translate(sp[0] / 16f, sp[1] / 16f, sp[2] / 16f);
            pose.mulPose(Axis.YP.rotationDegrees(sp[3] * 90 + (i % 2 == 0 ? 8 : -8)));
            if (block) {
                pose.translate(0, 0.14, 0);
                pose.scale(0.3f, 0.3f, 0.3f);
            } else {
                pose.translate(0, 0.19, 0);
                pose.scale(0.38f, 0.38f, 0.38f);
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, lightHere, OverlayTexture.NO_OVERLAY,
                    pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}

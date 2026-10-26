package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.archer.BowSystem;
import net.zennin.mushoku.client.render.BowRender;
import net.zennin.mushoku.client.render.QuiverRender;

/** Arcos y carcaj como objeto (inventario, piso, marcos, primera persona sin animacion): mismo dibujo que en la mano. */
public final class ItemModels {
    private ItemModels() {}

    public static void bow(ItemStack stack, ItemDisplayContext mode, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        Player p = Minecraft.getInstance().player;
        float back = BowRender.BRACE;
        boolean drawing = p != null && p.isUsingItem() && p.getUseItem() == stack;
        if (drawing) {
            float t = (p.getTicksUsingItem() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) / 20f;
            back = BowRender.BRACE + BowRender.DRAW * Math.min(1f, t * ((net.zennin.mushoku.item.MushokuBowItem) stack.getItem()).speed);
        }
        if (mode == ItemDisplayContext.GUI) pose.mulPose(Axis.ZP.rotationDegrees(-45));
        boolean arrow = drawing || BowSystem.isLoaded(stack);
        BowRender.draw(pose, buffers, BowRender.style(stack), back, arrow, new ItemStack(Items.ARROW), light, overlay);
        pose.popPose();
    }

    public static void quiver(ItemStack stack, ItemDisplayContext mode, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5f, 0.5f - QuiverRender.H / 2 / 16f - 0.1f, 0.5f);
        QuiverRender.draw(pose, buffers, stack, light, overlay);
        pose.popPose();
    }
}

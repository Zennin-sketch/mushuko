package net.zennin.mushoku.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Piedra magica. Si fue imbuida en un circulo, guarda un hechizo y brilla. */
public class MagicStoneItem extends Item {
    public MagicStoneItem(Properties props) { super(props); }

    /** Shift + clic derecho sobre el piso: apoya la piedra para refinarla (hechizo Refinamiento). */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        var player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return super.useOn(ctx);
        var level = ctx.getLevel();
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof net.zennin.mushoku.block.MagicCircleBlock) return super.useOn(ctx);
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        return player instanceof net.minecraft.server.level.ServerPlayer sp
                && net.zennin.mushoku.refine.Refining.place(level, ctx.getClickedPos(), ctx.getClickedFace(), sp, ctx.getItemInHand())
                ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return SpellData.get(stack) != null || super.isFoil(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        SpellData.tooltip(stack, lines);
    }
}

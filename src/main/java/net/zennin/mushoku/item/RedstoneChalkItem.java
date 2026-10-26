package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.block.ChalkLineBlock;
import net.zennin.mushoku.registry.ModBlocks;

import java.util.List;

/** Tiza rojiza: clic derecho arriba de un bloque dibuja una linea (como la redstone). */
public class RedstoneChalkItem extends Item {
    public RedstoneChalkItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (ctx.getClickedFace() != Direction.UP) {
            if (!level.isClientSide && player != null)
                player.displayClientMessage(Component.translatable("msg.remushoku.chalk_face").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().above();
        ChalkLineBlock line = (ChalkLineBlock) ModBlocks.CHALK_LINE;
        BlockState state = line.getStateForPlacement(new BlockPlaceContext(ctx));
        if (state == null || !level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(pos, line.withConnections(state, level, pos), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8f, 1.6f);
        if (player != null) ctx.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.redstone_chalk").withStyle(ChatFormatting.GRAY));
    }
}

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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.block.MagicCircleBlockEntity;
import net.zennin.mushoku.registry.ModBlocks;

import java.util.List;

/** Tiza: clic derecho en la cara de arriba de un bloque dibuja un circulo magico. */
public class ChalkItem extends Item {
    public ChalkItem(Properties props) { super(props); }

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
        BlockState circle = ModBlocks.MAGIC_CIRCLE.defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !circle.canSurvive(level, pos)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(pos, circle, Block.UPDATE_ALL);
        if (player != null && level.getBlockEntity(pos) instanceof MagicCircleBlockEntity be) be.setOwner(player.getUUID());
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 1.4f);
        ItemStack stack = ctx.getItemInHand();
        if (player != null) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.chalk").withStyle(ChatFormatting.GRAY));
    }
}

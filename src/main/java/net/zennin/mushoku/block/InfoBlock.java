package net.zennin.mushoku.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bloque cuya funcion llega en una parte siguiente (tablon de trabajos, invocador de jefe): avisa al usarlo. */
public class InfoBlock extends Block {
    private final String message;

    public InfoBlock(String message, Properties props) {
        super(props);
        this.message = message;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

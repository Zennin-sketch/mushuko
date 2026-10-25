package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.zennin.mushoku.guild.GuildSystem;

/** Tablon de trabajos del gremio: abre el menu de trabajos. */
public class JobBoardBlock extends Block {
    public JobBoardBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) GuildSystem.open(sp, "guild");
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

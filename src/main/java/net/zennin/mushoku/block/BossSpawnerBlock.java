package net.zennin.mushoku.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.zennin.mushoku.registry.ModEntities;

/**
 * Invocador de la sala del jefe: clic derecho despierta al jefe de la mazmorra.
 * En la mazmorra de hielo sale la Draca de Nieve Madre; en la musgosa, la Tortuga Gigante. El bloque se consume.
 */
public class BossSpawnerBlock extends Block {
    public BossSpawnerBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        EntityType<? extends Mob> type = isIcy(sl, pos) ? ModEntities.SNOW_DRAKE_MOM : ModEntities.GIANT_TURTLE;
        Mob boss = type.create(sl);
        if (boss == null) return InteractionResult.FAIL;
        BlockPos spawn = pos.above();
        boss.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot() + 180, 0);
        boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawn), MobSpawnType.TRIGGERED, null);
        boss.setTarget(player);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        sl.addFreshEntity(boss);
        sl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1f, 0.8f);
        player.displayClientMessage(Component.translatable("msg.remushoku.boss_awakens", boss.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
        return InteractionResult.CONSUME;
    }

    private static boolean isIcy(ServerLevel level, BlockPos center) {
        int ice = 0, moss = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-6, -3, -6), center.offset(6, 3, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) ice++;
            else if (s.is(Blocks.MOSSY_COBBLESTONE) || s.is(Blocks.MOSSY_STONE_BRICKS) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET)) moss++;
        }
        return ice > moss;
    }
}

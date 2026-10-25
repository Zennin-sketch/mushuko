package net.zennin.mushoku.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.registry.ModBlocks;
import net.zennin.mushoku.registry.ModItems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puerta con cerradura de las mazmorras. Con la Llave Musgosa o la Llave de Hielo (clic derecho):
 * se construye la sala del jefe debajo (igual que el original) y la puerta se abre tabla por tabla.
 */
public class DoorLockBlock extends Block {
    public DoorLockBlock(Properties props) { super(props); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean mossy = stack.is(ModItems.MOSSY_KEY), ice = stack.is(ModItems.ICE_KEY);
        if (!mossy && !ice) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.remushoku.door_locked").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel sl = (ServerLevel) level;
        // Mismas posiciones que el original: la sala queda debajo de la puerta.
        BlockPos roomPos = mossy ? pos.offset(-15, -28, -33) : pos.offset(-18, -25, -38);
        ResourceLocation room = MushokuMod.id(mossy ? "mossybossroom" : "icebossroom");
        sl.getStructureManager().get(room).ifPresent((StructureTemplate t) ->
                t.placeInWorld(sl, roomPos, roomPos, new StructurePlaceSettings(), sl.random, Block.UPDATE_ALL));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        openDoor(sl, pos);
        player.displayClientMessage(Component.translatable("msg.remushoku.door_opened").withStyle(ChatFormatting.GOLD), true);
        return ItemInteractionResult.CONSUME;
    }

    /** Busca las tablas y cerraduras conectadas y las rompe de a una. */
    private static void openDoor(ServerLevel level, BlockPos start) {
        List<BlockPos> order = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && order.size() < 64) {
            BlockPos p = queue.poll();
            order.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && start.distManhattan(n) <= 8) {
                    BlockState s = level.getBlockState(n);
                    if (s.is(ModBlocks.DOOR_PLANK) || s.is(ModBlocks.DOOR_LOCK)) queue.add(n);
                }
            }
        }
        for (int i = 0; i < order.size(); i++) {
            BlockPos p = order.get(i);
            Tasks.later(2 + i * 3, () -> {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, p, SoundEvents.CHERRY_WOOD_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
            });
        }
    }
}

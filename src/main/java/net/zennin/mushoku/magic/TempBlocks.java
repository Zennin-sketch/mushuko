package net.zennin.mushoku.magic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Bloques temporales (muros de hielo, etc.) que desaparecen solos despues de un tiempo. */
public final class TempBlocks {
    private record Entry(ServerLevel level, BlockPos pos, BlockState state, long expire) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private TempBlocks() {}

    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlockAndUpdate(pos, state);
        ENTRIES.add(new Entry(level, pos.immutable(), state, level.getGameTime() + ticks));
        return true;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Entry> it = ENTRIES.iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (e.level.getGameTime() >= e.expire) {
                    if (e.level.getBlockState(e.pos).is(e.state.getBlock())) e.level.setBlockAndUpdate(e.pos, Blocks.AIR.defaultBlockState());
                    it.remove();
                }
            }
        });
    }
}

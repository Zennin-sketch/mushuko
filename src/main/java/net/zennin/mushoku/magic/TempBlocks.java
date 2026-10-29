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
    private record Entry(ServerLevel level, BlockPos pos, BlockState state, long expire, BlockState original) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private TempBlocks() {}

    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlockAndUpdate(pos, state);
        ENTRIES.add(new Entry(level, pos.immutable(), state, level.getGameTime() + ticks, Blocks.AIR.defaultBlockState()));
        return true;
    }

    /** Cambia un bloque por otro un rato y despues devuelve el original (sin bloques con datos ni irrompibles). */
    public static boolean replace(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState old = level.getBlockState(pos);
        if (old.hasBlockEntity() || old.getDestroySpeed(level, pos) < 0 || old.is(state.getBlock())) return false;
        for (Entry e : ENTRIES) if (e.level == level && e.pos.equals(pos)) return false;
        level.setBlock(pos, state, 3);
        ENTRIES.add(new Entry(level, pos.immutable(), state, level.getGameTime() + ticks, old));
        return true;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Entry> it = ENTRIES.iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (e.level.getGameTime() >= e.expire) {
                    if (e.level.getBlockState(e.pos).is(e.state.getBlock())) e.level.setBlockAndUpdate(e.pos, e.original);
                    it.remove();
                }
            }
        });
    }
}

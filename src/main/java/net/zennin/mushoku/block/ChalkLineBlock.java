package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Linea de tiza rojiza (hecha con tiza y redstone). Roja no hace nada; imbuida con el hechizo Infusion de Mana pasa a
 * azul, brilla y funciona como redstone: toma cualquier senal, la lleva por la linea (pierde 1 por bloque) y
 * acciona mecanismos y circulos magicos cargados que toca.
 */
public class ChalkLineBlock extends Block {
    public static final MapCodec<ChalkLineBlock> CODEC = simpleCodec(ChalkLineBlock::new);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    /** Mientras se mide la senal de afuera, las lineas no cuentan (como hace la redstone vanilla). */
    private static boolean measuring, refreshing;

    public ChalkLineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, false).setValue(POWER, 0)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHARGED, POWER, NORTH, EAST, SOUTH, WEST); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    public static BooleanProperty side(Direction d) {
        return switch (d) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; default -> WEST; };
    }

    private static boolean connects(BlockState self, BlockState other) {
        if (other.getBlock() instanceof ChalkLineBlock) return true;
        return self.getValue(CHARGED) && (other.isSignalSource() || other.is(Blocks.REDSTONE_WIRE) || other.getBlock() instanceof MagicCircleBlock);
    }

    public BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) state = state.setValue(side(d), connects(state, level.getBlockState(pos.relative(d))));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (dir.getAxis().isHorizontal()) return state.setValue(side(dir), connects(state, neighbor));
        return state;
    }

    // ---------------------------------------------------------------- senal
    @Override
    protected boolean isSignalSource(BlockState state) { return state.getValue(CHARGED) && !measuring; }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        if (measuring || !state.getValue(CHARGED) || dir == Direction.DOWN) return 0;
        return state.getValue(POWER);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        // como la redstone: le da senal fuerte al bloque de abajo (asi enciende lo que este pegado a ese bloque)
        return dir == Direction.UP ? getSignal(state, level, pos, dir) : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        if (!canSurvive(state, level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        if (state.getValue(CHARGED)) refresh(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(state.getBlock()) && state.getValue(CHARGED)) refresh(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        super.onRemove(state, level, pos, now, moving);
        if (!level.isClientSide && !moving && !now.is(this)) {
            level.updateNeighborsAt(pos, this);
            for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
        }
    }

    private static int external(SignalGetter level, BlockPos pos) {
        measuring = true;
        try {
            return level.getBestNeighborSignal(pos);
        } finally {
            measuring = false;
        }
    }

    /** Recalcula la potencia de toda la linea conectada (lo de afuera entra, y baja 1 por bloque). */
    public static void refresh(Level level, BlockPos start) {
        if (refreshing) return;
        refreshing = true;
        try {
            doRefresh(level, start);
        } finally {
            refreshing = false;
        }
    }

    private static void doRefresh(Level level, BlockPos start) {
        Set<BlockPos> net = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        while (!todo.isEmpty() && net.size() < 512) {
            BlockPos p = todo.poll();
            if (!net.add(p)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                BlockState s = level.getBlockState(n);
                if (!net.contains(n) && s.getBlock() instanceof ChalkLineBlock && s.getValue(CHARGED)) todo.add(n);
            }
        }
        // fuentes de afuera
        java.util.Map<BlockPos, Integer> power = new java.util.HashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        for (BlockPos p : net) {
            int e = external(level, p);
            power.put(p, e);
            if (e > 0) q.add(p);
        }
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            int v = power.get(p) - 1;
            if (v <= 0) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                Integer cur = power.get(n);
                if (cur != null && cur < v) { power.put(n, v); q.add(n); }
            }
        }
        for (BlockPos p : net) {
            BlockState s = level.getBlockState(p);
            int v = power.get(p);
            if (s.getValue(POWER) != v) {
                level.setBlock(p, s.setValue(POWER, v), Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(p, s.getBlock());
                for (Direction d : Direction.values()) level.updateNeighborsAt(p.relative(d), s.getBlock());
            }
        }
    }

    /** Infusion de Mana: toda la linea roja conectada pasa a azul. Devuelve cuantos bloques cambio. */
    public static int infuse(ServerLevel level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        while (!todo.isEmpty() && seen.size() < 256) {
            BlockPos p = todo.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof ChalkLineBlock) || s.getValue(CHARGED) || !seen.add(p)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) todo.add(p.relative(d));
        }
        for (BlockPos p : seen) {
            BlockState s = level.getBlockState(p);
            BlockState blue = ((ChalkLineBlock) s.getBlock()).withConnections(s.setValue(CHARGED, true), level, p);
            level.setBlock(p, blue, Block.UPDATE_ALL);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.75f, 1f), 1f), p.getX() + 0.5, p.getY() + 0.15, p.getZ() + 0.5,
                    4, 0.25, 0.05, 0.25, 0);
        }
        if (!seen.isEmpty()) refresh(level, seen.iterator().next());
        return seen.size();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED) || state.getValue(POWER) == 0 || random.nextInt(3) != 0) return;
        float k = state.getValue(POWER) / 15f;
        level.addParticle(new DustParticleOptions(new Vector3f(0.35f + 0.3f * k, 0.65f + 0.25f * k, 1f), 0.6f),
                pos.getX() + random.nextDouble(), pos.getY() + 0.08, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    /** Color de la linea (lo usa el tinte del modelo). */
    public static int color(BlockState s) {
        if (!s.getValue(CHARGED)) return 0xB5483C;
        float k = s.getValue(POWER) / 15f;
        int r = Math.round(0x3A + (0x9C - 0x3A) * k), g = Math.round(0x6E + (0xE8 - 0x6E) * k), b = Math.round(0xD0 + (0xFF - 0xD0) * k);
        return r << 16 | g << 8 | b;
    }
}

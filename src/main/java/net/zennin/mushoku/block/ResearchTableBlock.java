package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.zennin.mushoku.menu.ResearchMenu;

/**
 * Mesa de Investigacion: escritorio de dos bloques (como una cama: se pone la mitad principal y la otra a su
 * derecha). Abre el menu donde se fabrican piedras de mana y pergaminos, y se graban hechizos.
 */
public class ResearchTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ResearchTableBlock> CODEC = simpleCodec(ResearchTableBlock::new);
    /** MAIN = la mitad que se coloco (suelta el item); SIDE = la otra. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SIDE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("side");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 15, 16), Block.box(1, 0, 1, 15, 12, 15));

    public ResearchTableBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, SIDE); }

    /** Llama de la vela (en la mitad principal) y algun destello de la amatista. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource rnd) {
        if (state.getValue(SIDE)) return;
        double[] c = rotate(13.75 / 16, 3.75 / 16, state.getValue(FACING));
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, pos.getX() + c[0], pos.getY() + 20.2 / 16, pos.getZ() + c[1], 0, 0, 0);
        if (rnd.nextInt(5) == 0)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + c[0], pos.getY() + 20.8 / 16, pos.getZ() + c[1], 0, 0.01, 0);
        if (rnd.nextInt(8) == 0) {
            double[] g = rotate(11.5 / 16, 11.5 / 16, state.getValue(FACING));
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, pos.getX() + g[0], pos.getY() + 1.3, pos.getZ() + g[1], 0, 0.2, 0);
        }
    }

    /** Punto del modelo (mirando al norte) girado como el bloque. */
    private static double[] rotate(double x, double z, Direction facing) {
        return switch (facing) {
            case EAST -> new double[]{1 - z, x};
            case SOUTH -> new double[]{1 - x, 1 - z};
            case WEST -> new double[]{z, 1 - x};
            default -> new double[]{x, z};
        };
    }

    /** Hacia donde esta la otra mitad, vista desde esta. */
    private static Direction toPartner(BlockState s) {
        Direction side = s.getValue(FACING).getCounterClockWise();
        return s.getValue(SIDE) ? side.getOpposite() : side;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos other = ctx.getClickedPos().relative(facing.getCounterClockWise());
        if (!ctx.getLevel().getBlockState(other).canBeReplaced(ctx) || !ctx.getLevel().getWorldBorder().isWithinBounds(other)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide)
            level.setBlock(pos.relative(toPartner(state)), state.setValue(SIDE, true), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos npos) {
        if (dir == toPartner(state) && !(neighbor.is(this) && neighbor.getValue(SIDE) != state.getValue(SIDE)))
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, npos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // romper la mitad de al lado en creativo no suelta nada (como la cama)
        if (!level.isClientSide && player.isCreative() && state.getValue(SIDE)) {
            BlockPos other = pos.relative(toPartner(state));
            if (level.getBlockState(other).is(this)) level.setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(getMenuProvider(state, level, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new ResearchMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.remushoku.research_table"));
    }
}

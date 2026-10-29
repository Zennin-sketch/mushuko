package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piedra magica apoyada en el piso (Shift + clic derecho con la piedra). El hechizo Refinamiento abre el menu para
 * intentar subirla de nivel. Clic derecho con la mano vacia la levanta.
 */
public class RefiningStoneBlock extends BaseEntityBlock {
    public static final MapCodec<RefiningStoneBlock> CODEC = simpleCodec(RefiningStoneBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

    public RefiningStoneBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(4, 0, 4, 12, 2, 12); }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RefiningStoneBlockEntity(pos, state); }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState n, LevelAccessor level, BlockPos pos, BlockPos np) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof RefiningStoneBlockEntity be && !be.rolling) {
            ItemStack st = be.stone().copy();
            level.removeBlock(pos, false);
            if (!st.isEmpty() && !player.getInventory().add(st)) player.drop(st, false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof RefiningStoneBlockEntity be && !be.stone().isEmpty()
                && !be.rolling && !level.isClientSide && !net.zennin.mushoku.refine.Refining.consumed(level, pos)) {
            Block.popResource(level, pos, be.stone().copy());
        }
        super.onRemove(state, level, pos, now, moving);
    }
}

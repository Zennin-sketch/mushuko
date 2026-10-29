package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lodo (hechizo Lodazal): se ve como un bloque de barro entero, pero se pisa a la altura de una losa, asi que los
 * pies se hunden medio bloque (arena movediza), y frena como una telarana.
 */
public class QuagmireMudBlock extends Block {
    public static final MapCodec<QuagmireMudBlock> CODEC = simpleCodec(QuagmireMudBlock::new);
    private static final VoxelShape SLAB = Block.box(0, 0, 0, 16, 8, 16);

    public QuagmireMudBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SLAB; }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return SLAB; }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // igual que la telarana; salvo durante la Rafaga (te saca del lodo)
        if (net.zennin.mushoku.magic.Escape.active(entity)) return;
        entity.makeStuckInBlock(state, new Vec3(0.25, 0.05f, 0.25));
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) { return false; }
}

package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloque invisible que se atraviesa: luz de las mazmorras y bloque para trepar. */
public class PassableBlock extends Block {
    private final boolean selectable;

    public PassableBlock(boolean selectable, Properties props) {
        super(props);
        this.selectable = selectable;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return selectable ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    protected boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
}

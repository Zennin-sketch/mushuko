package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Flor de Mana silvestre: crece sola en praderas y bosques; al romperla da petalos y semillas. */
public class WildManaBloomBlock extends BushBlock {
    public static final MapCodec<WildManaBloomBlock> CODEC = simpleCodec(WildManaBloomBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 15, 12);

    public WildManaBloomBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BushBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }
}

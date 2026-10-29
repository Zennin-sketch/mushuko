package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tramo de un pilar de hechizo (Pilar de Hielo / Pilar de Tierra): base ancha, tramos del medio que encajan uno con
 * otro y punta. Solido, pero con su propio modelo (no es un cubo entero). Lo pone y lo saca el hechizo.
 */
public class SpellPillarBlock extends Block {
    public enum Part implements StringRepresentable {
        BOTTOM("bottom"), MIDDLE("middle"), TOP("top");
        private final String name;
        Part(String n) { name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape BOTTOM = Block.box(1, 0, 1, 15, 16, 15), MIDDLE = Block.box(2.5, 0, 2.5, 13.5, 16, 13.5),
            TOP = Block.box(3.5, 0, 3.5, 12.5, 14, 12.5);

    public SpellPillarBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.MIDDLE));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return simpleCodec(SpellPillarBlock::new); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PART); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(PART)) { case BOTTOM -> BOTTOM; case TOP -> TOP; default -> MIDDLE; };
    }

    /** Estado para el tramo 'i' (1 = abajo) de un pilar de 'n' tramos. */
    public BlockState part(int i, int n) {
        return defaultBlockState().setValue(PART, i <= 1 ? Part.BOTTOM : i >= n ? Part.TOP : Part.MIDDLE);
    }
}

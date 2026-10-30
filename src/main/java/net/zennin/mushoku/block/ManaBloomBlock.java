package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.CropBlock;

/**
 * Flor de Mana: cultivo de 4 etapas con hojas verde azuladas y flores celestes que brillan un poco cuando esta madura.
 * Rota da semillas; madura da petalos de mana (para las pociones del Alambique) y semillas.
 */
public class ManaBloomBlock extends BeetrootBlock {
    public static final MapCodec<BeetrootBlock> CODEC = simpleCodec(ManaBloomBlock::new);

    public ManaBloomBlock(Properties props) { super(props); }

    @Override
    public MapCodec<BeetrootBlock> codec() { return CODEC; }

    @Override
    protected ItemLike getBaseSeedId() { return net.zennin.mushoku.registry.ModItems.MANA_BLOOM_SEEDS; }

    /** Crece un poco mas lento que la remolacha. */
    @Override
    protected void randomTick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level,
                              net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(2) == 0) super.randomTick(state, level, pos, random);
    }

    public static boolean mature(net.minecraft.world.level.block.state.BlockState s) { return s.getValue(AGE) >= MAX_AGE; }
}

package net.zennin.mushoku.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.block.MagicCircleBlock;
import net.zennin.mushoku.block.MagicCircleBlockEntity;

public final class ModBlocks {
    /** Circulo magico dibujado con tiza. No tiene item: se crea con la tiza y no suelta nada al romperse. */
    public static final Block MAGIC_CIRCLE = Registry.register(BuiltInRegistries.BLOCK, MushokuMod.id("magic_circle"),
            new MagicCircleBlock(BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().strength(0.3f).sound(SoundType.CALCITE)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(MagicCircleBlock.POWERED) ? 7 : 2)));

    public static final BlockEntityType<MagicCircleBlockEntity> MAGIC_CIRCLE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, MushokuMod.id("magic_circle"),
            BlockEntityType.Builder.of(MagicCircleBlockEntity::new, MAGIC_CIRCLE).build(null));

    private ModBlocks() {}

    public static void init() {}
}

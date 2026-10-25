package net.zennin.mushoku.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.util.valueproviders.UniformInt;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.block.BossSpawnerBlock;
import net.zennin.mushoku.block.DoorLockBlock;
import net.zennin.mushoku.block.InfoBlock;
import net.zennin.mushoku.block.MagicCircleBlock;
import net.zennin.mushoku.block.MagicCircleBlockEntity;
import net.zennin.mushoku.block.PassableBlock;
import net.zennin.mushoku.block.SandCloudBlock;
import net.zennin.mushoku.block.ShelfBlock;
import net.zennin.mushoku.block.ShelfBlockEntity;

public final class ModBlocks {
    /** Circulo magico dibujado con tiza. No tiene item: se crea con la tiza y no suelta nada al romperse. */
    public static final Block MAGIC_CIRCLE = Registry.register(BuiltInRegistries.BLOCK, MushokuMod.id("magic_circle"),
            new MagicCircleBlock(BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().strength(0.3f).sound(SoundType.CALCITE)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(MagicCircleBlock.POWERED) ? 7 : 2)));

    // --- Mundo (mismas durezas que el original; -1 = irrompible) ---
    public static final Block MAGIC_ORE = withItem("magic_ore", new DropExperienceBlock(UniformInt.of(2, 5),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(10f, 15f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final Block SHELF = withItem("shelf", new ShelfBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2f, 10f).sound(SoundType.WOOD).noOcclusion()));
    public static final Block JOB_BOARD = withItem("job_board", new InfoBlock("msg.remushoku.job_board_soon",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1f, 3600000f).sound(SoundType.WOOD)));
    public static final Block DOOR_LOCK = withItem("door_lock", new DoorLockBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(-1f, 3600000f).sound(SoundType.WOOD)));
    public static final Block DOOR_PLANK = withItem("door_plank", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(-1f, 3600000f).sound(SoundType.WOOD)));
    public static final Block CLIMB_BLOCK = withItem("climb_block", new PassableBlock(false, BlockBehaviour.Properties.of()
            .strength(-1f, 3600000f).noOcclusion().noLootTable().replaceable().pushReaction(PushReaction.BLOCK)));
    public static final Block GLOW = withItem("glow", new PassableBlock(false, BlockBehaviour.Properties.of()
            .strength(-1f, 3600000f).noOcclusion().noLootTable().replaceable().lightLevel(s -> 15)));
    public static final Block TP_POINT = withItem("tp_point", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(10f, 15f).sound(SoundType.STONE)));
    public static final Block ABSOLUTE_BARRIER = withItem("absolute_barrier", new Block(BlockBehaviour.Properties.of()
            .strength(10000f, 1.0E8f).noOcclusion().sound(SoundType.GLASS).isViewBlocking((s, l, p) -> false)));
    public static final Block BLOCK_SAND_CLOUD = withItem("block_sand_cloud", new SandCloudBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND).strength(-1f, 3600000f).noOcclusion().noLootTable().replaceable().sound(SoundType.SAND)));
    public static final Block BOSS_SPAWNER = withItem("boss_spawner", new BossSpawnerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(10f, 15f).sound(SoundType.STONE)));

    public static final BlockEntityType<MagicCircleBlockEntity> MAGIC_CIRCLE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, MushokuMod.id("magic_circle"),
            BlockEntityType.Builder.of(MagicCircleBlockEntity::new, MAGIC_CIRCLE).build(null));
    public static final BlockEntityType<ShelfBlockEntity> SHELF_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, MushokuMod.id("shelf"),
            BlockEntityType.Builder.of(ShelfBlockEntity::new, SHELF).build(null));

    private ModBlocks() {}

    private static Block withItem(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, MushokuMod.id(name), block);
        Item item = Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(name), new BlockItem(block, new Item.Properties()));
        ModItems.ALL.add(item);
        return block;
    }

    public static void init() {}
}

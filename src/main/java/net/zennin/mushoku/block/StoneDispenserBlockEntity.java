package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.registry.ModBlocks;

/** Inventario de 9 del Dispensador de Piedras (las tolvas solo le meten piedras magicas sin hechizo). */
public class StoneDispenserBlockEntity extends DispenserBlockEntity {
    public StoneDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STONE_DISPENSER_BE, pos, state);
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.remushoku.stone_dispenser"); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return ElementBookItem.stoneLevel(stack) > 0; }
}

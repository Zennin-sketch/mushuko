package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.menu.AlembicMenu;
import net.zennin.mushoku.registry.ModBlocks;
import net.zennin.mushoku.registry.ModItems;

/**
 * Huecos: 0 botella de agua, 1 ingrediente, 2 combustible, 3 pocion. Ingredientes: petalo de Flor de Mana = pocion chica,
 * fragmento de amatista = pocion, piedra elemental = pocion grande. Cada destilado tarda 10 s.
 */
public class AlembicBlockEntity extends BaseContainerBlockEntity {
    public static final int BREW_TIME = 200;
    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int progress, burn, burnMax;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) { case 0 -> progress; case 1 -> burn; default -> burnMax; }; }
        @Override public void set(int i, int v) { switch (i) { case 0 -> progress = v; case 1 -> burn = v; default -> burnMax = v; } }
        @Override public int getCount() { return 3; }
    };

    public AlembicBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.ALEMBIC_BE, pos, state); }

    public static boolean isWater(ItemStack s) {
        return s.is(Items.POTION) && s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    public static Item resultFor(ItemStack ingredient) {
        if (ingredient.is(ModItems.MANA_PETAL)) return ModItems.MANA_POTION_SMALL;
        if (ingredient.is(Items.AMETHYST_SHARD)) return ModItems.MANA_POTION;
        if (ingredient.is(ModItems.ELEMENTARY_STONE)) return ModItems.MANA_POTION_LARGE;
        return null;
    }

    public static int fuelTime(ItemStack s) {
        Integer t = AbstractFurnaceBlockEntity.getFuel().get(s.getItem());
        return t == null ? 0 : t;
    }

    private boolean canBrew() {
        Item r = resultFor(items.get(1));
        if (r == null || !isWater(items.get(0))) return false;
        ItemStack out = items.get(3);
        return out.isEmpty() || (out.is(r) && out.getCount() < out.getMaxStackSize());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity be) {
        boolean wasLit = be.burn > 0;
        if (be.burn > 0) be.burn--;
        if (be.canBrew()) {
            if (be.burn <= 0) {
                int t = fuelTime(be.items.get(2));
                if (t > 0) {
                    be.burn = be.burnMax = t;
                    ItemStack f = be.items.get(2);
                    Item rem = f.getItem().getCraftingRemainingItem();
                    f.shrink(1);
                    if (f.isEmpty() && rem != null) be.items.set(2, new ItemStack(rem));
                }
            }
            if (be.burn > 0 && ++be.progress >= BREW_TIME) {
                be.progress = 0;
                Item r = resultFor(be.items.get(1));
                be.items.get(1).shrink(1);
                be.items.get(0).shrink(1); // la botella de agua se vuelve la pocion
                ItemStack out = be.items.get(3);
                if (out.isEmpty()) be.items.set(3, new ItemStack(r));
                else out.grow(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1.1f);
            }
        } else if (be.progress > 0) be.progress = Math.max(0, be.progress - 2);
        boolean lit = be.burn > 0;
        if (lit != wasLit || state.getValue(AlembicBlock.LIT) != lit)
            level.setBlock(pos, state.setValue(AlembicBlock.LIT, lit), 3);
        setChanged(level, pos, state);
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.remushoku.alembic"); }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) { items = list; }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new AlembicMenu(id, inv, this, data); }

    @Override
    public int getContainerSize() { return 4; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack s) {
        return switch (slot) { case 0 -> isWater(s); case 1 -> resultFor(s) != null; case 2 -> fuelTime(s) > 0; default -> false; };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        ContainerHelper.saveAllItems(tag, items, reg);
        tag.putInt("Progress", progress);
        tag.putInt("Burn", burn);
        tag.putInt("BurnMax", burnMax);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        items = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, reg);
        progress = tag.getInt("Progress");
        burn = tag.getInt("Burn");
        burnMax = tag.getInt("BurnMax");
    }
}

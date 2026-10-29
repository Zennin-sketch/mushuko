package net.zennin.mushoku.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.block.AlembicBlockEntity;

/** Menu del Alambique de Cobre: agua, ingrediente, combustible y la pocion que sale. */
public class AlembicMenu extends AbstractContainerMenu {
    public static final int WATER_X = 44, WATER_Y = 51, ING_X = 44, ING_Y = 17, FUEL_X = 80, FUEL_Y = 53, OUT_X = 116, OUT_Y = 35;
    private final Container box;
    private final ContainerData data;

    public AlembicMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(4), new SimpleContainerData(3)); }

    public AlembicMenu(int id, Inventory inv, Container box, ContainerData data) {
        super(ModMenus.ALEMBIC, id);
        this.box = box;
        this.data = data;
        addSlot(new Slot(box, 0, WATER_X, WATER_Y) { @Override public boolean mayPlace(ItemStack s) { return AlembicBlockEntity.isWater(s); } });
        addSlot(new Slot(box, 1, ING_X, ING_Y) { @Override public boolean mayPlace(ItemStack s) { return AlembicBlockEntity.resultFor(s) != null; } });
        addSlot(new Slot(box, 2, FUEL_X, FUEL_Y) { @Override public boolean mayPlace(ItemStack s) { return AlembicBlockEntity.fuelTime(s) > 0; } });
        addSlot(new Slot(box, 3, OUT_X, OUT_Y) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public float progress() { return data.get(0) / (float) AlembicBlockEntity.BREW_TIME; }

    public float fuel() { return data.get(2) <= 0 ? 0 : data.get(1) / (float) data.get(2); }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem(), copy = st.copy();
        if (index < 4) {
            if (!moveItemStackTo(st, 4, 40, true)) return ItemStack.EMPTY;
        } else {
            int target = AlembicBlockEntity.isWater(st) ? 0 : AlembicBlockEntity.resultFor(st) != null ? 1 : AlembicBlockEntity.fuelTime(st) > 0 ? 2 : -1;
            if (target < 0 || !moveItemStackTo(st, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) { return box.stillValid(p); }
}

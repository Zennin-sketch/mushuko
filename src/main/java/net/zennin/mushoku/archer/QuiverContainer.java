package net.zennin.mushoku.archer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.item.QuiverItem;

/** La ranura del carcaj (1 objeto), guardada en el jugador. */
public final class QuiverContainer implements Container {
    public final Player owner;

    public QuiverContainer(Player owner) { this.owner = owner; }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return ModAttachments.quiver(owner).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? ModAttachments.quiver(owner) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack s = getItem(slot);
        if (s.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack out = s.split(amount);
        if (s.isEmpty()) ModAttachments.setQuiver(owner, ItemStack.EMPTY);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = getItem(slot);
        ModAttachments.setQuiver(owner, ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        ModAttachments.setQuiver(owner, stack);
        setChanged();
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override
    public void setChanged() {
        if (owner instanceof ServerPlayer sp) QuiverSystem.broadcast(sp);
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { ModAttachments.setQuiver(owner, ItemStack.EMPTY); }

    /** Solo acepta carcajes. */
    public static final class QuiverSlot extends Slot {
        public QuiverSlot(QuiverContainer c, int x, int y) { super(c, 0, x, y); }

        @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof QuiverItem; }
        @Override public int getMaxStackSize() { return 1; }
    }
}

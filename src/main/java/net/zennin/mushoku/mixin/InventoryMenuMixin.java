package net.zennin.mushoku.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.archer.QuiverContainer;
import net.zennin.mushoku.item.QuiverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ranura propia del carcaj en el inventario, arriba de la del escudo. Shift + clic sobre un carcaj lo manda ahi. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    @Unique private int remushoku$quiverIndex = -1;

    protected InventoryMenuMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void remushoku$addQuiverSlot(Inventory inv, boolean active, Player owner, CallbackInfo ci) {
        addSlot(new QuiverContainer.QuiverSlot(new QuiverContainer(owner), 77, 44));
        remushoku$quiverIndex = slots.size() - 1;
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void remushoku$quickMoveQuiver(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (remushoku$quiverIndex < 0 || index < 9 || index >= remushoku$quiverIndex) return;
        Slot from = slots.get(index);
        if (!from.hasItem() || !(from.getItem().getItem() instanceof QuiverItem)) return;
        if (slots.get(remushoku$quiverIndex).hasItem()) return;
        ItemStack st = from.getItem();
        if (moveItemStackTo(st, remushoku$quiverIndex, remushoku$quiverIndex + 1, false)) {
            from.setChanged();
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}

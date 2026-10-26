package net.zennin.mushoku.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.zennin.mushoku.archer.QuiverContainer;
import net.zennin.mushoku.client.QuiverSlotArt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En la pestana de inventario del creativo la ranura del carcaj va a la derecha del jugador. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;

    protected CreativeScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private static final int QX = 127, QY = 20;

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void remushoku$placeQuiverSlot(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;
        for (Slot s : menu.slots) {
            if (s.container instanceof QuiverContainer) {
                ((SlotAccessor) s).remushoku$setX(QX);
                ((SlotAccessor) s).remushoku$setY(QY);
            }
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void remushoku$quiverSlotBg(GuiGraphics g, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
        QuiverSlotArt.draw(g, leftPos + QX - 1, topPos + QY - 1, null);
    }
}

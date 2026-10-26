package net.zennin.mushoku.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.zennin.mushoku.client.QuiverSlotArt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dibuja el marco de la ranura del carcaj (el fondo del inventario vanilla no la tiene). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void remushoku$quiverSlotBg(GuiGraphics g, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        QuiverSlotArt.draw(g, leftPos + 76, topPos + 43, menu);
    }
}

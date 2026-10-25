package net.zennin.mushokucombat.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.zennin.mushokucombat.client.StaminaHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sube corazones, armadura, comida, aire y la vida de la montura para dejar lugar a la barra de estamina. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique private boolean mushokuCombat$pushed;

    @Unique
    private void mushokuCombat$push(GuiGraphics g) {
        mushokuCombat$pushed = StaminaHud.visible();
        if (mushokuCombat$pushed) {
            g.pose().pushPose();
            g.pose().translate(0, -StaminaHud.SHIFT, 0);
        }
    }

    @Unique
    private void mushokuCombat$pop(GuiGraphics g) {
        if (mushokuCombat$pushed) g.pose().popPose();
        mushokuCombat$pushed = false;
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void mushokuCombat$healthStart(GuiGraphics g, CallbackInfo ci) { mushokuCombat$push(g); }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void mushokuCombat$healthEnd(GuiGraphics g, CallbackInfo ci) { mushokuCombat$pop(g); }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"))
    private void mushokuCombat$vehicleStart(GuiGraphics g, CallbackInfo ci) { mushokuCombat$push(g); }

    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    private void mushokuCombat$vehicleEnd(GuiGraphics g, CallbackInfo ci) { mushokuCombat$pop(g); }
}

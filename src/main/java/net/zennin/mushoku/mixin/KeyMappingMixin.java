package net.zennin.mushoku.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Minecraft solo le avisa a UNA accion por tecla: si "Flecha Arriba" y "Avanzar" estan en W, una de las dos deja
 * de funcionar (por eso no te podias mover). Con esto, todas las acciones que comparten tecla reciben la pulsacion.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow @Final private static Map<String, KeyMapping> ALL;
    @Shadow private InputConstants.Key key;
    @Shadow private int clickCount;

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void mushoku$clickShared(InputConstants.Key pressed, CallbackInfo ci) {
        if (pressed == InputConstants.UNKNOWN) return;
        for (KeyMapping m : ALL.values()) {
            KeyMappingMixin self = (KeyMappingMixin) (Object) m;
            if (self.key.equals(pressed)) self.clickCount++;
        }
        ci.cancel();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void mushoku$setShared(InputConstants.Key pressed, boolean down, CallbackInfo ci) {
        if (pressed == InputConstants.UNKNOWN) return;
        for (KeyMapping m : ALL.values()) {
            if (((KeyMappingMixin) (Object) m).key.equals(pressed)) m.setDown(down);
        }
        ci.cancel();
    }
}

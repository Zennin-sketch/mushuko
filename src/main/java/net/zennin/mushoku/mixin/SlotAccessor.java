package net.zennin.mushoku.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para ubicar la ranura del carcaj en el inventario del creativo. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable @Accessor("x") void remushoku$setX(int x);
    @Mutable @Accessor("y") void remushoku$setY(int y);
}

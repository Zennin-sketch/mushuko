package net.zennin.mushoku.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para la prueba visual: que el maniqui "use" un item (tensar un arco) sin servidor. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("setLivingEntityFlag")
    void remushoku$setFlag(int flag, boolean value);
}

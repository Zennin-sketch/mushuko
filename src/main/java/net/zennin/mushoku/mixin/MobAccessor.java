package net.zennin.mushoku.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Acceso a los objetivos de los mobs (para que los hostiles ataquen al cuerpo que deja la Clarividencia). */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("targetSelector")
    GoalSelector mushoku$targetSelector();
}

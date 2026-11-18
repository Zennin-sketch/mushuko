/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para el reinicio del OneBlock: volver a poner el portal de salida y una dragona nueva. */
@Mixin(EndDragonFight.class)
public interface EndDragonFightAccessor {
    @Accessor("dragonKilled") void obSetDragonKilled(boolean value);
    @Accessor("previouslyKilled") void obSetPreviouslyKilled(boolean value);
    @Invoker("spawnExitPortal") void obSpawnExitPortal(boolean active);
    @Invoker("resetSpikeCrystals") void obResetSpikeCrystals();
    @Invoker("createNewDragon") EnderDragon obCreateNewDragon();
}

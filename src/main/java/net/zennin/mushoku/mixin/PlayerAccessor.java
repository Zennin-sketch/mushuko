package net.zennin.mushoku.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para la prueba visual: mostrar todas las capas de la skin del maniqui. */
@Mixin(Player.class)
public interface PlayerAccessor {
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> remushoku$customisation() {
        throw new AssertionError();
    }
}

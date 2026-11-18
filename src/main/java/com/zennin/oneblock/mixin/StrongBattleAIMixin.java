/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La IA de Cobblemon para NPC elige bien los movimientos pero nunca megaevoluciona: no mira la
 * opción de mega que le ofrece el combate. Esto se la activa: si el Pokémon del NPC puede
 * megaevolucionar en este turno y la IA eligió atacar, el ataque va con megaevolución.
 * Lo usa la Mega Altaria del líder 4.
 */
@Mixin(value = StrongBattleAI.class, remap = false)
public class StrongBattleAIMixin {
    @Inject(method = "choose", at = @At("RETURN"), remap = false)
    private void oneblock$megaevolucionar(ActiveBattlePokemon activeBattlePokemon, PokemonBattle battle, BattleSide aiSide,
                                          ShowdownMoveset moveset, boolean forceSwitch,
                                          CallbackInfoReturnable<ShowdownActionResponse> cir) {
        if (forceSwitch || moveset == null || !moveset.getCanMegaEvo()) return;
        if (cir.getReturnValue() instanceof MoveActionResponse respuesta && respuesta.getGimmickID() == null) {
            respuesta.setGimmickID(ShowdownMoveset.Gimmick.MEGA_EVOLUTION.getId());
        }
    }
}

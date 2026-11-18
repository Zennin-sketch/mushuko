/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.entity.npc.NPCBattleActor
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.util.party
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.ItemStack

/**
 * Reglas de los combates contra los líderes del OneBlock (NPCs oneblock:liderN):
 *  - Líder Espejo (lider1): solo con el equipo completo, 6 Pokémon.
 *  - Sin objetos de Cobblemon durante el combate (pociones, revivir, X Ataque...).
 *  - Al terminar, gane quien gane, el equipo del jugador se cura entero.
 */
object ReglasLideres {
    private fun lider(battle: PokemonBattle): NPCEntity? =
        battle.actors.filterIsInstance<NPCBattleActor>().map { it.npc }
            .firstOrNull { it.npc.id.namespace == "oneblock" && it.npc.id.path.startsWith("lider") }

    private fun enCombateConLider(player: ServerPlayer): Boolean {
        val battle = BattleRegistry.getBattleByParticipatingPlayer(player) ?: return false
        return lider(battle) != null
    }

    private fun esObjetoCobblemon(stack: ItemStack): Boolean =
        !stack.isEmpty && BuiltInRegistries.ITEM.getKey(stack.item).namespace == "cobblemon"

    private fun avisoObjetos(player: ServerPlayer) {
        player.displayClientMessage(
            Component.literal("Contra los líderes no se pueden usar objetos.").withStyle(ChatFormatting.RED), true)
    }

    fun registrar() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL) { evento ->
            val npc = lider(evento.battle) ?: return@subscribe
            if (npc.npc.id.path != "lider1") return@subscribe
            for (player in evento.battle.players) {
                if (player.party().count() < 6) {
                    val mensaje = Component.literal("El Líder Espejo solo acepta equipos completos: necesitás 6 Pokémon.")
                        .withStyle(ChatFormatting.GOLD)
                    player.sendSystemMessage(mensaje)
                    evento.reason = mensaje
                    evento.cancel()
                    return@subscribe
                }
            }
        }

        UseItemCallback.EVENT.register { player, level, hand ->
            val stack = player.getItemInHand(hand)
            if (!level.isClientSide && player is ServerPlayer && esObjetoCobblemon(stack) && enCombateConLider(player)) {
                avisoObjetos(player)
                InteractionResultHolder.fail(stack)
            } else {
                InteractionResultHolder.pass(stack)
            }
        }
        UseEntityCallback.EVENT.register { player, level, hand, _, _ ->
            val stack = player.getItemInHand(hand)
            if (!level.isClientSide && player is ServerPlayer && esObjetoCobblemon(stack) && enCombateConLider(player)) {
                avisoObjetos(player)
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }

        CobblemonEvents.BATTLE_ENDED.subscribe(Priority.NORMAL) { evento ->
            if (lider(evento.battle) == null) return@subscribe
            for (player in evento.battle.players) {
                // un tick despues: el combate todavia esta volcando la vida final a los Pokemon
                player.server.execute {
                    player.party().heal()
                    player.displayClientMessage(Component.literal("Tu equipo se curó.").withStyle(ChatFormatting.GREEN), true)
                }
            }
        }
    }
}

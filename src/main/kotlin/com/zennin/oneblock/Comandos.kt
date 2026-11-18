/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.zennin.oneblock.mixin.EndDragonFightAccessor
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

object Comandos {
    /** Nivel de cada lider: el tope de su fase. En la vuelta rapida, el equipo del jugador pelea a este nivel. */
    private val NIVEL_LIDER = mapOf(1 to 15, 2 to 27, 3 to 37, 4 to 47)

    fun registrar() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            // /oneblocklider <jugador> <n>: lo llama el dialogo del lider al tocar "¡Combatir!"
            dispatcher.register(
                Commands.literal("oneblocklider").requires { it.hasPermission(2) }
                    .then(Commands.argument("jugador", StringArgumentType.word())
                        .then(Commands.argument("lider", IntegerArgumentType.integer(1, 4))
                            .executes { ctx ->
                                val server = ctx.source.server
                                val player = server.playerList.getPlayerByName(StringArgumentType.getString(ctx, "jugador"))
                                    ?: return@executes 0
                                val n = IntegerArgumentType.getInteger(ctx, "lider")
                                val npc = player.serverLevel()
                                    .getEntitiesOfClass(NPCEntity::class.java, player.boundingBox.inflate(24.0)) { it.tags.contains("ob_lider$n") }
                                    .minByOrNull { it.distanceToSqr(player) } ?: return@executes 0
                                val vueltaRapida = (Reglas.marcador("#ng") ?: 0) >= 1
                                val base = if (n == 2) BattleFormat.GEN_9_DOUBLES else BattleFormat.GEN_9_SINGLES
                                // En la vuelta rapida el equipo del jugador pelea al nivel del lider.
                                // Van copias (cloneParties): los Pokemon de verdad siguen en 100.
                                val formato = base.copy(adjustLevel = if (vueltaRapida) (NIVEL_LIDER[n] ?: -1) else -1)
                                BattleBuilder.pvn(player = player, npcEntity = npc, battleFormat = formato, cloneParties = vueltaRapida)
                                    .ifErrored { it.sendTo(player) { t -> t.withStyle(ChatFormatting.RED) } }
                                1
                            })))

            // /oneblockreinicio end: portal de salida y dragona nueva (reinicio con el huevo)
            dispatcher.register(
                Commands.literal("oneblockreinicio").requires { it.hasPermission(2) }
                    .then(Commands.literal("end").executes { ctx ->
                        val end = ctx.source.server.getLevel(Level.END)
                        val pelea = end?.dragonFight
                        if (pelea == null) {
                            ctx.source.sendFailure(Component.literal("No hay pelea con la dragona en este End."))
                            return@executes 0
                        }
                        try {
                            val a = pelea as EndDragonFightAccessor
                            a.obSpawnExitPortal(false)
                            a.obSetDragonKilled(false)
                            a.obSetPreviouslyKilled(false)
                            a.obResetSpikeCrystals()
                            a.obCreateNewDragon()
                            1
                        } catch (e: Throwable) {
                            OneBlockLideres.LOGGER.error("No se pudo reiniciar el End", e)
                            0
                        }
                    }))
        }
    }
}

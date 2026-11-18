/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.npc.NPCPartyProvider
import com.cobblemon.mod.common.util.party
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory

object OneBlockLideres : ModInitializer {
    val LOGGER = LoggerFactory.getLogger("oneblock_lideres")

    override fun onInitialize() {
        // Tipo de equipo nuevo para los NPC de Cobblemon: "oneblock:espejo" (lider 1).
        NPCPartyProvider.types[EspejoPartyProvider.TYPE] = ::EspejoPartyProvider

        ServerLifecycleEvents.SERVER_STARTED.register { Reglas.server = it }
        ServerLifecycleEvents.SERVER_STOPPED.register { Reglas.server = null }

        // Banco de movimientos completo: al elegir el inicial, capturar, recibir, evolucionar y
        // sacar de la Pokébola; y al entrar al mundo, para los Pokémon que ya tenía.
        CobblemonEvents.STARTER_CHOSEN.subscribe(Priority.NORMAL) { Banco.completar(it.pokemon) }
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL) { Banco.completar(it.pokemon) }
        CobblemonEvents.POKEMON_GAINED.subscribe(Priority.NORMAL) { Banco.completar(it.pokemon) }
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL) { Banco.completar(it.pokemon) }
        CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.NORMAL) { Banco.completar(it.pokemon) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            handler.player.party().forEach { Banco.completar(it) }
        }

        // Tope de nivel por fase: no se gana experiencia por encima del tope.
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.NORMAL) { evento ->
            val tope = Reglas.topeDeNivel() ?: return@subscribe
            val pokemon = evento.pokemon
            if (pokemon.level >= tope) {
                evento.cancel()
                return@subscribe
            }
            val hastaElTope = pokemon.getExperienceToLevel(tope)
            if (evento.experience > hastaElTope) evento.experience = hastaElTope
        }

        // Zonas sin spawn (islas 1, 2 y 3): ahí no aparecen Pokémon salvajes.
        CobblemonEvents.ENTITY_SPAWN.subscribe(Priority.NORMAL) { evento ->
            val lugar = evento.spawnablePosition
            if (Reglas.enZonaSinSpawn(lugar.world, lugar.position)) evento.cancel()
        }

        LOGGER.info("OneBlock Cobblemon - Líderes, por Zennin: cargado.")
    }
}

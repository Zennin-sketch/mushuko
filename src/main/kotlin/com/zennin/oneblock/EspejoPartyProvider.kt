/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.api.npc.NPCPartyProvider
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.util.party
import com.cobblemon.mod.common.util.toProperties
import com.google.gson.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Equipo espejo: cada vez que alguien desafía al NPC, el NPC arma su equipo copiando el del
 * jugador: misma especie, forma, nivel, movimientos, naturaleza, habilidad, IVs, EVs, objeto
 * y shiny. Son copias: el equipo del jugador no se toca.
 *
 * isStatic = false es lo que hace que Cobblemon lo vuelva a pedir en cada desafío
 * (NPCEntity.getPartyForChallenge) en vez de armarlo una sola vez al aparecer el NPC.
 *
 * Uso en data/<ns>/npcs/<npc>.json:  "party": { "type": "oneblock:espejo" }
 */
class EspejoPartyProvider : NPCPartyProvider {
    companion object {
        val TYPE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("oneblock", "espejo")
    }

    override val type: ResourceLocation = TYPE
    override val isStatic: Boolean = false

    override fun loadFromJSON(json: JsonElement) {
        // No tiene opciones: el equipo sale siempre del jugador.
    }

    override fun provide(npc: NPCEntity, level: Int, players: List<ServerPlayer>): NPCPartyStore {
        val party = NPCPartyStore(npc)
        val player = players.firstOrNull()
        var copiados = 0
        if (player != null) {
            val registros = player.server.registryAccess()
            for (pokemon in player.party()) {
                try {
                    val copia = pokemon.clone(true, registros)
                    // El lider siempre arranca con el equipo sano, aunque el jugador llegue golpeado.
                    copia.heal()
                    if (party.add(copia)) copiados++
                } catch (e: Exception) {
                    OneBlockLideres.LOGGER.error("No se pudo copiar a ${pokemon.species.name} de ${player.name.string}", e)
                }
            }
        }
        if (copiados == 0) {
            // Nunca deberia pasar (para pelear hay que tener al menos un Pokemon), pero un NPC sin
            // equipo hace fallar el combate entero. Mejor un Magikarp que un error.
            party.add("magikarp level=5".toProperties().create())
        }
        return party
    }
}

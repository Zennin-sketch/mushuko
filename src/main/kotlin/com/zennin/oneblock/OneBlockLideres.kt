/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.api.npc.NPCPartyProvider
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object OneBlockLideres : ModInitializer {
    val LOGGER = LoggerFactory.getLogger("oneblock_lideres")

    override fun onInitialize() {
        // Tipo de equipo nuevo para los NPC de Cobblemon: "oneblock:espejo".
        // Se registra antes de que el servidor cargue los datapacks, que es cuando Cobblemon
        // lee data/oneblock/npcs/lider1.json y busca este tipo por nombre.
        NPCPartyProvider.types[EspejoPartyProvider.TYPE] = ::EspejoPartyProvider
        LOGGER.info("OneBlock Cobblemon - Líderes, por Zennin. Equipo espejo registrado.")
    }
}

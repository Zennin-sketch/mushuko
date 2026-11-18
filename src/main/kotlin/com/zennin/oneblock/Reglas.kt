/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.scores.ScoreHolder

/**
 * Lo que el mod lee del datapack, que es el que manda:
 *  - el tope de nivel de la fase: marcador ob.data, jugador "#lvlcap" (lo pone config/phaseN y
 *    pasa a 100 al completar el OneBlock). Si no existe, no hay tope.
 *  - las zonas sin spawn: storage oneblock:zonas, lista "lista" (la arma lider/preparar).
 */
object Reglas {
    var server: MinecraftServer? = null
    private val ZONAS = ResourceLocation.fromNamespaceAndPath("oneblock", "zonas")

    fun marcador(nombre: String): Int? {
        val server = server ?: return null
        val marcador = server.scoreboard
        val objetivo = marcador.getObjective("ob.data") ?: return null
        val info = marcador.getPlayerScoreInfo(ScoreHolder.forNameOnly(nombre), objetivo) ?: return null
        return info.value()
    }

    fun topeDeNivel(): Int? {
        val tope = marcador("#lvlcap") ?: return null
        return if (tope in 1..99) tope else null
    }

    fun enZonaSinSpawn(level: ServerLevel, pos: BlockPos): Boolean {
        val server = server ?: return false
        val lista = server.commandStorage.get(ZONAS).getList("lista", Tag.TAG_COMPOUND.toInt())
        val dimension = level.dimension().location().toString()
        for (i in 0 until lista.size) {
            val z = lista.getCompound(i)
            if (z.getString("d") != dimension) continue
            if (pos.x in z.getInt("x1")..z.getInt("x2") &&
                pos.y in z.getInt("y1")..z.getInt("y2") &&
                pos.z in z.getInt("z1")..z.getInt("z2")) return true
        }
        return false
    }
}

/*
 * OneBlock Cobblemon - Líderes
 * Copyright (c) 2026 Zennin (https://www.youtube.com/@Zorikzennin). Todos los derechos reservados.
 * Ver LICENCIA.txt. Prohibida su copia, redistribución o modificación sin permiso escrito.
 */
package com.zennin.oneblock

import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Pokemon

/**
 * Banco de movimientos: cada Pokémon del jugador tiene disponibles, para cambiar en la pantalla
 * de resumen, TODOS los movimientos de MT, tutor y huevo que puede aprender legalmente: los de
 * su forma y los de todas sus preevoluciones (un Breloom tiene los de Shroomish, como Falso
 * Tortazo). Los de nivel no: esos se aprenden subiendo de nivel.
 *
 * Solo agrega lo que falta. Nunca borra: lo que el Pokémon ya aprendió por nivel queda.
 */
object Banco {
    fun legales(form: FormData): Set<MoveTemplate> {
        val movimientos = linkedSetOf<MoveTemplate>()
        val vistas = mutableSetOf<FormData>()
        var actual: FormData? = form
        while (actual != null && vistas.add(actual)) {
            val aprende = actual.moves
            movimientos.addAll(aprende.tmMoves)
            movimientos.addAll(aprende.tutorMoves)
            movimientos.addAll(aprende.eggMoves)
            actual = actual.preEvolution?.form
        }
        return movimientos
    }

    fun completar(pokemon: Pokemon) {
        try {
            val conocidos = HashSet<MoveTemplate>()
            pokemon.moveSet.getMoves().forEach { conocidos.add(it.template) }
            pokemon.benchedMoves.forEach { conocidos.add(it.moveTemplate) }
            val faltan = legales(pokemon.form).filter { it !in conocidos }
            if (faltan.isEmpty()) return
            // de una sola vez: un solo aviso al cliente (de a uno el paquete se arma mientras la
            // lista cambia y el juego echa al jugador)
            pokemon.benchedMoves.addAll(faltan.map { BenchedMove(it, 0) })
        } catch (e: Exception) {
            OneBlockLideres.LOGGER.error("No se pudo completar el banco de ${pokemon.species.name}", e)
        }
    }
}

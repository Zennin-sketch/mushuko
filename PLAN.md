# Mushoku Tensei — Port a Fabric 1.21.1

Port del mod "MushokuRework" (remushoku) de Wiwar/adamr, Forge 1.20.1 (MCreator) → Fabric 1.21.1.
Licencia del original: Academic Free License 3.0 (permite obras derivadas).
Se mantiene el id `remushoku` para que las texturas, modelos y datos funcionen sin renombrar.

## Tamaño del original
- 54 ítems, 22 bloques, 47 entidades (11 con GeckoLib), 19 efectos
- 140 menús (GUIs), 155 mensajes de red, 784 procedimientos
- 104 estructuras, 21 tablas de botín, 34 recetas, worldgen (6 features, 4 estructuras)

## Fases

### Fase 1 — Base (HECHA, falta compilar)
- [x] Proyecto Gradle (Loom, mappings oficiales de Mojang, Java 21) + compilación automática en GitHub Actions
- [x] Recursos migrados: texturas, modelos, sonidos, idiomas (español), estructuras
- [x] Datos migrados al formato 1.21 (carpetas `recipe`, `loot_table`, `structure`, tags en singular; recetas con `id`)
- [x] Ítems: piedras mágicas, libros, monedas, llaves, varios
- [x] Armas con las stats exactas del original (13)
- [x] Pestaña creativa
- [x] Maná del jugador (guardado, se conserva al morir) con la regeneración original
- [x] Barra de maná medieval en el HUD

### Fase 2 — Clases y lanzamiento de hechizos (HECHA, en prueba)
- [x] Elección de clase (Mago / Espadachín) al entrar, y orbe de cambio de clase
- [x] Teclas: conjurar (Shift+Ctrl), flechas WASD, activar (Tab) — configurables en Controles
- [x] Las 56 combinaciones del original; costo de maná y maná máximo ganado por nivel
- [x] Libros elementales + piedra en la otra mano para aprender niveles 2-6
- [x] Modo sin elemento (Tab sin hechizo en cola)
- [x] 16 hechizos Principiante: Bola de Agua, Escudo de Agua, Flecha de Agua, Golpe de Hielo, Hoja de Hielo,
      Bola de Fuego, Mano Ardiente, Suelo Ardiente, Bala de Roca, Nube de Arena, Excavar, Ráfaga,
      Caída Suave, Pies Veloces, Curación, Crecimiento, Escudo Mágico
- [x] Comandos de prueba: /mushoku mana, /mushoku maxmana, /mushoku elemento, /mushoku clase

### Fase 3 — Hechizos completos y círculos mágicos
- Los ~60 hechizos (Principiante → Imperial)
- Tiza, círculos mágicos, mejora de piedras, pergaminos, báculos con piedra imbuida
- Modo sin elemento, curación y magia variada

### Fase 4 — Espadachín
- Puntos de habilidad, estilos Dios Espada / Agua / Norte
- Habilidades activas y pasivas, Touki, 3 ranuras asignables

### Fase 5 — Mundo y NPCs
- Bloques (mena mágica, tablón de trabajos, estantes, puertas, etc.)
- Entidades: bandidos, tenderos, recepcionista, Kalman III, dracos, jefes (GeckoLib para Fabric)
- Gremio: trabajos de rango F→A, tiendas, monedas
- Mazmorras y estructuras con su worldgen (los 12 "biome modifiers" de Forge se pasan a código)

### Fase 6 — Ojos demoníacos y extras
- 5 ojos demoníacos, armadura Mk2, objetos especiales, libro guía
- Capas 3D (3D Skin Layers) y personajes extra (Eris, Sylphiette)

## Cómo se compila
1. Subir este proyecto a un repositorio de GitHub.
2. GitHub Actions lo compila solo en cada cambio ("Compilar mod").
3. El .jar queda en Actions → la última ejecución → Artifacts → `mushoku-tensei-fabric`.

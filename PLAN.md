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

### Fase 3A — Hechizos completos (HECHA, en prueba)
- [x] Los 40 hechizos de Intermedio a Imperial (agua, fuego, tierra, viento)
- [x] Curación avanzada (X-Curación, Banquete, Curación Sagrada) y Perturbar Magia
- [x] Proyectil mágico con tamaño, empuje, elevación y explosión; tareas diferidas (tormentas, tornados)

### Fase 3B — Círculos mágicos (HECHA, en prueba)
- [x] Tiza (32 usos): clic derecho al suelo dibuja un círculo mágico
- [x] Inscribir: formar el hechizo con flechas (sin lanzarlo) + clic derecho al círculo con la mano vacía
- [x] Trampa: piedra del mismo nivel → se activa al pisarla (efecto según elemento y nivel; los de curación curan a quien los pisa)
- [x] Piedra imbuida: agachado + piedra del mismo nivel
- [x] Pergaminos: papel o pergamino en blanco sobre el círculo (cuesta el maná del hechizo), se lanzan gratis una vez
- [x] Mejora de piedras: 4 piedras iguales sobre un círculo vacío → 1 del nivel siguiente
- [x] Báculos (Varita, Migurd, Dragón, Areos, Aqua Heartia) con piedra imbuida y descuento de maná
- Pendiente: el minijuego original de colocar flechas de tiza (simplificado por ahora)

### Fase 4 — Espadachín (HECHA, en prueba)
- [x] Afinidad al elegir Espadachín (Dios Espada +3 daño, Dios Agua +2 corazones, Dios Norte +20% velocidad)
- [x] Puntos de habilidad: 1 por monstruo hostil, 5 por bandido (los bandidos llegan en la Fase 5)
- [x] Menú con V: los 3 árboles con los costos originales; fuera de tu afinidad solo hasta Avanzado
- [x] 23 habilidades: activas (Z / X / C), pasivas y encendibles
- [x] Touki por niveles (100 puntos, +100 maná máximo cada uno); encendido da daño, vida y velocidad y gasta maná
- [x] Magia del Dios Norte (hechizos hasta Intermedio), Bomba Adhesiva con Objetos
- [x] Comando de prueba: /mushoku puntos <n>

### Fase 5 — Mundo y NPCs (en partes)
#### 5A — Bloques y generación del mundo (HECHA, en prueba)
- [x] Mena mágica, estante (9 espacios), tablón de trabajos, puertas con cerradura, bloques de mazmorra
- [x] Llave Musgosa / de Hielo: abre la puerta y construye la sala del jefe (igual que el original)
- [x] Feature de estructuras + los 6 modificadores de bioma de mundo (mena, torre de fuego, árbol de tierra,
      casa de teletransporte, hielo del libro de agua, isla del cielo)
- [x] Ciudades, ciudad de arena, mazmorra musgosa y de hielo
- [x] Corregido: el grupo "bighouse" que faltaba en el original (las casas grandes no aparecían)
#### 5B — NPCs humanos (HECHA, en prueba)
- [x] Bandidos Dios Espada / Agua / Norte y Mago con sus stats, técnicas y aparición natural del original
- [x] Herrero, Tendero, Vendedor de Magia y Recepcionista (aparecen en las ciudades; tiendas en 5D)
- [x] Kalman III (jefe, 1024 de vida, barra de jefe, Touki por fases) con su pergamino de invocación
- [x] Huevos de aparición
#### 5C — Criaturas GeckoLib (HECHA, en prueba)
- [x] GeckoLib 4.7.2 incluido dentro del .jar (no hace falta instalarlo aparte)
- [x] Perro de Asalto, Rata Mugrienta (aparición natural), Draca de Nieve, Espectro Letal con sus animaciones
- [x] Jefes: Tortuga Gigante (mazmorra musgosa) y Draca de Nieve Madre (mazmorra de hielo) con barra de jefe
- [x] Invocador de jefe en la sala; botín de jefes según la guía del original
#### 5D — Gremio, tiendas y monedas (HECHA, en prueba)
- [x] Tablón de Trabajos y Recepcionista: rango F→S con la XP del original, 18 trabajos con objetivos y recompensas originales
- [x] Trabajos de matar, entregar objetos y construir gólems; subir de rango y abandonar
- [x] Herrero, Tendero y Vendedor de Magia con los precios del original; comprar y vender
- [x] Pago con cualquier combinación de monedas y cambio automático

### Fase 6 — Ojos demoníacos y extras
#### 6A (HECHA, en prueba)
- [x] Menús medievales (tablón con pergaminos, tiendas en fichas, libros)
- [x] 5 ojos demoníacos en 2 ranuras (G / H): Clarividencia, Absorción, Identificación, Vacío Absoluto, Poder Mágico
- [x] Armadura Mágica Mk 2 (modelo 3D GeckoLib) con bonus de set completo
- [x] Roca, Bolsa de Especias, Tótem y Capa de Intercambio, Garras de escalada (Objetos del Dios Norte)
- [x] Último Suspiro (+50% magia) y Aqua Heartia (+20%); Tomo de Conjuro Silencioso
- [x] Libro Guía (textos originales + capítulos nuevos) y Libro de Hechizos
#### 6B — Personajes extra (Eris, Sylphiette) y capas 3D de skin

## Cómo se compila
1. Subir este proyecto a un repositorio de GitHub.
2. GitHub Actions lo compila solo en cada cambio ("Compilar mod").
3. El .jar queda en Actions → la última ejecución → Artifacts → `mushoku-tensei-fabric`.

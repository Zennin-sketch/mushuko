"""Efectos de hechizos que no son la bala principal: nubes de tormenta, pinchos, cristales, explosiones, escudos,
y dos proyectiles nuevos (flecha de agua y tajo de fuego). Mismo formato que spell_models.py.

'preview' le dice a la pagina como mostrarlo: tipo, vida en ticks, escala del efecto, altura (bloques) y camara.
'outro' = [ticks, hundirse_px, achicarse 0..1]: el final del efecto (se apaga, se hunde y/o se achica)."""
import math, random
from spell_models import box, seg, group, emitter, lerp_hex

def fx(name, spells, groups, life, scale=1.0, height=0.0, cam=4.0, outro=None):
    d = dict(name=name, spells=spells, groups=groups, preview=dict(kind='fx', life=life, scale=scale, height=height, cam=cam))
    if outro:
        d['outro'] = outro
    return d

def projectile(name, spells, groups):
    return dict(name=name, spells=spells, groups=groups, preview=dict(kind='projectile'))

# ================================================================ Nubes de tormenta
def cloud_puffs(arms, steps, r0, r1, turn, rnd, size0, size1):
    """Bocanadas de nube a lo largo de brazos en espiral, ordenadas de afuera hacia adentro (asi se forman en espiral)."""
    puffs = []
    for a in range(arms):
        for k in range(steps):
            u = k / (steps - 1)
            ang = math.radians(a * 360 / arms + u * turn)
            r = r0 + (r1 - r0) * u
            w = size0 + (size1 - size0) * u + rnd.uniform(-1, 1)
            h = w * rnd.uniform(0.65, 0.85)
            puffs.append((u, (r * math.cos(ang), rnd.uniform(-0.8, 0.8) - u * 1.2, r * math.sin(ang)), (w, h, w * rnd.uniform(0.8, 1.1)),
                          rnd.uniform(0, 90)))
    puffs.sort(key=lambda p: -p[0])
    return puffs

def storm_cloud(scale, life, form, strikes, arms, steps, radius, rain=None, name='', spells=(), height=12):
    rnd = random.Random(11 + arms)
    puffs = cloud_puffs(arms, steps, 3, radius, 300, rnd, 10, 5)
    # Nucleo: masa central mas alta (la torre del cumulonimbo)
    core = [(0, (rnd.uniform(-3, 3), rnd.uniform(0, 4), rnd.uniform(-3, 3)), (rnd.uniform(7, 10), rnd.uniform(5, 8), rnd.uniform(7, 10)), rnd.uniform(0, 90))
            for _ in range(6)]
    under = [box(at, sz, lerp_hex('4f5563', '6d7483', rnd.random()), 0.94, rot=(0, ry, 0)) for _, at, sz, ry in puffs + core]
    top = [box((at[0], at[1] + sz[1] * 0.55, at[2]), (sz[0] * 0.8, sz[1] * 0.8, sz[2] * 0.8), lerp_hex('9aa0ad', 'c3c8d1', rnd.random()), 0.92, rot=(0, ry + 20, 0))
           for _, at, sz, ry in puffs + core]
    swirl = [0, form, 420, 1.9]
    stagger = [0, form * 0.8]
    groups = [
        group('cloud', 'translucent', under, spin=(0, 1.2, 0), swirl=swirl, stagger=stagger),
        group('cloud', 'translucent', top, spin=(0, 1.2, 0), swirl=swirl, stagger=stagger),
    ]
    flashes = [box((rnd.uniform(-radius * 0.5, radius * 0.5), rnd.uniform(-1, 2), rnd.uniform(-radius * 0.5, radius * 0.5)),
                   (rnd.uniform(5, 9), 3, rnd.uniform(5, 9)), 'cfe2ff', 0.55) for _ in range(5)]
    groups.append(group('cloud', 'emissive', flashes, glow=True, flicker=0.6, show=[[s - 2, s + 6] for s in strikes]))
    # Remolino de viento que la va armando
    wisps = []
    for a in range(arms):
        for k in range(16):
            u0, u1 = k / 16, (k + 1) / 16
            def pt(u):
                ang = math.radians(a * 360 / arms + 40 + u * 330)
                r = radius * 1.25 - u * radius * 0.9
                return (r * math.cos(ang), -1 + u, r * math.sin(ang))
            wisps.append(seg(pt(u0), pt(u1), 0.8, 0.6, 'c9ced8', 0.18 + 0.3 * math.sin(math.pi * u0)))
    groups.append(group('cloud', 'translucent', wisps, glow=True, spin=(0, 9, 0), show=[[0, form + 6]]))
    if rain:
        start, end, fall = rain
        groups.append(group('water', 'translucent', [], glow=True, show=[[start, end]],
                            emit=emitter(120, 8, (radius * 0.85, 0.5, radius * 0.85), (1.0, 1.0), ('b4cce4', 'dce9f6'), alpha=0.7,
                                         rise=-fall / 8, center=(0, -3, 0), shape=(0.18, 2.6, 0.18))))
    d = fx(name, list(spells), groups, life, scale=scale, height=height, cam=height * 1.9, outro=[life // 5, 0, 0.25])
    d['preview']['strikes'] = list(strikes)
    return d

# Momentos clave (ticks) que tambien usa el codigo del hechizo (SpellFxEntity / AdvancedSpells)
CUMULO_FORM, CUMULO_STRIKES, CUMULO_RAIN_END, CUMULO_LIFE = 60, (60, 80, 100, 120), 140, 175
CUMULO_SCALE, CUMULO_RADIUS, CUMULO_HEIGHT = 12, 48, 28   # radio 48 px * 12 / 16 = 36 bloques; a 28 bloques del suelo
STORM_FORM, STORM_LIFE = 14, 45

def cumulonimbus():
    """Cumulonimbus grande: el viento entra en espiral, se arma la base oscura de afuera hacia adentro, sube la torre
    del centro y se abre el yunque arriba. Antes de cada rayo destella por dentro; llueve primero suave y despues
    fuerte; al final se deshace abriendose."""
    rnd = random.Random(1907)
    F, R, life = CUMULO_FORM, CUMULO_RADIUS, CUMULO_LIFE
    fall = CUMULO_HEIGHT * 16 / CUMULO_SCALE
    # ---- base: bocanadas en 5 brazos de espiral, de afuera hacia adentro
    puffs = cloud_puffs(5, 22, 6, R, 280, rnd, 17, 9)
    base_dark = [box(at, sz, lerp_hex('3e4350', '5d6473', rnd.random()), 0.95, rot=(0, ry, 0)) for _, at, sz, ry in puffs]
    base_mid = [box((at[0], at[1] + sz[1] * 0.5, at[2]), (sz[0] * 0.85, sz[1] * 0.8, sz[2] * 0.85), lerp_hex('7c8392', '9ba1ad', rnd.random()),
                    0.93, rot=(0, ry + 25, 0)) for _, at, sz, ry in puffs]
    # bordes deshilachados de la base (mas claros y transparentes)
    fringe = []
    for k in range(40):
        ang = rnd.uniform(0, 2 * math.pi)
        r = R * rnd.uniform(0.92, 1.12)
        w = rnd.uniform(6, 11)
        fringe.append(box((r * math.cos(ang), rnd.uniform(-1, 2), r * math.sin(ang)), (w, w * 0.5, w), 'a7adb8', 0.5, rot=(0, rnd.uniform(0, 90), 0)))
    swirl = [0, F, 260, 1.5]
    groups = [
        group('cloud', 'translucent', base_dark, spin=(0, 0.5, 0), swirl=swirl, stagger=[0, F * 0.7]),
        group('cloud', 'translucent', base_mid, spin=(0, 0.5, 0), swirl=swirl, stagger=[6, F * 0.8]),
        group('cloud', 'translucent', fringe, spin=(0, 0.35, 0), swirl=[0, F, 200, 1.4], stagger=[F * 0.3, F * 0.9], pulse=(0.02, 37)),
    ]
    # ---- torre central que sube y se hincha
    tower = []
    for lvl in range(7):
        y = 3 + lvl * 4.5
        w = 22 - lvl * 1.6
        for _ in range(4):
            ox, oz = rnd.uniform(-5, 5), rnd.uniform(-5, 5)
            tower.append(box((ox, y, oz), (w * rnd.uniform(0.7, 1.0), rnd.uniform(6, 8), w * rnd.uniform(0.7, 1.0)),
                             lerp_hex('a4aab6', 'd3d7de', lvl / 6), 0.94, rot=(0, rnd.uniform(0, 90), 0)))
    groups.append(group('cloud', 'translucent', tower, spin=(0, 0.8, 0), rise=[F * 0.25, F, 22], stagger=[F * 0.25, F * 0.95]))
    # ---- yunque: capa ancha y chata arriba, se abre al final de la formacion
    anvil = [box(at, (sz[0] * 1.3, sz[1] * 0.45, sz[2] * 1.3), lerp_hex('c6cad3', 'e3e6eb', rnd.random()), 0.9, rot=(0, ry, 0))
             for _, at, sz, ry in cloud_puffs(4, 12, 4, R * 0.75, 200, rnd, 16, 10)]
    for bx in anvil:
        bx['at'][1] = round(bx['at'][1] + 36, 3)
    groups.append(group('cloud', 'translucent', anvil, spin=(0, -0.3, 0), offset=[0, 0, 0], grow=[F * 0.7, F + 12],
                        show=[[F * 0.7, 400]]))
    # ---- destellos por dentro: anuncian cada rayo (antes) y quedan vibrando un instante (despues)
    for i, st in enumerate(CUMULO_STRIKES):
        fl = [box((rnd.uniform(-R * 0.6, R * 0.6), rnd.uniform(0, 4), rnd.uniform(-R * 0.6, R * 0.6)),
                  (rnd.uniform(8, 16), 4, rnd.uniform(8, 16)), 'd8e6ff', 0.5) for _ in range(5)]
        groups.append(group('cloud', 'emissive', fl, glow=True, flicker=0.8, show=[[st - 7, st - 4], [st - 2, st + 5]]))
    ambient = [box((rnd.uniform(-R * 0.7, R * 0.7), rnd.uniform(0, 6), rnd.uniform(-R * 0.7, R * 0.7)),
                   (rnd.uniform(6, 12), 3, rnd.uniform(6, 12)), 'b9cdf2', 0.35) for _ in range(8)]
    groups.append(group('cloud', 'emissive', ambient, glow=True, flicker=0.9,
                        show=[[F - 20, F - 17], [F + 10, F + 12], [F + 31, F + 33], [F + 49, F + 52], [F + 70, F + 72]]))
    # ---- viento que la arma: espirales que se cierran desde afuera
    wisps = []
    for a in range(5):
        for k in range(22):
            u0, u1 = k / 22, (k + 1) / 22
            def pt(u, a=a):
                ang = math.radians(a * 72 + 40 + u * 340)
                r = R * 1.35 - u * R * 1.05
                return (r * math.cos(ang), -2 + u * 3, r * math.sin(ang))
            wisps.append(seg(pt(u0), pt(u1), 1.2, 0.8, 'c9ced8', 0.15 + 0.3 * math.sin(math.pi * u0)))
    groups.append(group('cloud', 'translucent', wisps, glow=True, spin=(0, 6, 0), show=[[0, F + 4]], stagger=[0, F * 0.4]))
    # niebla que entra hacia el centro mientras se forma
    groups.append(group('cloud', 'translucent', [], show=[[0, F - 4]],
                        emit=emitter(90, 26, (R * 1.2, 3, R * 1.2), (5.0, 2.0), ('8d94a2', 'c3c8d1'), alpha=0.45, outward=-1.4)))
    # ---- lluvia: primero unas gotas, despues el aguacero; cortina gris debajo mientras llueve fuerte
    drop = dict(rise=-fall / 9, center=(0, -3, 0), shape=(0.18, 3.0, 0.18))
    groups.append(group('water', 'translucent', [], glow=True, show=[[F - 6, F + 14]],
                        emit=emitter(140, 9, (R * 0.8, 0.5, R * 0.8), (1.0, 1.0), ('b4cce4', 'dce9f6'), alpha=0.6, **drop)))
    groups.append(group('water', 'translucent', [], glow=True, show=[[F + 8, CUMULO_RAIN_END]],
                        emit=emitter(520, 9, (R * 0.85, 0.5, R * 0.85), (1.0, 1.0), ('a9c2dc', 'e2edf8'), alpha=0.75, **drop)))
    curtain = [box((rnd.uniform(-R * 0.6, R * 0.6), -fall / 2, rnd.uniform(-R * 0.6, R * 0.6)),
                   (rnd.uniform(14, 24), fall, rnd.uniform(14, 24)), '9aa7b8', 0.07, rot=(0, rnd.uniform(0, 90), 0)) for _ in range(9)]
    groups.append(group('water', 'translucent', curtain, show=[[F + 12, CUMULO_RAIN_END - 4]], pulse=(0.04, 23)))
    # al final se deshace: se apaga mientras se abre un 20%
    d = fx('Cumulonimbus', ['cumulonimbus'], groups, life, scale=CUMULO_SCALE, height=CUMULO_HEIGHT, cam=CUMULO_HEIGHT * 2.6,
           outro=[30, 0, -0.2])
    d['preview']['strikes'] = list(CUMULO_STRIKES)
    return d

def storm_cloud_small():
    return storm_cloud(3, STORM_LIFE, STORM_FORM, (STORM_FORM,), 2, 10, 14, name='Nube del rayo', spells=['lightning'], height=11)

# ================================================================ Pinchos de tierra (Earth Lance)
def spike(base, axis_tilt, height, hw, steps, c0, c1):
    """Pincho de seccion romboidal que se afina hacia arriba, inclinado 'axis_tilt' grados sobre Z."""
    t = math.radians(axis_tilt)
    d = (-math.sin(t), math.cos(t), 0)
    out = []
    for i in range(steps):
        u = (i + 0.5) / steps
        w = max(0.25, hw * (1 - u) ** 1.1)
        h = height / steps
        c = (base[0] + d[0] * height * u, base[1] + d[1] * height * u, base[2])
        out.append(box(c, (2 * w, h * 1.03, 2 * w), lerp_hex(c0, c1, u), rot=(0, 45, axis_tilt)))
    return out

def earth_spike():
    main = spike((0, 0, 0), 0, 28, 4.5, 14, '8a7660', 'cbbb9f')
    side = spike((3.5, 0, 1), -24, 15, 2.6, 8, '8f7b64', 'c0ae92') + spike((-3.2, 0, -1.5), 28, 12, 2.2, 7, '8f7b64', 'c0ae92')
    rnd = random.Random(5)
    rubble = []
    for k in range(12):
        a = k / 12 * 2 * math.pi + rnd.uniform(-0.2, 0.2)
        r = rnd.uniform(4.5, 7)
        s = rnd.uniform(1.2, 2.6)
        rubble.append(box((r * math.cos(a), s * 0.3, r * math.sin(a)), (s, s * 0.8, s), lerp_hex('5e4f3e', '8a7a63', rnd.random()),
                          rot=(rnd.uniform(0, 30), rnd.uniform(0, 90), rnd.uniform(0, 30))))
    return fx('Pinchos de tierra', ['earth_lance'], [
        group('stone', 'solid', main + side, rise=[0, 3, 30]),
        group('stone', 'solid', rubble, grow=[0, 3]),
        group('stone', 'solid', [], emit=emitter(14, 12, (5, 1, 5), (1.4, 0.3), ('9a8a72', '6a5c4a'), outward=0.6, rise=0.5, burst=True)),
    ], life=50, scale=1.6, cam=6, outro=[10, 30, 0])

def earth_pillar_burst():
    """Pilar de Tierra: al salir el pilar, revientan lajas de roca alrededor de la base, salta el cascote y se levanta
    polvo (el pilar en si son bloques que van subiendo)."""
    rnd = random.Random(21)
    groups = []
    for k in range(7):
        a = k / 7 * 2 * math.pi + rnd.uniform(-0.25, 0.25)
        r = rnd.uniform(6.5, 9)
        tilt = rnd.uniform(18, 34)
        h = rnd.uniform(8, 14)
        base = (r * math.cos(a), -1, r * math.sin(a))
        # cada laja se inclina hacia afuera (gira el pincho y lo apunta segun su angulo)
        sh = spike((0, 0, 0), tilt, h, rnd.uniform(1.6, 2.6), 6, '7d6a56', 'b5a389')
        for b in sh:
            b['at'] = [b['at'][0], b['at'][1], b['at'][2]]
        t0 = 1 + k * 0.7
        groups.append(group('stone', 'solid', sh, offset=list(base), tilt=[0, -math.degrees(a), 0], rise=[t0, t0 + 4, h + 3]))
    rubble = []
    for k in range(16):
        a = k / 16 * 2 * math.pi + rnd.uniform(-0.2, 0.2)
        r = rnd.uniform(5, 10)
        sz = rnd.uniform(1.2, 2.8)
        rubble.append(box((r * math.cos(a), sz * 0.35, r * math.sin(a)), (sz, sz * 0.8, sz), lerp_hex('5e4f3e', '9a8a72', rnd.random()),
                          rot=(rnd.uniform(0, 40), rnd.uniform(0, 90), rnd.uniform(0, 40))))
    groups.append(group('stone', 'solid', rubble, grow=[0, 4]))
    groups.append(group('stone', 'solid', [], emit=emitter(18, 14, (6, 1, 6), (1.6, 0.3), ('9a8a72', '6a5c4a'), outward=0.9, rise=0.8, burst=True)))
    groups.append(group('cloud', 'translucent', [], emit=emitter(12, 24, (7, 1, 7), (3.0, 4.5), ('b8a890', '8a7c68'), alpha=0.55, outward=0.5, rise=0.3, burst=True)))
    return fx('Pilar de tierra (estallido de la base)', ['earth_pillar'], groups, life=70, scale=1.6, cam=6, outro=[14, 20, 0])


# ================================================================ Cristales de hielo (Ice Pillar / Absolute Zero)
def crystal(base, tilt, height, hw, color0, color1):
    """Prisma de hielo con punta piramidal, inclinado tilt=(x,z) grados."""
    tx, tz = math.radians(tilt[0]), math.radians(tilt[1])
    d = (-math.sin(tz), math.cos(tx) * math.cos(tz), math.sin(tx))
    body_h = height * 0.7
    out = []
    n = 4
    for i in range(n):
        u = (i + 0.5) / n
        c = [base[j] + d[j] * body_h * u for j in range(3)]
        out.append(box(c, (2 * hw, body_h / n * 1.03, 2 * hw), lerp_hex(color0, color1, u * 0.6), 0.7, rot=(tilt[0], 45, tilt[1])))
    tip_h = height - body_h
    m = 5
    for i in range(m):
        u = (i + 0.5) / m
        w = max(0.15, hw * (1 - u))
        c = [base[j] + d[j] * (body_h + tip_h * u) for j in range(3)]
        out.append(box(c, (2 * w, tip_h / m * 1.03, 2 * w), lerp_hex(color1, 'f4fcff', u), 0.75 + 0.2 * u, rot=(tilt[0], 45, tilt[1])))
    core = [box([base[j] + d[j] * height * 0.45 for j in range(3)], (hw * 0.5, height * 0.8, hw * 0.5), 'ffffff', 0.55, rot=(tilt[0], 45, tilt[1]))]
    return out, core

def ice_crystals():
    specs = [((0, 0, 0), (0, 0), 30, 3.4), ((4.5, 0, 1.5), (8, -22), 20, 2.2), ((-4, 0, 2), (12, 25), 18, 2.0),
             ((1, 0, -4.5), (-24, 6), 22, 2.3), ((-2, 0, -3), (-15, 18), 13, 1.5), ((3.5, 0, -2.5), (-10, -30), 11, 1.3)]
    groups = []
    for i, (b, t, h, w) in enumerate(specs):
        body, core = crystal(b, t, h, w, '8fcdf5', 'c8ecff')
        a = i * 1.5
        groups.append(group('ice', 'translucent', body + core, glow=True, rise=[a, a + 5, h + 2]))
    groups.append(group('ice', 'translucent', [], glow=True,
                        emit=emitter(12, 14, (6, 8, 6), (0.9, 0.2), ('ffffff', 'bfe8ff'), alpha=0.8, rise=0.35, center=(0, 8, 0))))
    groups.append(group('ice', 'translucent', [], glow=True,
                        emit=emitter(16, 10, (3, 1, 3), (1.3, 0.3), ('e6f7ff', 'a8dcff'), alpha=0.9, outward=1.2, rise=0.4, burst=True)))
    return fx('Cristales de hielo', ['ice_pillar', 'absolute_zero'], groups, life=100, scale=1.5, cam=6, outro=[15, 0, 0.6])

# ================================================================ Estallido de fuego (Explosion / Exodus Flame)
def fire_burst():
    return fx('Estallido de fuego', ['explosion', 'exodus_flame'], [
        group('fire', 'emissive', [box((0, 0, 0), (6, 6, 6), 'fff2c0')], glow=True, grow=[0, 3], show=[[0, 9]], spin=(5, 7, 0)),
        group('fire', 'emissive', [box((0, 0, 0), (12, 12, 12), 'ffa030', 0.75)], glow=True, grow=[0, 5], show=[[0, 12]], spin=(-4, 3, 6), flicker=0.3),
        group('fire', 'emissive', [box((0, 0, 0), (18, 18, 18), 'ff5010', 0.35)], glow=True, grow=[1, 8], show=[[1, 15]], spin=(2, -5, 3), flicker=0.4),
        group('fire', 'emissive', [], glow=True,
              emit=emitter(28, 14, (3, 3, 3), (1.4, 0.3), ('ffd060', 'a02008'), alpha=0.95, outward=2.6, rise=0.3, burst=True)),
        group('cloud', 'translucent', [],
              emit=emitter(14, 20, (4, 3, 4), (3.5, 5.5), ('5a5450', '2a2826'), alpha=0.7, outward=0.9, rise=0.8, burst=True)),
    ], life=20, scale=2.0, height=1, cam=8)

# ================================================================ Escudo de agua (Water Shield): muro de agua de 3x2 bloques
def water_wall():
    rnd = random.Random(9)
    sheet = []
    for ix in range(6):
        for iy in range(4):
            sheet.append(box((-20 + ix * 8, 4 + iy * 8, rnd.uniform(-0.4, 0.4)), (8.1, 8.1, 1.6), lerp_hex('3d9ae8', '6cc0ff', rnd.random()), 0.4))
    waves = []
    for row, y in enumerate((6, 13, 20, 27)):
        for k in range(24):
            x0, x1 = -24 + k * 2, -24 + (k + 1) * 2
            y0 = y + 1.2 * math.sin(x0 * 0.35 + row * 1.7)
            y1 = y + 1.2 * math.sin(x1 * 0.35 + row * 1.7)
            waves.append(seg((x0, y0, 1.1), (x1, y1, 1.1), 0.5, 0.7, 'd8f0ff', 0.55))
            waves.append(seg((x0, y0 + 0.8, -1.1), (x1, y1 + 0.8, -1.1), 0.5, 0.7, 'd8f0ff', 0.45))
    foam = [box((-22 + k * 4, 32 + rnd.uniform(-0.3, 0.6), 0), (3.6, 1.4, 2.4), 'f2fbff', 0.8) for k in range(12)]
    return fx('Muro de agua', ['water_shield'], [
        group('water', 'translucent', sheet, rise=[0, 6, 32], pulse=(0.015, 10)),
        group('water', 'translucent', waves, glow=True, rise=[0, 6, 32], pulse=(0.02, 7)),
        group('water', 'translucent', foam, glow=True, rise=[0, 6, 32], flicker=0.3),
        group('water', 'translucent', [], glow=True,
              emit=emitter(26, 10, (23, 14, 1), (1.0, 0.6), ('cfeaff', '7cc4ff'), alpha=0.7, rise=-2.4, center=(0, 18, 0), shape=(0.4, 2.2, 0.4))),
        group('water', 'translucent', [], glow=True,
              emit=emitter(14, 8, (24, 0.5, 2), (1.0, 0.2), ('ffffff', '9fd8ff'), alpha=0.8, outward=0.8, rise=0.6, center=(0, 0.5, 0))),
    ], life=160, scale=1.0, cam=6, outro=[15, 32, 0])

# ================================================================ Escudo magico (Magic Shield): anillos y circulo runico
def ring(radius, segments, width, thick, color, alpha, y=0.0):
    out = []
    for k in range(segments):
        a0, a1 = 2 * math.pi * k / segments, 2 * math.pi * (k + 1) / segments
        out.append(seg((radius * math.cos(a0), y, radius * math.sin(a0)), (radius * math.cos(a1), y, radius * math.sin(a1)), width, thick, color, alpha))
    return out

def magic_shield():
    floor = ring(18, 40, 0.8, 0.3, 'e8c86a', 0.85, 0.2) + ring(14, 32, 0.5, 0.3, 'e8c86a', 0.75, 0.2)
    for k in range(8):
        a = 2 * math.pi * k / 8
        floor.append(box((16 * math.cos(a), 0.2, 16 * math.sin(a)), (1.6, 0.3, 2.6), 'fff0b0', 0.9, rot=(0, -math.degrees(a), 0)))
        floor.append(seg((14 * math.cos(a), 0.2, 14 * math.sin(a)), (14 * math.cos(a + 2 * math.pi * 3 / 8), 0.2, 14 * math.sin(a + 2 * math.pi * 3 / 8)),
                         0.35, 0.25, 'e8c86a', 0.55))
    rings = []
    for i, (tilt, sp) in enumerate((((70, 0, 0), (0, 6, 0)), ((70, 60, 0), (0, -5, 0)), ((70, 120, 0), (0, 4, 0)))):
        rings.append(group('fire', 'emissive', ring(15, 36, 0.5, 0.5, '9fd6ff', 0.65), glow=True, tilt=tilt, spin=sp,
                           offset=[0, 15, 0], grow=[2 + i, 8 + i], flicker=0.15))
    return fx('Escudo mágico', ['magic_shield'], [
        group('fire', 'emissive', floor, glow=True, spin=(0, 1.5, 0), grow=[0, 6]),
        *rings,
        group('fire', 'emissive', [], glow=True,
              emit=emitter(16, 18, (15, 0.5, 15), (1.0, 0.2), ('fff0b0', '9fd6ff'), alpha=0.85, rise=1.4, center=(0, 0.5, 0))),
    ], life=300, scale=1.0, cam=5.5, outro=[20, 0, 0.5])

# ================================================================ Flecha de agua (Water Arrow)
def water_arrow():
    shaft = [box((0, 0, -1), (0.9, 0.9, 14), '6cc0ff', 0.7, rot=(0, 0, 45))]
    head = []
    for i in range(6):
        u = (i + 0.5) / 6
        hw = max(0.2, 1.8 * (1 - u))
        head.append(box((0, 0, 6 + i + 0.5), (2 * hw, 2 * hw, 1.02), lerp_hex('8fd2ff', 'f2fbff', u), 0.8, rot=(0, 0, 45)))
    helix = []
    for s in range(2):
        pts = []
        for k in range(25):
            u = k / 24
            a = math.radians(s * 180 + u * 720)
            r = 1.8 - u * 0.6
            pts.append((r * math.cos(a), r * math.sin(a), -8 + u * 14))
        for k in range(24):
            helix.append(seg(pts[k], pts[k + 1], 0.5, 0.4, 'e6f6ff', 0.65))
    return projectile('Flecha de agua', ['water_arrow'], [
        group('water', 'translucent', shaft + head, glow=True, spin=(0, 0, 10)),
        group('water', 'translucent', helix, glow=True, spin=(0, 0, 30)),
        group('water', 'translucent', [], glow=True,
              emit=emitter(8, 8, (1, 1, 2), (0.8, 0.2), ('9fd8ff', 'ffffff'), alpha=0.8, drift=1.0, center=(0, 0, -7))),
    ])

# ================================================================ Tajo de fuego (Flame Slice)
def fire_crescent():
    haze, body, edge = [], [], []
    step = 6
    for a in range(-84, 84, step):
        a0, a1 = math.radians(a), math.radians(a + step)
        am = (a0 + a1) / 2
        w = 0.3 + 3.2 * math.cos(am) ** 1.6

        def arc(r, ang):
            return (r * math.sin(ang), 0, -4 + r * math.cos(ang))
        body.append(seg(arc(8, a0), arc(8, a1), w, 0.9, lerp_hex('ff7a1a', 'ffb030', math.cos(am)), 0.75))
        haze.append(seg(arc(8 - w * 0.6, a0), arc(8 - w * 0.6, a1), w * 1.5, 1.6, 'ff4a10', 0.3))
        edge.append(seg(arc(8 + w / 2, a0), arc(8 + w / 2, a1), 0.5, 1.0, 'fff2b0', 0.6 + 0.4 * math.cos(am)))
    return projectile('Tajo de fuego', ['flame_slice'], [
        group('fire', 'emissive', haze, glow=True, pulse=(0.06, 4), flicker=0.35),
        group('fire', 'emissive', body, glow=True, pulse=(0.03, 3)),
        group('fire', 'emissive', edge, glow=True, flicker=0.15),
        group('fire', 'emissive', [], glow=True,
              emit=emitter(16, 10, (8, 1, 3), (1.2, 0.2), ('ffd84a', 'b8240a'), alpha=0.95, rise=0.5, drift=0.8)),
    ])


# ================================================================ Formacion en la mano (antes de quedar listo para lanzar)
# Duran FORM ticks y terminan exactamente como el proyectil, que despues queda flotando frente a la mano.
FORM = 22

def _spiral(arms, steps, r0, r1, turn, size0, size1, c0, c1, alpha=0.95):
    out = []
    for a in range(arms):
        for k in range(steps):
            u = k / max(1, steps - 1)
            ang = math.radians(a * 360 / arms + turn * u)
            r = r0 + (r1 - r0) * u
            s = size0 + (size1 - size0) * u
            out.append(box((r * math.cos(ang), 1.5 * math.sin(ang * 2), r * math.sin(ang)), (s, s, s), lerp_hex(c0, c1, u), alpha))
    return out

def fireball_form():
    from spell_models import fireball
    core, mid, shell, tail, embers = fireball()['groups']
    core['grow'] = [2, 14]
    mid['grow'] = [7, 18]
    shell['grow'] = [11, 21]
    tail['grow'] = [15, 22]
    embers['show'] = [[13, 40]]
    gather = group('fire', 'emissive', [], glow=True, show=[[0, 18]],
                   emit=emitter(34, 12, (13, 11, 13), (1.4, 0.5), ('ffe07a', 'ff4a0e'), alpha=1.0, outward=-1.0))
    sparks = group('fire', 'emissive', _spiral(3, 7, 11, 3, 300, 1.6, 0.7, 'ffd04a', 'ff6a1e'), glow=True,
                   spin=(0, 40, 0), show=[[0, 17]], swirl=[0, 16, 360, 1.6], flicker=0.35)
    return fx('Bola de fuego: se forma en la mano', [], [gather, sparks, core, mid, shell, tail, embers],
              life=FORM, scale=1.0, height=0.6, cam=2.0)

def water_ball_form():
    from spell_models import water_ball
    gs = water_ball()['groups']
    sphere, core, lights, shine, ribbons, drops = gs[0], gs[1], gs[2], gs[3], gs[4:8], gs[8]
    core['grow'] = [2, 12]
    sphere['grow'] = [6, 19]
    lights['grow'] = [10, 20]
    shine['show'] = [[15, 40]]
    for i, r in enumerate(ribbons):
        r['swirl'] = [0, 18 + i, 300, 2.4]
        r['grow'] = [0, 5]
    drops['show'] = [[14, 40]]
    gather = group('water', 'translucent', [], glow=True, show=[[0, 18]],
                   emit=emitter(30, 13, (13, 12, 13), (1.4, 0.6), ('7cc8ff', 'ffffff'), alpha=0.95, outward=-0.95))
    return fx('Bola de agua: se forma en la mano', [], [gather, core, sphere, lights, shine, *ribbons, drops],
              life=FORM, scale=1.0, height=0.6, cam=2.0)

def _gather(tex, render, count, life, colors, size=(1.3, 0.4), radius=(12, 11, 12), speed=-0.95, until=18):
    """Particulas que llegan a la mano desde alrededor durante la formacion."""
    return group(tex, render, [], glow=True, show=[[0, until]],
                 emit=emitter(count, life, radius, size, colors, alpha=0.95, outward=speed))

def stone_bullet_form():
    from spell_models import stone_bullet
    body, dust = stone_bullet()['groups']
    body['stagger'] = [2, 17]
    body['grow'] = [0, 6]
    dust['show'] = [[16, 40]]
    chips = group('stone', 'solid', _spiral(3, 6, 10, 3, 280, 1.4, 0.8, '8c7c66', 'c8baa0', 1.0),
                  spin=(0, 50, 0), show=[[0, 15]], swirl=[0, 14, 360, 1.5])
    return fx('Bala de piedra: se forma en la mano', [], [_gather('stone', 'solid', 22, 12, ('a89a82', '6a5c4a')), chips, body, dust],
              life=FORM, scale=1.0, height=0.6, cam=2.0)

def icicle_form():
    from spell_models import icicle
    body, core, frost = icicle()['groups']
    body['stagger'] = [3, 19]
    core['show'] = [[12, 40]]
    frost['show'] = [[16, 40]]
    return fx('Lanza de hielo: se forma en la mano', [], [_gather('ice', 'translucent', 26, 12, ('ffffff', '9ad4f7')), body, core, frost],
              life=FORM, scale=1.0, height=0.6, cam=2.2)

def wind_blade_form():
    from spell_models import wind_blade
    haze, body, edge, streaks, motes = wind_blade()['groups']
    haze['grow'] = [2, 12]
    body['stagger'] = [5, 16]
    edge['show'] = [[13, 40]]
    streaks['show'] = [[16, 40]]
    motes['show'] = [[15, 40]]
    swirl = group('wind', 'translucent', _spiral(3, 8, 11, 4, 320, 1.0, 0.5, 'd4fff0', 'ffffff', 0.7), glow=True,
                  spin=(0, 70, 0), show=[[0, 15]], swirl=[0, 14, 420, 1.6])
    return fx('Hoja de viento: se forma en la mano', [], [_gather('wind', 'translucent', 20, 10, ('ffffff', 'c6eedd'), size=(0.9, 0.2)),
                                                           swirl, haze, body, edge, streaks, motes],
              life=FORM, scale=1.0, height=0.6, cam=2.2)

def _ring_z(radius, segments, width, thick, color, alpha, z=0.0):
    """Anillo alrededor del eje Z (el eje de vuelo de los proyectiles)."""
    out = []
    for k in range(segments):
        a0, a1 = 2 * math.pi * k / segments, 2 * math.pi * (k + 1) / segments
        out.append(seg((radius * math.cos(a0), radius * math.sin(a0), z), (radius * math.cos(a1), radius * math.sin(a1), z), width, thick, color, alpha))
    return out

def wind_drill_form():
    """El aire entra en corrientes que giran alrededor de la mano, se junta en un anillo y de ahi el taladro se
    estira hacia adelante hasta la punta; al final un golpe de aire sale del anillo."""
    from spell_models import wind_drill
    strands, cone, motes = wind_drill()['groups']
    # las hebras crecen de atras hacia la punta (todas a la vez, no una hebra despues de otra)
    body, tip = strands['boxes'][:-1], strands['boxes'][-1:]
    n = len(body) // 3
    strands['boxes'] = [body[st * n + k] for k in range(n) for st in range(3)] + tip
    strands['stagger'] = [5, 17]
    cone['stagger'] = [7, 18]
    motes['show'] = [[15, 40]]
    # corrientes de aire que llegan girando desde atras y afuera, y se cierran sobre el anillo
    streams = []
    for a in range(4):
        pts = []
        for k in range(19):
            u = k / 18
            ang = math.radians(a * 90 + u * 300)
            r = 10.5 - u * 5.0
            pts.append((r * math.cos(ang), r * math.sin(ang), -14 + u * 6))
        for k in range(18):
            u = k / 18
            streams.append(seg(pts[k], pts[k + 1], 0.3 + 0.9 * u, 0.35, lerp_hex('bfe9d8', 'ffffff', u), 0.15 + 0.55 * u))
    intake = group('wind', 'translucent', streams, glow=True, spin=(0, 0, 55), show=[[0, 12]], stagger=[0, 6])
    ring = group('wind', 'translucent', _ring_z(5.6, 20, 0.7, 0.5, 'e6fff4', 0.75) + _ring_z(4.2, 16, 0.4, 0.4, 'ffffff', 0.6),
                 glow=True, spin=(0, 0, 70), offset=[0, 0, -8], grow=[3, 9], show=[[3, 19]])
    blast = group('wind', 'translucent', _ring_z(6.5, 24, 1.0, 0.4, 'ffffff', 0.55), glow=True,
                  offset=[0, 0, -8], grow=[16, 22], show=[[16, 22]])
    flash = group('wind', 'translucent', [box((0, 0, 10.6), (1.6, 1.6, 1.6), 'ffffff', 0.85, rot=(0, 0, 45))], glow=True,
                  show=[[16, 21]], pulse=(0.3, 3))
    return fx('Taladro de viento: se forma en la mano', [], [_gather('wind', 'translucent', 18, 10, ('ffffff', 'c6eedd'), size=(0.9, 0.2), radius=(13, 13, 6)),
                                                              intake, ring, strands, cone, blast, flash, motes],
              life=FORM, scale=1.0, height=0.6, cam=2.4)

def water_arrow_form():
    """Las gotas se juntan en una bola de agua; la bola se estira hacia los dos lados en el astil, se afila la punta,
    la espiral se enrosca alrededor y una onda de agua cierra la formacion."""
    shaft_head, helix, drops = water_arrow()['groups']
    shaft = []
    for i in range(14):
        zc = -8 + i + 0.5
        shaft.append(box((0, 0, zc), (0.9, 0.9, 1.05), '6cc0ff', 0.7, rot=(0, 0, 45)))
    shaft.sort(key=lambda b: abs(b['at'][2] + 1))
    head = shaft_head['boxes'][1:]
    shaft_head['boxes'] = shaft + head
    shaft_head['stagger'] = [5, 16]
    hb = helix['boxes']
    hn = len(hb) // 2
    helix['boxes'] = [hb[st * hn + k] for k in range(hn) for st in range(2)]
    helix['stagger'] = [10, 19]
    helix['show'] = [[10, 40]]
    drops['show'] = [[16, 40]]
    blob = group('water', 'translucent', [box((0, 0, -1), (3.4, 3.4, 3.4), '7cc8ff', 0.75, rot=(0, 45, 45)),
                                          box((0, 0, -1), (2.6, 2.6, 2.6), 'cdeeff', 0.6, rot=(45, 0, 30))],
                 glow=True, spin=(20, 35, 0), grow=[1, 7], show=[[1, 12]], pulse=(0.12, 4))
    wave = group('water', 'translucent', _ring_z(4.5, 22, 0.7, 0.4, 'e6f6ff', 0.6), glow=True,
                 offset=[0, 0, -1], grow=[15, 21], show=[[15, 21]])
    return fx('Flecha de agua: se forma en la mano', [], [_gather('water', 'translucent', 24, 11, ('7cc8ff', 'ffffff'), radius=(11, 11, 11)),
                                                           blob, shaft_head, helix, wave, drops],
              life=FORM, scale=1.0, height=0.6, cam=2.2)

def fire_crescent_form():
    haze, body, edge, embers = fire_crescent()['groups']
    haze['grow'] = [3, 13]
    body['stagger'] = [5, 17]
    edge['show'] = [[14, 40]]
    embers['show'] = [[15, 40]]
    return fx('Tajo de fuego: se forma en la mano', [], [_gather('fire', 'emissive', 28, 12, ('ffe07a', 'ff4a0e')), haze, body, edge, embers],
              life=FORM, scale=1.0, height=0.6, cam=2.2)

def _glow(name, tex, render, c0, c1, c2):
    """Hechizos que crean estructuras: solo un brillo del elemento en la mano (sin formacion)."""
    core = group(tex, render, [box((0, 0, 0), (2.4, 2.4, 2.4), c0, 0.9)], glow=True, spin=(30, 45, 0), pulse=(0.15, 8))
    shell = group(tex, render, [box((0, 0, 0), (4.2, 4.2, 4.2), c1, 0.35)], glow=True, spin=(-20, 30, 15), pulse=(0.1, 11))
    motes = group(tex, render, [], glow=True, emit=emitter(10, 12, (3.5, 3.5, 3.5), (0.8, 0.2), (c2, c1), alpha=0.85, rise=0.25))
    return projectile(name, [], [core, shell, motes])

def glow_fire(): return _glow('Brillo de fuego (hechizos de estructura)', 'fire', 'emissive', 'fff2b0', 'ff7a1a', 'ffd84a')
def glow_water(): return _glow('Brillo de agua (hechizos de estructura)', 'water', 'translucent', 'e6f6ff', '5cb2f6', 'b8e4ff')
def glow_earth(): return _glow('Brillo de tierra (hechizos de estructura)', 'stone', 'solid', 'c8baa0', '8c7c66', 'a89a82')
def glow_wind(): return _glow('Brillo de viento (hechizos de estructura)', 'wind', 'translucent', 'ffffff', 'c6eedd', 'eafff8')
def glow_none(): return _glow('Brillo de mana (sin elemento)', 'water', 'translucent', 'fffbe0', 'ffd86a', 'ffffff')



# ================================================================ Campo Minado: bolita de fuego en la mano y las minas
def minefield_ball():
    """Bolita de fuego chica y apretada: nucleo blanco, brasas que la orbitan y costra oscura que se agrieta."""
    core = group('fire', 'emissive', [box((0, 0, 0), (3, 3, 3), 'fff6c8')], glow=True, pulse=(0.18, 4))
    mid = group('fire', 'emissive', [box((0, 0, 0), (5, 5, 5), 'ffa030', 0.85)], glow=True, spin=(6, 9, 0), pulse=(0.08, 6))
    crust = []
    for i, (rx, ry, rz) in enumerate([(0, 0, 0), (45, 45, 0), (0, 45, 45), (45, 0, 45)]):
        crust.append(box((0, 0, 0), (6.4, 6.4, 6.4) if i == 0 else (5.6, 5.6, 5.6), lerp_hex('3a1a10', '6a2a14', i / 3), 0.55, rot=(rx, ry, rz)))
    shell = group('fire', 'emissive', crust, glow=False, spin=(-3, 5, 2), flicker=0.25)
    orbit = []
    for k in range(4):
        a = k * math.pi / 2
        orbit.append(box((4.6 * math.cos(a), 0.8 * math.sin(2 * a), 4.6 * math.sin(a)), (1.2, 1.2, 1.2), 'ffd04a', 0.95))
    embers_orbit = group('fire', 'emissive', orbit, glow=True, spin=(0, 24, 0), pulse=(0.2, 3))
    embers = group('fire', 'emissive', [], glow=True,
                   emit=emitter(14, 12, (3.5, 3, 3.5), (1.0, 0.2), ('ffd84a', 'b8240a'), alpha=0.95, rise=0.5))
    return projectile('Campo Minado: bolita de fuego en la mano', ['minefield'], [core, mid, shell, embers_orbit, embers])


def minefield_ball_form():
    core, mid, shell, orbit, embers = minefield_ball()['groups']
    core['grow'] = [2, 10]
    mid['grow'] = [5, 13]
    shell['grow'] = [9, 16]
    orbit['grow'] = [11, 18]
    embers['show'] = [[10, 40]]
    gather = group('fire', 'emissive', [], glow=True, show=[[0, 14]],
                   emit=emitter(26, 10, (10, 9, 10), (1.2, 0.4), ('ffe07a', 'ff4a0e'), alpha=1.0, outward=-0.9))
    return fx('Campo Minado: la bolita se forma', [], [gather, core, mid, shell, orbit, embers], life=FORM, scale=1.0, height=0.6, cam=2.0)


def minefield_mine():
    """Mina de fuego: piedra volcanica agrietada con el nucleo encendido, pinchos de brasa y chispas que suben."""
    rnd = random.Random(31)
    core = group('fire', 'emissive', [box((0, 0, 0), (3.4, 3.4, 3.4), 'fff0b0')], glow=True, pulse=(0.25, 6))
    glow = group('fire', 'emissive', [box((0, 0, 0), (5.2, 5.2, 5.2), 'ff8a20', 0.6)], glow=True, pulse=(0.15, 6), flicker=0.3)
    rock = []
    for i, (rx, ry, rz) in enumerate([(0, 0, 0), (45, 0, 0), (0, 45, 0), (0, 0, 45)]):
        rock.append(box((0, 0, 0), (6, 6, 6), lerp_hex('2a1a14', '4a2a1a', rnd.random()), 0.92, rot=(rx, ry, rz)))
    spikes = []
    for k in range(8):
        a = k / 8 * 2 * math.pi
        up = 1.6 if k % 2 == 0 else -1.6
        spikes.append(box((4.2 * math.cos(a), up, 4.2 * math.sin(a)), (1.4, 1.4, 1.4), 'ff5a10', 0.95,
                          rot=(rnd.uniform(0, 45), math.degrees(a), 45)))
    shell = group('stone', 'solid', rock, spin=(0, 3, 0))
    hot = group('fire', 'emissive', spikes, glow=True, spin=(0, 3, 0), pulse=(0.12, 5), flicker=0.2)
    sparks = group('fire', 'emissive', [], glow=True,
                   emit=emitter(10, 14, (3, 2, 3), (1.0, 0.2), ('ffd84a', 'c03008'), alpha=0.95, rise=0.7))
    return fx('Campo Minado: mina', ['minefield'], [shell, core, glow, hot, sparks], life=200, scale=1.0, height=0.3, cam=2.0)


FX_MODELS = dict(fireball_form=fireball_form, water_ball_form=water_ball_form, stone_bullet_form=stone_bullet_form,
                 icicle_form=icicle_form, wind_blade_form=wind_blade_form, wind_drill_form=wind_drill_form,
                 water_arrow_form=water_arrow_form, fire_crescent_form=fire_crescent_form,
                 glow_fire=glow_fire, glow_water=glow_water, glow_earth=glow_earth, glow_wind=glow_wind, glow_none=glow_none, water_arrow=water_arrow, fire_crescent=fire_crescent, storm_cloud=storm_cloud_small, cumulonimbus=cumulonimbus,
                 earth_spike=earth_spike, earth_pillar_burst=earth_pillar_burst, ice_crystals=ice_crystals, fire_burst=fire_burst, water_wall=water_wall,
                 magic_shield=magic_shield, minefield_ball=minefield_ball, minefield_ball_form=minefield_ball_form,
                 minefield_mine=minefield_mine)

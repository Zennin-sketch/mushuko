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

# Momentos clave (ticks) que tambien usa el codigo del hechizo
CUMULO_FORM, CUMULO_STRIKES, CUMULO_RAIN_END, CUMULO_LIFE = 50, (50, 75, 100), 120, 150
STORM_FORM, STORM_LIFE = 14, 45

def cumulonimbus():
    # escala 6: radio ~ 24 px * 6 = 9 bloques; la lluvia cae ~18 bloques (48 px * 6 / 16)
    return storm_cloud(6, CUMULO_LIFE, CUMULO_FORM, CUMULO_STRIKES, 3, 15, 24, rain=(CUMULO_FORM, CUMULO_RAIN_END, 48),
                       name='Cumulonimbus', spells=['cumulonimbus'], height=16)

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

FX_MODELS = dict(water_arrow=water_arrow, fire_crescent=fire_crescent, storm_cloud=storm_cloud_small, cumulonimbus=cumulonimbus,
                 earth_spike=earth_spike, ice_crystals=ice_crystals, fire_burst=fire_burst, water_wall=water_wall,
                 magic_shield=magic_shield)

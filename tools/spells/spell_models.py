"""Modelos voxel de los hechizos (estetica Minecraft) + texturas.

Genera:
  assets/remushoku/spell_models/<modelo>.json   (lo lee MagicProjectileRenderer y la pagina de preview)
  assets/remushoku/textures/entity/spell/<material>.png

Unidades: pixeles (1/16 de bloque). El modelo mira hacia +Z (direccion de vuelo).
Cada caja: centro 'at', tamano 'size', giro propio 'rot' (grados, orden X luego Y luego Z) y color/alpha.
Cada grupo: textura, tipo de render (solid / translucent / emissive), 'glow' (brillo maximo),
'tilt' (inclinacion fija), 'spin' (grados por tick en X,Y,Z, despues del tilt), 'pulse' [amplitud, periodo en ticks],
'flicker' (parpadeo del alpha) y opcionalmente 'emit': cubitos que nacen, suben/se alejan, se achican y desaparecen.
"""
import json, math, os, random, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'remushoku')

def box(at, size, color, alpha=1.0, rot=(0, 0, 0)):
    return dict(at=[round(v, 3) for v in at], size=[round(v, 3) for v in size], rot=[round(v, 2) for v in rot],
                color=color, alpha=round(alpha, 3))

def seg(p0, p1, width, thick, color, alpha):
    """Caja larga que une p0 con p1 (eje largo = Z local). 'width' en X local, 'thick' en Y local."""
    d = [b - a for a, b in zip(p0, p1)]
    length = math.sqrt(sum(c * c for c in d)) or 0.01
    yaw = math.degrees(math.atan2(d[0], d[2]))
    pitch = math.degrees(math.atan2(d[1], math.hypot(d[0], d[2])))
    mid = [(a + b) / 2 for a, b in zip(p0, p1)]
    return box(mid, (width, thick, length * 1.08), color, alpha, rot=(-pitch, yaw, 0))

def group(tex, render, boxes, glow=False, spin=(0, 0, 0), pulse=(0, 1), flicker=0.0, tilt=(0, 0, 0), emit=None, **timeline):
    """timeline (todo en ticks de edad del efecto):
      offset=[x,y,z]       traslado fijo del grupo
      show=[[a,b],...]     ventanas en las que se ve (con fundido de 3 ticks)
      grow=[a,b]           escala 0 -> 1
      rise=[a,b,dist]      sube desde -dist hasta su lugar
      swirl=[a,b,grados,radio]  entra girando en espiral: gira 'grados' de mas y se cierra desde 'radio' veces su tamano
      stagger=[a,b]        cada caja aparece una tras otra entre a y b"""
    g = dict(texture='remushoku:textures/entity/spell/%s.png' % tex, render=render, glow=glow, tilt=list(tilt),
             spin=list(spin), pulse=list(pulse), flicker=flicker, boxes=boxes)
    for k, v in timeline.items():
        g[k] = v
    if emit:
        g['emit'] = emit
    return g

def emitter(count, life, radius, size, colors, alpha=0.9, rise=0.0, drift=0.0, outward=0.0, center=(0, 0, 0),
            shape=(1, 1, 1), burst=False):
    """Cubitos: cada uno nace en una posicion al azar dentro de 'radius' (alrededor de 'center'), vive 'life' ticks,
    sube 'rise' px/tick, se va hacia atras 'drift' px/tick y hacia afuera 'outward' px/tick, mientras se achica y se apaga.
    'shape' estira el cubito (gotas de lluvia); 'burst' = salen todos juntos una sola vez (explosiones)."""
    return dict(count=count, life=life, radius=list(radius), size=list(size), colors=list(colors), alpha=alpha,
                rise=rise, drift=drift, outward=outward, center=list(center), shape=list(shape), burst=burst)

def lerp_hex(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return ''.join('%02x' % round(x + (y - x) * t) for x, y in zip(ca, cb))

# ---------------------------------------------------------------- Bala de piedra (Stone Cannon / Rock Bullet)
# Rombo limpio: bipiramide de seccion cuadrada girada 45 grados, escalonada pixel a pixel. La punta de adelante
# es mas larga que la de atras (como la bala de Rudeus) y cada capa gira apenas para sugerir la mecha.
def stone_bullet():
    body = []
    z0, zmax, z1, hwmax = -7, -1, 9, 4.0
    n = z1 - z0
    for i in range(n):
        zc = z0 + i + 0.5
        u = (zc - z0) / (zmax - z0) if zc < zmax else (z1 - zc) / (z1 - zmax)
        hw = max(0.3, hwmax * u)
        shade = lerp_hex('8c7c66', 'c8baa0', max(0.0, 1 - abs(zc - zmax) / 9))
        body.append(box((0, 0, zc), (2 * hw, 2 * hw, 1.02), shade, rot=(0, 0, 45)))
    # Cinturon claro en la parte mas ancha (el "filo" del rombo)
    belt = [box((0, 0, zmax), (2 * hwmax + 0.5, 2 * hwmax + 0.5, 0.6), 'ddd0b6', rot=(0, 0, 45))]
    return dict(name='Bala de piedra', spells=['rock_bullet', 'stone_cannon', 'spike_rain'],
                groups=[group('stone', 'solid', body + belt, spin=(0, 0, 40)),
                        group('stone', 'solid', [], emit=emitter(6, 8, (2.5, 2.5, 1), (1.0, 0.2), ('9a8a72', '6a5c4a'),
                                                                    alpha=1.0, drift=1.2, outward=0.25))])

# ---------------------------------------------------------------- Bola de fuego (Fireball / Ex-Flame / Exodus)
# Cubos anidados (nucleo blanco, capa naranja, cascara roja) + lenguas detras + brasas cubicas que suben y se apagan.
def fireball():
    core = group('fire', 'emissive', [box((0, 0, 0), (4, 4, 4), 'fff6c8')], glow=True, pulse=(0.12, 5))
    mid = group('fire', 'emissive', [box((0, 0, 0), (7, 7, 7), 'ffb83a', 0.85)], glow=True, spin=(4, 6, 0), pulse=(0.06, 7))
    shell = group('fire', 'emissive', [box((0, 0, 0), (10, 10, 10), 'ff6a1e', 0.3)], glow=True, spin=(-3, 2, 5), pulse=(0.08, 9), flicker=0.3)
    rnd = random.Random(7)
    tongues = []
    for i, (z, s) in enumerate([(-6, 3.2), (-8.5, 2.4), (-10.5, 1.8), (-12.5, 1.2), (-5.5, 2.4), (-7.5, 1.6)]):
        x, y = rnd.uniform(-2.5, 2.5), rnd.uniform(-2, 3)
        tongues.append(box((x, y, z), (s, s, s), lerp_hex('ff9a20', 'd8300e', i / 5), 0.75, rot=(rnd.uniform(0, 45), rnd.uniform(0, 45), 0)))
    tail = group('fire', 'emissive', tongues, glow=True, pulse=(0.15, 4), flicker=0.45)
    embers = group('fire', 'emissive', [], glow=True,
                   emit=emitter(24, 16, (5.5, 4.5, 5.5), (1.3, 0.2), ('ffd84a', 'b8240a'), alpha=0.95, rise=0.55, drift=0.3))
    return dict(name='Bola de fuego', spells=['fireball', 'flame_slice', 'ex_flame', 'exodus_flame', 'fire_rain'],
                groups=[core, mid, shell, tail, embers])

# ---------------------------------------------------------------- Bola de agua (Water Ball / Water Cannon)
# Como en la escena del anime: esfera translucida con manchas de luz adentro y cintas de agua que la rodean
# girando en planos distintos, soltando gotitas.
def water_ball():
    R = 5.0
    sphere = []
    for i in range(10):
        y = -R + i + 0.5
        r = math.sqrt(max(0.0, R * R - y * y))
        sphere.append(box((0, y, 0), (2 * r * 0.92, 1.02, 2 * r * 0.92), '5cb2f6', 0.34))
        sphere.append(box((0, y, 0), (2 * r, 1.02, 2 * r * 0.6), '5cb2f6', 0.34))
        sphere.append(box((0, y, 0), (2 * r * 0.6, 1.02, 2 * r), '5cb2f6', 0.34))
    rnd = random.Random(3)
    lights = []
    for _ in range(7):
        a, b = rnd.uniform(0, 2 * math.pi), rnd.uniform(-0.9, 0.9)
        rr = rnd.uniform(1.5, 3.6)
        s = rnd.uniform(0.8, 1.8)
        lights.append(box((rr * math.cos(a) * math.cos(b), rr * math.sin(b), rr * math.sin(a) * math.cos(b)), (s, s * 0.7, s), 'f2fbff', 0.75))
    core = [box((0, 0, 0), (4.5, 4.5, 4.5), 'a8dcff', 0.35)]
    shine = [box((-2.2, 2.6, 3.0), (1.6, 1.6, 0.8), 'ffffff', 0.95)]

    def ribbon(radius, span, width, phase=0.0):
        boxes, steps = [], int(span / 10)
        for k in range(steps):
            a0, a1 = math.radians(phase + k * 10), math.radians(phase + (k + 1) * 10)
            u = (k + 0.5) / steps
            taper = math.sin(math.pi * u) ** 0.7
            r = radius + 0.6 * math.sin(u * 5)
            p0 = (r * math.cos(a0), 0.4 * math.sin(u * 7), r * math.sin(a0))
            p1 = (r * math.cos(a1), 0.4 * math.sin((u + 0.1) * 7), r * math.sin(a1))
            boxes.append(seg(p0, p1, 0.6, width * taper + 0.3, lerp_hex('b8e4ff', 'ffffff', taper), 0.3 + 0.55 * taper))
        return boxes

    ribbons = [
        group('water', 'translucent', ribbon(6.6, 230, 2.6), glow=True, tilt=(20, 0, 15), spin=(0, 22, 0)),
        group('water', 'translucent', ribbon(7.4, 200, 2.0, 120), glow=True, tilt=(-35, 0, 60), spin=(0, -18, 0)),
        group('water', 'translucent', ribbon(6.1, 170, 1.8, 240), glow=True, tilt=(70, 30, 0), spin=(0, 26, 0)),
        group('water', 'translucent', ribbon(8.0, 140, 1.5, 60), glow=True, tilt=(-10, 0, -40), spin=(0, 15, 0)),
    ]
    drops = group('water', 'translucent', [], glow=True,
                  emit=emitter(10, 10, (5, 5, 5), (1.1, 0.3), ('9fd8ff', 'ffffff'), alpha=0.8, drift=0.5, outward=0.5))
    return dict(name='Bola de agua', spells=['water_ball', 'water_cannon'], groups=[
        group('water', 'translucent', sphere, spin=(0, 3, 1.5), pulse=(0.05, 12)),
        group('water', 'translucent', core, glow=True, pulse=(0.1, 8)),
        group('water', 'translucent', lights, glow=True, spin=(3, -5, 2)),
        group('water', 'translucent', shine, glow=True),
        *ribbons, drops])

# ---------------------------------------------------------------- Lanza de hielo (Ice Smash / Icicle Lance)
# Carambano de seccion romboidal: base gruesa y una punta larga que se afina pixel a pixel hasta quedar filosa.
def icicle():
    body = []
    for z0, z1, hw in [(-9, -7, 1.5), (-7, -4, 2.25), (-4, -1, 2.1)]:
        body.append(box((0, 0, (z0 + z1) / 2), (2 * hw, 2 * hw, z1 - z0), '9ad4f7', 0.72, rot=(0, 0, 45)))
    L = 17
    for i in range(L):
        u = (i + 0.5) / L
        hw = max(0.12, 2.0 * (1 - u) ** 1.25)
        body.append(box((0, 0, -1 + i + 0.5), (2 * hw, 2 * hw, 1.02), lerp_hex('a8dcff', 'f4fcff', u), 0.72 + 0.2 * u, rot=(0, 0, 45)))
    core = [box((0, 0, 1), (0.7, 0.7, 18), 'ffffff', 0.85, rot=(0, 0, 45))]
    tip = [box((0, 0, 15.2), (0.25, 0.25, 1.6), 'ffffff', 1.0, rot=(0, 0, 45))]
    shards = []
    for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        shards.append(box((sx * 2.6, sy * 2.6, -5.5), (1.1, 1.1, 4.5), 'b7e2ff', 0.7, rot=(-sy * 28, sx * 28, 45)))
    frost = group('ice', 'translucent', [], glow=True,
                  emit=emitter(8, 12, (2.5, 2.5, 6), (0.8, 0.2), ('ffffff', 'bfe8ff'), alpha=0.85, drift=0.6, outward=0.15))
    return dict(name='Lanza de hielo', spells=['ice_smash', 'icicle_lance', 'freezing_rain'], groups=[
        group('ice', 'translucent', body + shards + tip, glow=True, spin=(0, 0, 8)),
        group('ice', 'translucent', core, glow=True, spin=(0, 0, 8), flicker=0.2), frost])

# ---------------------------------------------------------------- Hoja de viento (Wind Slice / Sonic Boom / tajo de espada)
# Media luna fina: gruesa al centro, puntas filosas, tres capas (bruma, cuerpo y filo blanco) y estelas detras.
def wind_blade():
    haze, body, edge = [], [], []
    step = 6
    for a in range(-84, 84, step):
        a0, a1 = math.radians(a), math.radians(a + step)
        am = (a0 + a1) / 2
        w = 0.25 + 3.0 * math.cos(am) ** 1.6

        def arc(r, ang):
            return (r * math.sin(ang), 0, -4 + r * math.cos(ang))
        rb = 8.0
        body.append(seg(arc(rb, a0), arc(rb, a1), w, 0.7, lerp_hex('bff5e0', 'e8fff6', math.cos(am)), 0.45))
        haze.append(seg(arc(rb - w * 0.6, a0), arc(rb - w * 0.6, a1), w * 1.4, 1.4, 'd4fff0', 0.16))
        edge.append(seg(arc(rb + w / 2, a0), arc(rb + w / 2, a1), 0.45, 0.9, 'ffffff', 0.55 + 0.4 * math.cos(am)))
    streaks = []
    for x, z, L in ((-4, -7, 6), (-1.5, -8, 8), (1.5, -8, 8), (4, -7, 6), (0, -9, 5)):
        streaks.append(box((x, 0, z), (0.35, 0.35, L), 'eafff8', 0.28))
    motes = group('wind', 'translucent', [], glow=True,
                  emit=emitter(10, 8, (7, 0.6, 2), (0.7, 0.15), ('ffffff', 'd4fff0'), alpha=0.75, drift=1.0))
    return dict(name='Hoja de viento', spells=['wind_slice', 'sonic_boom', 'sonic_blast', 'sword_wind'], groups=[
        group('wind', 'translucent', haze, glow=True, pulse=(0.06, 4), flicker=0.3),
        group('wind', 'translucent', body, glow=True, pulse=(0.03, 4)),
        group('wind', 'translucent', edge, glow=True, flicker=0.12),
        group('wind', 'translucent', streaks, glow=True, flicker=0.4), motes])

# ---------------------------------------------------------------- Taladro de viento (Tornado Impact)
# Tres cintas de viento en espiral que forman un cono y terminan en punta, con un cono interior de bruma.
def wind_drill():
    strands = []
    N = 34
    for s in range(3):
        pts = []
        for k in range(N + 1):
            u = k / N
            z = -8 + u * 19
            r = 0.2 + 5.6 * (1 - u) ** 0.85
            a = math.radians(s * 120 + u * 620)
            pts.append((r * math.cos(a), r * math.sin(a), z))
        for k in range(N):
            u = (k + 0.5) / N
            strands.append(seg(pts[k], pts[k + 1], 0.35 + 1.6 * (1 - u), 0.5, lerp_hex('c6eedd', 'ffffff', u), 0.35 + 0.5 * u))
    cone = []
    for i in range(18):
        u = (i + 0.5) / 18
        hw = 0.15 + 3.6 * (1 - u) ** 0.9
        cone.append(box((0, 0, -8 + u * 18), (2 * hw, 2 * hw, 1.05), 'e6fff4', 0.13, rot=(0, 0, 45 + i * 10)))
    tip = [box((0, 0, 10.6), (0.3, 0.3, 1.6), 'ffffff', 0.9)]
    motes = group('wind', 'translucent', [], glow=True,
                  emit=emitter(12, 10, (5, 5, 3), (0.8, 0.15), ('ffffff', 'c6eedd'), alpha=0.7, drift=1.2, outward=0.35))
    return dict(name='Taladro de viento', spells=['tornado_impact'], groups=[
        group('wind', 'translucent', strands + tip, glow=True, spin=(0, 0, 34)),
        group('wind', 'translucent', cone, glow=True, spin=(0, 0, -18), flicker=0.25), motes])

MODELS = dict(stone_bullet=stone_bullet, fireball=fireball, water_ball=water_ball, icicle=icicle,
              wind_blade=wind_blade, wind_drill=wind_drill)

def all_models():
    """Proyectiles de este archivo + efectos de spell_fx.py."""
    sys.path.insert(0, HERE)
    from spell_fx import FX_MODELS
    return {**MODELS, **FX_MODELS}

# ---------------------------------------------------------------- Texturas 16x16 (casi blancas: el color lo pone cada caja)
def texture(mat):
    rnd = random.Random(sum(map(ord, mat)))
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            if mat == 'stone':
                v = 0.74 + 0.26 * n
                if (x + 2 * y) % 11 == 0 and n > 0.4: v = 0.58        # grietas
            elif mat == 'fire':
                v = 0.8 + 0.2 * n
                if (x * 7 + y * 3) % 9 == 0: v = 1.0
            elif mat == 'water':
                v = 0.84 + 0.1 * math.sin((x + y * 0.5) * 0.9) + 0.06 * n
            elif mat == 'ice':
                v = 0.86 + 0.1 * n
                if (x - y) % 6 == 0: v = 1.0                           # vetas diagonales
            elif mat == 'cloud':
                v = 0.82 + 0.12 * math.sin(x * 0.7) * math.cos(y * 0.6) + 0.06 * n
            else:  # wind
                v = 0.8 + 0.2 * (0.5 + 0.5 * math.sin(y * 1.3 + x * 0.25)) * (0.7 + 0.3 * n)
            c = max(0, min(255, round(255 * v)))
            px[x, y] = (c, c, c, 255)
    return img

def main():
    mdir = os.path.join(ASSETS, 'spell_models')
    tdir = os.path.join(ASSETS, 'textures', 'entity', 'spell')
    os.makedirs(mdir, exist_ok=True)
    os.makedirs(tdir, exist_ok=True)
    for name, fn in all_models().items():
        with open(os.path.join(mdir, name + '.json'), 'w') as f:
            json.dump(fn(), f, indent=1)
    for mat in ('stone', 'fire', 'water', 'ice', 'wind', 'cloud'):
        texture(mat).save(os.path.join(tdir, mat + '.png'))
    print(len(all_models()), 'modelos ->', mdir)

if __name__ == '__main__':
    main()

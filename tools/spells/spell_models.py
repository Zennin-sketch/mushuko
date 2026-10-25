"""Modelos voxel de los hechizos (estetica Minecraft) + texturas.

Genera:
  assets/remushoku/spell_models/<modelo>.json   (lo lee MagicProjectileRenderer y la pagina de preview)
  assets/remushoku/textures/entity/spell/<material>.png

Unidades: pixeles (1/16 de bloque). El modelo mira hacia +Z (direccion de vuelo).
Cada caja: centro 'at', tamano 'size', giro propio 'rot' (grados, orden X luego Y luego Z) y color/alpha.
Cada grupo: textura, tipo de render (solid / translucent / emissive), 'glow' (brillo maximo),
'spin' (grados por tick en X,Y,Z), 'pulse' [amplitud, periodo en ticks] y 'flicker' (parpadeo del alpha).
"""
import json, math, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'remushoku')

def box(at, size, color, alpha=1.0, rot=(0, 0, 0)):
    return dict(at=[round(v, 3) for v in at], size=[round(v, 3) for v in size], rot=[round(v, 2) for v in rot],
                color=color, alpha=round(alpha, 3))

def group(tex, render, boxes, glow=False, spin=(0, 0, 0), pulse=(0, 1), flicker=0.0):
    return dict(texture='remushoku:textures/entity/spell/%s.png' % tex, render=render, glow=glow,
                spin=list(spin), pulse=list(pulse), flicker=flicker, boxes=boxes)

def lerp_hex(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return ''.join('%02x' % round(x + (y - x) * t) for x, y in zip(ca, cb))

# ---------------------------------------------------------------- Bala de piedra (Stone Cannon / Rock Bullet)
# El "rombo" de Rudeus: perfil de diamante (gordo al medio, punta larga adelante), seccion cuadrada girada 45
# grados y estrias en espiral como una mecha de taladro. Gira muy rapido sobre su eje para perforar.
def stone_bullet():
    slices = [(-6, -4.5, 1.0), (-4.5, -3, 2.0), (-3, -1, 3.0), (-1, 1, 3.5), (1, 3, 3.0), (3, 5, 2.25), (5, 7, 1.5), (7, 9, 0.75)]
    body, ridges = [], []
    for i, (z0, z1, hw) in enumerate(slices):
        t = i / (len(slices) - 1)
        body.append(box((0, 0, (z0 + z1) / 2), (2 * hw, 2 * hw, z1 - z0), lerp_hex('9a8a74', 'd6cab2', t), rot=(0, 0, 45)))
        if 0 < i < len(slices) - 1:
            for side in (0, 180):
                a = math.radians(i * 32 + side + 45)
                r = hw * 0.72
                ridges.append(box((r * math.cos(a), r * math.sin(a), (z0 + z1) / 2), (0.7, hw * 0.4, (z1 - z0) * 1.02),
                                  lerp_hex('6e6152', '9c8e78', t), rot=(0, 0, math.degrees(a) + 90)))
    return dict(name='Bala de piedra', spells=['rock_bullet', 'stone_cannon', 'spike_rain'],
                groups=[group('stone', 'solid', body + ridges, spin=(0, 0, 45))])

# ---------------------------------------------------------------- Bola de fuego (Fireball / Ex-Flame / Exodus)
# Cubos anidados: nucleo casi blanco, capa naranja y cascara roja translucida girando en sentidos opuestos,
# mas lenguas de fuego cubicas que quedan atras.
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
    return dict(name='Bola de fuego', spells=['fireball', 'ex_flame', 'exodus_flame', 'fire_rain'],
                groups=[core, mid, shell, tail])

# ---------------------------------------------------------------- Bola de agua (Water Ball / Water Cannon)
# Esfera voxel (cruz de cajas + nucleo) translucida, nucleo claro y un brillo blanco fijo, gotas detras.
def water_ball():
    body = [box((0, 0, 0), (8, 8, 8), '4aa2f2', 0.5), box((0, 0, 0), (10, 6, 6), '4aa2f2', 0.5),
            box((0, 0, 0), (6, 10, 6), '4aa2f2', 0.5), box((0, 0, 0), (6, 6, 10), '4aa2f2', 0.5)]
    inner = [box((0, 0, 0), (5, 5, 5), '8fd2ff', 0.45)]
    shine = [box((-2.2, 2.6, 2.6), (1.6, 1.6, 1), 'ffffff', 0.9), box((-3.1, 1.4, 2.6), (0.8, 0.8, 0.8), 'ffffff', 0.7)]
    drops = [box((1.5, -1, -7), (1.6, 1.6, 1.6), '4a98e8', 0.6), box((-1.2, 1, -9), (1.1, 1.1, 1.1), '4a98e8', 0.5),
             box((0.4, 0.3, -10.8), (0.8, 0.8, 0.8), '4a98e8', 0.45)]
    return dict(name='Bola de agua', spells=['water_ball', 'water_cannon'], groups=[
        group('water', 'translucent', body, spin=(0, 3, 1.5), pulse=(0.06, 12)),
        group('water', 'translucent', inner, glow=True, spin=(2, -4, 0), pulse=(0.1, 8)),
        group('water', 'translucent', shine, glow=True),
        group('water', 'translucent', drops, pulse=(0.2, 6))])

# ---------------------------------------------------------------- Lanza de hielo (Ice Smash / Icicle Lance)
# Carambano largo de seccion romboidal con nucleo blanco y esquirlas hacia atras.
def icicle():
    slices = [(-9, -7, 1.5), (-7, -4, 2.25), (-4, 0, 2.0), (0, 4, 1.5), (4, 7, 1.0), (7, 10, 0.5)]
    body = [box((0, 0, (z0 + z1) / 2), (2 * hw, 2 * hw, z1 - z0), lerp_hex('8fcdf5', 'e6f7ff', i / 5), 0.72, rot=(0, 0, 45))
            for i, (z0, z1, hw) in enumerate(slices)]
    core = [box((0, 0, 0), (0.8, 0.8, 16), 'ffffff', 0.85, rot=(0, 0, 45))]
    shards = []
    for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        shards.append(box((sx * 2.6, sy * 2.6, -5.5), (1.1, 1.1, 4.5), 'b7e2ff', 0.7, rot=(-sy * 28, sx * 28, 45)))
    return dict(name='Lanza de hielo', spells=['ice_smash', 'icicle_lance', 'freezing_rain'], groups=[
        group('ice', 'translucent', body + shards, glow=True, spin=(0, 0, 8)),
        group('ice', 'translucent', core, glow=True, spin=(0, 0, 8), flicker=0.2)])

# ---------------------------------------------------------------- Hoja de viento (Wind Slice / Sonic Boom / tajo de espada)
# Media luna horizontal convexa hacia adelante, filo blanco.
def wind_blade():
    outer, edge = [], []
    for a in range(-75, 76, 15):
        r = math.radians(a)
        w = 0.8 + 2.4 * math.cos(r) ** 2
        outer.append(box((8 * math.sin(r), 0, -4 + 8 * math.cos(r)), (3.4, 1.2, w), 'd9fff0', 0.5, rot=(0, a, 0)))
        edge.append(box((9.2 * math.sin(r), 0, -4 + 9.2 * math.cos(r)), (3.2, 0.7, 0.8), 'ffffff', 0.85, rot=(0, a, 0)))
    return dict(name='Hoja de viento', spells=['wind_slice', 'sonic_blast', 'sword_wind'], groups=[
        group('wind', 'translucent', outer, glow=True, pulse=(0.05, 4), flicker=0.25),
        group('wind', 'translucent', edge, glow=True, flicker=0.15)])

# ---------------------------------------------------------------- Taladro de viento (Tornado Impact)
# "Viento con forma de taladro que gira": anillos en espiral que se achican hacia la punta.
def wind_drill():
    rings = []
    for i in range(7):
        z = -7 + i * 2.4
        rad = 5.2 - i * 0.7
        n = 8 if rad > 2 else 5
        for k in range(n):
            a = math.radians(k * 360 / n + i * 22)
            rings.append(box((rad * math.cos(a), rad * math.sin(a), z), (max(0.8, rad * 0.75), 0.9, 2.0),
                             lerp_hex('cfeee2', 'ffffff', i / 6), 0.45, rot=(0, 0, math.degrees(a) + 90)))
    core = [box((0, 0, 0), (1.2, 1.2, 14), 'ffffff', 0.35)]
    return dict(name='Taladro de viento', spells=['tornado_impact'], groups=[
        group('wind', 'translucent', rings, glow=True, spin=(0, 0, 36), flicker=0.2),
        group('wind', 'translucent', core, glow=True, spin=(0, 0, -20))])

MODELS = dict(stone_bullet=stone_bullet, fireball=fireball, water_ball=water_ball, icicle=icicle,
              wind_blade=wind_blade, wind_drill=wind_drill)

# ---------------------------------------------------------------- Texturas 16x16 (casi blancas: el color lo pone cada caja)
def texture(mat):
    rnd = random.Random(sum(map(ord, mat)))
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            if mat == 'stone':
                v = 0.72 + 0.28 * n
                if (x + 2 * y) % 11 == 0 and n > 0.4: v = 0.55        # grietas
            elif mat == 'fire':
                v = 0.8 + 0.2 * n
                if (x * 7 + y * 3) % 9 == 0: v = 1.0
            elif mat == 'water':
                v = 0.82 + 0.12 * math.sin((x + y * 0.5) * 0.9) + 0.06 * n
            elif mat == 'ice':
                v = 0.85 + 0.1 * n
                if (x - y) % 6 == 0: v = 1.0                           # vetas diagonales
            else:  # wind
                v = 0.75 + 0.25 * (0.5 + 0.5 * math.sin(y * 1.3 + x * 0.25)) * (0.7 + 0.3 * n)
            c = max(0, min(255, round(255 * v)))
            px[x, y] = (c, c, c, 255)
    return img

def main():
    mdir = os.path.join(ASSETS, 'spell_models')
    tdir = os.path.join(ASSETS, 'textures', 'entity', 'spell')
    os.makedirs(mdir, exist_ok=True)
    os.makedirs(tdir, exist_ok=True)
    for name, fn in MODELS.items():
        with open(os.path.join(mdir, name + '.json'), 'w') as f:
            json.dump(fn(), f, indent=1)
    for mat in ('stone', 'fire', 'water', 'ice', 'wind'):
        texture(mat).save(os.path.join(tdir, mat + '.png'))
    print(len(MODELS), 'modelos ->', mdir)

if __name__ == '__main__':
    main()

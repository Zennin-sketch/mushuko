"""Tramos de los pilares de hechizo (Pilar de Hielo / Pilar de Tierra): modelos 3D de bloque y texturas.

Genera blockstates/<pilar>.json, models/block/<pilar>_<parte>.json y textures/block/<pilar>.png.
Cada pilar tiene base (ancha, con cristales o piedras sueltas), tramo del medio (encaja arriba y abajo con los demas)
y punta. Coordenadas en pixeles del bloque (0..16)."""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))


def el(f, t, rot=None, tex='#t'):
    e = dict(**{'from': list(f), 'to': list(t)}, faces={k: dict(texture=tex) for k in ('north', 'south', 'east', 'west', 'up', 'down')})
    if rot:
        axis, ang, origin = rot
        e['rotation'] = dict(axis=axis, angle=ang, origin=list(origin))
    return e


def y45(f, t):
    return el(f, t, ('y', 45, (8, 8, 8)))


ICE = {
    'middle': [
        y45((3, 0, 3), (13, 16, 13)),
        el((11, 4, 6.5), (13.5, 11, 9), ('z', 22.5, (12, 4, 7.5))),
        el((3, 9, 2.5), (5, 14, 4.5), ('x', -22.5, (4, 9, 3.5))),
    ],
    'bottom': [
        y45((3, 0, 3), (13, 16, 13)),
        y45((1.5, 0, 1.5), (14.5, 3, 14.5)),
        el((0, 0, 5), (3.5, 8, 8.5), ('z', -22.5, (2, 0, 7))),
        el((12.5, 0, 8), (16, 10, 11.5), ('z', 22.5, (14, 0, 10))),
        el((5.5, 0, 0), (9, 6, 3.5), ('x', 22.5, (7, 0, 2))),
        el((8, 0, 12.5), (11, 7, 15.5), ('x', -22.5, (9.5, 0, 14))),
    ],
    'top': [
        y45((3, 0, 3), (13, 8, 13)),
        y45((4.5, 8, 4.5), (11.5, 11, 11.5)),
        y45((6, 11, 6), (10, 13.5, 10)),
        y45((7.2, 13.5, 7.2), (8.8, 16, 8.8)),
        el((10.5, 3, 7), (12.5, 11, 9), ('z', 22.5, (11.5, 3, 8))),
    ],
}

EARTH = {
    'middle': [
        el((2.5, 0, 2.5), (13.5, 16, 13.5)),
        el((1.5, 3, 4), (2.5, 9, 11)),
        el((13.5, 8, 3), (14.5, 13, 9)),
        el((5, 10, 13.5), (11, 15, 14.5)),
        el((4, 1, 1.5), (10, 5, 2.5)),
    ],
    'bottom': [
        el((2, 0, 2), (14, 16, 14)),
        el((0, 0, 1), (16, 3, 15)),
        el((1, 0, 0), (15, 2.5, 16)),
        el((-1, 0, 4), (2, 4.5, 9)),
        el((14, 0, 7), (17, 5.5, 12)),
        el((5, 0, -1), (10, 3.5, 2)),
        el((7, 0, 14), (12, 4, 17)),
    ],
    'top': [
        el((3, 0, 3), (13, 10, 13)),
        el((4.5, 10, 4), (12, 13, 11.5)),
        el((5, 13, 5), (8, 15.5, 8)),
        el((8.5, 13, 7), (11, 14.5, 10)),
        el((6, 13, 9), (9, 14, 11)),
        el((2, 2, 5), (3, 7, 10)),
    ],
}


def tex_ice():
    rnd = random.Random(3)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = (x + y) / 30
            r, g, b = int(150 + 70 * t), int(205 + 40 * t), 255
            n = rnd.randint(-10, 10)
            a = 170
            if (x + 2 * y) % 11 == 0 or (2 * x - y) % 13 == 0:
                r, g, b, a = 245, 252, 255, 215
            if x in (0, 15) or y in (0, 15):
                r, g, b, a = int(r * 0.8), int(g * 0.85), b, 200
            img.putpixel((x, y), (max(0, min(255, r + n)), max(0, min(255, g + n)), b, a))
    return img


def tex_earth():
    rnd = random.Random(7)
    img = Image.new('RGBA', (16, 16))
    base = [(122, 108, 92), (138, 122, 104), (104, 92, 78), (150, 136, 116)]
    for y in range(16):
        for x in range(16):
            c = base[rnd.randrange(4)] if rnd.random() < 0.35 else base[(x // 4 + y // 3) % 2]
            if (x * 3 + y * 5) % 17 == 0 or (y == 7 and 3 < x < 12) or (x == 10 and 2 < y < 7):
                c = (72, 62, 52)
            if rnd.random() < 0.08:
                c = (96, 120, 70)  # musgo
            img.putpixel((x, y), c + (255,))
    return img


def write(name, parts, tex):
    os.makedirs(os.path.join(A, 'models', 'block'), exist_ok=True)
    for part, els in parts.items():
        m = dict(parent='block/block', textures=dict(t='remushoku:block/' + name, particle='remushoku:block/' + name), elements=els)
        if name == 'ice_pillar':
            m['ambientocclusion'] = False
        with open(os.path.join(A, 'models', 'block', '%s_%s.json' % (name, part)), 'w') as f:
            json.dump(m, f, indent=1)
    with open(os.path.join(A, 'blockstates', name + '.json'), 'w') as f:
        json.dump(dict(variants={'part=' + p: dict(model='remushoku:block/%s_%s' % (name, p)) for p in parts}), f, indent=1)
    tex.save(os.path.join(A, 'textures', 'block', name + '.png'))


write('ice_pillar', ICE, tex_ice())
write('earth_pillar', EARTH, tex_earth())
print('pilares listos')

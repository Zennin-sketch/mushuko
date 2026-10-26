"""Arcos con modelo 3D (elementos de modelo de item, en voxeles finos) y 4 estados: quieto y tensando 0/1/2.

Mismo acomodo que el arco de vanilla (asi sirven sus transformaciones de mano): la cuerda va en diagonal de arriba a
la izquierda a abajo a la derecha, la madera se curva hacia la esquina de arriba a la derecha y la flecha apunta hacia
arriba a la derecha. Al tensar, el punto de encaje de la cuerda baja hacia la esquina de abajo a la izquierda, las
palas se doblan y aparece la flecha.

Genera:
  assets/remushoku/models/item/<arco>.json (+ _pulling_0/1/2)  y  textures/item/bows/<arco>.png (paleta de colores)
"""
import json, math, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))

D = 1 / math.sqrt(2)
UP_RIGHT = (D, D)        # hacia donde apunta la flecha
NOCK_DIR = (-D, -D)      # hacia donde se tensa la cuerda


class Palette:
    def __init__(self):
        self.colors = []

    def cell(self, rgba):
        rgba = tuple(rgba) if len(rgba) == 4 else tuple(rgba) + (255,)
        if rgba not in self.colors:
            self.colors.append(rgba)
        i = self.colors.index(rgba)
        return i % 16, i // 16

    def image(self):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for i, c in enumerate(self.colors):
            img.putpixel((i % 16, i // 16), c)
        return img


def hexc(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


class Model:
    def __init__(self, pal):
        self.pal = pal
        self.el = []

    def cube(self, x, y, w, h, z0, z1, color):
        u, v = self.pal.cell(color)
        x0, y0 = max(0.0, x - w / 2), max(0.0, y - h / 2)
        x1, y1 = min(16.0, x + w / 2), min(16.0, y + h / 2)
        if x1 - x0 < 0.05 or y1 - y0 < 0.05:
            return
        face = {'uv': [u, v, u + 1, v + 1], 'texture': '#pal'}
        self.el.append({'from': [round(x0, 3), round(y0, 3), round(z0, 3)], 'to': [round(x1, 3), round(y1, 3), round(z1, 3)],
                        'faces': {f: dict(face) for f in ('north', 'south', 'east', 'west', 'up', 'down')}})

    def line(self, p0, p1, size, depth, color, step=0.45, color1=None):
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        n = max(1, int(math.hypot(dx, dy) / step))
        for k in range(n + 1):
            t = k / n
            c = color if color1 is None else lerp(color, color1, t)
            self.cube(p0[0] + dx * t, p0[1] + dy * t, size, size, 8 - depth / 2, 8 + depth / 2, c)


def bezier(p0, p1, p2, t):
    a = [(1 - t) ** 2 * p0[i] + 2 * (1 - t) * t * p1[i] + t * t * p2[i] for i in range(2)]
    return a


def build(spec, stage):
    """stage: -1 quieto, 0..2 tensando."""
    pal = spec['pal']
    m = Model(pal)
    pull = [0, 2.6, 4.0, 5.4][stage + 1]
    flex = [0, 0.35, 0.6, 0.9][stage + 1]
    # puntas y empunadura (en el plano 0..16)
    ta, tb = list(spec['tip_a']), list(spec['tip_b'])
    grip = spec['grip']
    # al tensar, las puntas se doblan hacia la cuerda y hacia el centro
    for t in (ta, tb):
        t[0] += NOCK_DIR[0] * flex * 1.2 + (grip[0] - t[0]) * 0.05 * flex
        t[1] += NOCK_DIR[1] * flex * 1.2 + (grip[1] - t[1]) * 0.05 * flex
    ctrl_a = spec['ctrl_a']
    ctrl_b = spec['ctrl_b']
    # palas: dos curvas de la empunadura a cada punta, mas gruesas cerca de la mano
    for tip, ctrl in ((ta, ctrl_a), (tb, ctrl_b)):
        N = 26
        prev = None
        for k in range(N + 1):
            u = k / N
            p = bezier(grip, ctrl, tip, u)
            w = spec['w0'] + (spec['w1'] - spec['w0']) * u
            col = lerp(spec['wood0'], spec['wood1'], u)
            if spec.get('stripe') and k % 4 == 2:
                col = spec['stripe']
            m.cube(p[0], p[1], w, w, 8 - spec['depth'] / 2, 8 + spec['depth'] / 2, col)
            if spec.get('edge') and 0.15 < u < 0.92:
                # filo de otro color del lado de afuera (arriba a la derecha)
                m.cube(p[0] + 0.32 * w, p[1] + 0.32 * w, w * 0.45, w * 0.45, 8 - spec['depth'] / 2 - 0.05,
                       8 + spec['depth'] / 2 + 0.05, spec['edge'])
            prev = p
    # empunadura
    for dx in (-0.9, -0.3, 0.3, 0.9):
        m.cube(grip[0] + dx * D, grip[1] + dx * -D, spec['w0'] + 0.5, spec['w0'] + 0.5,
               8 - spec['depth'] / 2 - 0.25, 8 + spec['depth'] / 2 + 0.25, spec['grip_c'] if abs(dx) < 0.9 else spec['grip_c2'])
    # decoraciones propias de cada arco
    for deco in spec.get('deco', []):
        deco(m, ta, tb, grip, stage)
    # cuerda (con encaje)
    nock = ((ta[0] + tb[0]) / 2 + NOCK_DIR[0] * pull, (ta[1] + tb[1]) / 2 + NOCK_DIR[1] * pull)
    m.line(ta, nock, spec['string_w'], spec['string_w'], spec['string'], step=0.3)
    m.line(nock, tb, spec['string_w'], spec['string_w'], spec['string'], step=0.3)
    # flecha
    if stage >= 0:
        L = 10.6 + pull * 0.9
        head = (nock[0] + UP_RIGHT[0] * L, nock[1] + UP_RIGHT[1] * L)
        m.line(nock, (head[0] - UP_RIGHT[0] * 1.6, head[1] - UP_RIGHT[1] * 1.6), 0.55, 0.55, hexc('8a6440'), step=0.35)
        for k in range(4):  # punta
            s = 1.5 - k * 0.35
            m.cube(head[0] - UP_RIGHT[0] * (1.6 - k * 0.45), head[1] - UP_RIGHT[1] * (1.6 - k * 0.45), s, s, 7.7, 8.3, hexc('c8ccd4'))
        for k in range(3):  # plumas
            c = (nock[0] + UP_RIGHT[0] * (0.8 + k * 0.7), nock[1] + UP_RIGHT[1] * (0.8 + k * 0.7))
            m.cube(c[0] - 0.45, c[1] + 0.45, 0.6, 0.6, 7.75, 8.25, hexc('e8e4dc'))
            m.cube(c[0] + 0.45, c[1] - 0.45, 0.6, 0.6, 7.75, 8.25, hexc('e8e4dc'))
    return m.el


DISPLAY = {
    'thirdperson_righthand': {'rotation': [-80, 260, -40], 'translation': [-1, -2, 2.5], 'scale': [0.9, 0.9, 0.9]},
    'thirdperson_lefthand': {'rotation': [-80, -280, 40], 'translation': [-1, -2, 2.5], 'scale': [0.9, 0.9, 0.9]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
}


# ---------------------------------------------------------------- decoraciones
def leaf_tips(color, vein):
    def f(m, ta, tb, grip, stage):
        for t, sgn in ((ta, 1), (tb, -1)):
            for k in range(3):
                m.cube(t[0] + 0.5 + k * 0.45, t[1] + 0.9 * sgn - k * 0.15 * sgn, 1.2 - k * 0.3, 0.8, 7.3, 8.7, color)
            m.cube(t[0] + 0.6, t[1] + 0.5 * sgn, 0.5, 0.5, 7.2, 8.8, vein)
        for k in range(3):  # incrustacion plateada en la empunadura
            m.cube(grip[0] + 0.9 + k * 0.4, grip[1] + 0.9 + k * 0.4, 0.4, 0.4, 7.0, 9.0, hexc('dfe6ee'))
    return f


def fangs(color):
    def f(m, ta, tb, grip, stage):
        for t, sgn in ((ta, 1), (tb, -1)):
            for k in range(4):
                s = 1.1 - k * 0.25
                m.cube(t[0] - 0.4 * k * 0.6, t[1] + sgn * (0.6 + k * 0.5), s, s, 7.4, 8.6, color)
        for dx in (-1.6, 1.6):  # piel en la empunadura
            m.cube(grip[0] + dx * D, grip[1] - dx * D, 1.6, 1.6, 7.0, 9.0, hexc('8a6a48'))
    return f


def crystals(c0, c1):
    def f(m, ta, tb, grip, stage):
        # cristal flotando delante de la empunadura y puntas de cristal
        cx, cy = grip[0] + 1.8, grip[1] + 1.8
        for k, s in enumerate((1.6, 1.1, 0.6)):
            m.cube(cx + k * 0.5, cy + k * 0.5, s, s, 8 - s / 2, 8 + s / 2, lerp(c0, c1, k / 2))
        for t in (ta, tb):
            m.cube(t[0], t[1], 1.2, 1.2, 7.4, 8.6, c1)
    return f


def horns(color, gem):
    def f(m, ta, tb, grip, stage):
        for t, sgn in ((ta, 1), (tb, -1)):
            # cuerno en espiral hacia afuera
            for k in range(6):
                a = k * 0.9
                r = 1.4 - k * 0.18
                m.cube(t[0] + 0.6 + r * math.cos(a), t[1] + sgn * (0.6 + r * math.sin(a) * 0.8), 0.9 - k * 0.1, 0.9 - k * 0.1,
                       7.3, 8.7, color)
        m.cube(grip[0] + 1.0, grip[1] + 1.0, 1.3, 1.3, 6.9, 9.1, gem)
        m.cube(grip[0] + 1.0, grip[1] + 1.0, 0.5, 0.5, 6.8, 9.2, hexc('ffd0d0'))
    return f


def wraps(color):
    def f(m, ta, tb, grip, stage):
        for t in (ta, tb):
            m.cube(t[0], t[1], 1.0, 1.0, 7.3, 8.7, color)
    return f


def spec(**k):
    k.setdefault('pal', Palette())
    return k


BOWS = {
    'hunter_bow': spec(
        name='Arco de cazador', tip_a=(2.2, 13.8), tip_b=(13.8, 2.2), grip=(10.6, 10.6), ctrl_a=(7.8, 14.6), ctrl_b=(14.6, 7.8),
        w0=1.5, w1=0.8, depth=1.3, wood0=hexc('8a5a32'), wood1=hexc('b98a56'), grip_c=hexc('5a3a22'), grip_c2=hexc('3e2614'),
        string=hexc('e8e0cc'), string_w=0.35, deco=[wraps(hexc('5a3a22'))], speed=1.0, damage=1.1, durability=500),
    'elven_bow': spec(
        name='Arco élfico del bosque', tip_a=(1.6, 13.2), tip_b=(13.2, 1.6), grip=(10.9, 10.9), ctrl_a=(9.5, 16.5), ctrl_b=(16.5, 9.5),
        w0=1.3, w1=0.6, depth=1.1, wood0=hexc('d8c79a'), wood1=hexc('f0e6c8'), edge=hexc('6aa860'), grip_c=hexc('3f6e3a'),
        grip_c2=hexc('cfd8de'), string=hexc('f4fff0'), string_w=0.3, deco=[leaf_tips(hexc('5fae55'), hexc('b8e6a0'))],
        speed=1.25, damage=1.0, durability=700),
    'beast_bow': spec(
        name='Arco de hueso de bestia', tip_a=(2.5, 13.5), tip_b=(13.5, 2.5), grip=(10.3, 10.3), ctrl_a=(7.0, 14.2), ctrl_b=(14.2, 7.0),
        w0=2.0, w1=1.1, depth=1.6, wood0=hexc('e6dcc4'), wood1=hexc('cfc2a2'), stripe=hexc('5a4030'), grip_c=hexc('6a4a30'),
        grip_c2=hexc('8a6a48'), string=hexc('b89a70'), string_w=0.45, deco=[fangs(hexc('f4efe2'))],
        speed=0.85, damage=1.35, durability=900),
    'mana_bow': spec(
        name='Arco de cristal de maná', tip_a=(1.9, 13.6), tip_b=(13.6, 1.9), grip=(10.7, 10.7), ctrl_a=(8.6, 15.8), ctrl_b=(15.8, 8.6),
        w0=1.4, w1=0.7, depth=1.2, wood0=hexc('3d7fe0', 220), wood1=hexc('a8dcff', 200), edge=hexc('e8f6ff', 230),
        grip_c=hexc('2a3a66'), grip_c2=hexc('c8b06a'), string=hexc('bff0ff'), string_w=0.3,
        deco=[crystals(hexc('5cb2f6', 210), hexc('e6f8ff', 230))], speed=1.1, damage=1.2, durability=800),
    'demon_bow': spec(
        name='Arco del Rey Demonio', tip_a=(2.0, 13.9), tip_b=(13.9, 2.0), grip=(10.5, 10.5), ctrl_a=(8.0, 15.4), ctrl_b=(15.4, 8.0),
        w0=1.8, w1=0.9, depth=1.5, wood0=hexc('2a2430'), wood1=hexc('4a3f52'), edge=hexc('b02a2a'), grip_c=hexc('1a1418'),
        grip_c2=hexc('6a1a1a'), string=hexc('ff6a5a'), string_w=0.35, deco=[horns(hexc('d8cfc0'), hexc('d01a2a'))],
        speed=0.9, damage=1.5, durability=1200),
}


def main():
    mdir = os.path.join(ASSETS, 'models', 'item')
    tdir = os.path.join(ASSETS, 'textures', 'item', 'bows')
    os.makedirs(tdir, exist_ok=True)
    info = {}
    for bid, sp in BOWS.items():
        sp['pal'] = Palette()
        for stage in (-1, 0, 1, 2):
            els = build(sp, stage)
            name = bid if stage < 0 else '%s_pulling_%d' % (bid, stage)
            model = {'gui_light': 'front', 'textures': {'pal': 'remushoku:item/bows/' + bid, 'particle': 'remushoku:item/bows/' + bid},
                     'elements': els, 'display': DISPLAY}
            if stage < 0:
                model['overrides'] = [
                    {'predicate': {'pulling': 1}, 'model': 'remushoku:item/%s_pulling_0' % bid},
                    {'predicate': {'pulling': 1, 'pull': 0.65}, 'model': 'remushoku:item/%s_pulling_1' % bid},
                    {'predicate': {'pulling': 1, 'pull': 0.9}, 'model': 'remushoku:item/%s_pulling_2' % bid},
                ]
            with open(os.path.join(mdir, name + '.json'), 'w') as f:
                json.dump(model, f, separators=(',', ':'))
        sp['pal'].image().save(os.path.join(tdir, bid + '.png'))
        info[bid] = dict(name=sp['name'], speed=sp['speed'], damage=sp['damage'], durability=sp['durability'])
        print(bid, len(els), 'elementos,', len(sp['pal'].colors), 'colores')
    with open(os.path.join(HERE, 'bows_info.json'), 'w') as f:
        json.dump(info, f, indent=1, ensure_ascii=False)


if __name__ == '__main__':
    main()

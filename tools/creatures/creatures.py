"""Criaturas del mundo de Mushoku Tensei hechas a mano en codigo (modelo, texturas y animaciones de GeckoLib).
Uso: python3 creatures.py  -> escribe geo/, animations/ y textures/entities/ de cada una."""
import math, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo import Model, Fur, Solid, Scales, Bark, Leaves, Rock, Crystal, Membrane, Vine, Striped, Plates, cycle, S, C, pulse

EYE_DARK = Solid('1a1210', 0.02)
TEETH = Solid('efe8d8', 0.03)
CLAW = Solid('3a3430', 0.05)
NOSE = Solid('201818', 0.03)


# ===================================================================== canidos (Coyote Jauria, Lobo Corrosivo, Bestia Santa)
def canine(name, L, H, W, bh, head, mats, ruff=False, tail_len=9, ear=4, mane=False, horn=False, drool=False, mark=False):
    """L largo del cuerpo, H alto de patas, W ancho, bh alto del cuerpo, head = tamano de cabeza."""
    m = Model(name, 128 if L < 30 else 256)
    top = H + bh
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, H + bh / 2, 0))
    m.cube('body', (-W / 2, H, -L / 2), (W, bh, L * 0.55), 'fur', inflate=0.3)          # pecho
    m.cube('body', (-W / 2 + 0.5, H + 0.5, L * 0.05), (W - 1, bh - 1, L * 0.45), 'fur')  # lomo y cadera
    if ruff:
        m.cube('body', (-W / 2 - 1, H - 1, -L / 2 - 1), (W + 2, bh + 2, L * 0.3), 'mane', inflate=0.2)
    # cuello y cabeza
    m.bone('neck', 'body', (0, top - 2, -L / 2 + 2))
    m.cube('neck', (-W * 0.3, top - 4.5, -L / 2 - 2.5), (W * 0.6, bh * 0.75, 5.5), 'mane' if mane else 'fur', rot=(-15, 0, 0), pivot=(0, top - 2, -L / 2 + 2))
    hx, hy, hz = 0, top - 0.5, -L / 2 - 3
    m.bone('head', 'neck', (hx, hy, hz))
    hw, hh, hd = head
    m.cube('head', (-hw / 2, hy - hh / 2, hz - hd), (hw, hh, hd), 'fur')
    sw, sh_, sd = hw * 0.5, hh * 0.45, hd * 0.9
    m.cube('head', (-sw / 2, hy - hh / 2, hz - hd - sd), (sw, sh_, sd), 'snout')
    m.cube('head', (-0.9, hy - hh / 2 + sh_ - 1.2, hz - hd - sd - 0.3), (1.8, 1.2, 0.8), NOSE)
    for side in (-1, 1):
        ex = side * (hw / 2 - 1.2) - 0.75
        m.cube('head', (ex, hy + 0.3, hz - hd - 0.1), (1.5, 1, 0.3), 'eye')
        # orejas
        ex2 = side * (hw / 2 - 1.3)
        m.bone('ear_' + ('l' if side > 0 else 'r'), 'head', (ex2, hy + hh / 2, hz - hd * 0.4))
        en = 'ear_' + ('l' if side > 0 else 'r')
        m.cube(en, (ex2 - 1.2, hy + hh / 2 - 0.3, hz - hd * 0.5), (2.4, ear * 0.6, 1.2), 'ear', rot=(-8, 0, side * -14), pivot=(ex2, hy + hh / 2, hz - hd * 0.4))
        m.cube(en, (ex2 - 0.6 - side * 0.3, hy + hh / 2 - 0.3 + ear * 0.6, hz - hd * 0.5 + 0.2), (1.2, ear * 0.45, 0.8), 'ear',
               rot=(-8, 0, side * -14), pivot=(ex2, hy + hh / 2, hz - hd * 0.4))
    if drool:  # gotas de acido colgando de los colmillos
        for i, x in enumerate((-sw / 2 + 0.6, sw / 2 - 1.4)):
            m.cube('jaw', (x, hy - hh / 2 - 1.2 - 2 - i, hz - hd - sd + 0.8), (0.8, 2 + i, 0.8), 'drool')
    if mark:  # marca dorada en la frente
        m.cube('head', (-1.5, hy + hh / 2 - 2.5, hz - hd - 0.15), (3, 2, 0.3), 'gold')
    if horn:
        for side in (-1, 1):
            m.cube('head', (side * 2 - 0.5, hy + hh / 2 - 0.5, hz - hd * 0.7), (1, 5, 1), 'horn', rot=(-35, 0, side * -18), pivot=(side * 2, hy + hh / 2, hz - hd * 0.6))
    m.bone('jaw', 'head', (0, hy - hh / 2 + 0.5, hz - hd + 0.5))
    m.cube('jaw', (-sw / 2 + 0.3, hy - hh / 2 - 1.2, hz - hd - sd + 0.4), (sw - 0.6, 1.5, sd - 0.4), 'snout')
    for side in (-1, 1):
        m.cube('jaw', (side * (sw / 2 - 1) - 0.25, hy - hh / 2 + 0.2, hz - hd - sd + 0.6), (0.5, 1, 0.5), TEETH)
        m.cube('head', (side * (sw / 2 - 1) - 0.25, hy - hh / 2 - 0.5, hz - hd - sd + 0.8), (0.5, 1, 0.5), TEETH)
    # patas: muslo y pata con rodilla
    legs = {'fl': (-1, -L / 2 + 3), 'fr': (1, -L / 2 + 3), 'bl': (-1, L / 2 - 4), 'br': (1, L / 2 - 4)}
    lw = max(2, W * 0.28)
    for n, (side, z) in legs.items():
        x = side * (W / 2 - lw / 2 - 0.2)
        m.bone('leg_' + n, 'body', (x, H + 1, z))
        m.cube('leg_' + n, (x - lw / 2, H * 0.45, z - lw / 2), (lw, H * 0.55 + 1.5, lw + 0.5), 'fur')
        m.bone('shin_' + n, 'leg_' + n, (x, H * 0.48, z))
        m.cube('shin_' + n, (x - lw / 2 + 0.45, 0.8, z - lw / 2 + 0.45), (lw - 0.9, H * 0.48, lw - 0.9), 'leg')
        m.cube('shin_' + n, (x - lw / 2, 0, z - lw / 2 - 1.2), (lw, 1.2, lw + 1.2), 'paw')
        for k in (-1, 0, 1):
            m.cube('shin_' + n, (x + k * lw * 0.3 - 0.25, 0, z - lw / 2 - 1.7), (0.5, 0.6, 0.6), CLAW)
    # cola en dos tramos
    m.bone('tail', 'body', (0, top - 1.5, L / 2))
    # cola: sale hacia atras y cae (colgando como la de un canido)
    m.cube('tail', (-1.5, top - 3, L / 2 - 0.5), (3, 3, tail_len * 0.55), 'tail', inflate=0.3, rot=(-20, 0, 0), pivot=(0, top - 1.5, L / 2))
    tz = L / 2 + tail_len * 0.55 * 0.94
    ty = top - 1.5 - tail_len * 0.55 * 0.34
    m.bone('tail2', 'tail', (0, ty, tz))
    m.cube('tail2', (-1.7, ty - 1.7, tz - 0.5), (3.4, 3.4, tail_len * 0.45), 'tail', inflate=0.4, rot=(-35, 0, 0), pivot=(0, ty, tz))
    tl = tail_len * 0.45
    m.cube('tail2', (-1.4, ty - 1.4 - tl * 0.57, tz + tl * 0.82 - 0.5), (2.8, 2.8, 2.5), 'tail_tip', inflate=0.3, rot=(-35, 0, 0),
           pivot=(0, ty - tl * 0.57, tz + tl * 0.82))

    def idle(t):
        b = S(t)
        return {'body': {'position': [0, 0.25 * b, 0]}, 'head': {'rotation': [2 * S(t, 0.2), 12 * S(t, 0.1, 1) * pulse(t, 0.3, 0.9), 0]},
                'ear_l': {'rotation': [0, 0, 8 * pulse(t, 0.55, 0.62)]}, 'ear_r': {'rotation': [0, 0, -6 * pulse(t, 0.2, 0.27)]},
                'tail': {'rotation': [0, 10 * S(t, 0, 2), 0]}, 'tail2': {'rotation': [0, 14 * S(t, -0.1, 2), 0]},
                'jaw': {'rotation': [3 + 3 * S(t), 0, 0]}}

    def walk(t):
        out = {'body': {'position': [0, 0.6 * abs(S(t)), 0], 'rotation': [0, 0, 2 * S(t)]},
               'head': {'rotation': [3 * S(t, 0.25, 2), 0, 0]}, 'tail': {'rotation': [8 * S(t, 0, 2), 12 * S(t), 0]},
               'tail2': {'rotation': [0, 16 * S(t, -0.15), 0]}, 'jaw': {'rotation': [6, 0, 0]}}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            s = S(t, ph)
            out['leg_' + n] = {'rotation': [32 * s, 0, 0]}
            back = n.startswith('b')
            out['shin_' + n] = {'rotation': [(25 if back else -30) * max(0, S(t, ph + 0.25)), 0, 0]}
        return out

    def attack(t):
        lunge = pulse(t, 0.0, 0.8)
        bite = pulse(t, 0.25, 0.6)
        return {'body': {'position': [0, -0.5 * lunge, -4 * lunge], 'rotation': [6 * lunge, 0, 0]},
                'head': {'rotation': [-12 * bite + 10 * pulse(t, 0.55, 0.85), 0, 0]}, 'jaw': {'rotation': [40 * bite, 0, 0]},
                'leg_fl': {'rotation': [-30 * lunge, 0, 0]}, 'leg_fr': {'rotation': [-25 * lunge, 0, 0]},
                'leg_bl': {'rotation': [20 * lunge, 0, 0]}, 'leg_br': {'rotation': [25 * lunge, 0, 0]},
                'ear_l': {'rotation': [-25 * lunge, 0, 0]}, 'ear_r': {'rotation': [-25 * lunge, 0, 0]}}

    anims = {'idle': cycle(3.0, 12, idle), 'walk': cycle(0.8, 8, walk), 'attack': cycle(0.6, 8, attack, loop=False)}
    return m, anims


def coyote():
    m, a = canine('pack_coyote', L=22, H=8, W=7, bh=7, head=(6, 5, 5), mats=None, tail_len=10, ear=4)
    pal = dict(fur=Fur('b08a5a', '6e5232', 'd8c09a', belly='e2d2b0', stripes='5a4026', stripe_every=6),
               mane=Fur('9a7a50', '5a4026'), snout=Fur('d4bc94', '9a7e5a'), ear=Fur('8a6a44', '4a3420'),
               leg=Fur('a07e52', '5e4428'), paw=Fur('4a3828', '2a1e16'), tail=Fur('a07a4c', '5e4428'),
               tail_tip=Fur('3a2a1e', '1e1610'), eye=Solid('e8b030', 0.02), horn=Solid('cccccc'))
    m.save({'pack_coyote': pal}, a)


def acid_wolf():
    m, a = canine('acid_wolf', L=26, H=11, W=9, bh=9, head=(7, 6, 6.5), mats=None, ruff=True, tail_len=12, ear=4.5, drool=True)
    pal = dict(fur=Fur('2c3a2a', '141c14', '4a5e40', belly='3a4630', stripes='6cff3a', stripe_every=5),
               mane=Fur('1e2a1e', '0c120c', '3a4a32'), snout=Fur('34402e', '1a2216'), ear=Fur('222c20', '0e140e'),
               leg=Fur('263224', '101810'), paw=Fur('121812', '080c08'), tail=Fur('283626', '101810', stripes='6cff3a', stripe_every=4),
               tail_tip=Fur('1a221a', '0a0e0a'), eye=Solid('9cff3c', 0.02, glow=True), drool=Solid('8cff40', 0.05, glow=True),
               horn=Solid('cccccc'))
    m.save({'acid_wolf': pal}, a)


def sacred_beast():
    m, a = canine('sacred_beast', L=34, H=13, W=12, bh=12, head=(9, 8, 8.5), mats=None, ruff=True, tail_len=18, ear=6, mane=True, mark=True)
    pal = dict(fur=Fur('f4f2ec', 'c8c4bc', 'ffffff', belly='fffdf8', stripes='e8c25a', stripe_every=7),
               mane=Fur('fffdf6', 'd6d2c8', 'ffffff'), snout=Fur('f8f6f0', 'd8d4cc'), ear=Fur('ece8e0', 'b8b2a8'),
               leg=Fur('eeece4', 'c0bab0'), paw=Fur('d8d2c4', 'a8a090'), tail=Fur('fafaf4', 'd0ccc4', stripes='e8c25a', stripe_every=5),
               tail_tip=Fur('fff6d8', 'e8c25a'), eye=Solid('ffd040', 0.02, glow=True), gold=Solid('ffd24a', 0.04, glow=True),
               horn=Solid('ffe6a0'))
    m.save({'sacred_beast': pal}, a)


# ===================================================================== Tortuga Petrea
def rock_turtle():
    m = Model('rock_turtle', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 14, 0))
    m.cube('body', (-15, 8, -18), (30, 6, 36), 'belly')                       # plastron
    for i, (w, h, d, y) in enumerate(((34, 7, 40, 13), (28, 6, 34, 20), (20, 5, 26, 26), (12, 3, 16, 31))):
        m.cube('body', (-w / 2, y, -d / 2), (w, h, d), 'shell')
    # rocas y cristales del caparazon
    for (x, z, s, h) in ((-8, -6, 6, 6), (6, 4, 7, 7), (-3, 9, 5, 5), (9, -10, 4, 5), (-12, 8, 4, 4), (2, -14, 5, 4)):
        m.cube('body', (x - s / 2, 28 + (h > 5) * 1, z - s / 2), (s, h, s), 'rock', rot=(0, (x * 13) % 45, 8), pivot=(x, 28, z))
    m.bone('neck', 'body', (0, 16, -18))
    m.cube('neck', (-4, 12, -26), (8, 7, 9), 'skin')
    m.bone('head', 'neck', (0, 16, -25))
    m.cube('head', (-5, 12, -35), (10, 8, 10), 'skin')
    m.cube('head', (-4.5, 18, -35.5), (9, 2.5, 8), 'rock')                   # frente de piedra
    for side in (-1, 1):
        m.cube('head', (side * 4.6 - 0.5, 16, -33), (1, 1.5, 2), EYE_DARK)
    m.bone('jaw', 'head', (0, 13, -27))
    m.cube('jaw', (-4.5, 10.5, -35), (9, 2.5, 9), 'skin')
    for n, (x, z) in {'fl': (-14, -12), 'fr': (14, -12), 'bl': (-14, 12), 'br': (14, 12)}.items():
        m.bone('leg_' + n, 'body', (x, 12, z))
        m.cube('leg_' + n, (x - 4, 0, z - 4), (8, 13, 8), 'skin')
        m.cube('leg_' + n, (x - 4.5, 0, z - 5.5), (9, 3, 3), 'rock')
        for k in (-1, 1):
            m.cube('leg_' + n, (x + k * 2.5 - 0.75, 0, z - 6), (1.5, 1, 1), CLAW)
    m.bone('tail', 'body', (0, 13, 18))
    m.cube('tail', (-2.5, 11, 18), (5, 4, 8), 'skin', rot=(15, 0, 0), pivot=(0, 13, 18))

    def idle(t):
        return {'body': {'position': [0, 0.3 * S(t), 0]}, 'head': {'rotation': [4 * S(t, 0.1), 15 * S(t, 0, 0.5) * pulse(t, 0.2, 1), 0]},
                'neck': {'position': [0, 0, -1.2 * pulse(t, 0.4, 0.9)]}, 'jaw': {'rotation': [4 * pulse(t, 0.6, 0.75), 0, 0]},
                'tail': {'rotation': [0, 8 * S(t), 0]}}

    def walk(t):
        out = {'body': {'rotation': [0, 3 * S(t), 3 * S(t)], 'position': [0, 0.5 * abs(S(t)), 0]},
               'head': {'rotation': [0, -4 * S(t), 0]}, 'tail': {'rotation': [0, 10 * S(t), 0]}}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            out['leg_' + n] = {'rotation': [18 * S(t, ph), 0, 6 * S(t, ph + 0.25)]}
        return out

    def attack(t):
        p = pulse(t, 0, 0.7)
        snap = pulse(t, 0.3, 0.6)
        return {'neck': {'position': [0, 1 * p, -6 * p]}, 'head': {'rotation': [-15 * snap + 8 * p, 0, 0]},
                'jaw': {'rotation': [45 * snap, 0, 0]}, 'body': {'rotation': [4 * p, 0, 0]}}

    pal = dict(shell=Rock('8a8478', '5e5a52', moss='5e8a3a', moss_top=0.35), rock=Rock('9c968a', '6a655c', moss='6a9a40', moss_top=0.5),
               belly=Rock('b8ae98', '8a8070'), skin=Fur('7a7e6a', '4e5244', '9aa088', belly='a8a890'))
    m.save({'rock_turtle': pal}, {'idle': cycle(4.0, 12, idle), 'walk': cycle(1.6, 8, walk), 'attack': cycle(0.8, 8, attack, loop=False)})


# ===================================================================== serpientes (Anaconda Almendra, Cobras)
def snake(name, n, seg_len, thick, raise_front=0, hood=False, fangs='fang'):
    """Cadena de segmentos armada de la cola hacia la cabeza (s0 = cuello). Cada segmento cuelga del de atras, asi
    al levantar los de adelante (cobra) se levanta solo la parte delantera."""
    m = Model(name, 256 if n > 8 else 128)
    m.bone('root', None, (0, 0, 0))
    y = thick / 2
    front = -n * seg_len / 2
    parent = 'root'
    for i in reversed(range(n)):
        th = thick * (1.0 if i < n * 0.6 else 1 - (i - n * 0.6) / (n * 0.5))
        th = max(1.5, th)
        b = 's%d' % i
        z0 = front + i * seg_len           # frente de este segmento
        rot = [-90.0 / raise_front, 0, 0] if (raise_front and i < raise_front) else None
        m.bone(b, parent, (0, y, z0 + seg_len), rot)
        m.cube(b, (-th / 2, y - th / 2, z0 - 0.2), (th, th, seg_len + 0.4), 'scales')
        if hood and i == 0:
            m.cube(b, (-th * 1.5, y - th / 2 + 0.6, z0 + 0.5), (th * 3.0, th * 0.3, seg_len * 1.1), 'hood')
        parent = b
    hz = front
    hw = thick * 1.25
    m.bone('head', 's0', (0, y, hz), [90 if raise_front else 0, 0, 0])
    m.cube('head', (-hw / 2, y - thick * 0.45, hz - thick * 1.4), (hw, thick * 0.75, thick * 1.4), 'head')
    for side in (-1, 1):
        m.cube('head', (side * (hw / 2 - 0.2) - 0.3, y + thick * 0.05, hz - thick * 1.1), (0.6, 0.8, 0.8), 'eye')
    m.bone('jaw', 'head', (0, y - thick * 0.4, hz))
    m.cube('jaw', (-hw / 2 + 0.3, y - thick * 0.75, hz - thick * 1.3), (hw - 0.6, thick * 0.32, thick * 1.3), 'belly')
    for side in (-1, 1):
        m.cube('head', (side * hw * 0.25 - 0.3, y - thick * 0.75, hz - thick * 1.3), (0.6, thick * 0.35, 0.6), fangs)
    m.bone('tongue', 'jaw', (0, y - thick * 0.5, hz - thick * 1.3))
    m.cube('tongue', (-0.3, y - thick * 0.55, hz - thick * 1.3 - 2.5), (0.6, 0.2, 2.5), 'tongue')

    def wave(t, amp, k=0.9, ground_from=0):
        out = {}
        for i in range(n):
            a = amp * (0.4 + 0.6 * min(1, i / 3)) * S(t, -i * 0.11 * k)
            if i < ground_from:
                a *= 0.3
            out['s%d' % i] = {'rotation': [0, a, 0]}
        return out

    def idle(t):
        out = wave(t, 3, ground_from=raise_front)
        out['head'] = {'rotation': [4 * S(t, 0.3), 8 * S(t, 0.1, 0.5), 0]}
        out['tongue'] = {'position': [0, 0, -1.2 * (pulse(t, 0.1, 0.18) + pulse(t, 0.6, 0.68))], 'rotation': [0, 0, 0]}
        if raise_front:
            out['s0']['rotation'] = [0, 5 * S(t, 0, 1), 6 * S(t, 0.25)]
            out['s1']['rotation'] = [0, -4 * S(t, 0.1), -4 * S(t, 0.35)]
        return out

    def walk(t):
        out = wave(t, 10, ground_from=raise_front)
        out['head'] = {'rotation': [0, -8 * S(t, 0.1), 0]}
        return out

    def attack(t):
        out = {}
        strike = pulse(t, 0.15, 0.6)
        coil = pulse(t, 0.0, 0.25)
        for i in range(min(4, n)):
            out['s%d' % i] = {'rotation': [(14 * coil - 22 * strike) * (1 - i * 0.2), 0, 0]}
        out['head'] = {'rotation': [-10 * coil + 18 * strike, 0, 0]}
        out['jaw'] = {'rotation': [55 * pulse(t, 0.2, 0.55), 0, 0]}
        out['root'] = {'position': [0, 0, -5 * strike]}
        return out

    return m, {'idle': cycle(3.0, 12, idle), 'walk': cycle(1.2, 12, walk), 'attack': cycle(0.7, 10, attack, loop=False)}


def anaconda():
    m, a = snake('almond_anaconda', 12, 7, 8)
    pal = dict(scales=Scales('c8a46a', '8a6a3a', 'eadcb8', blotch='6a4a24', blotch_edge='3a2814'), head=Scales('c09a60', '7a5a30', 'e6d6b0'),
               belly=Solid('ead8b0'), eye=Solid('d8a020', 0.02), fang=TEETH, tongue=Solid('b02030'), hood=Solid('c8a46a'))
    m.save({'almond_anaconda': pal}, a)


def cobras():
    m, a = snake('fang_cobra', 9, 5, 3.5, raise_front=3, hood=True)
    ivory = dict(scales=Scales('e8e2d0', '9a9480', 'f6f2e6'), head=Scales('f0eadc', 'a8a290', 'faf6ee'), belly=Solid('f4efe0'),
                 hood=Scales('d8d0b8', '6a6250', 'efe8d6', blotch='3a3428'), eye=Solid('3a6ad0', 0.02, glow=True),
                 fang=Solid('fffff4', 0.01), tongue=Solid('202020'))
    ruby = dict(scales=Scales('3a1a1e', '1e0c10', '7a3a3a'), head=Scales('4a2024', '22100e', '8a4040'), belly=Solid('8a4a44'),
                hood=Scales('5a1e22', '200a0c', '904a44', blotch='d02030', blotch_edge='600c14'), eye=Solid('ffcc30', 0.02, glow=True),
                fang=Solid('e0202e', 0.02, glow=True), tongue=Solid('202020'))
    m.save({'cobra_ivory': ivory, 'cobra_ruby': ruby}, a)


# ===================================================================== Treant (Menor, Mayor, Zombie, Petreo)
def treant():
    m = Model('treant', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 20, 0))
    m.cube('body', (-7, 20, -5), (14, 22, 10), 'bark')                       # tronco
    m.cube('body', (-8, 20, -6), (16, 6, 12), 'bark', inflate=0.3)            # cintura de raices
    m.cube('body', (-8.5, 41, -4.5), (17, 6, 9), 'bark')                      # hombros (anchos: ahi nacen los brazos)
    # cara tallada: cejas, ojos y boca
    m.cube('body', (-5, 37, -5.3), (4, 1.5, 0.6), 'bark_dark')
    m.cube('body', (1, 37, -5.3), (4, 1.5, 0.6), 'bark_dark')
    m.cube('body', (-4, 35, -5.2), (2, 2, 0.4), 'eye')
    m.cube('body', (2, 35, -5.2), (2, 2, 0.4), 'eye')
    m.cube('body', (-3, 28, -5.3), (6, 3, 0.5), 'bark_dark')
    # copa
    m.bone('crown', 'body', (0, 48, 0))
    for (x, y, z, w, h, d) in ((-10, 47, -9, 20, 9, 18), (-13, 50, -6, 8, 7, 12), (5, 50, -6, 8, 7, 12), (-7, 55, -6, 14, 7, 12),
                               (-4, 61, -3, 8, 4, 7)):
        m.cube('crown', (x, y, z), (w, h, d), 'leaves')
    for (x, z, ang) in ((-6, 0, -25), (6, 2, 25), (0, 5, 0)):
        m.cube('crown', (x - 1, 46, z - 1), (2, 9, 2), 'bark', rot=(0, 0, ang), pivot=(x, 46, z))
    # raices colgantes de la copa (manglar)
    for (x, z, l) in ((-11, -5, 7), (-9, 6, 5), (10, -4, 6), (11, 5, 8), (-4, -8, 4), (5, 8, 5), (0, -9, 6)):
        m.cube('crown', (x - 0.5, 47 - l, z - 0.5), (1, l, 1), 'vine')
    # brazos (ramas) con dedos
    for side, nm in ((-1, 'arm_r'), (1, 'arm_l')):
        sx = side * 9
        m.bone(nm, 'body', (sx, 44, 0))
        m.cube(nm, (sx - 2.5, 41, -2.5), (5, 5, 5), 'bark', inflate=0.3)    # nudo del hombro
        m.cube(nm, (sx - 2, 28, -2), (4, 16, 4), 'bark', rot=(0, 0, side * 5), pivot=(sx, 44, 0))
        m.bone(nm + '_fore', nm, (sx + side * 1.4, 29, 0))
        m.cube(nm + '_fore', (sx + side * 1.4 - 1.5, 16, -1.5), (3, 14, 3), 'bark')
        m.cube(nm + '_fore', (sx + side * 1.4 - 2, 27, -2), (4, 3, 4), 'bark', inflate=0.2)  # codo
        for k, ang in ((-1, -20), (0, 0), (1, 20)):
            m.cube(nm + '_fore', (sx + side * 1.4 + k * 1.2 - 0.5, 10, -0.5 + k * 0.5), (1, 6, 1), 'bark_dark', rot=(ang, 0, side * 10 * k),
                   pivot=(sx + side * 1.4, 16, 0))
        m.cube(nm, (sx + side * 1 - 3, 40, -3), (6, 4, 6), 'leaves')         # hojas en el hombro
    # piernas (raices)
    for side, nm in ((-1, 'leg_r'), (1, 'leg_l')):
        x = side * 4
        m.bone(nm, 'body', (x, 20, 0))
        m.cube(nm, (x - 3, 6, -3), (6, 15, 6), 'bark')
        m.bone(nm + '_root', nm, (x, 6, 0))
        m.cube(nm + '_root', (x - 3.5, 0, -4), (7, 6, 7), 'bark')
        for k, (dx, dz) in enumerate(((-3, -5), (3, -5), (0, 4), (side * 5, 0))):
            m.cube(nm + '_root', (x + dx - 1, 0, dz - 1), (2, 2, 4 if dz else 2), 'bark_dark', rot=(0, (dx * 15) % 60, 0), pivot=(x + dx, 1, dz))

    def idle(t):
        return {'body': {'rotation': [0, 0, 1.5 * S(t)], 'position': [0, 0.3 * S(t, 0.25), 0]},
                'crown': {'rotation': [2 * S(t, 0.1), 0, 3 * S(t, 0.3)]},
                'arm_r': {'rotation': [3 * S(t, 0.2), 0, -3 * S(t)]}, 'arm_l': {'rotation': [3 * S(t, 0.7), 0, 3 * S(t)]},
                'arm_r_fore': {'rotation': [-6 - 4 * S(t, 0.4), 0, 0]}, 'arm_l_fore': {'rotation': [-6 - 4 * S(t, 0.9), 0, 0]}}

    def walk(t):
        s = S(t)
        return {'body': {'rotation': [3, 4 * s, 4 * s], 'position': [0, 0.8 * abs(s), 0]}, 'crown': {'rotation': [0, 0, -5 * s]},
                'leg_r': {'rotation': [24 * s, 0, 0]}, 'leg_l': {'rotation': [-24 * s, 0, 0]},
                'leg_r_root': {'rotation': [-10 * max(0, S(t, 0.25)), 0, 0]}, 'leg_l_root': {'rotation': [-10 * max(0, S(t, 0.75)), 0, 0]},
                'arm_r': {'rotation': [-18 * s, 0, 0]}, 'arm_l': {'rotation': [18 * s, 0, 0]},
                'arm_r_fore': {'rotation': [-12, 0, 0]}, 'arm_l_fore': {'rotation': [-12, 0, 0]}}

    def attack(t):
        up = pulse(t, 0, 0.5)
        slam = pulse(t, 0.4, 0.8)
        return {'arm_r': {'rotation': [-150 * up - 40 * slam, 0, 0]}, 'arm_l': {'rotation': [-150 * up - 40 * slam, 0, 0]},
                'arm_r_fore': {'rotation': [-20 * up, 0, 0]}, 'arm_l_fore': {'rotation': [-20 * up, 0, 0]},
                'body': {'rotation': [-8 * up + 14 * slam, 0, 0]}, 'crown': {'rotation': [6 * slam, 0, 0]}}

    anims = {'idle': cycle(4.0, 12, idle), 'walk': cycle(1.6, 8, walk), 'attack': cycle(1.0, 10, attack, loop=False)}
    base = dict(bark=Bark('6a4a2e', '3e2a18', moss='5e7a34', moss_amt=0.06), bark_dark=Solid('2e1e12', 0.08),
                leaves=Leaves('3e7a2a', '7ab83c', holes=0.1), eye=Solid('c8ff6a', 0.03, glow=True), vine=Vine('5a4a2a', '3a2a18'))
    elder = dict(bark=Bark('5a4430', '30221a', moss='4e7a2e', moss_amt=0.3), bark_dark=Solid('241810', 0.08),
                 leaves=Leaves('2e6a26', '5e9a34', holes=0.06, fruit='c8a03a'), eye=Solid('ffe070', 0.03, glow=True), vine=Vine('5a4a2a', '3a2a18'))
    zombie = dict(bark=Bark('5a5450', '2e2a28', moss='4a3a5a', moss_amt=0.15), bark_dark=Solid('1c181a', 0.08),
                  leaves=Leaves('4a4038', '6a5a48', holes=0.45), eye=Solid('c060ff', 0.03, glow=True), vine=Vine('000000', '000000', visible=False))
    stone = dict(bark=Rock('8a8680', '5a5752', moss='6a8a4a', moss_top=0.2), bark_dark=Solid('3a3834', 0.06),
                 # copa mezcla de manglar (verde oscuro, raices colgantes) y azalea florecida (flores rosas y fucsias)
                 leaves=Leaves('3a5e26', '6e9e3a', holes=0.07, fruit='e07ad0', fruit_rate=0.12, fruit2='b84aa8'),
                 eye=Solid('ffa040', 0.03, glow=True), vine=Vine('6a5236', '3c7a2a'))
    m.save({'treant_lesser': base, 'treant_elder': elder, 'treant_zombie': zombie, 'treant_stone': stone}, anims)


# ===================================================================== Wyrm Rojo (dragon, inspirado en Slyrak)
WYRM_FOLD = {  # alas plegadas (en el suelo): se suman a la pose abierta del modelo
    'wing_l': [0, 70, -18], 'wing_r': [0, -70, 18], 'wing_l_fore': [0, -150, 0], 'wing_r_fore': [0, 150, 0],
    'wing_l_hand': [0, 150, 0], 'wing_r_hand': [0, -150, 0]}


def red_wyrm():
    m = Model('red_wyrm', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 19, 0))
    m.cube('body', (-8, 12, -15), (16, 13, 17), 'scales')                     # pecho
    m.cube('body', (-6, 10.5, -14), (12, 2, 15), 'plates')                     # placas de la panza (pecho)
    m.cube('body', (-7, 13, 1), (14, 11, 14), 'scales')                       # cadera
    m.cube('body', (-5, 11.5, 1), (10, 2, 13), 'plates')
    for i in range(7):                                                         # cresta del lomo
        m.cube('body', (-0.8, 24.5 - (i % 2) * 0.5, -13 + i * 4), (1.6, 3 - (i % 2), 2.5), 'spike')
    # cuello en S (5 tramos) con placas claras abajo
    parent, z, y = 'body', -15, 22
    rest = [-38, -12, 4, 10, 14]
    for i in range(5):
        b = 'neck%d' % i
        th = 7 - i * 0.5
        m.bone(b, parent, (0, y, z), [rest[i], 0, 0])
        m.cube(b, (-th / 2, y - th / 2, z - 6.5), (th, th, 7), 'scales')
        m.cube(b, (-th / 2 + 0.8, y - th / 2 - 0.6, z - 6.3), (th - 1.6, 1.2, 6.6), 'plates')
        m.cube(b, (-0.6, y + th / 2 - 0.3, z - 5), (1.2, 2.2, 2.4), 'spike')
        parent, z = b, z - 6.5
    # cabeza: craneo, hocico, corona de cuernos hacia atras, ojos y mandibula con dientes
    m.bone('head', parent, (0, y, z), [30, 0, 0])
    m.cube('head', (-4, y - 3, z - 7), (8, 6, 7), 'scales')
    m.cube('head', (-3, y - 3, z - 14), (6, 4, 7), 'scales')
    m.cube('head', (-3.5, y + 2.5, z - 8), (7, 1.5, 5), 'spike')               # ceja
    for side in (-1, 1):
        m.cube('head', (side * 3 - 0.8, y + 1.5, z - 2), (1.6, 1.6, 10), 'horn', rot=(-18, side * 14, 0), pivot=(side * 3, y + 2, z - 2))
        m.cube('head', (side * 4 - 0.6, y - 0.5, z - 2), (1.2, 1.2, 7), 'horn', rot=(-5, side * 32, 0), pivot=(side * 4, y, z - 2))
        m.cube('head', (side * 1.5 - 0.5, y + 2.5, z - 3), (1, 1, 6), 'horn', rot=(-30, side * 6, 0), pivot=(side * 1.5, y + 2.5, z - 3))
        m.cube('head', (side * 4 - 0.4 + (0.2 if side > 0 else 0), y + 0.6, z - 6.5), (0.6, 1, 2), 'eye')
        for k in range(3):
            m.cube('head', (side * 2.4 - 0.3, y - 3.8, z - 13.5 + k * 2.2), (0.6, 1.1, 0.6), TEETH)
    m.cube('head', (-0.8, y + 1, z - 14.2), (1.6, 1, 1), 'spike')               # cuernito del hocico
    m.bone('jaw', 'head', (0, y - 2.5, z - 5))
    m.cube('jaw', (-2.8, y - 4.8, z - 13.8), (5.6, 2.2, 9.5), 'plates')
    for side in (-1, 1):
        for k in range(3):
            m.cube('jaw', (side * 2.2 - 0.3, y - 3, z - 13 + k * 2.4), (0.6, 1, 0.6), TEETH)
    # alas grandes: brazo, antebrazo y mano con tres dedos; membrana entre los huesos (abiertas en el modelo)
    for side, nm in ((1, 'wing_l'), (-1, 'wing_r')):
        sx = side * 7
        m.bone(nm, 'body', (sx, 24, -9))
        o = lambda x0, L: (sx + (0 if side > 0 else -L) + x0 * side, )
        m.cube(nm, (sx if side > 0 else sx - 16, 23, -10), (16, 2.6, 2.6), 'scales')
        m.cube(nm, (sx if side > 0 else sx - 16, 23.6, -8), (16, 0.6, 16), 'membrane')
        m.bone(nm + '_fore', nm, (sx + side * 16, 24, -9))
        fx = sx + side * 16
        m.cube(nm + '_fore', (fx if side > 0 else fx - 20, 23.2, -9.8), (20, 2, 2), 'scales')
        m.cube(nm + '_fore', (fx if side > 0 else fx - 20, 23.6, -8), (20, 0.6, 18), 'membrane')
        m.bone(nm + '_hand', nm + '_fore', (fx + side * 20, 24, -9))
        hx = fx + side * 20
        m.cube(nm + '_hand', (hx - 1.2, 23, -10.4), (2.4, 2.4, 2.4), 'scales')
        m.cube(nm + '_hand', (hx - 0.6 + side * 0.6, 25, -11), (1.2, 3, 1.2), CLAW)  # pulgar con garra
        for k, (ang, L) in enumerate(((15, 26), (40, 22), (68, 17))):
            fy = side * ang
            m.cube(nm + '_hand', (hx if side > 0 else hx - L, 23.4, -9.6), (L, 1.2, 1.2), 'horn', rot=(0, -fy, 0), pivot=(hx, 24, -9))
            m.cube(nm + '_hand', (hx if side > 0 else hx - L, 23.7, -9), (L * 0.95, 0.5, 11 - k * 2), 'membrane_tip',
                   rot=(0, -fy + side * 12, 0), pivot=(hx, 24, -9))
    # patas: las de atras mas grandes
    for n, (x, z, big) in {'fl': (-6, -10, 0), 'fr': (6, -10, 0), 'bl': (-6.5, 8, 1), 'br': (6.5, 8, 1)}.items():
        w = 5 + big
        m.bone('leg_' + n, 'body', (x, 15, z))
        m.cube('leg_' + n, (x - w / 2, 7, z - w / 2 - 0.5), (w, 10, w + 1), 'scales', inflate=0.2)
        m.bone('shin_' + n, 'leg_' + n, (x, 8, z))
        m.cube('shin_' + n, (x - w / 2 + 0.8, 1.2, z - 1.5 + big), (w - 1.6, 8, w - 1.6), 'scales')
        m.cube('shin_' + n, (x - w / 2, 0, z - 3.5), (w, 1.6, 6), 'scales')
        for k in (-1, 0, 1):
            m.cube('shin_' + n, (x + k * 1.6 - 0.4, 0, z - 5), (0.8, 1.2, 1.6), CLAW)
    # cola larga con puas y punta de pala
    parent, z = 'body', 15
    for i in range(8):
        b = 'tail%d' % i
        th = 9 - i * 0.95
        m.bone(b, parent, (0, 20 - i * 0.7, z), [6 if i == 0 else 3, 0, 0])
        m.cube(b, (-th / 2, 20 - i * 0.7 - th / 2, z), (th, th, 7.4), 'scales')
        m.cube(b, (-th / 2 + 0.6, 20 - i * 0.7 - th / 2 - 0.5, z + 0.2), (th - 1.2, 1, 7), 'plates')
        m.cube(b, (-0.6, 20 - i * 0.7 + th / 2 - 0.5, z + 2.5), (1.2, 2 - i * 0.12, 2.5), 'spike')
        parent, z = b, z + 7
    m.cube('tail7', (-3, 13.5, z - 2), (6, 0.8, 6), 'spike', rot=(0, 45, 0), pivot=(0, 14, z + 1))

    names = ['neck%d' % i for i in range(5)]

    def fold(out, amt=1.0):
        for k, v in WYRM_FOLD.items():
            r = out.setdefault(k, {}).get('rotation', [0, 0, 0])
            out[k]['rotation'] = [r[i] + v[i] * amt for i in range(3)]
        return out

    def tail(out, amp, speed=1.0, t=0.0):
        for i in range(8):
            out['tail%d' % i] = {'rotation': [2 * S(t, -i * 0.1), amp * S(t * speed, -i * 0.11), 0]}
        return out

    def idle(t):
        out = {'body': {'position': [0, 0.3 * S(t), 0]}, 'head': {'rotation': [3 * S(t, 0.2), 14 * S(t, 0, 0.5) * pulse(t, 0.2, 1), 0]},
               'jaw': {'rotation': [3 + 3 * S(t, 0.1), 0, 0]}}
        for i in range(5):
            out['neck%d' % i] = {'rotation': [1.5 * S(t, i * 0.08), 3 * S(t, i * 0.1, 0.5), 0]}
        return tail(fold(out), 6, 1, t)

    def walk(t):
        out = idle(t)
        out['body'] = {'position': [0, 0.6 * abs(S(t)), 0], 'rotation': [0, 2 * S(t), 2 * S(t)]}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            out['leg_' + n] = {'rotation': [26 * S(t, ph), 0, 0]}
            out['shin_' + n] = {'rotation': [-18 * max(0, S(t, ph + 0.25)), 0, 0]}
        for i in range(5):
            out['neck%d' % i] = {'rotation': [2 * S(t, i * 0.1, 2), -3 * S(t, i * 0.1), 0]}
        return tail(out, 12, 1, t)

    def sleep(t):
        out = {'body': {'position': [0, -6, 0]}, 'neck0': {'rotation': [30, -20, 0]}, 'neck1': {'rotation': [10, -30, 0]},
               'neck2': {'rotation': [0, -30, 0]}, 'neck3': {'rotation': [-4, -25, 0]}, 'neck4': {'rotation': [-8, -15, 0]},
               'head': {'rotation': [-20 + 2 * S(t), -10, 0]}, 'jaw': {'rotation': [0, 0, 0]}}
        for n in ('fl', 'fr', 'bl', 'br'):
            out['leg_' + n] = {'rotation': [-70 if n[0] == 'f' else 60, 0, 0]}
            out['shin_' + n] = {'rotation': [60 if n[0] == 'f' else -50, 0, 0]}
        fold(out, 1.05)
        for i in range(8):
            out['tail%d' % i] = {'rotation': [-2, -16 + 1.5 * S(t, -i * 0.1), 0]}
        out['body']['scale'] = [1 + 0.02 * S(t), 1 + 0.03 * S(t), 1]
        return out

    def fly(t):
        f = S(t)
        out = {'body': {'position': [0, -2.5 * f, 0], 'rotation': [-3, 0, 0]},
               'wing_l': {'rotation': [0, 0, -50 * f]}, 'wing_r': {'rotation': [0, 0, 50 * f]},
               'wing_l_fore': {'rotation': [0, 0, -22 * S(t, -0.12)]}, 'wing_r_fore': {'rotation': [0, 0, 22 * S(t, -0.12)]},
               'wing_l_hand': {'rotation': [0, 0, -18 * S(t, -0.2)]}, 'wing_r_hand': {'rotation': [0, 0, 18 * S(t, -0.2)]},
               'neck0': {'rotation': [20, 0, 0]}, 'neck1': {'rotation': [8, 0, 0]}, 'head': {'rotation': [-14 + 3 * f, 0, 0]}}
        for n in ('fl', 'fr', 'bl', 'br'):
            out['leg_' + n] = {'rotation': [55 if n[0] == 'b' else -25, 0, 0]}
            out['shin_' + n] = {'rotation': [-35 if n[0] == 'b' else 45, 0, 0]}
        for i in range(8):
            out['tail%d' % i] = {'rotation': [-1 + 3 * S(t, -i * 0.1), 3 * S(t, -i * 0.1, 0.5), 0]}
        return out

    def glide(t):
        out = fly(0.25)
        out['body'] = {'position': [0, 0, 0], 'rotation': [4, 0, 6 * S(t)]}
        for w, sgn in (('wing_l', -1), ('wing_r', 1)):
            out[w] = {'rotation': [0, 0, sgn * (8 + 4 * S(t, 0.1))]}
            out[w + '_fore'] = {'rotation': [0, 0, sgn * 3]}
            out[w + '_hand'] = {'rotation': [0, 0, sgn * 2]}
        return out

    def hover(t):
        out = fly(t)
        out['body'] = {'position': [0, -1.5 * S(t), 0], 'rotation': [-28, 0, 0]}
        out['neck0'] = {'rotation': [38, 0, 0]}
        out['head'] = {'rotation': [8, 0, 0]}
        for n in ('fl', 'fr'):
            out['leg_' + n] = {'rotation': [-10 + 8 * S(t), 0, 0]}
        return out

    def takeoff(t):
        crouch = pulse(t, 0, 0.45)
        jump = pulse(t, 0.35, 1.0)
        out = fly(min(1, t * 2.2))
        out['body'] = {'position': [0, -4 * crouch + 8 * jump, 0], 'rotation': [-10 * jump, 0, 0]}
        for n in ('bl', 'br'):
            out['leg_' + n] = {'rotation': [-25 * crouch + 40 * jump, 0, 0]}
        return out

    def breath(t):
        out = idle(t)
        rise = pulse(t, 0, 0.25)
        out['neck0'] = {'rotation': [-14 * rise + 22 * (t > 0.2), 0, 0]}
        out['neck1'] = {'rotation': [8 * (t > 0.2), 0, 0]}
        out['neck2'] = {'rotation': [-6, 0, 0]}
        out['head'] = {'rotation': [-25 * rise - 18 * (t > 0.2), 2 * S(t, 0, 6), 0]}
        out['jaw'] = {'rotation': [10 * rise + 40 * (t > 0.2) * (1 - pulse(t, 0.97, 1.0)), 0, 0]}
        out['body'] = {'position': [0, 0, 0], 'rotation': [-4 * rise, 0, 0]}
        return out

    def bite(t):
        out = idle(t)
        rear = pulse(t, 0, 0.45)
        b = pulse(t, 0.35, 0.8)
        out['neck0'] = {'rotation': [-20 * rear + 40 * b, 0, 0]}
        out['neck1'] = {'rotation': [-8 * rear + 16 * b, 0, 0]}
        out['head'] = {'rotation': [20 * rear - 30 * b, 0, 0]}
        out['jaw'] = {'rotation': [55 * pulse(t, 0.2, 0.7), 0, 0]}
        out['body'] = {'position': [0, 0, -3 * b], 'rotation': [-4 * rear + 6 * b, 0, 0]}
        return out

    def tail_whip(t):
        out = idle(t)
        sw = S(t, -0.25) if 0.1 < t < 0.9 else -1
        k = pulse(t, 0.05, 0.95)
        out['body'] = {'rotation': [0, -25 * k * sw, 0]}
        for i in range(8):
            out['tail%d' % i] = {'rotation': [0, 18 * k * S(t, -i * 0.06), 0]}
        return out

    def wing(t):
        out = idle(t)
        o = pulse(t, 0, 1)
        for w, sgn in (('wing_l', 1), ('wing_r', -1)):
            out[w] = {'rotation': [0, -WYRM_FOLD[w][1] * o, -WYRM_FOLD[w][2] * o + sgn * 30 * o * S(t, 0, 3)]}
            out[w + '_fore'] = {'rotation': [0, -WYRM_FOLD[w + '_fore'][1] * o, 0]}
            out[w + '_hand'] = {'rotation': [0, -WYRM_FOLD[w + '_hand'][1] * o, 0]}
        out['body'] = {'rotation': [-12 * o, 0, 0], 'position': [0, 2 * o, 0]}
        return out

    def roar(t):
        out = idle(t)
        r = pulse(t, 0.05, 0.95)
        out['neck0'] = {'rotation': [-22 * r, 0, 0]}
        out['neck1'] = {'rotation': [-10 * r, 0, 0]}
        out['head'] = {'rotation': [-30 * r, 2 * S(t, 0, 8) * r, 0]}
        out['jaw'] = {'rotation': [60 * r, 0, 0]}
        for w in ('wing_l', 'wing_r'):
            out[w]['rotation'] = [x * (1 - 0.5 * r) for x in out[w]['rotation']]
        return out

    pal = dict(scales=Striped('b02a1e', '3a0e0c', belly='e6d6be', plate='b8a68e', every=5), plates=Plates('ead9c0', 'b5a189'),
               spike=Solid('2a1210', 0.06), horn=Solid('3a2a24', 0.05, edge='1a100c'), eye=Solid('ffd040', 0.02, glow=True),
               membrane=Membrane('a8331f', '5a120c', edge='3a0a08', torn=0.0), membrane_tip=Membrane('9a2c1a', '520e0a', edge='2e0806', torn=0.35))
    m.save({'red_wyrm': pal}, {'idle': cycle(3.0, 12, idle), 'walk': cycle(1.4, 8, walk), 'sleep': cycle(4.0, 8, sleep),
                               'fly': cycle(1.0, 8, fly), 'glide': cycle(3.0, 8, glide), 'hover': cycle(0.7, 8, hover),
                               'takeoff': cycle(1.2, 10, takeoff, loop=False), 'breath': cycle(2.0, 16, breath, loop=False),
                               'bite': cycle(0.8, 10, bite, loop=False), 'tail': cycle(0.9, 10, tail_whip, loop=False),
                               'wing': cycle(0.9, 12, wing, loop=False), 'roar': cycle(2.0, 16, roar, loop=False)})


# ===================================================================== Hidra Manatita
def hydra():
    """Hidra de manatita: cuerpo verde oscuro y macizo, nueve cuellos largos con cabezas de cresta de cristal, el lomo
    cubierto de racimos de cristal azul y una cola larga."""
    m = Model('manatite_hydra', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 18, 0))
    m.cube('body', (-15, 11, -16), (30, 17, 22), 'scales')                    # pecho (alto: de ahi salen los cuellos)
    m.cube('body', (-13, 12, 6), (26, 14, 16), 'scales')                      # cadera
    m.cube('body', (-12, 9.5, -14), (24, 2, 34), 'belly')
    rnd = __import__('random').Random(7)
    # racimos de cristal azul por todo el lomo (cada racimo: uno grande y dos chicos inclinados)
    for (x, z, h) in ((-9, -10, 11), (8, -8, 13), (0, -2, 15), (-11, 2, 9), (10, 4, 10), (-4, 10, 12), (6, 14, 9),
                      (-8, 18, 7), (2, 20, 8), (12, -14, 7), (-13, -15, 6), (0, -13, 9)):
        y0 = 26 if z < 6 else 24.5
        for j, (dx, dz, hh, tilt) in enumerate(((0, 0, h, 0), (1.8, 1.2, h * 0.6, 28), (-1.6, -1, h * 0.5, -32))):
            ang = rnd.uniform(-20, 20)
            m.cube('body', (x + dx - 1.4 + j * 0.2, y0, z + dz - 1.4), (2.8 - j * 0.6, hh, 2.8 - j * 0.6), 'crystal',
                   rot=(ang * 0.6 + (tilt if j else 0) * 0.5, 45, ang + tilt), pivot=(x + dx, y0 + 0.5, z + dz))
    for n, (x, z) in {'fl': (-13, -10), 'fr': (13, -10), 'bl': (-12, 14), 'br': (12, 14)}.items():
        m.bone('leg_' + n, 'body', (x, 15, z))
        m.cube('leg_' + n, (x - 4.5, 2, z - 4.5), (9, 13, 9), 'scales')
        m.cube('leg_' + n, (x - 5, 0, z - 6), (10, 2.5, 10), 'scales')
        m.cube('leg_' + n, (x + (1 if x > 0 else -1) * 3.5 - 1, 9, z - 1), (2, 4, 2), 'crystal', rot=(0, 45, (1 if x > 0 else -1) * 30), pivot=(x, 10, z))
        for k in (-1, 0, 1):
            m.cube('leg_' + n, (x + k * 3 - 0.7, 0, z - 7.5), (1.4, 1.4, 2.2), CLAW)
    # cola larga y afinada con cristales
    parent, z = 'body', 22
    for i in range(7):
        b = 'tail%d' % i
        th = 12 - i * 1.5
        y = 20 - i * 1.2
        m.bone(b, parent, (0, y, z), [5 if i == 0 else 2, 0, 0])
        m.cube(b, (-th / 2, y - th / 2, z), (th, th, 8.5), 'scales')
        m.cube(b, (-th / 2 + 1, y - th / 2 - 0.6, z + 0.3), (th - 2, 1, 8), 'belly')
        if i < 5:
            m.cube(b, (-1, y + th / 2 - 0.5, z + 3), (2, 5 - i * 0.7, 2), 'crystal', rot=(-25, 45, 0), pivot=(0, y + th / 2, z + 4))
        parent, z = b, z + 8.5
    # nueve cuellos: cinco abajo y cuatro arriba, abiertos en abanico
    heads = [(-12, 23, -55, 22), (-6, 23, -27, 16), (0, 23, 0, 12), (6, 23, 27, 16), (12, 23, 55, 22),
             (-9, 26.5, -40, 42), (-3, 27.5, -13, 52), (3, 27.5, 13, 52), (9, 26.5, 40, 42)]
    NSEG = 5
    NBEND = [0, 2, 3, 3, 2]
    for k, (x, ny, yaw, lift) in enumerate(heads):
        parent, nz = 'body', -14 if ny < 25 else -10
        for i in range(NSEG):
            b = 'n%d_%d' % (k, i)
            th = 5.6 - i * 0.45
            rot = [-lift if i == 0 else NBEND[i], yaw if i == 0 else 0, 0]
            m.bone(b, parent, (x, ny, nz), rot)
            m.cube(b, (x - th / 2, ny - th / 2, nz - 6.6), (th, th, 7), 'scales')
            m.cube(b, (x - th / 2 + 0.7, ny - th / 2 - 0.5, nz - 6.4), (th - 1.4, 0.8, 6.6), 'belly')
            if i % 2 == 0:
                m.cube(b, (x - 0.6, ny + th / 2 - 0.4, nz - 4.5), (1.2, 2.2 - i * 0.2, 1.6), 'crystal')
            parent, nz = b, nz - 6.6
        hb = 'h%d' % k
        m.bone(hb, parent, (x, ny, nz), [18, 0, 0])
        m.cube(hb, (x - 3.2, ny - 2.4, nz - 6.5), (6.4, 4.8, 6.5), 'scales')
        m.cube(hb, (x - 2.3, ny - 2.4, nz - 11.5), (4.6, 3.2, 5), 'scales')
        m.cube(hb, (x - 1.5, ny + 0.6, nz - 11.6), (3, 0.6, 0.6), 'spike')
        # cresta de cristal: tres puntas hacia atras y dos a los costados
        for j, (dx, hh, rx, rz) in enumerate(((0, 6, -55, 0), (-1.6, 4.5, -50, 18), (1.6, 4.5, -50, -18), (-2.8, 3, -35, 40), (2.8, 3, -35, -40))):
            m.cube(hb, (x + dx - 0.7, ny + 2, nz - 4 + j * 0.3), (1.4, hh, 1.4), 'crystal', rot=(rx, 0, rz), pivot=(x + dx, ny + 2, nz - 3.5))
        for side in (-1, 1):
            m.cube(hb, (x + side * 3.2 - 0.3 + (0.3 if side > 0 else 0), ny + 0.2, nz - 5.5), (0.6, 0.9, 1.6), 'eye')
        jb = 'j%d' % k
        m.bone(jb, hb, (x, ny - 2, nz - 4))
        m.cube(jb, (x - 2.1, ny - 3.8, nz - 11.3), (4.2, 1.6, 7.6), 'belly')
        for side in (-1, 1):
            m.cube(hb, (x + side * 1.6 - 0.3, ny - 3.2, nz - 11), (0.6, 1, 0.6), TEETH)
            m.cube(jb, (x + side * 1.4 - 0.3, ny - 2.6, nz - 10.5), (0.6, 0.9, 0.6), TEETH)

    def necks(t, amp=1.0):
        out = {}
        for k, (x, ny, yaw, lift) in enumerate(heads):
            ph = k * 0.137
            for i in range(NSEG):
                base = -lift if i == 0 else NBEND[i]
                out['n%d_%d' % (k, i)] = {'rotation': [base + 4 * amp * S(t, ph + i * 0.08), (yaw if i == 0 else 0) + 6 * amp * S(t, ph + 0.25 + i * 0.1), 0]}
            out['h%d' % k] = {'rotation': [18 + 6 * S(t, ph + 0.4), 12 * S(t, ph, 0.5), 0]}
            out['j%d' % k] = {'rotation': [5 + 14 * pulse((t + ph) % 1, 0.3, 0.5), 0, 0]}
        return out

    def idle(t):
        out = necks(t)
        out['body'] = {'position': [0, 0.4 * S(t), 0]}
        for i in range(7):
            out['tail%d' % i] = {'rotation': [0, 5 * S(t, -i * 0.1), 0]}
        return out

    def walk(t):
        out = necks(t, 1.5)
        out['body'] = {'position': [0, 0.7 * abs(S(t)), 0], 'rotation': [0, 2 * S(t), 3 * S(t)]}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            out['leg_' + n] = {'rotation': [22 * S(t, ph), 0, 0]}
        for i in range(7):
            out['tail%d' % i] = {'rotation': [0, 10 * S(t, -i * 0.12), 0]}
        return out

    def attack(t):
        out = necks(t)
        for n, k in enumerate((1, 2, 3, 6, 7)):
            back = pulse(t, 0, 0.4)
            strike = pulse(t, 0.28 + n * 0.06, 0.72 + n * 0.06)
            lift = heads[k][3]
            out['n%d_0' % k] = {'rotation': [-lift - 20 * back + 50 * strike, heads[k][2], 0]}
            out['h%d' % k] = {'rotation': [18 + 20 * back - 35 * strike, 0, 0]}
            out['j%d' % k] = {'rotation': [55 * strike, 0, 0]}
        return out

    def roar(t):
        out = necks(t, 0.4)
        r = pulse(t, 0.05, 0.95)
        for k, (x, ny, yaw, lift) in enumerate(heads):
            out['n%d_0' % k]['rotation'][0] -= 18 * r
            out['n%d_0' % k]['rotation'][1] = yaw * (1 + 0.35 * r)
            out['h%d' % k] = {'rotation': [18 - 40 * r, 3 * S(t, k * 0.1, 8) * r, 0]}
            out['j%d' % k] = {'rotation': [55 * r, 0, 0]}
        return out

    pal = dict(scales=Scales('2c4a2a', '16261a', '4a6a3a'), belly=Plates('7a8a5a', '4a5a38'),
               crystal=Crystal('4aa0ff', 'd8f6ff', '2a5ec8'), eye=Solid('ffe060', 0.02, glow=True), spike=Solid('1a2a1a', 0.05))
    m.save({'manatite_hydra': pal}, {'idle': cycle(4.0, 16, idle), 'walk': cycle(1.6, 8, walk),
                                     'attack': cycle(1.2, 12, attack, loop=False), 'roar': cycle(2.0, 12, roar, loop=False)})


if __name__ == '__main__':
    for fn in (coyote, acid_wolf, sacred_beast, rock_turtle, anaconda, cobras, treant, red_wyrm, hydra):
        fn()
        print('ok', fn.__name__)

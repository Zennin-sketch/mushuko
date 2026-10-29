"""Criaturas del mundo de Mushoku Tensei hechas a mano en codigo (modelo, texturas y animaciones de GeckoLib).
Uso: python3 creatures.py  -> escribe geo/, animations/ y textures/entities/ de cada una."""
import math, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo import Model, Fur, Solid, Scales, Bark, Leaves, Rock, Crystal, Membrane, cycle, S, C, pulse

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
    m.cube('body', (-6, 42, -4.5), (12, 6, 9), 'bark')                        # hombros
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
    # brazos (ramas) con dedos
    for side, nm in ((-1, 'arm_r'), (1, 'arm_l')):
        sx = side * 8
        m.bone(nm, 'body', (sx, 44, 0))
        m.cube(nm, (sx - 2 + side * 1, 28, -2), (4, 16, 4), 'bark', rot=(0, 0, side * 10), pivot=(sx, 44, 0))
        m.bone(nm + '_fore', nm, (sx + side * 3.5, 29, 0))
        m.cube(nm + '_fore', (sx + side * 3.5 - 1.5, 16, -1.5), (3, 14, 3), 'bark')
        for k, ang in ((-1, -20), (0, 0), (1, 20)):
            m.cube(nm + '_fore', (sx + side * 3.5 + k * 1.2 - 0.5, 10, -0.5 + k * 0.5), (1, 6, 1), 'bark_dark', rot=(ang, 0, side * 10 * k),
                   pivot=(sx + side * 3.5, 16, 0))
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
                leaves=Leaves('3e7a2a', '7ab83c', holes=0.1), eye=Solid('c8ff6a', 0.03, glow=True))
    elder = dict(bark=Bark('5a4430', '30221a', moss='4e7a2e', moss_amt=0.3), bark_dark=Solid('241810', 0.08),
                 leaves=Leaves('2e6a26', '5e9a34', holes=0.06, fruit='c8a03a'), eye=Solid('ffe070', 0.03, glow=True))
    zombie = dict(bark=Bark('5a5450', '2e2a28', moss='4a3a5a', moss_amt=0.15), bark_dark=Solid('1c181a', 0.08),
                  leaves=Leaves('4a4038', '6a5a48', holes=0.45), eye=Solid('c060ff', 0.03, glow=True))
    stone = dict(bark=Rock('8a8680', '5a5752', moss='6a8a4a', moss_top=0.2), bark_dark=Solid('3a3834', 0.06),
                 leaves=Rock('7a8a6a', '4a5a40', moss='6a9a40', moss_top=0.6), eye=Solid('ffa040', 0.03, glow=True))
    m.save({'treant_lesser': base, 'treant_elder': elder, 'treant_zombie': zombie, 'treant_stone': stone}, anims)


# ===================================================================== Wyrm Rojo (dragon)
def red_wyrm():
    m = Model('red_wyrm', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 20, 0))
    m.cube('body', (-9, 14, -14), (18, 13, 28), 'scales')
    m.cube('body', (-7, 13, -12), (14, 3, 24), 'belly')
    for i in range(6):  # puas del lomo
        m.cube('body', (-1, 27, -12 + i * 5), (2, 3 - (i % 2), 2.5), 'spike')
    # cuello en 3 tramos y cabeza con cuernos
    parent, z, y = 'body', -14, 24
    for i in range(3):
        b = 'neck%d' % i
        m.bone(b, parent, (0, y, z), [-12 if i == 0 else -8, 0, 0])
        m.cube(b, (-4 + i * 0.5, y - 4, z - 7), (8 - i, 8 - i, 7.5), 'scales')
        m.cube(b, (-1, y + 3.5 - i, z - 6), (2, 2, 3), 'spike')
        parent, z = b, z - 7
    m.bone('head', parent, (0, y, z), [20, 0, 0])
    m.cube('head', (-5, y - 3.5, z - 9), (10, 7, 9), 'scales')
    m.cube('head', (-3.5, y - 3.5, z - 16), (7, 4.5, 7), 'scales')
    for side in (-1, 1):
        m.cube('head', (side * 3 - 0.75, y + 2.5, z - 5), (1.5, 1.5, 8), 'horn', rot=(-25, side * 12, 0), pivot=(side * 3, y + 3, z - 2))
        m.cube('head', (side * 4.6 - 0.5, y + 0.5, z - 8.5), (1, 1.2, 2), 'eye')
        m.cube('head', (side * 2 - 0.4, y - 3.6, z - 15.5), (0.8, 1.2, 0.8), TEETH)
    m.bone('jaw', 'head', (0, y - 3, z - 4))
    m.cube('jaw', (-3.3, y - 5.5, z - 15.5), (6.6, 2.5, 11.5), 'belly')
    # alas: brazo y membrana
    for side, nm in ((-1, 'wing_r'), (1, 'wing_l')):
        sx = side * 8
        m.bone(nm, 'body', (sx, 26, -8))
        m.cube(nm, (sx if side > 0 else sx - 22, 25, -9), (22, 2, 2.5), 'scales')
        m.cube(nm, (sx if side > 0 else sx - 22, 25.5, -7), (22, 0.6, 16), 'membrane')
        m.bone(nm + '_tip', nm, (sx + side * 22, 26, -8))
        m.cube(nm + '_tip', (sx + side * 22 if side > 0 else sx - 44, 25.2, -8.6), (22, 1.6, 1.8), 'scales')
        m.cube(nm + '_tip', (sx + side * 22 if side > 0 else sx - 44, 25.5, -7), (22, 0.6, 13), 'membrane')
        m.cube(nm + '_tip', (sx + side * 44 - (1 if side > 0 else 0) - 0.5, 24.5, -10), (1.5, 1.5, 3), 'horn')
    for n, (x, z) in {'fl': (-7, -9), 'fr': (7, -9), 'bl': (-7, 9), 'br': (7, 9)}.items():
        m.bone('leg_' + n, 'body', (x, 16, z))
        m.cube('leg_' + n, (x - 2.5, 7, z - 3), (5, 10, 6), 'scales')
        m.bone('shin_' + n, 'leg_' + n, (x, 8, z))
        m.cube('shin_' + n, (x - 2, 1, z - 2), (4, 8, 4), 'scales')
        m.cube('shin_' + n, (x - 2.5, 0, z - 4), (5, 1.5, 6), 'scales')
        for k in (-1, 0, 1):
            m.cube('shin_' + n, (x + k * 1.6 - 0.4, 0, z - 5), (0.8, 1, 1.2), CLAW)
    parent, z = 'body', 14
    for i in range(5):
        b = 'tail%d' % i
        th = 9 - i * 1.6
        m.bone(b, parent, (0, 22 - i * 0.6, z), [8 if i == 0 else 4, 0, 0])
        m.cube(b, (-th / 2, 22 - i * 0.6 - th / 2, z), (th, th, 8.4), 'scales')
        m.cube(b, (-0.8, 22 - i * 0.6 + th / 2 - 0.5, z + 2), (1.6, 2, 3), 'spike')
        parent, z = b, z + 8
    m.cube('tail4', (-3, 19, z - 3), (6, 1, 5), 'membrane')

    def idle(t):
        out = {'body': {'position': [0, 0.4 * S(t), 0]}, 'head': {'rotation': [3 * S(t, 0.2), 10 * S(t, 0, 0.5), 0]},
               'jaw': {'rotation': [4 + 4 * S(t, 0.1), 0, 0]},
               'wing_r': {'rotation': [0, 30, -50 + 4 * S(t)]}, 'wing_l': {'rotation': [0, -30, 50 - 4 * S(t)]},
               'wing_r_tip': {'rotation': [0, 120, 0]}, 'wing_l_tip': {'rotation': [0, -120, 0]}}
        for i in range(3):
            out['neck%d' % i] = {'rotation': [(-12 if i == 0 else -8) + 2 * S(t, i * 0.1), 3 * S(t, i * 0.1, 0.5), 0]}
        for i in range(5):
            out['tail%d' % i] = {'rotation': [8 if i == 0 else 4, 8 * S(t, -i * 0.12), 0]}
        return out

    def walk(t):
        out = idle(t)
        out['body'] = {'position': [0, 0.8 * abs(S(t)), 0], 'rotation': [0, 3 * S(t), 2 * S(t)]}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            out['leg_' + n] = {'rotation': [28 * S(t, ph), 0, 0]}
            out['shin_' + n] = {'rotation': [-20 * max(0, S(t, ph + 0.25)), 0, 0]}
        for i in range(5):
            out['tail%d' % i] = {'rotation': [8 if i == 0 else 4, 14 * S(t, -i * 0.15), 0]}
        return out

    def fly(t):
        f = S(t)
        out = {'body': {'position': [0, -2 * f, 0], 'rotation': [-4, 0, 0]},
               'wing_r': {'rotation': [0, 0, 45 * f]}, 'wing_l': {'rotation': [0, 0, -45 * f]},
               'wing_r_tip': {'rotation': [0, 0, 25 * S(t, -0.12)]}, 'wing_l_tip': {'rotation': [0, 0, -25 * S(t, -0.12)]}}
        for n in ('fl', 'fr', 'bl', 'br'):
            out['leg_' + n] = {'rotation': [40 if n[0] == 'b' else -20, 0, 0]}
            out['shin_' + n] = {'rotation': [-30 if n[0] == 'b' else 30, 0, 0]}
        for i in range(5):
            out['tail%d' % i] = {'rotation': [2 + 3 * S(t, -i * 0.1), 4 * S(t, -i * 0.1, 0.5), 0]}
        return out

    def attack(t):
        out = idle(t)
        rear = pulse(t, 0, 0.45)
        bite = pulse(t, 0.35, 0.75)
        out['neck0'] = {'rotation': [-12 - 25 * rear + 35 * bite, 0, 0]}
        out['neck1'] = {'rotation': [-8 - 10 * rear + 15 * bite, 0, 0]}
        out['head'] = {'rotation': [20 + 20 * rear - 30 * bite, 0, 0]}
        out['jaw'] = {'rotation': [8 + 50 * pulse(t, 0.2, 0.7), 0, 0]}
        out['body'] = {'position': [0, 0, -4 * bite], 'rotation': [-6 * rear + 6 * bite, 0, 0]}
        out['wing_r'] = {'rotation': [0, 30 - 30 * rear, -50 + 60 * rear]}
        out['wing_l'] = {'rotation': [0, -30 + 30 * rear, 50 - 60 * rear]}
        return out

    pal = dict(scales=Scales('a02218', '5a0e0a', 'd8803a'), belly=Scales('d8904a', 'a05a28', 'f0c080'), spike=Solid('2a1410', 0.06),
               horn=Solid('3a2a24', 0.05, edge='1a100c'), eye=Solid('ffd040', 0.02, glow=True), membrane=Membrane('c0482c', '6a160e'))
    m.save({'red_wyrm': pal}, {'idle': cycle(3.0, 12, idle), 'walk': cycle(1.0, 8, walk), 'fly': cycle(0.9, 8, fly),
                               'attack': cycle(0.9, 10, attack, loop=False)})


# ===================================================================== Hidra Manatita
def hydra():
    m = Model('manatite_hydra', 256)
    m.bone('root', None, (0, 0, 0))
    m.bone('body', 'root', (0, 18, 0))
    m.cube('body', (-16, 12, -18), (32, 16, 36), 'scales')
    m.cube('body', (-14, 10, -16), (28, 3, 32), 'belly')
    # cristales de manatita en el lomo
    for (x, z, h, ang) in ((-8, -8, 10, -15), (6, -4, 13, 12), (-3, 6, 9, 5), (9, 10, 8, 20), (-10, 12, 7, -25), (0, -14, 8, 0), (12, -12, 6, 30)):
        m.cube('body', (x - 1.5, 26, z - 1.5), (3, h, 3), 'crystal', rot=(ang * 0.5, 45, ang), pivot=(x, 27, z))
    for n, (x, z) in {'fl': (-14, -12), 'fr': (14, -12), 'bl': (-14, 12), 'br': (14, 12)}.items():
        m.bone('leg_' + n, 'body', (x, 14, z))
        m.cube('leg_' + n, (x - 4, 0, z - 4), (8, 15, 8), 'scales')
        for k in (-1, 0, 1):
            m.cube('leg_' + n, (x + k * 2.6 - 0.6, 0, z - 5.5), (1.2, 1.2, 2), CLAW)
    parent, z = 'body', 18
    for i in range(4):
        b = 'tail%d' % i
        th = 12 - i * 2.5
        m.bone(b, parent, (0, 20, z))
        m.cube(b, (-th / 2, 20 - th / 2, z), (th, th, 9), 'scales')
        parent, z = b, z + 9
    # cinco cuellos con cabezas
    heads = [(-13, -42), (-6.5, -20), (0, 0), (6.5, 20), (13, 42)]
    for k, (x, yaw) in enumerate(heads):
        parent, nz, ny = 'body', -16, 24
        for i in range(4):
            b = 'n%d_%d' % (k, i)
            rot = [(-34 if i == 0 else -8 + i * 5), yaw if i == 0 else 0, (yaw * -0.25 if i == 0 else 0)]
            m.bone(b, parent, (x if i == 0 else x, ny, nz), rot)
            m.cube(b, (x - 2.6 + i * 0.3, ny - 2.6 + i * 0.3, nz - 7), (5.2 - i * 0.6, 5.2 - i * 0.6, 7.4), 'scales')
            m.cube(b, (x - 0.6, ny + 2.2 - i * 0.3, nz - 5), (1.2, 1.5, 2), 'crystal')
            parent, nz = b, nz - 7
        hb = 'h%d' % k
        m.bone(hb, parent, (x, ny, nz), [25, 0, 0])
        m.cube(hb, (x - 3.5, ny - 2.5, nz - 7), (7, 5, 7), 'scales')
        m.cube(hb, (x - 2.5, ny - 2.5, nz - 12), (5, 3.5, 5), 'scales')
        for side in (-1, 1):
            m.cube(hb, (x + side * 3.4 - 0.4, ny + 0.5, nz - 5.5), (0.8, 1, 1.4), 'eye')
            m.cube(hb, (x + side * 2.2 - 0.6, ny + 2, nz - 3), (1.2, 3, 1.2), 'crystal', rot=(-35, 0, side * -20), pivot=(x + side * 2.2, ny + 2, nz - 3))
        jb = 'j%d' % k
        m.bone(jb, hb, (x, ny - 2, nz - 4))
        m.cube(jb, (x - 2.3, ny - 4, nz - 11.5), (4.6, 1.8, 8), 'belly')
        m.cube(hb, (x - 1.6, ny - 3, nz - 11.5), (0.6, 1, 0.6), TEETH)
        m.cube(hb, (x + 1, ny - 3, nz - 11.5), (0.6, 1, 0.6), TEETH)

    def necks(t, amp=1.0):
        out = {}
        for k in range(5):
            ph = k * 0.21
            for i in range(4):
                base = -30 if i == 0 else -10 + i * 6
                out['n%d_%d' % (k, i)] = {'rotation': [base + 5 * amp * S(t, ph + i * 0.08), (heads[k][1] if i == 0 else 0) + 7 * amp * S(t, ph + 0.25 + i * 0.1), 0]}
            out['h%d' % k] = {'rotation': [25 + 6 * S(t, ph + 0.4), 10 * S(t, ph, 0.5), 0]}
            out['j%d' % k] = {'rotation': [6 + 8 * pulse((t + ph) % 1, 0.3, 0.5), 0, 0]}
        return out

    def idle(t):
        out = necks(t)
        out['body'] = {'position': [0, 0.4 * S(t), 0]}
        for i in range(4):
            out['tail%d' % i] = {'rotation': [0, 6 * S(t, -i * 0.1), 0]}
        return out

    def walk(t):
        out = necks(t, 1.4)
        out['body'] = {'position': [0, 0.7 * abs(S(t)), 0], 'rotation': [0, 2 * S(t), 3 * S(t)]}
        for n, ph in (('fl', 0), ('br', 0), ('fr', 0.5), ('bl', 0.5)):
            out['leg_' + n] = {'rotation': [20 * S(t, ph), 0, 0]}
        for i in range(4):
            out['tail%d' % i] = {'rotation': [0, 12 * S(t, -i * 0.15), 0]}
        return out

    def attack(t):
        out = necks(t)
        for k in (1, 2, 3):
            back = pulse(t, 0, 0.4)
            strike = pulse(t, 0.3 + (k - 1) * 0.08, 0.75 + (k - 1) * 0.08)
            out['n%d_0' % k] = {'rotation': [-30 - 25 * back + 45 * strike, heads[k][1], 0]}
            out['h%d' % k] = {'rotation': [25 + 15 * back - 30 * strike, 0, 0]}
            out['j%d' % k] = {'rotation': [55 * strike, 0, 0]}
        return out

    pal = dict(scales=Scales('3a5a6a', '1a2a34', '6a8a9a'), belly=Scales('8aa0a0', '4a6060', 'b8cccc'),
               crystal=Crystal('6a4ad8', 'd8c8ff', '2a1a6a'), eye=Solid('ff4040', 0.02, glow=True))
    m.save({'manatite_hydra': pal}, {'idle': cycle(4.0, 16, idle), 'walk': cycle(1.6, 8, walk), 'attack': cycle(1.2, 12, attack, loop=False)})


if __name__ == '__main__':
    for fn in (coyote, acid_wolf, sacred_beast, rock_turtle, anaconda, cobras, treant, red_wyrm, hydra):
        fn()
        print('ok', fn.__name__)

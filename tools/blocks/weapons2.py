"""Espadas y lanzas nuevas: modelos 3D de item (en diagonal como las espadas, igual que spears.py) y texturas de 32 px.
Espadas (una por estilo de la novela):
- sanctuary_blade: Espada del Santuario (Estilo Dios de la Espada) - hoja recta larga, guarda en cruz plateada, gema azul.
- north_god_greatsword: Mandoble del Dios del Norte - hoja ancha y gruesa, guarda pesada, piel en el mango.
- water_god_blade: Hoja del Dios del Agua - hoja fina y levemente curva, guarda redonda, borla celeste.
Lanzas:
- millis_holy_spear: Lanza Sagrada de Millis - asta blanca, punta de plata con alas doradas y estandarte azul.
- red_wyrm_spear: Lanza de Colmillo de Wyrm Rojo - asta oscura, punta de colmillo con escamas rojas.
- great_forest_spear: Lanza de Cazador del Gran Bosque - asta de madera, punta de hoja, plumas atadas."""
import json, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))
REG = {'shaft': [0, 0, 2, 16], 'grip': [2, 0, 4, 8], 'metal': [4, 0, 8, 8], 'accent': [8, 0, 12, 8], 'tip': [12, 0, 16, 4],
       'dark': [12, 4, 16, 8], 'gem': [2, 8, 4, 12], 'extra': [4, 8, 8, 16], 'edge': [8, 8, 12, 16], 'cloth': [12, 8, 16, 16]}


def texture(name, pal):
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    sh = lambda c, k: tuple(max(0, min(255, int(v * k))) for v in c)

    def rect(r, fn):
        u0, v0, u1, v1 = r
        for y in range(v0 * 2, v1 * 2):
            for x in range(u0 * 2, u1 * 2):
                img.putpixel((x, y), tuple(fn(x - u0 * 2, y - v0 * 2, (u1 - u0) * 2, (v1 - v0) * 2)) + (255,))
    rect(REG['shaft'], lambda x, y, w, h: sh(pal['shaft'], (1.1 if x == 0 else 0.85) * (0.9 if (y // 3 + x) % 4 == 0 else 1)))
    rect(REG['grip'], lambda x, y, w, h: sh(pal['grip'], 1.15 if (x + y) % 3 == 0 else 0.88 if y % 2 else 1))
    rect(REG['metal'], lambda x, y, w, h: sh(pal['metal'], 1.2 if x == 1 else 0.78 if x == w - 1 else 1 - y * 0.01))
    rect(REG['accent'], lambda x, y, w, h: sh(pal['accent'], 1.18 if (x + y * 2) % 5 == 0 else 0.86 if x % 3 == 0 else 1))
    rect(REG['tip'], lambda x, y, w, h: sh(pal['metal'], 1.35 if x < 2 else 1.08))
    rect(REG['dark'], lambda x, y, w, h: sh(pal['dark'], 1.12 if (x + y) % 4 == 0 else 1))
    rect(REG['gem'], lambda x, y, w, h: (255, 255, 255) if (x, y) == (1, 1) else sh(pal['gem'], 1.25 if x + y < 4 else 0.8))
    rect(REG['extra'], lambda x, y, w, h: sh(pal.get('extra', pal['accent']), 1.1 if (x * 7 + y) % 6 == 0 else 0.88 if y % 4 == 0 else 1))
    # filo: claro en el borde, canal en el medio
    rect(REG['edge'], lambda x, y, w, h: sh(pal.get('edge', pal['metal']), [1.3, 1.1, 0.95, 0.75, 0.75, 0.95, 1.1, 1.3][x] * (1 - y * 0.008)))
    rect(REG['cloth'], lambda x, y, w, h: sh(pal.get('cloth', pal['accent']), 1.12 if y % 4 == 0 else 0.9 if (x + y) % 5 == 0 else 1))
    img.save(os.path.join(A, 'textures', 'item', name + '.png'))


def maker(shift, lo=-16):
    def el(f, t, reg):
        f = [f[0], max(lo, f[1] + shift), f[2]]; t = [t[0], max(lo + 0.5, t[1] + shift), t[2]]
        assert all(-16 <= v <= 32 for v in f + t), (f, t)
        return {'from': f, 'to': t, 'faces': {k: {'texture': '#0', 'uv': REG[reg]} for k in ('north', 'south', 'east', 'west', 'up', 'down')},
                'rotation': {'angle': -45, 'axis': 'z', 'origin': [8, 8, 8]}}

    def box(cx, y0, y1, w, d=None, reg='shaft'):
        d = d if d is not None else w
        return el([cx - w / 2, y0, 8 - d / 2], [cx + w / 2, y1, 8 + d / 2], reg)
    return el, box


# ---------------------------------------------------------------- espadas (mano en y 0..4 despues del corrimiento)
def sanctuary():
    el, box = maker(-4)
    return [box(8, 0, 1.6, 1.8, reg='metal'), box(8, 1.6, 7, 1.1, reg='grip'),                        # pomo y mango
            box(8, 7, 8, 7.0, 1.4, 'metal'), box(4.6, 7.5, 9, 0.8, 1.1, 'metal'), box(11.4, 7.5, 9, 0.8, 1.1, 'metal'),
            box(8, 6.9, 8.1, 1.3, 1.6, 'gem'),                                                           # gema azul
            box(8, 8, 28, 1.9, 0.45, 'edge'), box(8, 28, 30.2, 1.1, 0.4, 'edge'), box(8, 30.2, 31.4, 0.5, 0.35, 'tip')]


def northgod():
    el, box = maker(-4)
    return [box(8, -1, 1, 2.4, reg='dark'), box(8, 1, 8, 1.4, reg='grip'), box(8, 4, 6.5, 2.0, 1.8, 'cloth'),   # piel en el mango
            box(8, 8, 10, 7.6, 2.4, 'dark'), box(8, 7.4, 8, 3.0, 2.0, 'metal'),
            box(8, 10, 28.5, 3.6, 0.8, 'edge'), box(8, 12, 26, 0.9, 0.9, 'dark'),                    # hoja ancha y lomo
            box(8, 28.5, 30.5, 2.4, 0.7, 'edge'), box(8, 30.5, 31.8, 1.1, 0.6, 'tip')]


def watergod():
    el, box = maker(-4)
    e = [box(8, 0.3, 1.5, 1.3, reg='accent'), box(8, 1.5, 7.2, 0.95, reg='grip'),
         box(8, 7.2, 8, 3.0, 3.0, 'metal'),                                                           # guarda redonda (tsuba)
         el([8.4, 2.5, 7.7], [9.0, 6, 8.3], 'cloth')]                                                 # borla celeste
    # hoja fina con una curva suave (tramos corridos un poco hacia el filo)
    y, x = 8, 8.0
    for i, (ln, w) in enumerate([(5, 1.25), (5, 1.2), (5, 1.15), (4, 1.05), (3, 0.9)]):
        e.append(box(x, y, y + ln, w, 0.35, 'edge'))
        y += ln; x += 0.18
    e.append(box(x, y, y + 1.4, 0.45, 0.3, 'tip'))
    return e


# ---------------------------------------------------------------- lanzas (mismo agarre que las lanzas Superd)
def millis():
    el, box = maker(-8, lo=-15.5)
    return [box(8, -7.5, -6, 1.5, reg='accent'), box(8, -6, 21, 1.0), box(8, 3.5, 11.5, 1.3, reg='grip'),
            box(8, 20.5, 22, 2.0, reg='accent'),
            el([8.6, 14, 7.8], [12.2, 20, 8.2], 'cloth'),                                              # estandarte azul
            box(8, 22, 28, 2.2, 0.6, 'metal'), box(8, 28, 30.4, 1.2, 0.5, 'metal'), box(8, 30.4, 31.6, 0.5, 0.4, 'tip'),
            box(5.6, 21.6, 23.2, 1.6, 0.4, 'accent'), box(10.4, 21.6, 23.2, 1.6, 0.4, 'accent'),       # alas doradas
            box(4.6, 22.8, 24.2, 0.8, 0.35, 'accent'), box(11.4, 22.8, 24.2, 0.8, 0.35, 'accent')]


def wyrm():
    el, box = maker(-8, lo=-15.5)
    e = [box(8, -7.5, -5.5, 1.6, reg='tip'), box(8, -5.5, 20, 1.05, reg='dark'), box(8, 3.5, 11.5, 1.35, reg='grip')]
    for k in range(4):                                                                                  # escamas en el cuello
        y = 15 + k * 1.4
        e.append(box(8, y, y + 1.0, 1.7 - k * 0.1, 1.7 - k * 0.1, 'accent'))
    e += [box(8, 20.5, 22, 2.4, 1.3, 'accent'),
          box(8, 22, 26, 2.0, 0.9, 'extra'), box(8, 26, 29, 1.4, 0.75, 'extra'), box(8, 29, 31.3, 0.7, 0.55, 'tip'),  # colmillo
          box(6.2, 21.5, 23.5, 0.7, 0.6, 'extra'), box(9.8, 21.5, 23.5, 0.7, 0.6, 'extra')]
    return e


def forest():
    el, box = maker(-8, lo=-15.5)
    return [box(8, -7.5, -6, 1.3, reg='grip'), box(8, -6, 21, 1.0), box(8, 3.5, 11.5, 1.3, reg='grip'),
            box(8, 20.2, 21.6, 1.5, reg='grip'),                                                        # atadura
            el([8.6, 16.5, 7.75], [9.3, 20, 8.25], 'cloth'), el([6.7, 16, 7.75], [7.4, 19.5, 8.25], 'extra'),  # plumas
            box(8, 21.6, 24, 1.6, 0.5, 'metal'), box(8, 24, 27.5, 2.4, 0.5, 'metal'), box(8, 27.5, 30, 1.4, 0.45, 'metal'),
            box(8, 30, 31.4, 0.6, 0.4, 'tip')]


WEAPONS = {
    'sanctuary_blade': (sanctuary, 'sword', dict(shaft=(200, 200, 205), grip=(40, 50, 90), metal=(200, 206, 216), accent=(190, 196, 206),
                                                 dark=(70, 70, 80), gem=(60, 110, 255), edge=(214, 220, 230), cloth=(40, 50, 90))),
    'north_god_greatsword': (northgod, 'great', dict(shaft=(90, 80, 70), grip=(70, 46, 30), metal=(150, 150, 156), accent=(150, 120, 80),
                                                     dark=(56, 54, 60), gem=(200, 60, 40), edge=(176, 180, 188), cloth=(196, 186, 168))),
    'water_god_blade': (watergod, 'sword', dict(shaft=(200, 200, 205), grip=(220, 226, 236), metal=(110, 150, 200), accent=(120, 180, 230),
                                                dark=(50, 70, 100), gem=(80, 180, 255), edge=(206, 222, 238), cloth=(110, 200, 240))),
    'millis_holy_spear': (millis, 'spear', dict(shaft=(236, 232, 222), grip=(60, 70, 120), metal=(212, 218, 228), accent=(230, 190, 80),
                                                dark=(80, 80, 90), gem=(240, 220, 120), cloth=(50, 80, 170))),
    'red_wyrm_spear': (wyrm, 'spear', dict(shaft=(60, 50, 50), grip=(40, 26, 22), metal=(210, 200, 180), accent=(176, 44, 36),
                                           dark=(52, 34, 32), gem=(240, 120, 40), extra=(236, 224, 196))),
    'great_forest_spear': (forest, 'spear', dict(shaft=(140, 104, 64), grip=(90, 70, 46), metal=(170, 176, 170), accent=(80, 120, 60),
                                                 dark=(60, 50, 40), gem=(90, 160, 80), extra=(230, 230, 220), cloth=(170, 60, 40))),
}

SWORD_DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]},
    'gui': {'rotation': [0, 0, 0], 'translation': [1.2, 1.2, 0], 'scale': [0.58, 0.58, 0.58]},
    'ground': {'rotation': [0, 0, 0], 'translation': [1.2, 3.2, 0], 'scale': [0.45, 0.45, 0.45]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [-1.2, 1.2, 0], 'scale': [0.6, 0.6, 0.6]}}
SPEAR_DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [1.0, 1.0, 1.0]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [1.0, 1.0, 1.0]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'gui': {'rotation': [0, 0, 0], 'translation': [1.4, 1.4, 0], 'scale': [0.5, 0.5, 0.5]},
    'ground': {'rotation': [0, 0, 0], 'translation': [1.2, 3.2, 0], 'scale': [0.4, 0.4, 0.4]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [-1.6, 1.6, 0], 'scale': [0.55, 0.55, 0.55]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]}}

for name, (fn, kind, pal) in WEAPONS.items():
    texture(name, pal)
    disp = json.loads(json.dumps(SPEAR_DISPLAY if kind == 'spear' else SWORD_DISPLAY))
    if kind == 'great':
        for k in ('thirdperson_righthand', 'thirdperson_lefthand'): disp[k]['scale'] = [1.0, 1.0, 1.0]
    model = {'textures': {'0': 'remushoku:item/' + name, 'particle': 'remushoku:item/' + name}, 'elements': fn(), 'display': disp}
    with open(os.path.join(A, 'models', 'item', name + '.json'), 'w') as f:
        json.dump(model, f, indent=1)
print('armas listas')

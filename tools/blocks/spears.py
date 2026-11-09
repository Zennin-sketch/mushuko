"""Lanzas Superd: modelos 3D de item (puestos en diagonal como las espadas) y sus texturas.
- superd_warrior_spear: tridente de los guerreros (asta clara, tres puntas plateadas, borla roja como su joya).
- superd_tail_spear: lanza de cola (hueso segmentado verdoso, hoja de hoja de sauce, anillo esmeralda).
- laplace_spear: lanza maldita de Laplace (asta negra con venas violetas, hoja dentada y un ojo violeta)."""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))

# regiones del atlas (unidades de 16; la textura es de 32 px)
REG = {'shaft': [0, 0, 2, 16], 'grip': [2, 0, 4, 8], 'metal': [4, 0, 8, 8], 'accent': [8, 0, 12, 8], 'tip': [12, 0, 16, 4],
       'dark': [12, 4, 16, 8], 'gem': [2, 8, 4, 12], 'extra': [4, 8, 8, 16]}


def texture(name, pal):
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    def rect(r, fn):
        u0, v0, u1, v1 = r
        for y in range(v0 * 2, v1 * 2):
            for x in range(u0 * 2, u1 * 2):
                img.putpixel((x, y), tuple(fn(x - u0 * 2, y - v0 * 2, (u1 - u0) * 2, (v1 - v0) * 2)) + (255,))
    sh = lambda c, k: tuple(max(0, min(255, int(v * k))) for v in c)
    rect(REG['shaft'], lambda x, y, w, h: sh(pal['shaft'], (1.1 if x == 0 else 0.85) * (pal.get('seg', 0) and (0.8 if y % 8 == 0 else 1) or 1)
                                            * (0.92 if (y // 3 + x) % 4 == 0 else 1)) if not (pal.get('vein') and (y * 3 + x * 5) % 11 == 0) else pal['vein'])
    rect(REG['grip'], lambda x, y, w, h: sh(pal['grip'], 1.15 if (x + y) % 3 == 0 else 0.9 if y % 2 else 1))
    rect(REG['metal'], lambda x, y, w, h: sh(pal['metal'], 1.2 if x == 1 else 0.75 if x == w - 1 else 1 - y * 0.012))
    rect(REG['accent'], lambda x, y, w, h: sh(pal['accent'], 1.15 if (x + y * 2) % 5 == 0 else 0.85 if x % 3 == 0 else 1))
    rect(REG['tip'], lambda x, y, w, h: sh(pal['metal'], 1.35 if x < 2 else 1.05))
    rect(REG['dark'], lambda x, y, w, h: sh(pal['dark'], 1.1 if (x + y) % 4 == 0 else 1))
    rect(REG['gem'], lambda x, y, w, h: (255, 255, 255) if (x, y) == (1, 1) else sh(pal['gem'], 1.25 if x + y < 4 else 0.8))
    rect(REG['extra'], lambda x, y, w, h: sh(pal.get('extra', pal['accent']), 1.1 if (x * 7 + y) % 6 == 0 else 0.9 if y % 4 == 0 else 1))
    img.save(os.path.join(A, 'textures', 'item', name + '.png'))


SHIFT = -8  # la mano agarra el mango: todo baja 8 px por el eje de la lanza (lo de abajo del mango queda corto)


def el(f, t, reg, rot=None):
    f = [f[0], max(-16, f[1] + SHIFT), f[2]]; t = [t[0], max(-15.5, t[1] + SHIFT), t[2]]
    e = {'from': f, 'to': t, 'faces': {k: {'texture': '#0', 'uv': REG[reg]} for k in ('north', 'south', 'east', 'west', 'up', 'down')},
         'rotation': {'angle': -45, 'axis': 'z', 'origin': [8, 8, 8]}}
    return e


def box(cx, y0, y1, w, d=None, reg='shaft'):
    d = d if d is not None else w
    return el([cx - w / 2, y0, 8 - d / 2], [cx + w / 2, y1, 8 + d / 2], reg)


def warrior():
    e = [box(8, -7.5, -6, 1.3, reg='metal'), box(8, -6, 21, 1.0), box(8, 3.5, 11.5, 1.3, reg='grip'),
         box(8, 20.5, 22.5, 2.0, reg='metal'),
         el([8.6, 17.5, 7.65], [9.4, 21, 8.35], 'accent'), el([6.6, 17.5, 7.65], [7.4, 21, 8.35], 'accent'),   # borla roja
         box(8, 22.5, 23.5, 6.2, 1.0, 'metal'),                                                                 # travesano
         box(8, 23.5, 30, 1.2, 0.8, 'metal'), box(8, 30, 31.6, 0.6, 0.6, 'tip'),
         box(5.4, 23.5, 28, 1.0, 0.8, 'metal'), box(5.4, 28, 29.4, 0.5, 0.5, 'tip'),
         box(10.6, 23.5, 28, 1.0, 0.8, 'metal'), box(10.6, 28, 29.4, 0.5, 0.5, 'tip'),
         box(8, 18.6, 20.2, 1.5, 1.5, 'gem')]                                                                     # joya roja
    return e


def tail():
    e = [box(8, -7.5, -6, 1.6, reg='extra')]
    y = -6
    while y < 19:                       # vertebras: asta segmentada con nudos
        e.append(box(8, y, y + 3.2, 1.0))
        e.append(box(8, y + 3.2, y + 4, 1.4, reg='extra'))
        y += 4
    e += [box(8, 3.5, 11.5, 1.3, reg='grip'), box(8, 19, 20.5, 1.9, reg='gem'),     # anillo esmeralda
          box(8, 20.5, 22.5, 1.6, 0.7, 'metal'), box(8, 22.5, 27, 2.4, 0.6, 'metal'), box(8, 27, 29.5, 1.4, 0.55, 'metal'),
          box(8, 29.5, 31, 0.6, 0.5, 'tip')]
    return e


def laplace():
    e = [box(8, -7.5, -6, 1.5, reg='dark'), box(8, -6, 20, 1.0), box(8, 3.5, 11.5, 1.3, reg='grip'),
         box(8, 16, 18, 1.8, reg='dark'), box(8, 18, 19.8, 1.6, 1.6, 'gem'),          # ojo violeta
         box(8, 19.8, 21, 2.2, reg='dark'),
         box(8, 21, 26, 2.8, 0.6, 'metal'), box(8, 26, 29, 1.6, 0.55, 'metal'), box(8, 29, 31.2, 0.7, 0.5, 'tip'),
         box(5.9, 21, 23.5, 0.9, 0.5, 'metal'), box(10.1, 21, 23.5, 0.9, 0.5, 'metal'),    # puas
         box(5.4, 22.5, 24.5, 0.6, 0.45, 'tip'), box(10.6, 22.5, 24.5, 0.6, 0.45, 'tip'),
         el([8.5, 12, 7.65], [9.2, 16, 8.35], 'accent')]                                    # tira de tela gastada
    return e


SPEARS = {
    'superd_warrior_spear': (warrior, dict(shaft=(222, 214, 196), grip=(70, 48, 34), metal=(196, 204, 214), accent=(190, 32, 36),
                                           dark=(60, 60, 66), gem=(220, 30, 40))),
    'superd_tail_spear': (tail, dict(shaft=(214, 220, 196), grip=(60, 80, 52), metal=(206, 210, 200), accent=(110, 160, 90),
                                     dark=(70, 80, 60), gem=(40, 180, 110), extra=(186, 196, 160), seg=1)),
    'laplace_spear': (laplace, dict(shaft=(40, 32, 48), grip=(30, 20, 34), metal=(120, 100, 140), accent=(90, 40, 110),
                                    dark=(26, 20, 32), gem=(190, 60, 255), vein=(170, 70, 230))),
}

for name, (fn, pal) in SPEARS.items():
    texture(name, pal)
    model = {'textures': {'0': 'remushoku:item/' + name, 'particle': 'remushoku:item/' + name}, 'elements': fn(),
             'display': {
                 'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [1.0, 1.0, 1.0]},
                 'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [1.0, 1.0, 1.0]},
                 'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
                 'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
                 'gui': {'rotation': [0, 0, 0], 'translation': [1.4, 1.4, 0], 'scale': [0.5, 0.5, 0.5]},
                 'ground': {'rotation': [0, 0, 0], 'translation': [1.2, 3.2, 0], 'scale': [0.4, 0.4, 0.4]},
                 'fixed': {'rotation': [0, 180, 0], 'translation': [-1.6, 1.6, 0], 'scale': [0.55, 0.55, 0.55]},
                 'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]}}}
    with open(os.path.join(A, 'models', 'item', name + '.json'), 'w') as f:
        json.dump(model, f, indent=1)
print('lanzas listas')

"""Daga de bandido: modelo 3D de item (hoja de acero con canal, guarda oscura, mango con cuero, pomo de bronce y un
trapo rojo atado) puesto en diagonal como las espadas, asi usa las mismas poses de mano que un item 'handheld'."""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
P = lambda x, y, c: img.putpixel((x, y), tuple(c) + (255,))
# acero (u0-4 v0-8): borde claro, canal oscuro en el medio
for y in range(8):
    for x in range(4):
        c = [(214, 220, 228), (172, 180, 192), (120, 128, 142), (196, 204, 214)][x]
        k = 1 - y * 0.02
        P(x, y, tuple(int(v * k) for v in c))
# guarda (u4-8 v0-2) hierro oscuro con remaches
for y in range(2):
    for x in range(4, 8):
        P(x, y, (70, 66, 72) if (x + y) % 3 else (110, 104, 100))
# mango (u4-6 v2-8): cuero con vueltas
for y in range(2, 8):
    for x in range(4, 6):
        P(x, y, (92, 58, 34) if y % 2 else (64, 40, 24))
# pomo (u6-8 v2-4) bronce
for y in range(2, 4):
    for x in range(6, 8):
        P(x, y, (196, 150, 70) if (x + y) % 2 else (150, 110, 48))
# trapo rojo (u6-8 v4-8)
for y in range(4, 8):
    for x in range(6, 8):
        P(x, y, (176, 30, 34) if (x + y) % 3 else (120, 18, 22))
os.makedirs(os.path.join(A, 'textures', 'item'), exist_ok=True)
img.save(os.path.join(A, 'textures', 'item', 'bandit_dagger.png'))


def el(f, t, uv, rot=True):
    faces = {k: {'texture': '#0', 'uv': uv} for k in ('north', 'south', 'east', 'west', 'up', 'down')}
    e = {'from': f, 'to': t, 'faces': faces}
    if rot:
        e['rotation'] = {'angle': -45, 'axis': 'z', 'origin': [8, 8, 8]}
    return e


S = 'steel'
R = {'steel': [0, 0, 4, 8], 'tip': [0, 0, 4, 3], 'guard': [4, 0, 8, 2], 'grip': [4, 2, 6, 8], 'pommel': [6, 2, 8, 4], 'rag': [6, 4, 8, 8]}
els = [
    el([7.25, 0.5, 7.25], [8.75, 2, 8.75], R['pommel']),
    el([7.5, 2, 7.5], [8.5, 6.5, 8.5], R['grip']),
    el([8.5, 3.5, 7.6], [9.6, 5.8, 8.4], R['rag']),
    el([5.25, 6.5, 7.25], [10.75, 7.5, 8.75], R['guard']),
    el([7, 7.5, 7.6], [9, 12.5, 8.4], R['steel']),
    el([7.35, 12.5, 7.65], [8.65, 14.5, 8.35], R['steel']),
    el([7.7, 14.5, 7.7], [8.3, 15.75, 8.3], R['tip']),
]
# sin 'parent': si hereda de item/generated, el juego arma el item con la textura plana y se pierden los cubos
model = {'textures': {'0': 'remushoku:item/bandit_dagger', 'particle': 'remushoku:item/bandit_dagger'},
         'elements': els,
         'display': {'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
                     'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]},
                     'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
                     'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
                     'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
                     'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                     'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
                     'fixed': {'rotation': [0, 180, 0], 'scale': [1, 1, 1]}}}
with open(os.path.join(A, 'models', 'item', 'bandit_dagger.json'), 'w') as f:
    json.dump(model, f, indent=2)
print('daga lista')

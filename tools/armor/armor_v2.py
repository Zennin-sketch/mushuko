"""Armaduras v2 (rediseno) como modelos 3D de GeckoLib + texturas pintadas por codigo + iconos.

Genera, para cada armadura:
  assets/remushoku/geo/armor_v2/<nombre>.geo.json
  assets/remushoku/textures/armor_v2/<nombre>.png  (+ <nombre>_glowmask.png si tiene partes que brillan)
  assets/remushoku/textures/item/<item>.png y models/item/<item>.json (icono de frente)

Coordenadas Bedrock: Y arriba, los pies en 0, la cabeza de 24 a 32, mirando a -Z (el frente).
Cada caja: hueso, origen (esquina minima), tamano, material y detalle opcional.
"""
import json, math, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))

BONES = {  # hueso de armadura -> (hueso del cuerpo, pivote)
    'armorHead': ('bipedHead', [0, 24, 0]),
    'armorBody': ('bipedBody', [0, 24, 0]),
    'armorRightArm': ('bipedRightArm', [-5, 22, 0]),
    'armorLeftArm': ('bipedLeftArm', [5, 22, 0]),
    'armorRightLeg': ('bipedRightLeg', [-1.9, 12, 0]),
    'armorLeftLeg': ('bipedLeftLeg', [1.9, 12, 0]),
    'armorRightBoot': ('bipedRightLeg', [-1.9, 12, 0]),
    'armorLeftBoot': ('bipedLeftLeg', [1.9, 12, 0]),
}
MIRROR = {'armorRightArm': 'armorLeftArm', 'armorRightLeg': 'armorLeftLeg', 'armorRightBoot': 'armorLeftBoot'}

# Materiales: base, oscuro (bordes), claro (luz de arriba), y si brilla
MATS = {
    'steel':   dict(base=(92, 98, 112), dark=(48, 52, 62), light=(150, 158, 175)),
    'steel_l': dict(base=(128, 136, 152), dark=(70, 76, 90), light=(186, 194, 208)),
    'dark':    dict(base=(44, 46, 54), dark=(24, 25, 30), light=(70, 74, 86)),
    'gold':    dict(base=(196, 152, 58), dark=(120, 86, 30), light=(240, 206, 110)),
    'crystal': dict(base=(70, 170, 255), dark=(30, 90, 190), light=(200, 240, 255), glow=True),
    'bronze':  dict(base=(122, 88, 56), dark=(70, 48, 30), light=(170, 128, 84)),
    'rune':    dict(base=(70, 50, 90), dark=(40, 28, 52), light=(180, 120, 255), glow_lines=True),
    'black':   dict(base=(28, 28, 32), dark=(12, 12, 14), light=(60, 60, 68)),
    'lens':    dict(base=(30, 40, 70), dark=(15, 20, 40), light=(140, 170, 230)),
    'leather': dict(base=(52, 84, 140), dark=(30, 48, 86), light=(84, 120, 186)),
    'cloth':   dict(base=(226, 222, 210), dark=(160, 152, 136), light=(250, 248, 240)),
    'feather': dict(base=(150, 230, 255), dark=(70, 160, 210), light=(235, 252, 255), glow=True),
    'crimson': dict(base=(150, 32, 36), dark=(84, 14, 18), light=(196, 64, 60)),
    'fur':     dict(base=(222, 206, 176), dark=(160, 140, 108), light=(244, 234, 214)),
    'ember':   dict(base=(255, 140, 40), dark=(200, 70, 20), light=(255, 230, 140), glow=True),
    'cloth_d': dict(base=(60, 30, 34), dark=(36, 16, 20), light=(90, 50, 54)),
}

def box(bone, origin, size, mat, detail=None, rot=None, pivot=None):
    return dict(bone=bone, origin=list(origin), size=list(size), mat=mat, detail=detail, rot=rot, pivot=pivot)

def mirrored(b):
    """Copia del lado izquierdo (espejo en X) para brazos, piernas y botas."""
    o, s = b['origin'], b['size']
    m = dict(b, bone=MIRROR[b['bone']], origin=[-(o[0] + s[0]), o[1], o[2]])
    if b['rot']:
        m['rot'] = [b['rot'][0], -b['rot'][1], -b['rot'][2]]
        m['pivot'] = [-b['pivot'][0], b['pivot'][1], b['pivot'][2]]
    return m

def sym(boxes):
    out = []
    for b in boxes:
        out.append(b)
        if b['bone'] in MIRROR:
            out.append(mirrored(b))
    return out

# ================================================================ Disenos
def mk2():
    B, R, L, F = 'armorBody', 'armorRightArm', 'armorRightLeg', 'armorRightBoot'
    return sym([
        box(B, [-4.75, 15, -3], [9.5, 9.25, 6], 'steel'),
        box(B, [-4.5, 12, -2.6], [9, 3, 5.2], 'dark', 'bands'),
        box(B, [-3.5, 17, -3.6], [7, 6, 0.75], 'steel_l', 'rivets'),
        box(B, [-1.25, 19.25, -4.1], [2.5, 2.5, 0.75], 'crystal'),
        box(B, [-4.5, 23.5, -3.5], [9, 1.5, 7], 'gold'),
        box(B, [-3.5, 14, 3], [7, 8, 2.5], 'dark', 'vents'),
        box(B, [-3, 21.5, 4], [1.5, 3, 1.5], 'steel_l'),
        box(B, [1.5, 21.5, 4], [1.5, 3, 1.5], 'steel_l'),
        box(R, [-9.5, 21, -3.25], [6, 3.5, 6.5], 'steel', 'rivets'),
        box(R, [-9.6, 21, -3.35], [6.2, 0.75, 6.7], 'gold'),
        box(R, [-8.6, 16, -2.6], [4.8, 5, 5.2], 'dark', 'bands'),
        box(R, [-8.9, 12, -2.9], [5.4, 4.5, 5.8], 'steel', 'rivets'),
        box(R, [-9.1, 13.5, -0.75], [0.5, 1.5, 1.5], 'crystal'),
        box(L, [-4.4, 6, -2.6], [4.8, 6, 5.2], 'steel', 'plates'),
        box(L, [-4.2, 4.5, -3.3], [4.4, 2.5, 1.2], 'gold'),
        box(F, [-4.5, 0, -3.4], [5, 5, 6.2], 'steel', 'rivets'),
        box(F, [-4.6, 0, -3.6], [5.2, 1, 6.6], 'dark'),
        box(F, [-2.75, 2.5, -3.75], [1.5, 1.5, 0.4], 'crystal'),
    ])

def curse_helmet():
    H = 'armorHead'
    return [
        box(H, [-4.6, 23.6, -4.6], [9.2, 9, 9.2], 'bronze', 'visor'),
        box(H, [-4.7, 27.5, -4.7], [9.4, 1, 9.4], 'rune'),
        box(H, [-0.6, 32.4, -4.2], [1.2, 1.8, 8.4], 'gold'),
        box(H, [-4.9, 24, -3.5], [0.5, 4, 3], 'bronze'),
        box(H, [4.4, 24, -3.5], [0.5, 4, 3], 'bronze'),
        box(H, [-1.25, 31.0, -4.9], [2.5, 1.5, 0.5], 'rune'),
    ]

def sunglasses():
    H = 'armorHead'
    return [
        box(H, [-0.75, 28, -4.55], [1.5, 0.6, 0.4], 'black'),
        box(H, [-3.9, 26.6, -4.6], [3.2, 2.2, 0.4], 'lens', 'shine'),
        box(H, [0.7, 26.6, -4.6], [3.2, 2.2, 0.4], 'lens', 'shine'),
        box(H, [-4.1, 28.6, -4.65], [3.6, 0.5, 0.5], 'gold'),
        box(H, [0.5, 28.6, -4.65], [3.6, 0.5, 0.5], 'gold'),
        box(H, [-4.6, 27.9, -4.3], [0.4, 0.5, 6.5], 'black'),
        box(H, [4.2, 27.9, -4.3], [0.4, 0.5, 6.5], 'black'),
    ]

def enchanted_shoes():
    F = 'armorRightBoot'
    return sym([
        box(F, [-4.3, 0, -3.2], [4.6, 3.5, 5.8], 'leather', 'stitch'),
        box(F, [-4.4, 3.5, -2.6], [4.8, 1.5, 5.2], 'cloth'),
        box(F, [-4.45, 3.4, -2.65], [4.9, 0.4, 5.3], 'gold'),
        box(F, [-5.0, 1.8, 0.5], [0.6, 3.2, 3.2], 'feather', 'feather'),
        box(F, [-4.35, 0.8, -3.3], [4.7, 0.6, 0.2], 'feather'),
    ])

def flame_cloak():
    B, R = 'armorBody', 'armorRightArm'
    return sym([
        box(B, [-4.3, 12.3, -2.3], [8.6, 8, 4.6], 'cloth_d'),
        box(B, [-5, 20.5, -3.2], [10, 3.5, 6.4], 'crimson', 'trim'),
        box(B, [-4.5, 23.6, -2.8], [9, 1.6, 5.6], 'fur'),
        box(B, [-4.8, 5, 2.4], [9.6, 18, 0.8], 'crimson', 'flames'),
        box(B, [-1, 21.5, -3.6], [2, 2, 0.6], 'ember'),
        box(R, [-8.6, 19.5, -2.7], [4.8, 4.5, 5.4], 'crimson', 'trim'),
    ])

ARMORS = {
    # nombre: (diseno, items [(id, tipo, huesos visibles)], defensa por pieza y material base)
    'mk2_v2': (mk2, [('magic_armor_mk_2_v2_chestplate', 'chest'), ('magic_armor_mk_2_v2_leggings', 'legs'), ('magic_armor_mk_2_v2_boots', 'feet')]),
    'curse_helmet_v2': (curse_helmet, [('curse_suppressing_helmet_v2', 'head')]),
    'sunglasses_v2': (sunglasses, [('magical_sunglasses_v2', 'head')]),
    'enchanted_shoes_v2': (enchanted_shoes, [('enchanted_shoes_v2', 'feet')]),
    'flame_cloak_v2': (flame_cloak, [('flame_trapping_cloak_v2', 'chest')]),
}
SLOT_BONES = {'head': ['armorHead'], 'chest': ['armorBody', 'armorRightArm', 'armorLeftArm'],
              'legs': ['armorRightLeg', 'armorLeftLeg'], 'feet': ['armorRightBoot', 'armorLeftBoot']}

# ================================================================ UV y pintura
def uv_size(s):
    w, h, d = [max(1, math.ceil(x)) for x in s]
    return 2 * (d + w), d + h, (w, h, d)

def pack(boxes, tex=128):
    """Acomoda las regiones UV en filas."""
    x = y = row = 0
    for b in boxes:
        W, H, dims = uv_size(b['size'])
        if x + W > tex:
            x, y, row = 0, y + row, 0
        b['uv'] = [x, y]
        b['dims'] = dims
        x += W
        row = max(row, H)
    while y + row > tex:
        tex *= 2
        return pack(boxes, tex)
    return tex

def faces(b):
    """Regiones de cada cara: nombre -> (x, y, ancho, alto)."""
    u, v = b['uv']
    w, h, d = b['dims']
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h),
            'north': (u + d, v + d, w, h), 'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}

def paint(boxes, tex, seed=1):
    img = Image.new('RGBA', (tex, tex), (0, 0, 0, 0))
    glow = Image.new('RGBA', (tex, tex), (0, 0, 0, 0))
    px, gx = img.load(), glow.load()
    rnd = random.Random(seed)
    any_glow = False

    def put(x, y, c, g=False):
        nonlocal any_glow
        if 0 <= x < tex and 0 <= y < tex:
            px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)
            if g:
                gx[x, y] = px[x, y]
                any_glow = True

    for b in boxes:
        m = MATS[b['mat']]
        det = b['detail']
        for name, (fx, fy, fw, fh) in faces(b).items():
            for j in range(fh):
                for i in range(fw):
                    n = rnd.uniform(-9, 9)
                    c = [m['base'][k] + n for k in range(3)]
                    # luz de arriba (filas de arriba mas claras), bordes oscuros
                    if j == 0 and name not in ('down',):
                        c = [(m['light'][k] + c[k]) / 2 for k in range(3)]
                    if i == 0 or j == fh - 1 or i == fw - 1:
                        c = [(m['dark'][k] * 2 + c[k]) / 3 for k in range(3)]
                    g = m.get('glow', False)
                    if det == 'rivets' and (i in (1, fw - 2)) and (j in (1, fh - 2)) and fw > 3 and fh > 3:
                        c = m['light']
                    if det == 'bands' and j % 2 == 1:
                        c = [x * 0.75 for x in c]
                    if det == 'plates' and j == fh // 2:
                        c = m['dark']
                    if det == 'vents' and name == 'south' and 1 <= i < fw - 1 and j % 2 == 1 and 1 <= j < fh - 1:
                        c, g = (255, 120, 40), True
                    if det == 'visor' and name == 'north':
                        if j in (3, 4) and 1 <= i < fw - 1 or (i in (fw // 2 - 1, fw // 2) and 3 <= j < fh - 2):
                            c = (10, 8, 6)
                    if det == 'shine' and name == 'north' and i == 1 and j == 0:
                        c = (230, 240, 255)
                    if det == 'stitch' and j == 1 and i % 2 == 0:
                        c = MATS['cloth']['light']
                    if det == 'trim' and j == fh - 1:
                        c = MATS['gold']['base']
                    if det == 'feather' and name in ('east', 'west') and (i + j) % 3 == 0:
                        c = m['light']
                    if det == 'flames' and name in ('south', 'north'):
                        # llamas de abajo hacia arriba: naranja que sube en lenguas
                        k = fh - 1 - j
                        tongue = 4 + 3 * math.sin(i * 1.3) + 2 * math.sin(i * 0.7 + 1)
                        if k < tongue:
                            t = k / max(1, tongue)
                            c = [MATS['ember']['light'][q] * (1 - t) + MATS['ember']['dark'][q] * t for q in range(3)]
                            g = k < tongue * 0.5
                    if m.get('glow_lines') and (i + j) % 3 == 0:
                        c, g = m['light'], True
                    put(fx + i, fy + j, c, g)
    return img, (glow if any_glow else None)

# ================================================================ Geo de GeckoLib
def geo(name, boxes, tex):
    bones = {}
    order = []
    for armor_bone, (body_bone, pivot) in BONES.items():
        if body_bone not in bones:
            bones[body_bone] = dict(name=body_bone, pivot=pivot)
            order.append(body_bone)
        bones[armor_bone] = dict(name=armor_bone, parent=body_bone, pivot=pivot, cubes=[])
        order.append(armor_bone)
    for b in boxes:
        c = dict(origin=[round(x, 3) for x in b['origin']], size=[round(x, 3) for x in b['size']], uv=b['uv'])
        if b['rot']:
            c['rotation'] = b['rot']
            c['pivot'] = b['pivot']
        bones[b['bone']]['cubes'].append(c)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': dict(identifier='geometry.' + name, texture_width=tex, texture_height=tex,
                            visible_bounds_width=3, visible_bounds_height=3.5, visible_bounds_offset=[0, 1.25, 0]),
        'bones': [bones[n] for n in order]}]}

# ================================================================ Iconos (vista de frente)
def icon(boxes, bones, img):
    """Dibuja de frente las cajas de esos huesos en 16x16 con el color medio de su cara frontal."""
    sel = [b for b in boxes if b['bone'] in bones]
    if not sel:
        return Image.new('RGBA', (16, 16))
    x0 = min(b['origin'][0] for b in sel); x1 = max(b['origin'][0] + b['size'][0] for b in sel)
    y0 = min(b['origin'][1] for b in sel); y1 = max(b['origin'][1] + b['size'][1] for b in sel)
    span = max(x1 - x0, y1 - y0)
    sc = 14 / span
    ox, oy = (16 - (x1 - x0) * sc) / 2, (16 - (y1 - y0) * sc) / 2
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    po = out.load()
    src = img.load()
    for b in sorted(sel, key=lambda b: -b['origin'][2]):  # lo de adelante al final
        fx, fy, fw, fh = faces(b)['north']
        for yy in range(16):
            for xx in range(16):
                wx = x0 + (xx + 0.5 - ox) / sc
                wy = y1 - (yy + 0.5 - oy) / sc
                if b['origin'][0] <= wx <= b['origin'][0] + b['size'][0] and b['origin'][1] <= wy <= b['origin'][1] + b['size'][1]:
                    u = fx + min(fw - 1, int((wx - b['origin'][0]) / b['size'][0] * fw))
                    v = fy + min(fh - 1, int((b['origin'][1] + b['size'][1] - wy) / b['size'][1] * fh))
                    po[xx, yy] = src[u, v]
    # contorno oscuro estilo item de Minecraft
    edge = out.copy()
    pe = edge.load()
    for yy in range(16):
        for xx in range(16):
            if po[xx, yy][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = xx + dx, yy + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and po[nx, ny][3] > 0:
                        pe[xx, yy] = (20, 18, 24, 255)
                        break
    return edge

def main():
    gdir = os.path.join(ASSETS, 'geo', 'armor_v2')
    tdir = os.path.join(ASSETS, 'textures', 'armor_v2')
    idir = os.path.join(ASSETS, 'textures', 'item')
    mdir = os.path.join(ASSETS, 'models', 'item')
    for d in (gdir, tdir, idir, mdir):
        os.makedirs(d, exist_ok=True)
    for name, (fn, items) in ARMORS.items():
        boxes = fn()
        tex = pack(boxes)
        img, glow = paint(boxes, tex, seed=len(name))
        img.save(os.path.join(tdir, name + '.png'))
        gm = os.path.join(tdir, name + '_glowmask.png')
        if glow:
            glow.save(gm)
        elif os.path.exists(gm):
            os.remove(gm)
        with open(os.path.join(gdir, name + '.geo.json'), 'w') as f:
            json.dump(geo(name, boxes, tex), f, indent=1)
        for item, slot in items:
            icon(boxes, SLOT_BONES[slot], img).save(os.path.join(idir, item + '.png'))
            with open(os.path.join(mdir, item + '.json'), 'w') as f:
                json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + item}}, f, indent=1)
    print(len(ARMORS), 'armaduras v2 ->', gdir)

if __name__ == '__main__':
    main()

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
    'robe_grey': dict(base=(150, 150, 157), dark=(96, 96, 104), light=(196, 196, 204)),
    'robe_dark': dict(base=(92, 94, 104), dark=(56, 58, 66), light=(128, 130, 140)),
    'shirt':   dict(base=(234, 230, 220), dark=(176, 170, 156), light=(250, 248, 242)),
    'brown':   dict(base=(110, 72, 40), dark=(64, 40, 20), light=(150, 104, 64)),
    'brown_pants': dict(base=(120, 86, 58), dark=(74, 50, 32), light=(156, 118, 84)),
    'boot_brown': dict(base=(84, 56, 36), dark=(46, 30, 18), light=(120, 84, 56)),
    'cloak_brown': dict(base=(128, 90, 62), dark=(76, 50, 32), light=(166, 124, 88)),
    'hat_brown': dict(base=(86, 58, 42), dark=(48, 30, 20), light=(122, 88, 64)),
    'hat_band': dict(base=(150, 42, 40), dark=(96, 22, 22), light=(196, 80, 70)),
    'robe_roxy': dict(base=(92, 62, 46), dark=(54, 34, 24), light=(130, 94, 70)),
    'stocking': dict(base=(52, 54, 72), dark=(30, 30, 44), light=(80, 84, 104)),
    'black_leather': dict(base=(42, 38, 38), dark=(20, 18, 18), light=(78, 70, 68)),
    'dark_metal': dict(base=(84, 88, 96), dark=(46, 48, 54), light=(136, 140, 150)),
    'leather_brown': dict(base=(124, 82, 46), dark=(72, 46, 24), light=(170, 120, 74)),
    'tunic':   dict(base=(58, 96, 92), dark=(32, 58, 56), light=(92, 136, 130)),
    'pants_grey': dict(base=(98, 94, 88), dark=(58, 56, 52), light=(132, 128, 120)),
    'ribbon_blue': dict(base=(70, 110, 200), dark=(40, 64, 130), light=(130, 170, 240)),
    'ribbon_brown': dict(base=(96, 54, 30), dark=(56, 30, 16), light=(136, 84, 52)),
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
B, H, RA, LA, RL, RF = 'armorBody', 'armorHead', 'armorRightArm', 'armorLeftArm', 'armorRightLeg', 'armorRightBoot'

def mk2():
    """Armadura Magica Mk-II v2 con la ropa de Rudeus (temporada 2): tunica gris con capucha caida, camisa blanca,
    cinturon con bolsa, faldones largos, pantalon marron y botas. El nucleo de mana queda como broche azul."""
    return sym([
        box(B, [-4.6, 12, -2.6], [9.2, 12.3, 5.2], 'robe_grey', 'robe'),
        box(B, [-1.6, 19.5, -2.75], [3.2, 4.6, 0.3], 'shirt'),
        box(B, [-4.4, 22.8, -2.9], [8.8, 1.6, 5.8], 'robe_dark'),
        box(B, [-3.6, 18.5, 2.5], [7.2, 5, 1.6], 'robe_dark', 'folds'),
        box(B, [-0.75, 21.4, -3.0], [1.5, 1.5, 0.4], 'crystal'),
        box(B, [-4.75, 12.6, -2.75], [9.5, 1.3, 5.5], 'brown'),
        box(B, [-0.8, 12.5, -2.95], [1.6, 1.5, 0.3], 'gold'),
        box(B, [2.2, 10.2, -3.0], [2.2, 2.6, 1.3], 'brown', 'stitch'),
        box(B, [-4.8, 3.5, -2.8], [9.6, 8.6, 5.6], 'robe_grey', 'tails'),
        box(RA, [-8.5, 12.5, -2.5], [4.6, 11.7, 5], 'robe_grey', 'folds'),
        box(RA, [-8.7, 11.8, -2.7], [5, 1.6, 5.4], 'robe_dark'),
        box(RL, [-4.15, 4, -2.25], [4.3, 8, 4.5], 'brown_pants'),
        box(RF, [-4.3, 0, -2.6], [4.5, 4.6, 5.2], 'boot_brown', 'boots'),
        box(RF, [-4.3, 0, -3.1], [4.5, 1.6, 0.6], 'boot_brown'),
    ])

def flame_cloak():
    """Capa Atrapallamas v2: capa marron con capucha que cubre todo el cuerpo y tapa la cara (como su icono).
    Es del torso, pero se ve de la cabeza a los pies; el dobladillo guarda brasas que brillan."""
    return sym([
        box(H, [-4.8, 23.4, -4.8], [9.6, 9.6, 9.6], 'cloak_brown', 'hood'),
        box(H, [-1.5, 29.5, 4.6], [3, 3, 1.6], 'cloak_brown'),
        box(B, [-5, 12, -3], [10, 12.6, 6], 'cloak_brown', 'folds'),
        box(B, [-5.3, 0.5, -3.3], [10.6, 11.6, 6.6], 'cloak_brown', 'hem'),
        box(B, [-1, 22, -3.4], [2, 2, 0.5], 'ember'),
        box(RA, [-9, 10.5, -3], [5.2, 13.6, 6], 'cloak_brown', 'folds'),
    ])

def roxy():
    """Roxy: sombrero de bruja con banda, manto corto sobre los hombros, blusa blanca con lazo azul, tunica,
    medias oscuras y botas."""
    return sym([
        box(H, [-7, 31.2, -7], [14, 0.8, 14], 'hat_brown'),
        box(H, [-4.4, 32, -4.4], [8.8, 1, 8.8], 'hat_band'),
        box(H, [-4.2, 33, -4.2], [8.4, 2.6, 8.4], 'hat_brown'),
        box(H, [-3, 35.6, -2.6], [6, 2.6, 6], 'hat_brown'),
        box(H, [-1.9, 38.2, -1.2], [3.8, 2.2, 3.8], 'hat_brown'),
        box(H, [-1, 40.4, 0.4], [2, 1.8, 2], 'hat_brown'),
        box(B, [-4.5, 12, -2.5], [9, 12.2, 5], 'robe_roxy', 'robe'),
        box(B, [-2, 17, -2.6], [4, 7, 0.3], 'shirt'),
        box(B, [-0.8, 21, -2.8], [1.6, 1.4, 0.3], 'ribbon_blue'),
        box(B, [-5.2, 19.5, -3], [10.4, 4.6, 6], 'robe_roxy', 'trim_white'),
        box(B, [-4.8, 6, -2.8], [9.6, 6.2, 5.6], 'robe_roxy', 'tails'),
        box(RA, [-8.4, 12.5, -2.4], [4.4, 11.5, 4.8], 'robe_roxy', 'folds'),
        box(RA, [-8.5, 12, -2.5], [4.6, 1.2, 5], 'shirt'),
        box(RL, [-4.1, 4, -2.1], [4.1, 8, 4.2], 'stocking'),
        box(RF, [-4.2, 0, -2.5], [4.3, 4, 5], 'boot_brown', 'boots'),
    ])

def ghislaine():
    """Ghislaine: parche en el ojo izquierdo, top y taparrabos de cuero negro, cinturon, hombrera de metal,
    brazaletes y botas altas."""
    return sym([
        box(H, [-4.3, 27.3, -4.3], [8.6, 0.6, 8.6], 'black_leather'),
        box(H, [0.8, 26.6, -4.45], [2.4, 2.2, 0.4], 'black_leather'),
        box(B, [-4.3, 18.5, -2.3], [8.6, 3.4, 4.6], 'black_leather', 'stitch'),
        box(B, [-3.5, 21.9, -2.3], [1.2, 2.2, 4.6], 'black_leather'),
        box(B, [2.3, 21.9, -2.3], [1.2, 2.2, 4.6], 'black_leather'),
        box(B, [-4.5, 11.8, -2.5], [9, 1.5, 5], 'brown'),
        box(B, [-0.8, 11.7, -2.7], [1.6, 1.7, 0.3], 'dark_metal'),
        box(B, [-2.5, 6.5, -2.7], [5, 5.3, 0.5], 'black_leather', 'folds'),
        box(B, [-2.5, 6.5, 2.2], [5, 5.3, 0.5], 'black_leather', 'folds'),
        box(RA, [-8.8, 20.5, -2.8], [5, 3.5, 5.6], 'dark_metal', 'rivets'),
        box(RA, [-8.3, 12, -2.3], [4.6, 4.5, 4.6], 'black_leather', 'stitch'),
        box(RL, [-4.1, 9, -2.2], [4.2, 1, 4.4], 'black_leather'),
        box(RF, [-4.2, 0, -2.4], [4.4, 7.5, 4.8], 'black_leather', 'straps'),
    ])

def paul():
    """Paul: tunica verdosa, peto de cuero con remaches, hombrera izquierda, cinturon de espadachin, brazaletes,
    pantalon gris y botas."""
    return sym([
        box(B, [-4.3, 12, -2.3], [8.6, 12.1, 4.6], 'tunic'),
        box(B, [-4.6, 15.5, -2.6], [9.2, 7.5, 5.2], 'leather_brown', 'rivets'),
        box(B, [-4.6, 12, -2.6], [9.2, 1.4, 5.2], 'brown'),
        box(B, [-4.9, 10, -1.5], [1, 3, 3], 'brown'),
        box(B, [-4.3, 7, -2.3], [8.6, 5, 4.6], 'tunic', 'tails'),
        box(LA, [3.8, 20.5, -2.8], [5, 3.5, 5.6], 'leather_brown', 'rivets'),
        box(RA, [-8.2, 13, -2.2], [4.4, 11, 4.4], 'tunic'),
        box(RA, [-8.4, 12, -2.4], [4.8, 4, 4.8], 'leather_brown', 'stitch'),
        box(RL, [-4.1, 3.5, -2.1], [4.2, 8.5, 4.2], 'pants_grey'),
        box(RF, [-4.2, 0, -2.5], [4.4, 5, 5], 'boot_brown', 'boots'),
    ])

def eris():
    """Eris (aventurera): diadema, blusa blanca con lazo marron, peto de cuero, cinturon, brazaletes, shorts marrones
    y botas."""
    return sym([
        box(H, [-4.3, 30, -4.3], [8.6, 0.8, 8.6], 'black_leather'),
        box(B, [-4.3, 12, -2.3], [8.6, 12.1, 4.6], 'shirt', 'folds'),
        box(B, [-1, 21.5, -2.6], [2, 1.6, 0.4], 'ribbon_brown'),
        box(B, [-4.5, 16, -2.6], [9, 6, 5.2], 'leather_brown', 'stitch'),
        box(B, [-4.6, 12, -2.6], [9.2, 1.3, 5.2], 'brown'),
        box(RA, [-8.2, 12.5, -2.2], [4.4, 11.5, 4.4], 'shirt', 'folds'),
        box(RA, [-8.4, 12, -2.4], [4.8, 4.5, 4.8], 'leather_brown', 'stitch'),
        box(RL, [-4.15, 8, -2.2], [4.3, 4.2, 4.4], 'brown_pants'),
        box(RF, [-4.2, 0, -2.5], [4.4, 6, 5], 'boot_brown', 'boots'),
    ])

def curse_helmet():
    return [
        box(H, [-4.6, 23.6, -4.6], [9.2, 9, 9.2], 'bronze', 'visor'),
        box(H, [-4.7, 27.5, -4.7], [9.4, 1, 9.4], 'rune'),
        box(H, [-0.6, 32.4, -4.2], [1.2, 1.8, 8.4], 'gold'),
        box(H, [-4.9, 24, -3.5], [0.5, 4, 3], 'bronze'),
        box(H, [4.4, 24, -3.5], [0.5, 4, 3], 'bronze'),
        box(H, [-1.25, 31.0, -4.9], [2.5, 1.5, 0.5], 'rune'),
    ]

def sunglasses():
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
    return sym([
        box(RF, [-4.3, 0, -3.2], [4.6, 3.5, 5.8], 'leather', 'stitch'),
        box(RF, [-4.4, 3.5, -2.6], [4.8, 1.5, 5.2], 'cloth'),
        box(RF, [-4.45, 3.4, -2.65], [4.9, 0.4, 5.3], 'gold'),
        box(RF, [-5.0, 1.8, 0.5], [0.6, 3.2, 3.2], 'feather', 'feather'),
        box(RF, [-4.35, 0.8, -3.3], [4.7, 0.6, 0.2], 'feather'),
    ])

# nombre: (diseno, [(item, ranura)], cubre todo el cuerpo desde el torso)
ARMORS = {
    'mk2_v2': (mk2, [('magic_armor_mk_2_v2_chestplate', 'chest'), ('magic_armor_mk_2_v2_leggings', 'legs'), ('magic_armor_mk_2_v2_boots', 'feet')], False),
    'curse_helmet_v2': (curse_helmet, [('curse_suppressing_helmet_v2', 'head')], False),
    'sunglasses_v2': (sunglasses, [('magical_sunglasses_v2', 'head')], False),
    'enchanted_shoes_v2': (enchanted_shoes, [('enchanted_shoes_v2', 'feet')], False),
    'flame_cloak_v2': (flame_cloak, [('flame_trapping_cloak_v2', 'chest')], True),
    'outfit_roxy': (roxy, [('roxy_hat', 'head'), ('roxy_robe', 'chest'), ('roxy_stockings', 'legs'), ('roxy_boots', 'feet')], False),
    'outfit_ghislaine': (ghislaine, [('ghislaine_eyepatch', 'head'), ('ghislaine_top', 'chest'), ('ghislaine_straps', 'legs'), ('ghislaine_boots', 'feet')], False),
    'outfit_paul': (paul, [('paul_armor', 'chest'), ('paul_pants', 'legs'), ('paul_boots', 'feet')], False),
    'outfit_eris': (eris, [('eris_hairband', 'head'), ('eris_blouse', 'chest'), ('eris_shorts', 'legs'), ('eris_boots', 'feet')], False),
}
SLOT_BONES = {'head': ['armorHead'], 'chest': ['armorBody', 'armorRightArm', 'armorLeftArm'],
              'legs': ['armorRightLeg', 'armorLeftLeg'], 'feet': ['armorRightBoot', 'armorLeftBoot']}
# Iconos hechos a mano (en vez del generado de frente)
ICON_FROM = {'flame_trapping_cloak_v2': 'addon_flametrappingcloak'}

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
                        c = m['light']
                    if det in ('folds', 'hem') and name in ('north', 'south', 'east', 'west') and i % 3 == 1:
                        c = [x * 0.82 for x in c]
                    if det == 'hem' and j >= fh - 2 and name not in ('up', 'down'):
                        c, g = MATS['ember']['base'], j == fh - 1
                    if det == 'hood' and name == 'north' and 1 <= i < fw - 1 and 1 <= j < fh - 1:
                        c = (16, 11, 8)  # la cara queda en sombra
                    if det == 'robe' and name == 'north' and i == fw // 2:
                        c = m['dark']
                    if det == 'tails' and name == 'north' and fw // 2 - 1 <= i <= fw // 2:
                        c = (30, 30, 34)
                    if det == 'trim_white' and j == fh - 1:
                        c = MATS['shirt']['base']
                    if det == 'boots' and j <= 1:
                        c = m['light']
                    if det == 'straps' and j % 3 == 0:
                        c = MATS['dark_metal']['light']
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
    import shutil
    for name, (fn, items, full) in ARMORS.items():
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
            if item in ICON_FROM:
                shutil.copy(os.path.join(idir, ICON_FROM[item] + '.png'), os.path.join(idir, item + '.png'))
            else:
                icon(boxes, SLOT_BONES[slot], img).save(os.path.join(idir, item + '.png'))
            with open(os.path.join(mdir, item + '.json'), 'w') as f:
                json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + item}}, f, indent=1)
    print(len(ARMORS), 'armaduras v2 ->', gdir)

if __name__ == '__main__':
    main()

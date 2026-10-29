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
MIRROR = {'armorRightArm': 'armorLeftArm', 'armorRightLeg': 'armorLeftLeg', 'armorRightBoot': 'armorLeftBoot',
          'clothFR': 'clothFL', 'clothR': 'clothL'}
# Huesos de tela (hijos del torso) que el renderer mueve con las piernas: F adelante, B atras, R/L costados (R = derecha
# del jugador = -X), FR/FL las dos hojas de adelante de un abrigo abierto.
CLOTH = ('clothF', 'clothB', 'clothR', 'clothL', 'clothFR', 'clothFL')

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

MATS.update({
    # Roxy (de la skin del NPC): sombrero negro azulado, banda marron, blusa blanca lila, corse gris, falda negra
    'hat_black':  dict(base=(44, 38, 58), dark=(20, 16, 28), light=(84, 76, 108)),
    'roxy_band':  dict(base=(150, 98, 66), dark=(92, 56, 34), light=(196, 140, 98)),
    'blouse':     dict(base=(236, 232, 244), dark=(176, 168, 196), light=(252, 251, 255)),
    'frill_black': dict(base=(36, 30, 44), dark=(16, 14, 20), light=(70, 62, 84)),
    'corset':     dict(base=(72, 72, 82), dark=(38, 38, 46), light=(110, 110, 124)),
    'sole':       dict(base=(122, 78, 48), dark=(70, 42, 24), light=(160, 110, 72)),
    # Rudeus: gris de la tunica, gris oscuro del forro, cuero de los arneses
    'coat_grey':  dict(base=(140, 140, 148), dark=(88, 88, 96), light=(188, 188, 196)),
    'coat_in':    dict(base=(70, 72, 80), dark=(40, 42, 48), light=(104, 106, 116)),
    'harness':    dict(base=(118, 80, 48), dark=(66, 42, 22), light=(164, 120, 78)),
    'pants_rud':  dict(base=(112, 112, 120), dark=(70, 70, 78), light=(168, 168, 176)),
    # Capa atrapallamas
    'cloak':      dict(base=(124, 84, 56), dark=(72, 46, 28), light=(166, 120, 84)),
    'cloak_in':   dict(base=(62, 38, 26), dark=(34, 20, 12), light=(92, 60, 40)),
    'shadow':     dict(base=(14, 10, 8), dark=(6, 4, 4), light=(24, 18, 14)),
    # Ghislaine: piel clara de la estola, cuero oscuro, verde caqui del taparrabos
    'fur_tan':    dict(base=(214, 190, 150), dark=(150, 120, 84), light=(244, 230, 200)),
    'khaki':      dict(base=(120, 112, 70), dark=(72, 66, 38), light=(160, 152, 100)),
    'wrap':       dict(base=(78, 52, 40), dark=(42, 26, 18), light=(114, 82, 62)),
    # Eris: granate oscuro, piel crema
    'maroon':     dict(base=(98, 52, 50), dark=(56, 26, 26), light=(140, 84, 78)),
    'fur_cream':  dict(base=(232, 220, 196), dark=(170, 152, 122), light=(250, 244, 230)),
    # Paul
    'gambeson':   dict(base=(66, 96, 112), dark=(36, 56, 68), light=(104, 138, 154)),
    'steel_d':    dict(base=(110, 116, 128), dark=(58, 62, 72), light=(176, 182, 196)),
})


def box(bone, origin, size, mat, detail=None, rot=None, pivot=None):
    return dict(bone=bone, origin=list(origin), size=list(size), mat=mat, detail=detail, rot=rot, pivot=pivot)


def rbox(bone, origin, size, mat, detail=None, pivot=(0, 0, 0), e=(0, 0, 0)):
    """Caja girada. e = giro que se VE (grados, en el marco Bedrock: X levanta el frente hacia arriba/atras,
    Y gira a la izquierda del jugador, Z inclina). GeckoLib espeja X al cargar y aplica (-x, -y, z): pasando eso
    al marco Bedrock queda (-x, y, -z), asi que en el json va (-ex, ey, -ez)."""
    ex, ey, ez = e
    if not (ex or ey or ez):
        return box(bone, origin, size, mat, detail)
    return box(bone, origin, size, mat, detail, rot=[round(-ex, 2), round(ey, 2), round(-ez, 2)], pivot=list(pivot))


def mirrored(b):
    """Copia espejada en X. Para brazos/piernas/botas cambia al hueso del otro lado."""
    o, s = b['origin'], b['size']
    bone = MIRROR.get(b['bone'], b['bone'])
    if bone.startswith('ring_'):
        head, ang = bone.rsplit('_', 1)
        bone = '%sm_%03d' % (head, (360 - int(ang)) % 360)
    m = dict(b, bone=bone, origin=[-(o[0] + s[0]), o[1], o[2]])
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


def xm(*boxes):
    """Una caja (de cabeza o torso) y su espejo."""
    out = []
    for b in boxes:
        out += [b, mirrored(b)]
    return out


def panel(bone, piv, w, length, mat, det=None, yaw=0, flare=0, thick=0.5, sway=0):
    """Pano que cuelga desde piv (arriba, cara interna) mirando hacia afuera. yaw: 0 adelante, 90 a la derecha del
    jugador (-X), 180 atras. flare: cuanto se abre hacia afuera al bajar. sway: inclinacion de costado."""
    px, py, pz = piv
    return rbox(bone, [px - w / 2, py - length, pz - thick], [w, length, thick], mat, det, pivot=piv, e=(flare, yaw, sway))


def skirt(bone, y, length, hw, hd, flare, mat, det=None, cz=0.0, corner=None, thick=0.5, front=True, split=0.0, cflare=None,
          cloth=False):
    """Falda/faldon de 8 panos alrededor de un rectangulo (medio ancho hw, media profundidad hd).
    split > 0 abre el frente en dos (como un abrigo). cloth=True: cada pano va en su hueso de tela, que se mueve con
    las piernas (adelante, atras, costados)."""
    def b(which):
        return which if cloth else bone
    out = []
    corner = corner if corner is not None else min(hw, hd) * 0.9
    cf = cflare if cflare is not None else flare
    if front:
        if split:
            w = hw - split / 2
            out += [panel(b('clothFR'), (-(split / 2 + w / 2), y, cz - hd), w, length, mat, det, 0, flare, thick),
                    panel(b('clothFL'), ((split / 2 + w / 2), y, cz - hd), w, length, mat, det, 0, flare, thick)]
        else:
            out.append(panel(b('clothF'), (0, y, cz - hd), 2 * hw, length, mat, det, 0, flare, thick))
    out.append(panel(b('clothB'), (0, y, cz + hd), 2 * hw, length, mat, det, 180, flare, thick))
    out.append(panel(b('clothR'), (-hw, y, cz), 2 * hd, length, mat, det, 90, flare, thick))
    out.append(panel(b('clothL'), (hw, y, cz), 2 * hd, length, mat, det, -90, flare, thick))
    k = 0.72  # las esquinas van un poco hacia adentro para no asomar arriba
    for sx, sz, yw in ((-1, -1, 45), (1, -1, -45), (-1, 1, 135), (1, 1, -135)):
        if sz < 0 and not front:
            continue
        cb = 'clothB' if sz > 0 else ('clothF' if not split else ('clothFR' if sx < 0 else 'clothFL'))
        out.append(panel(b(cb), (sx * hw * k + sx * 0.6, y, cz + sz * (hd * k + 0.6)), corner, length, mat, det, yw, cf, thick))
    return out


def chain(bone, base, segs, mat, det=None, side=0.0):
    """Punta curva: segmentos (ancho, largo, angulo hacia atras) apilados, cada uno girado sobre su base."""
    out = []
    bx, by, bz = base
    for w, l, a in segs:
        out.append(rbox(bone, [bx + side - w / 2, by, bz - w / 2], [w, l, w], mat, det, pivot=(bx + side, by, bz), e=(a, 0, 0)))
        r = math.radians(a)
        by, bz = by + l * math.cos(r), bz + l * math.sin(r)
    return out


_RING = [0]


def seg_panel(bone, piv, w, length, mat, det=None, yaw=0, flare=0, thick=0.5, n=3):
    """Pano partido en n tramos de arriba a abajo (mismo pivote y giro): la tela se curva al deformarse con las
    piernas. Solo el tramo de abajo lleva el detalle del borde."""
    px, py, pz = piv
    out = []
    for k in range(n):
        top = py - length * k / n
        h = length / n + (0.15 if k < n - 1 else 0)
        d = det if k == n - 1 else ('folds' if det else None)
        out.append(rbox(bone, [px - w / 2, top - h, pz - thick], [w, h, thick], mat, d, pivot=piv, e=(flare, yaw, 0)))
    return out


def ring_bone(tag, x, z):
    """Hueso de tela segun donde esta alrededor de la cintura (0 adelante, 90 derecha = -X, 180 atras): el renderer lo
    mueve con la pierna de ese lado (adelante empuja la pierna que va adelante, atras la que va atras)."""
    ang = int(round(math.degrees(math.atan2(-x, -z)))) % 360
    _RING[0] += 1
    return 'ring_%s%02d_%03d' % (tag, _RING[0], ang)


def ring_panel(tag, piv, w, length, mat, det=None, yaw=None, flare=0, thick=0.5):
    """Pano en su propio hueso de tela (con pivote en su borde de arriba)."""
    x, y, z = piv
    yw = yaw if yaw is not None else math.degrees(math.atan2(-x, -z))
    return seg_panel(ring_bone(tag, x, z), piv, w, length, mat, det, yw, flare, thick, n=4 if length > 12 else 3)


def ring_skirt(tag, y, length, hw, hd, flare, mat, det=None, n=16, thick=0.5, gap=0.0, cz=0.0):
    """Falda de n panos alrededor de un ovalo (medio ancho hw, media profundidad hd), cada uno en su hueso: se mueve
    por partes con cada pierna y, como los vecinos se mueven parecido, los panos se estiran y se unen (como codos).
    gap: abertura adelante en grados (abrigo abierto)."""
    out = []
    per = 2 * math.pi * math.sqrt((hw * hw + hd * hd) / 2)
    bottom = per + 2 * math.pi * length * math.sin(math.radians(flare)) * 0.8
    w = bottom / n * 1.12
    for i in range(n):
        a = 360.0 * (i + 0.5) / n
        d = min(a, 360 - a)
        if gap and d < gap / 2:
            continue
        r = math.radians(a)
        x, z = -math.sin(r) * hw, cz - math.cos(r) * hd
        out += seg_panel(ring_bone(tag, x, z - cz), (x, y, z), w, length, mat, det, a, flare, thick, n=3 if length > 4 else 2)
    return out


# ================================================================ Disenos
B, H, RA, LA, RL, RF = 'armorBody', 'armorHead', 'armorRightArm', 'armorLeftArm', 'armorRightLeg', 'armorRightBoot'
LL, LF = 'armorLeftLeg', 'armorLeftBoot'


def mk2():
    """Mk-II v2 = ropa de Rudeus (temporada 2): abrigo gris largo abierto adelante con forro oscuro, cuello alto,
    capucha caida en la espalda, arnes de cuero cruzado, cinturon con bolsas, faldones con abertura, mangas
    anchas con punos vueltos, pantalon gris y botas con cana doblada. El nucleo de mana es el broche del cuello."""
    out = []
    # torso: camisa/forro oscuro y abrigo encima en dos mitades (se ve el forro en el medio)
    out.append(box(B, [-4.3, 12, -2.3], [8.6, 12.1, 4.6], 'coat_in', 'seam'))
    out.append(box(B, [-4.7, 12, -2.2], [9.4, 12.4, 4.9], 'coat_grey', 'folds'))
    out += xm(box(B, [-4.75, 12, -2.85], [3.6, 12.4, 0.7], 'coat_grey', 'lapel'))
    # cuello alto abierto (solapas que se abren un poco hacia afuera)
    out += xm(rbox(B, [-4.4, 23.7, -2.95], [3.2, 2.2, 0.6], 'coat_in', 'trim_light', pivot=(-2.8, 23.7, -2.65), e=(-14, 0, 8)))
    out += xm(box(B, [-4.95, 23.6, -2.4], [0.7, 2.1, 4.8], 'coat_grey'))
    out.append(box(B, [-4.5, 23.6, 2.2], [9, 2.4, 0.8], 'coat_grey', 'trim_light'))
    # capucha caida: dos bolsas de tela apiladas en la espalda
    out.append(box(B, [-3.9, 18.8, 2.65], [7.8, 4.9, 1.6], 'coat_grey', 'folds'))
    out.append(box(B, [-3.0, 17.4, 2.75], [6, 1.6, 1.3], 'coat_grey'))
    out.append(box(B, [-3.2, 19.6, 4.2], [6.4, 3.8, 0.3], 'coat_in'))
    # arnes: dos correas cruzadas adelante y atras + hebilla central
    for z, t in ((-3.2, 0.35), (2.95, 0.35)):
        out.append(rbox(B, [-0.55, 12.6, z], [1.1, 11, t], 'harness', 'stitch', pivot=(0, 18.1, z), e=(0, 0, 30)))
        out.append(rbox(B, [-0.55, 12.6, z], [1.1, 11, t], 'harness', 'stitch', pivot=(0, 18.1, z), e=(0, 0, -30)))
    out.append(box(B, [-0.9, 17.5, -3.5], [1.8, 1.4, 0.4], 'gold', 'rivets'))
    # broche con el nucleo de mana
    out.append(box(B, [-1.15, 21.6, -3.35], [2.3, 2.2, 0.3], 'gold'))
    out.append(box(B, [-0.75, 22.0, -3.6], [1.5, 1.5, 0.4], 'crystal'))
    # cinturon, hebilla y bolsas
    out.append(box(B, [-5.0, 12.2, -3.0], [10, 1.4, 6.0], 'harness', 'stitch'))
    out.append(box(B, [-0.9, 12.05, -3.25], [1.8, 1.7, 0.3], 'gold'))
    # bolsas del cinturon: cada una cuelga de su zona y se mueve con la pierna de ese lado
    for o, sz in (([-5.6, 10.2, -1.6], [1.4, 2.8, 2.8]), ([2.0, 10.4, -3.6], [2.2, 2.4, 1.2]), ([-3.6, 10.8, 3.1], [2.4, 2.0, 1.0])):
        cx, cz = o[0] + sz[0] / 2, o[2] + sz[2] / 2
        bb = box(ring_bone('p', cx, cz), o, sz, 'harness', 'flap')
        bb['rot'], bb['pivot'] = [0, 0, 0], [cx, 12.6, cz]
        out.append(bb)
    # faldones hasta la rodilla en 18 panos (abiertos adelante): cada parte sigue a su pierna
    out += ring_skirt('k', 12.4, 8.6, 4.75, 2.85, 9, 'coat_grey', 'coat_edge', n=18, gap=24)
    # mangas: hombro, manga ancha con pliegues, correa en el codo y puno vuelto
    out += sym([
        box(RA, [-8.75, 21.7, -2.75], [5.2, 2.6, 5.5], 'coat_grey', 'trim_light'),
        box(RA, [-8.55, 14.6, -2.55], [4.8, 7.4, 5.1], 'coat_grey', 'folds'),
        box(RA, [-8.7, 17.6, -2.7], [5.1, 0.7, 5.4], 'harness'),
        box(RA, [-8.95, 12.6, -2.95], [5.6, 2.4, 5.9], 'coat_in', 'trim_light'),
        box(RA, [-8.4, 12.0, -2.4], [4.5, 0.8, 4.8], 'coat_in'),
        # piernas: pantalon con rodillera de tela; botas con cana doblada, correa y suela
        box(RL, [-4.2, 4.0, -2.2], [4.3, 8.1, 4.4], 'pants_rud', 'stripe'),
        box(RL, [-4.3, 6.2, -2.5], [4.5, 2.0, 0.6], 'coat_in', 'stitch'),
        box(RF, [-4.3, 0.5, -2.55], [4.5, 4.5, 5.0], 'boot_brown', 'folds'),
        box(RF, [-4.5, 4.4, -2.75], [4.9, 1.6, 5.4], 'harness', 'trim_light'),
        box(RF, [-4.3, 0.5, -3.3], [4.5, 1.8, 0.8], 'boot_brown'),
        box(RF, [-4.4, 2.3, -2.7], [4.7, 0.6, 5.3], 'harness'),
        box(RF, [-4.35, 0, -3.45], [4.6, 0.5, 6.0], 'sole'),
    ])
    return out


def flame_cloak():
    """Capa Atrapallamas v2: capa larga de la cabeza a los tobillos. Capucha con visera, punta caida atras y la cara
    en sombra (solo se ven dos brasas); esclavina sobre los hombros; cuerpo de la capa en panos con pliegues que
    se abren al bajar; broche de brasa; el dobladillo guarda llamas que brillan."""
    out = []
    # capucha
    out.append(box(H, [-4.9, 31.7, -4.5], [9.8, 1.3, 9.6], 'cloak', 'folds'))
    out += xm(box(H, [-5.1, 23.4, -4.5], [1.2, 8.5, 9.6], 'cloak', 'folds'))
    out.append(box(H, [-4.9, 23.4, 4.2], [9.8, 8.4, 1.2], 'cloak', 'folds'))
    out.append(rbox(H, [-5.0, 30.6, -6.0], [10.0, 1.4, 1.6], 'cloak', 'trim_light', pivot=(0, 31.3, -4.5), e=(-10, 0, 0)))
    out += xm(rbox(H, [-5.2, 23.2, -5.3], [1.4, 7.6, 1.2], 'cloak', 'folds', pivot=(-4.5, 31, -4.5), e=(0, 0, -4)))
    out.append(box(H, [-4.0, 23.3, -4.35], [8, 7.6, 0.3], 'shadow', 'eyes'))
    out += chain(H, (0, 30.6, 4.2), [(5.0, 2.6, 105), (3.6, 2.4, 135), (2.4, 2.0, 160), (1.4, 1.6, 175)], 'cloak', 'folds')
    # esclavina (capa corta sobre los hombros) con borde dorado
    out.append(box(B, [-6.2, 23.6, -3.4], [12.4, 1.0, 6.8], 'cloak'))
    out += skirt(B, 24.3, 5.6, 5.6, 3.3, 42, 'cloak', 'mantle', corner=3.2, cflare=46, thick=0.6)
    # broche y cadenita
    out.append(box(B, [-1.2, 21.8, -4.3], [2.4, 2.4, 0.6], 'gold', 'rivets'))
    out.append(box(B, [-0.7, 22.3, -4.6], [1.4, 1.4, 0.4], 'ember'))
    # cuerpo de la capa: frente en dos hojas, costados, esquinas y espalda en cuatro pliegues
    L_ = 21.4
    top = 22.2
    out += xm(*ring_panel('c', (-4.6, top, -3.4), 7.4, L_, 'cloak', 'cloakflame', yaw=-6, flare=6))
    out += xm(*ring_panel('c', (-8.6, top, 0), 6.6, L_, 'cloak', 'cloakflame', yaw=90, flare=7))
    out += xm(*ring_panel('c', (-7.6, top, -2.9), 2.8, L_, 'cloak', 'cloakflame', yaw=45, flare=8))
    out += xm(*ring_panel('c', (-7.6, top, 2.9), 2.8, L_, 'cloak', 'cloakflame', yaw=135, flare=8))
    for x, dz, yw in ((-6.0, 0, 192), (-2.1, 0.8, 168), (2.1, 0.8, 192), (6.0, 0, 168)):
        out += (ring_panel('c', (x, top, 3.4 + dz), 4.6, L_ - dz * 0.5, 'cloak', 'cloakflame', yaw=yw, flare=7))
    # forro oscuro que se ve por la abertura del frente: dos mitades, cada una con su pierna
    for x0 in (-3.2, 0.0):
        out += seg_panel(ring_bone('i', x0 + 1.6, -2.6), (x0 + 1.6, 22.2, -2.3), 3.2, 20.8, 'cloak_in', 'folds', 0, 0, 0.3, n=5)
    # mangas (los brazos salen de la capa con la misma tela) y punos con brasas
    out += sym([
        box(RA, [-8.7, 13.2, -2.7], [5.0, 10.9, 5.4], 'cloak', 'folds'),
        box(RA, [-9.0, 12.0, -3.0], [5.6, 1.8, 6.0], 'cloak_in', 'hem'),
    ])
    return out


def roxy():
    """Roxy (sin la tunica de viaje, como la skin del NPC): sombrero de bruja negro con ala caida, banda marron y
    punta doblada hacia atras; blusa blanca con volados y mangas abullonadas; corse gris con cordones; lazo;
    falda negra acampanada en dos pisos con volado blanco; moño atras; medias blancas con franjas y botas."""
    out = []
    # --- sombrero
    out.append(box(H, [-5.2, 31.0, -5.2], [10.4, 0.6, 10.4], 'hat_black'))
    out.append(rbox(H, [-8.6, 31.0, -8.6], [17.2, 0.6, 3.4], 'hat_black', 'brim', pivot=(0, 31.3, -5.2), e=(-12, 0, 0)))
    out.append(rbox(H, [-8.6, 31.0, 5.2], [17.2, 0.6, 3.4], 'hat_black', 'brim', pivot=(0, 31.3, 5.2), e=(10, 0, 0)))
    out += xm(rbox(H, [-8.6, 31.0, -5.2], [3.4, 0.6, 10.4], 'hat_black', 'brim', pivot=(-5.2, 31.3, 0), e=(0, 0, -11)))
    out.append(box(H, [-4.5, 31.6, -4.5], [9, 1.6, 9], 'roxy_band', 'stitch'))
    out.append(box(H, [-1.2, 31.7, -4.8], [2.4, 1.4, 0.4], 'gold', 'rivets'))
    out.append(box(H, [-4.1, 33.2, -4.1], [8.2, 2.4, 8.2], 'hat_black', 'folds'))
    out.append(box(H, [-3.3, 35.6, -3.2], [6.6, 2.2, 6.6], 'hat_black', 'folds'))
    out += chain(H, (0, 37.7, 0.2), [(5.2, 2.2, 8), (4.0, 2.2, 24), (3.0, 2.0, 46), (2.0, 1.8, 74), (1.2, 1.6, 104)], 'hat_black', 'folds')
    # --- blusa y corse
    out.append(box(B, [-4.4, 12, -2.4], [8.8, 12.2, 4.8], 'blouse', 'folds'))
    out.append(box(B, [-4.65, 12.6, -2.65], [9.3, 5.4, 5.3], 'corset', 'lace'))
    out.append(box(B, [-3.0, 17.9, -2.75], [6.0, 1.0, 0.35], 'corset'))
    out.append(box(B, [-4.8, 14.1, -2.8], [9.6, 0.9, 5.6], 'roxy_band', 'stitch'))
    out.append(box(B, [-0.8, 13.95, -3.0], [1.6, 1.2, 0.3], 'gold'))
    # pechera con volados (jabot) y cuello con volado y lazo negro
    for k in range(3):
        out.append(box(B, [-1.4 + k * 0.2, 21.4 - k * 1.3, -2.85 - k * 0.05], [2.8 - k * 0.4, 1.3, 0.45], 'blouse', 'frill'))
    out.append(box(B, [-3.4, 23.3, -2.9], [6.8, 1.2, 0.6], 'blouse', 'frill'))
    out += xm(box(B, [-4.6, 23.3, -2.6], [0.6, 1.2, 5.2], 'blouse', 'frill'))
    out.append(box(B, [-0.6, 22.3, -3.25], [1.2, 1.1, 0.5], 'frill_black'))
    out += xm(rbox(B, [-2.4, 22.2, -3.15], [1.8, 1.3, 0.4], 'frill_black', pivot=(-0.6, 22.8, -3.0), e=(0, 0, 12)))
    out += xm(rbox(B, [-0.9, 19.6, -3.1], [0.6, 2.7, 0.3], 'frill_black', pivot=(-0.6, 22.3, -3.0), e=(0, 0, -14)))
    # falda: piso negro con tablas y volado blanco abajo (2 px mas arriba que la cintura, para no chocar con las piernas)
    out += ring_skirt('s', 14.9, 5.4, 4.9, 2.95, 24, 'frill_black', 'pleats', n=16)
    out += ring_skirt('f', 10.2, 2.6, 6.9, 5.0, 34, 'blouse', 'frill', n=20)
    # moño grande atras
    out.append(box(B, [-0.9, 14.9, 2.7], [1.8, 1.6, 0.9], 'frill_black'))
    out += xm(rbox(B, [-3.9, 14.6, 2.8], [3.0, 2.4, 0.6], 'frill_black', 'folds', pivot=(-0.9, 15.7, 3.1), e=(0, 0, -10)))
    out += xm(rbox(B, [-1.6, 10.6, 2.9], [0.9, 4.2, 0.3], 'frill_black', pivot=(-0.6, 14.9, 3.1), e=(-18, 0, -14)))
    # mangas abullonadas: hombro con ribete negro, globo, franja, manga y puno con volado
    out += sym([
        box(RA, [-8.8, 23.5, -2.8], [5.2, 0.8, 5.6], 'frill_black'),
        box(RA, [-8.8, 18.7, -2.8], [5.2, 4.8, 5.6], 'blouse', 'puff'),
        box(RA, [-8.6, 17.8, -2.6], [4.8, 0.9, 5.2], 'frill_black'),
        box(RA, [-8.4, 15.0, -2.4], [4.4, 2.9, 4.8], 'blouse', 'folds'),
        box(RA, [-8.75, 13.9, -2.75], [5.1, 1.3, 5.5], 'blouse', 'frill'),
        box(RA, [-8.55, 15.0, -2.55], [4.7, 0.5, 5.1], 'frill_black'),
        # medias blancas hasta la rodilla con franjas negras y volado; botas negras con puno blanco y suela marron
        box(RL, [-4.15, 2.6, -2.15], [4.2, 5.2, 4.3], 'blouse', 'sock'),
        box(RL, [-4.35, 7.6, -2.35], [4.6, 1.0, 4.7], 'blouse', 'frill'),
        box(RL, [-4.3, 7.3, -2.3], [4.5, 0.4, 4.6], 'frill_black'),
        box(RF, [-4.3, 0.5, -2.5], [4.5, 3.0, 5.0], 'frill_black', 'folds'),
        box(RF, [-4.3, 0.5, -3.2], [4.5, 1.7, 0.7], 'frill_black'),
        box(RF, [-4.5, 3.2, -2.7], [4.9, 0.9, 5.4], 'blouse', 'frill'),
        box(RF, [-4.35, 0, -3.35], [4.6, 0.5, 5.9], 'sole'),
        box(RF, [-3.6, 0, 2.2], [3.0, 1.2, 0.8], 'sole'),
        box(RF, [-4.45, 1.6, -1.2], [0.4, 1.0, 2.4], 'gold'),
    ])
    return out


def ghislaine():
    """Ghislaine (de la skin del NPC): estola de piel clara sobre los hombros con mechones, vendas de cuero oscuro
    en el pecho con tirantes y tahali cruzado, dos cinturones, taparrabos verde caqui en panos deshilachados,
    hombreras de piel, brazaletes con aros de oro, rodilleras y botas altas con borde de piel. Parche en el ojo."""
    out = []
    out.append(box(H, [-4.35, 27.4, -4.35], [8.7, 0.6, 8.7], 'black_leather'))
    out.append(box(H, [0.6, 26.2, -4.6], [2.9, 2.8, 0.5], 'black_leather', 'stitch'))
    out.append(rbox(H, [-4.4, 27.9, -4.45], [4.6, 0.5, 0.4], 'black_leather', pivot=(0.6, 28.1, -4.4), e=(0, 0, 0)))
    # pecho
    out.append(box(B, [-4.35, 18.3, -2.35], [8.7, 4.0, 4.7], 'wrap', 'bands'))
    out += xm(box(B, [-3.6, 22.3, -2.35], [1.3, 1.9, 4.7], 'wrap'))
    out.append(rbox(B, [-0.6, 11.5, -2.85], [1.2, 13.4, 0.4], 'black_leather', 'rivets', pivot=(0, 18.2, -2.7), e=(0, 0, -32)))
    out.append(rbox(B, [-0.6, 11.5, 2.45], [1.2, 13.4, 0.4], 'black_leather', 'rivets', pivot=(0, 18.2, 2.6), e=(0, 0, 32)))
    # estola de piel: anillo abierto sobre los hombros + mechones
    out += skirt(B, 24.4, 3.0, 4.8, 2.8, 58, 'fur_tan', 'fur', corner=3.2, cflare=60, thick=1.0)
    for x, z, w, h in ((-3.5, -3.6, 2.2, 1.6), (1.6, -3.7, 2.0, 1.4), (-1.0, 3.6, 2.6, 1.8), (2.6, 3.4, 1.8, 1.4), (-4.9, 0.4, 1.4, 1.6)):
        out.append(box(B, [x, 21.6, z - 0.4], [w, h, 1.0], 'fur_tan', 'fur'))
    # cinturones
    out.append(box(B, [-4.6, 12.6, -2.6], [9.2, 1.3, 5.2], 'black_leather', 'stitch'))
    out.append(rbox(B, [-4.9, 11.6, -2.85], [9.8, 1.0, 5.7], 'wrap', None, pivot=(0, 12.1, 0), e=(0, 0, 7)))
    out.append(box(B, [1.6, 11.4, -3.05], [1.6, 1.4, 0.3], 'gold'))
    # taparrabos: panos largos adelante y atras, cortos a los costados
    # (cada pano en su hueso de tela: se dobla con las piernas como la falda de Roxy)
    out += ring_panel('g', (0, 12.0, -2.9), 4.4, 7.4, 'khaki', 'fray', yaw=0, flare=10)
    out += ring_panel('g', (0, 12.0, 2.9), 5.0, 7.8, 'khaki', 'fray', yaw=180, flare=10)
    out += xm(*ring_panel('g', (-4.7, 12.0, 0), 4.6, 4.6, 'khaki', 'fray', yaw=90, flare=24))
    out += xm(*ring_panel('g', (-3.6, 12.0, -2.4), 2.6, 5.4, 'khaki', 'fray', yaw=40, flare=20))
    out += xm(*ring_panel('g', (-3.6, 12.0, 2.4), 2.6, 5.4, 'khaki', 'fray', yaw=140, flare=20))
    out += sym([
        box(RA, [-9.1, 20.0, -3.1], [5.6, 4.4, 6.2], 'fur_tan', 'fur'),
        box(RA, [-8.6, 24.2, -2.6], [4.4, 0.8, 5.2], 'fur_tan', 'fur'),
        box(RA, [-8.55, 12.6, -2.55], [4.8, 4.0, 5.1], 'black_leather', 'straps'),
        box(RA, [-8.7, 15.9, -2.7], [5.1, 0.6, 5.4], 'gold'),
        box(RA, [-8.7, 12.8, -2.7], [5.1, 0.5, 5.4], 'gold'),
        box(RL, [-4.25, 9.4, -2.25], [4.4, 0.9, 4.5], 'black_leather', 'stitch'),
        box(RL, [-4.3, 5.2, -2.7], [4.5, 2.4, 0.7], 'dark_metal', 'rivets'),
        box(RF, [-4.3, 0.5, -2.55], [4.5, 6.6, 5.0], 'boot_brown', 'straps'),
        box(RF, [-4.65, 6.6, -2.9], [5.2, 1.8, 5.7], 'fur_tan', 'fur'),
        box(RF, [-4.3, 0.5, -3.3], [4.5, 1.8, 0.8], 'boot_brown'),
        box(RF, [-4.35, 0, -3.45], [4.6, 0.5, 6.0], 'sole'),
        box(RF, [-4.45, 3.4, -2.65], [4.8, 0.5, 5.2], 'gold'),
    ])
    return out


def paul():
    """Paul: gambeson azul verdoso acolchado, coraza de cuero con cuello alto y remaches, hombrera izquierda de tres
    placas de acero, tahali con vaina en la cadera, escarcelas de cuero sobre los muslos, brazaletes, pantalon gris
    y botas con cana doblada."""
    out = []
    out.append(box(B, [-4.3, 12, -2.3], [8.6, 12.1, 4.6], 'gambeson', 'quilt'))
    out.append(box(B, [-4.7, 14.2, -2.7], [9.4, 8.6, 5.4], 'leather_brown', 'rivets'))
    out.append(box(B, [-3.6, 15.0, -3.0], [7.2, 6.8, 0.4], 'leather_brown', 'plates'))
    out.append(box(B, [-4.4, 22.8, -2.6], [8.8, 1.8, 5.2], 'gambeson', 'quilt'))
    out.append(box(B, [-4.9, 12.2, -2.9], [9.8, 1.6, 5.8], 'brown', 'stitch'))
    out.append(box(B, [-0.9, 12.0, -3.15], [1.8, 2.0, 0.3], 'steel_d'))
    # tahali cruzado y vaina corta en la cadera derecha
    out.append(rbox(B, [-0.6, 11.0, -3.1], [1.2, 14, 0.4], 'brown', 'stitch', pivot=(0, 18, -3.0), e=(0, 0, 34)))
    out.append(rbox(B, [-0.6, 11.0, 2.75], [1.2, 14, 0.4], 'brown', 'stitch', pivot=(0, 18, 2.9), e=(0, 0, -34)))
    out.append(rbox(B, [-6.0, 4.0, -1.0], [1.4, 8.4, 2.2], 'black_leather', 'trim', pivot=(-5.3, 12, 0), e=(-20, 0, 8)))
    # escarcelas (adelante dos, costados y atras)
    out += xm(*ring_panel('p', (-2.3, 12.4, -3.0), 4.0, 5.0, 'leather_brown', 'rivets', yaw=0, flare=12))
    out += xm(*ring_panel('p', (-4.9, 12.4, 0), 4.2, 4.4, 'leather_brown', 'rivets', yaw=90, flare=16))
    for x in (-2.8, 0.0, 2.8):  # atras en tres partes, asi cada pierna mueve la suya
        out += ring_panel('p', (x, 12.4, 3.0), 3.0, 4.6, 'gambeson', 'quilt', yaw=180, flare=12)
    out += [
        box(LA, [3.4, 22.0, -3.1], [5.8, 2.6, 6.2], 'steel_d', 'rivets'),
        box(LA, [3.8, 20.2, -2.95], [5.4, 2.0, 5.9], 'steel_d', 'plates'),
        box(LA, [4.1, 18.6, -2.8], [5.0, 1.8, 5.6], 'steel_d', 'plates'),
        box(LA, [5.2, 24.4, -2.0], [3.2, 0.8, 4.0], 'steel_l'),
    ]
    out += sym([
        box(RA, [-8.4, 16.0, -2.4], [4.5, 8.0, 4.8], 'gambeson', 'quilt'),
        box(RA, [-8.6, 12.4, -2.6], [4.9, 4.0, 5.2], 'leather_brown', 'straps'),
        box(RA, [-8.75, 15.8, -2.75], [5.2, 0.8, 5.5], 'leather_brown', 'stitch'),
        box(RL, [-4.2, 4.0, -2.2], [4.3, 8.1, 4.4], 'pants_grey', 'seam'),
        box(RL, [-4.35, 5.4, -2.65], [4.6, 2.4, 0.7], 'leather_brown', 'rivets'),
        box(RF, [-4.3, 0.5, -2.55], [4.5, 4.6, 5.0], 'boot_brown', 'folds'),
        box(RF, [-4.55, 4.6, -2.8], [5.0, 1.8, 5.5], 'leather_brown', 'trim_light'),
        box(RF, [-4.3, 0.5, -3.3], [4.5, 1.8, 0.8], 'boot_brown'),
        box(RF, [-4.35, 0, -3.45], [4.6, 0.5, 6.0], 'sole'),
    ])
    return out


def eris():
    """Eris espadachina (de la skin del NPC): chaqueta granate con cuello y hombros de piel crema, blusa blanca
    con cinta negra, corse de cuero con cordones, falda corta tableada con ribete claro, guanteletes, short negro,
    medias granate y botas altas. Diadema negra con mono al costado."""
    out = []
    out.append(box(H, [-4.3, 32.0, -1.6], [8.6, 0.6, 1.8], 'black_leather'))
    out += xm(box(H, [-4.6, 27.0, -1.6], [0.6, 5.6, 1.8], 'black_leather'))
    out.append(box(H, [-5.0, 30.4, -1.3], [1.0, 1.2, 1.2], 'black_leather'))
    out.append(rbox(H, [-6.8, 30.9, -1.1], [2.0, 1.4, 0.8], 'black_leather', pivot=(-5.0, 31.0, -0.7), e=(0, 0, -14)))
    out.append(rbox(H, [-6.8, 29.6, -1.1], [2.0, 1.4, 0.8], 'black_leather', pivot=(-5.0, 31.0, -0.7), e=(0, 0, 16)))
    # chaqueta abierta, blusa y cinta
    out.append(box(B, [-4.45, 12, -2.45], [8.9, 12.2, 4.9], 'maroon', 'folds'))
    out.append(box(B, [-1.7, 16.8, -2.6], [3.4, 7.2, 0.3], 'shirt', 'folds'))
    out += xm(box(B, [-4.6, 16.8, -2.75], [2.9, 7.4, 0.5], 'maroon', 'lapel'))
    out.append(box(B, [-0.7, 22.0, -2.95], [1.4, 1.2, 0.4], 'black'))
    out += xm(rbox(B, [-2.4, 21.9, -2.9], [1.7, 1.3, 0.3], 'black', pivot=(-0.7, 22.5, -2.8), e=(0, 0, 14)))
    # cuello de piel
    out += skirt(B, 24.4, 2.2, 4.7, 2.7, 62, 'fur_cream', 'fur', corner=3.0, cflare=64, thick=0.9)
    # corse y cinturon con portaespada a la izquierda
    out.append(box(B, [-4.7, 12.5, -2.7], [9.4, 4.2, 5.4], 'leather_brown', 'lace'))
    out.append(box(B, [-4.9, 12.0, -2.9], [9.8, 0.9, 5.8], 'black_leather'))
    out.append(box(B, [3.9, 8.4, -1.4], [1.4, 3.8, 1.6], 'black_leather', 'stitch'))
    # falda corta tableada
    out += ring_skirt('e', 12.5, 4.4, 4.9, 2.9, 26, 'maroon', 'pleats_light', n=16)
    out += sym([
        box(RA, [-8.7, 22.4, -2.7], [5.4, 1.8, 5.4], 'fur_cream', 'fur'),
        box(RA, [-8.45, 15.0, -2.45], [4.9, 7.6, 4.9], 'maroon', 'folds'),
        box(RA, [-8.55, 17.0, -2.55], [5.1, 0.6, 5.1], 'shirt'),
        box(RA, [-8.55, 19.0, -2.55], [5.1, 0.6, 5.1], 'shirt'),
        box(RA, [-8.65, 12.2, -2.65], [5.3, 3.4, 5.3], 'leather_brown', 'rivets'),
        box(RA, [-8.85, 15.2, -2.85], [5.7, 0.8, 5.7], 'leather_brown'),
        box(RL, [-4.2, 8.6, -2.2], [4.3, 3.5, 4.4], 'black'),
        box(RL, [-4.15, 3.0, -2.15], [4.2, 5.8, 4.3], 'maroon', 'bands'),
        box(RF, [-4.3, 0.5, -2.55], [4.5, 6.2, 5.0], 'boot_brown', 'folds'),
        box(RF, [-4.55, 6.0, -2.8], [5.0, 1.6, 5.5], 'leather_brown', 'trim_light'),
        box(RF, [-4.3, 0.5, -3.3], [4.5, 1.8, 0.8], 'boot_brown'),
        box(RF, [-4.4, 2.8, -2.7], [4.7, 0.5, 5.3], 'black_leather'),
        box(RF, [-4.35, 0, -3.45], [4.6, 0.5, 6.0], 'sole'),
    ])
    return out


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
SLOT_BONES = {'head': ['armorHead'], 'chest': ['armorBody', 'armorRightArm', 'armorLeftArm', *CLOTH],
              'legs': ['armorRightLeg', 'armorLeftLeg'], 'feet': ['armorRightBoot', 'armorLeftBoot']}
# Iconos hechos a mano (en vez del generado de frente)
ICON_FROM = {'flame_trapping_cloak_v2': 'addon_flametrappingcloak'}

# ================================================================ UV y pintura
# UV por cara (no box UV): GeckoLib redondea el box UV hacia abajo y en las cajas finitas (0.5 px) eso deja caras
# de 0 px que leen el borde de la textura y titilan. Con UV por cara cada cara tiene su region de al menos 1 px.
def uv_dims(s):
    return tuple(max(1, int(round(x))) for x in s)


def pack(boxes, tex=128):
    """Acomoda las regiones UV en filas (con 1 px de margen)."""
    x = y = row = 0
    for b in boxes:
        w, h, d = uv_dims(b['size'])
        W, Hh = 2 * (d + w) + 1, d + h + 1
        if x + W > tex:
            x, y, row = 0, y + row, 0
        b['uv'] = [x, y]
        b['dims'] = (w, h, d)
        x += W
        row = max(row, Hh)
    if y + row > tex:
        return pack(boxes, tex * 2)
    return tex


def faces(b):
    """Regiones de cada cara: nombre -> (x, y, ancho, alto)."""
    u, v = b['uv']
    w, h, d = b['dims']
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h),
            'north': (u + d, v + d, w, h), 'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


SIDES = ('north', 'south', 'east', 'west')


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

    def mix(a, b, t):
        return [a[k] * (1 - t) + b[k] * t for k in range(3)]

    for b in boxes:
        m = MATS[b['mat']]
        det = b['detail']
        for name, (fx, fy, fw, fh) in faces(b).items():
            # una cara de 0 px (cajas finitas) igual se pinta 1 px para que GeckoLib lea el color correcto
            FW, FH = max(fw, 1), max(fh, 1)
            for j in range(FH):
                for i in range(FW):
                    n = rnd.uniform(-8, 8)
                    if det == 'fur':
                        n = rnd.uniform(-22, 22)
                    c = [m['base'][k] + n for k in range(3)]
                    side = name in SIDES
                    if j == 0 and name != 'down':
                        c = mix(c, m['light'], 0.5)
                    if (i == 0 or i == FW - 1 or j == FH - 1) and FW > 2 and FH > 2:
                        c = mix(c, m['dark'], 0.6)
                    g = m.get('glow', False)
                    skip = False
                    last = j == FH - 1
                    if det == 'rivets' and (i in (1, FW - 2)) and (j in (1, FH - 2)) and FW > 3 and FH > 3:
                        c = m['light']
                    elif det == 'bands' and side and j % 3 == 1:
                        c = mix(c, m['dark'], 0.6)
                    elif det == 'plates' and side and j == FH - 1:
                        c = m['light']
                    elif det == 'visor' and name == 'north':
                        if j in (3, 4) and 1 <= i < FW - 1 or (i in (FW // 2 - 1, FW // 2) and 3 <= j < FH - 2):
                            c = (10, 8, 6)
                    elif det == 'shine' and name == 'north' and i == 1 and j == 0:
                        c = (230, 240, 255)
                    elif det == 'stitch' and side and j == 1 and i % 2 == 0 and FH > 2:
                        c = m['light']
                    elif det in ('folds', 'hem', 'puff') and side and i % 3 == 1:
                        c = mix(c, m['dark'], 0.35)
                    elif det == 'seam' and name == 'north' and i == FW // 2:
                        c = m['dark']
                    elif det == 'lapel' and side and (i == 0 or i == FW - 1):
                        c = m['light']
                    elif det == 'trim_light' and side and last:
                        c = m['light']
                    elif det == 'trim' and side and last:
                        c = MATS['gold']['base']
                    elif det == 'trim_ember' and side and last:
                        c, g = MATS['ember']['base'], True
                    elif det == 'boots' and j <= 1:
                        c = m['light']
                    elif det == 'straps' and side and j % 3 == 0:
                        c = MATS['dark_metal']['light']
                    elif det == 'stripe' and name in ('east', 'west') and i == FW // 2:
                        c = m['light']
                    elif det == 'flap' and side and j <= 1:
                        c = mix(c, m['dark'], 0.5) if j == 1 else m['light']
                    elif det == 'quilt' and side and (i % 3 == 0 or j % 3 == 0):
                        c = mix(c, m['dark'], 0.45)
                    elif det == 'lace' and name == 'north':
                        mid = FW // 2
                        if i in (mid - 1, mid):
                            c = (30, 26, 30)
                        if j % 2 == 0 and i in (mid - 1, mid):
                            c = (232, 226, 236)
                    elif det == 'sock' and side and j in (1, 3):
                        c = MATS['frill_black']['base']
                    elif det == 'pleats' and side:
                        if i % 2 == 1:
                            c = mix(c, m['dark'], 0.45)
                        if last:
                            c = m['light']
                    elif det == 'pleats_light' and side:
                        if i % 2 == 1:
                            c = mix(c, m['dark'], 0.45)
                        if j >= FH - 2:
                            c = MATS['fur_cream']['base'] if last else MATS['fur_cream']['dark']
                    elif det == 'frill' and side:
                        # volado: borde de festones (pixel si, pixel no) y una linea mas oscura arriba
                        if last and FH > 1:
                            c = m['light'] if i % 2 == 0 else mix(c, m['dark'], 0.55)
                        elif j == FH - 2 and FH > 2:
                            c = mix(c, m['dark'], 0.4)
                    elif det == 'brim' and side and last:
                        c = m['light']
                    elif det == 'fur':
                        if rnd.random() < 0.18:
                            c = m['light']
                        if side and last and FH > 1 and (i + int(fx)) % 2 == 0:
                            skip = True
                    elif det == 'fray' and side:
                        if last and rnd.random() < 0.45 and FH > 2:
                            skip = True
                        elif i % 4 == 2:
                            c = mix(c, m['dark'], 0.3)
                    elif det == 'coat_edge' and side:
                        if last or (name == 'north' and (i == 0 or i == FW - 1)):
                            c = m['light']
                        elif i % 3 == 1:
                            c = mix(c, m['dark'], 0.3)
                    elif det == 'mantle' and side:
                        if last:
                            c = MATS['gold']['base']
                        elif i % 3 == 1:
                            c = mix(c, m['dark'], 0.35)
                    elif det == 'cloakflame' and side:
                        k = FH - 1 - j
                        tongue = 3.2 + 2.2 * math.sin(i * 1.7 + fx) + 1.2 * math.sin(i * 0.6 + 2)
                        if i % 3 == 1:
                            c = mix(c, m['dark'], 0.35)
                        if k < tongue:
                            t = k / max(1.0, tongue)
                            c = mix((255, 160, 60), (140, 40, 14), t ** 0.8)
                            g = k < tongue * 0.35
                    elif det == 'hem' and side and j >= FH - 2:
                        c, g = MATS['ember']['base'], last
                    elif det == 'eyes' and name == 'north':
                        c = mix(c, (0, 0, 0), 0.3)
                        if j == 3 and i in (2, FW - 3):
                            c, g = (255, 118, 40), True
                    elif det == 'feather' and name in ('east', 'west') and (i + j) % 3 == 0:
                        c = m['light']
                    if m.get('glow_lines') and (i + j) % 3 == 0:
                        c, g = m['light'], True
                    if not skip:
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
    # Huesos de tela: cuelgan del torso y giran desde lo mas alto de sus panos (la cintura o los hombros)
    for cb in CLOTH:
        tops = [bx['pivot'][1] if bx['pivot'] else bx['origin'][1] + bx['size'][1] for bx in boxes if bx['bone'] == cb]
        if tops:
            bones[cb] = dict(name=cb, parent='armorBody', pivot=[0, round(max(tops), 3), 0], cubes=[])
            order.append(cb)
    # Huesos de tela por zona (ring_*): pivote en el borde de arriba de su pano, colgados del torso
    for b in boxes:
        if b['bone'].startswith('ring_') and b['bone'] not in bones:
            piv = b['pivot'] if b['pivot'] else [b['origin'][0] + b['size'][0] / 2, b['origin'][1] + b['size'][1], b['origin'][2]]
            bones[b['bone']] = dict(name=b['bone'], parent='armorBody', pivot=[round(v, 3) for v in piv], cubes=[])
            order.append(b['bone'])
    for b in boxes:
        uv = {}
        for name, (fx, fy, fw, fh) in faces(b).items():
            uv[name] = dict(uv=[fx, fy + fh], uv_size=[fw, -fh]) if name == 'down' else dict(uv=[fx, fy], uv_size=[fw, fh])
        c = dict(origin=[round(x, 3) for x in b['origin']], size=[round(x, 3) for x in b['size']], uv=uv)
        if b['rot']:
            c['rotation'] = b['rot']
            c['pivot'] = b['pivot']
        bones[b['bone']]['cubes'].append(c)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': dict(identifier='geometry.' + name, texture_width=tex, texture_height=tex,
                            visible_bounds_width=3, visible_bounds_height=3.5, visible_bounds_offset=[0, 1.25, 0]),
        'bones': [bones[n] for n in order]}]}

# ================================================================ Iconos (vista de frente, con las rotaciones)
def rot_matrix(b):
    """Matriz de giro que se ve en el marco Bedrock (la misma cuenta que GeckoLib, ver rbox)."""
    if not b['rot']:
        return None
    ex, ey, ez = [math.radians(v) for v in (-b['rot'][0], b['rot'][1], -b['rot'][2])]
    cx, sx, cy, sy, cz, sz = math.cos(ex), math.sin(ex), math.cos(ey), math.sin(ey), math.cos(ez), math.sin(ez)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    mul = lambda A, B: [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(Rz, mul(Ry, Rx))


def world_points(b, step=0.25):
    """Puntos de las caras de la caja (ya girada) con el pixel de textura que les toca."""
    o, s = b['origin'], b['size']
    R = rot_matrix(b)
    pv = b['pivot'] or [0, 0, 0]
    fcs = faces(b)
    w, h, d = b['dims']

    def tr(p):
        if R is None:
            return p
        q = [p[k] - pv[k] for k in range(3)]
        return [sum(R[i][k] * q[k] for k in range(3)) + pv[i] for i in range(3)]

    def frange(n):
        k = max(1, int(n / step))
        return [(i + 0.5) / k for i in range(k)]
    # cara norte (z minimo) y caras de costado/arriba, con su region de textura
    specs = {
        'north': lambda a, c: ([o[0] + a * s[0], o[1] + s[1] - c * s[1], o[2]], a, c),
        'south': lambda a, c: ([o[0] + s[0] - a * s[0], o[1] + s[1] - c * s[1], o[2] + s[2]], a, c),
        'east': lambda a, c: ([o[0], o[1] + s[1] - c * s[1], o[2] + s[2] - a * s[2]], a, c),
        'west': lambda a, c: ([o[0] + s[0], o[1] + s[1] - c * s[1], o[2] + a * s[2]], a, c),
        'up': lambda a, c: ([o[0] + a * s[0], o[1] + s[1], o[2] + c * s[2]], a, c),
    }
    dims = {'north': (s[0], s[1]), 'south': (s[0], s[1]), 'east': (s[2], s[1]), 'west': (s[2], s[1]), 'up': (s[0], s[2])}
    for name, fn in specs.items():
        fx, fy, fw, fh = fcs[name]
        for a in frange(dims[name][0]):
            for c in frange(dims[name][1]):
                p, aa, cc = fn(a, c)
                u = fx + min(max(fw, 1) - 1, int(aa * max(fw, 1)))
                v = fy + min(max(fh, 1) - 1, int(cc * max(fh, 1)))
                yield tr(p), (u, v), name


def icon(boxes, bones, img):
    """Dibuja de frente (con z-buffer) las cajas de esos huesos en 16x16."""
    sel = [b for b in boxes if b['bone'] in bones]
    if not sel:
        return Image.new('RGBA', (16, 16))
    pts = []
    for b in sel:
        pts += list(world_points(b))
    xs = [p[0][0] for p in pts]; ys = [p[0][1] for p in pts]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    sc = 14 / max(x1 - x0, y1 - y0)
    ox, oy = (16 - (x1 - x0) * sc) / 2, (16 - (y1 - y0) * sc) / 2
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    po, src = out.load(), img.load()
    zb = {}
    shade = {'north': 1.0, 'south': 0.7, 'east': 0.8, 'west': 0.8, 'up': 1.1}
    for p, (u, v), name in pts:
        xx = int((p[0] - x0) * sc + ox); yy = int((y1 - p[1]) * sc + oy)
        if not (0 <= xx < 16 and 0 <= yy < 16):
            continue
        col = src[u, v]
        if col[3] == 0:
            continue
        if (xx, yy) not in zb or p[2] < zb[(xx, yy)]:
            zb[(xx, yy)] = p[2]
            f = shade[name]
            po[xx, yy] = tuple(max(0, min(255, int(col[k] * f))) for k in range(3)) + (255,)
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
        _RING[0] = 0
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
                sb = SLOT_BONES[slot] + ([bx['bone'] for bx in boxes if bx['bone'].startswith('ring_')] if slot == 'chest' else [])
                icon(boxes, sb, img).save(os.path.join(idir, item + '.png'))
            with open(os.path.join(mdir, item + '.json'), 'w') as f:
                json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + item}}, f, indent=1)
    print(len(ARMORS), 'armaduras v2 ->', gdir)

if __name__ == '__main__':
    main()

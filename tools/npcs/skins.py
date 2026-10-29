"""Skins de las NPC nuevas (bandidos y hombres/mujeres bestia). Pinta skins de 64x64 estilo anime: ojos grandes con
brillo, flequillo, pelo largo en las mujeres, ropa con sombras y detalles. Uso: python3 skins.py [preview]"""
import os, random, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku', 'textures', 'entities'))


def sh(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


class Skin:
    """Caras de cada pieza (u, v, ancho, alto) en la skin de 64x64. slim = brazos de 3 px."""

    def __init__(self, slim):
        self.img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.slim = slim
        aw = 3 if slim else 4
        self.parts = {
            'head': dict(top=(8, 0, 8, 8), bottom=(16, 0, 8, 8), right=(0, 8, 8, 8), front=(8, 8, 8, 8), left=(16, 8, 8, 8), back=(24, 8, 8, 8)),
            'hat': dict(top=(40, 0, 8, 8), bottom=(48, 0, 8, 8), right=(32, 8, 8, 8), front=(40, 8, 8, 8), left=(48, 8, 8, 8), back=(56, 8, 8, 8)),
            'body': dict(top=(20, 16, 8, 4), bottom=(28, 16, 8, 4), right=(16, 20, 4, 12), front=(20, 20, 8, 12), left=(28, 20, 4, 12), back=(32, 20, 8, 12)),
            'jacket': dict(top=(20, 32, 8, 4), bottom=(28, 32, 8, 4), right=(16, 36, 4, 12), front=(20, 36, 8, 12), left=(28, 36, 4, 12), back=(32, 36, 8, 12)),
            'rarm': dict(top=(44, 16, aw, 4), bottom=(44 + aw, 16, aw, 4), right=(40, 20, 4, 12), front=(44, 20, aw, 12), left=(44 + aw, 20, 4, 12), back=(48 + aw, 20, aw, 12)),
            'rsleeve': dict(top=(44, 32, aw, 4), bottom=(44 + aw, 32, aw, 4), right=(40, 36, 4, 12), front=(44, 36, aw, 12), left=(44 + aw, 36, 4, 12), back=(48 + aw, 36, aw, 12)),
            'larm': dict(top=(36, 48, aw, 4), bottom=(36 + aw, 48, aw, 4), right=(32, 52, 4, 12), front=(36, 52, aw, 12), left=(36 + aw, 52, 4, 12), back=(40 + aw, 52, aw, 12)),
            'lsleeve': dict(top=(52, 48, aw, 4), bottom=(52 + aw, 48, aw, 4), right=(48, 52, 4, 12), front=(52, 52, aw, 12), left=(52 + aw, 52, 4, 12), back=(56 + aw, 52, aw, 12)),
            'rleg': dict(top=(4, 16, 4, 4), bottom=(8, 16, 4, 4), right=(0, 20, 4, 12), front=(4, 20, 4, 12), left=(8, 20, 4, 12), back=(12, 20, 4, 12)),
            'rpants': dict(top=(4, 32, 4, 4), bottom=(8, 32, 4, 4), right=(0, 36, 4, 12), front=(4, 36, 4, 12), left=(8, 36, 4, 12), back=(12, 36, 4, 12)),
            'lleg': dict(top=(20, 48, 4, 4), bottom=(24, 48, 4, 4), right=(16, 52, 4, 12), front=(20, 52, 4, 12), left=(24, 52, 4, 12), back=(28, 52, 4, 12)),
            'lpants': dict(top=(4, 48, 4, 4), bottom=(8, 48, 4, 4), right=(0, 52, 4, 12), front=(4, 52, 4, 12), left=(8, 52, 4, 12), back=(12, 52, 4, 12)),
        }

    def px(self, part, face, x, y, c):
        u, v, w, h = self.parts[part][face]
        if 0 <= x < w and 0 <= y < h and c is not None:
            self.img.putpixel((u + x, v + y), tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,)))

    def fill(self, part, fn, faces=('top', 'bottom', 'right', 'front', 'left', 'back')):
        """fn(face, x, y, w, h) -> color o None."""
        for f in faces:
            u, v, w, h = self.parts[part][f]
            for y in range(h):
                for x in range(w):
                    c = fn(f, x, y, w, h)
                    if c is not None:
                        self.px(part, f, x, y, c)


def noise(seed):
    r = random.Random(seed)
    return lambda: r.uniform(0.92, 1.06)


def paint(spec):
    s = Skin(spec['slim'])
    rnd = noise(hash(spec['name']) & 0xffff)
    skin, hair, eye = spec['skin'], spec['hair'], spec['eye']
    long_hair, female = spec.get('long', False), spec['female']

    # ------------------------------------------------------------------ cabeza (estilo de las NPC del mod)
    strand = random.Random(len(spec['name']) * 7)
    tones = [sh(hair, 1.18), sh(hair, 1.05), sh(hair, 0.95), sh(hair, 0.82), sh(hair, 0.68)]

    def hair_px(x, y, h=8):
        """Pelo con mechones verticales: tono por columna, mas oscuro abajo y una franja de brillo arriba."""
        col = (x * 3 + (x // 2)) % 3
        t = col + (1 if y > h * 0.55 else 0) + (1 if y > h * 0.8 else 0)
        if y in (1, 2) and (x + y) % 3 == 0:
            t = 0
        return tones[max(0, min(4, t + (1 if strand.random() < 0.15 else 0)))]

    def head(f, x, y, w, h):
        if f == 'top':
            return hair_px(x, 2)
        if f == 'bottom':
            return sh(skin, 0.8)
        if f == 'back':
            return hair_px(x, y)
        if f in ('right', 'left'):
            near_face = (x >= 5 if f == 'right' else x <= 2)
            if y <= 3 or long_hair or not near_face or y <= 4:
                return hair_px(x, y)
            return sh(skin, 0.9)
        return sh(skin, 1.0 if y < 6 else 1.04)
    s.fill('head', head)
    F = lambda x, y, c: s.px('head', 'front', x, y, c)
    # flequillo con mechones que bajan hasta los ojos, y mechones laterales que enmarcan la cara
    fringe = spec.get('fringe', [3, 4, 3, 2, 3, 4, 3, 3])
    for x in range(8):
        for y in range(fringe[x]):
            F(x, y, hair_px(x, y + 1))
        F(x, fringe[x], sh(hair_px(x, 3), 0.75) if fringe[x] < 5 and strand.random() < 0.5 else None)
    side = 8 if long_hair else 6
    for y in range(2, side):
        F(0, y, hair_px(0, y)); F(7, y, hair_px(7, y))
    if long_hair:
        for y in range(5, 8):
            F(1, y, hair_px(1, y) if y >= 6 else None); F(6, y, hair_px(6, y) if y >= 6 else None)
    # ojos de 2 px de alto (filas 4 y 5 de la cara) en el estilo de Fresh Moves: cada fila tiene un pixel de afuera
    # (blanco o pestana) y uno de adentro (iris). Ademas se copian al rincon libre de la textura de la cabeza (u 4..7,
    # v 2..5), que es de donde el pack de "solo ojos" saca los ojos que se mueven, parpadean y siguen la mirada.
    lash = spec.get('lash', sh(hair, 0.35) if female else (238, 238, 242))
    white = (244, 244, 248)
    eye_rows = {4: (lash, sh(eye, 0.8 if female else 1.1)), 5: (white, sh(eye, 1.22 if female else 0.65))}
    lids = {4: sh(lash, 0.9) if female else sh(skin, 0.92), 5: sh(skin, 1.0)}
    raw = s.img.putpixel
    for row, (outer, inner) in eye_rows.items():
        F(1, row, outer); F(2, row, inner); F(5, row, inner); F(6, row, outer)
        v = 2 * (row - 3)  # fila 4 de la cara -> v 2..3, fila 5 -> v 4..5 (como en la skin de Steve del pack)
        for x, c in ((4, lids[row]), (5, lids[row]), (6, lids[row]), (7, lids[row])):
            raw((x, v), tuple(c) + (255,))
        for x, c in ((4, outer), (5, inner), (6, inner), (7, outer)):
            raw((x, v + 1), tuple(c[:3]) + (255,))
    if spec.get('liner'):  # delineado en punta hacia afuera
        F(0, 4, spec['liner']); F(7, 4, spec['liner'])
    # cejas: en la cara (fila 3) y en el rincon de cejas del pack (u 60..63, v 0) para que se muevan con la expresion
    brow = sh(hair, 0.55)
    for x in (1, 2, 5, 6):
        if fringe[x] <= 3:
            F(x, 3, brow)
    for x in range(60, 64):
        raw((x, 0), tuple(brow) + (255,))
    # boca, rubor, pecas y cicatriz
    if female:
        F(1, 6, (240, 160, 160)); F(6, 6, (240, 160, 160))
        lip = spec.get('lips', sh(skin, 0.82))
        F(3, 7, lip); F(4, 7, sh(lip, 0.9))
        for (x, y) in spec.get('freckles', ()):
            F(x, y, sh(skin, 0.78))
    else:
        F(3, 7, sh(skin, 0.86)); F(4, 7, sh(skin, 0.86))
        if spec.get('scar'):
            F(5, 6, sh(skin, 0.75)); F(6, 7, sh(skin, 0.75))
    if spec.get('mask'):
        for x in range(8):
            for y in (6, 7):
                F(x, y, sh(spec['mask'], (1.05 if y == 6 else 0.85) * (0.92 if x in (0, 7) else 1)))
        for y in (6, 7):
            s.px('head', 'right', 7, y, sh(spec['mask'], 0.8)); s.px('head', 'left', 0, y, sh(spec['mask'], 0.8))
            s.px('head', 'right', 6, y, sh(spec['mask'], 0.75)); s.px('head', 'left', 1, y, sh(spec['mask'], 0.75))
    # capa del sombrero: volumen del pelo (mechones sueltos), vincha o capucha
    def hat(f, x, y, w, h):
        if spec.get('hood'):
            hood = spec['hood']
            if f == 'front':
                if y == 0 or x in (0, 7):
                    return sh(hood, 0.8 if y == 0 else 0.7)
                return None
            if f == 'bottom':
                return None
            return sh(hood, (1.08 if y < 2 else 0.95 if y < 6 else 0.82) * (0.92 if (x + y) % 4 == 0 else 1))
        if spec.get('bandana'):
            bd = spec['bandana']
            if f in ('front', 'right', 'left', 'back') and y in (1, 2):
                return sh(bd, (1.05 if y == 1 else 0.85) * (1.25 if (x * 3 + y) % 5 == 0 else 1))
            if f == 'top':
                return sh(bd, 0.95 * (1.2 if (x + y) % 4 == 0 else 1))
            if f == 'back' and 3 <= y <= 6 and x in (3, 4, 5) and (x - 3) <= (y - 3) + 1:
                return sh(bd, 0.8 if x == 4 else 0.92)
        if spec.get('ponytail') and f == 'back' and x in (3, 4) and y >= 1:
            return spec['ponytail'] if y == 2 else hair_px(x, y)
        if spec.get('band') and y == 2 and f in ('front', 'right', 'left', 'back'):
            return spec['band'] if f != 'front' or 1 <= x <= 6 else sh(spec['band'], 0.8)
        if f == 'back' and long_hair:
            return hair_px(x, y)
        if f in ('right', 'left') and long_hair and y >= 2:
            return hair_px(x, y)
        if f == 'front' and y <= 1 and x in (1, 4, 6):
            return hair_px(x, y)  # mechones que sobresalen del flequillo
        if f == 'top' and (x + y) % 3 == 0:
            return hair_px(x, 1)
        return None
    s.fill('hat', hat)

    # ------------------------------------------------------------------ torso
    shirt, top, belt, pants, boots = spec['shirt'], spec.get('top'), spec.get('belt', (70, 46, 28)), spec['pants'], spec['boots']

    def body(f, x, y, w, h):
        if f == 'top':
            return sh(shirt, 1.05)
        if f == 'bottom':
            return sh(pants, 0.9)
        k = rnd()
        if y >= 10:
            return sh(pants, k)
        if y == 8 or y == 9:
            if f == 'front' and x in (3, 4):
                return (210, 180, 90) if y == 8 else (150, 120, 60)  # hebilla
            return sh(belt, k * (1.0 if y == 8 else 0.75))
        c = shirt
        edge = False
        if top and f in ('front', 'back') and (x <= 1 or x >= 6) and y <= 7:
            c = top  # chaleco abierto adelante
            edge = f == 'front' and x in (1, 6)
        if top and f in ('right', 'left') and y <= 7:
            c = top
        if spec.get('crop') and f == 'front' and y in (6, 7):
            c = skin  # top corto
            return sh(c, 0.95 if y == 6 else 1)
        shade = 1.0
        if f == 'front':
            shade = 1.08 - 0.03 * y + (0.06 if x in (3, 4) else 0) - (0.08 if x in (0, 7) else 0)
        elif f == 'back':
            shade = 0.9
        if edge:
            shade *= 0.72  # borde del chaleco
        if f == 'front' and y == 7:
            shade *= 0.85  # sombra arriba del cinturon
        return sh(c, k * shade)
    s.fill('body', body)
    # cuello y detalles del frente
    B = lambda x, y, c: s.px('body', 'front', x, y, c)
    B(3, 0, sh(skin, 0.95)); B(4, 0, sh(skin, 0.95))
    if spec.get('vneck'):
        B(3, 1, sh(skin, 0.9)); B(4, 1, sh(skin, 0.9))
    if spec.get('lace'):
        for y in range(2, 8, 2):
            B(3, y, spec['lace']); B(4, y + 1, spec['lace'])
    if spec.get('strap'):  # correa del carcaj cruzada
        for i in range(10):
            B(min(7, i), i, spec['strap']); s.px('body', 'back', max(0, 7 - i), i, spec['strap'])
    if spec.get('fur'):  # cuello de piel
        for x in range(8):
            B(x, 0, sh(spec['fur'], rnd())); s.px('body', 'back', x, 0, sh(spec['fur'], rnd()))
        for y in range(2):
            s.px('body', 'right', y, 0, sh(spec['fur'], 0.95)); s.px('body', 'left', y, 0, sh(spec['fur'], 0.95))
    if female and spec.get('chest_shade', True):
        for x in (1, 2, 5, 6):
            B(x, 5, sh(shirt if not top or x in (2, 5) else top, 0.78))

    if spec.get('pouch'):
        for (x, y) in ((6, 9), (7, 9), (6, 10), (7, 10)):
            B(x, y, sh(spec['pouch'], 1.1 if y == 9 else 0.85))
    if spec.get('necklace'):
        for x in (2, 3, 4, 5):
            B(x, 1 if x in (2, 5) else 2, spec['necklace'])
        B(3, 3, (240, 235, 220)); B(4, 3, (240, 235, 220))  # colmillo
    if spec.get('dagger'):
        s.px('body', 'front', 1, 9, (90, 90, 100)); s.px('body', 'front', 1, 10, (170, 170, 180)); s.px('body', 'front', 1, 11, (190, 190, 200))
    # costados del torso un poco mas oscuros (volumen)
    for y in range(8):
        for f in ('right', 'left'):
            u, v, w, h = s.parts['body'][f]
            c = s.img.getpixel((u + (0 if f == 'right' else w - 1), v + y))
            s.img.putpixel((u + (0 if f == 'right' else w - 1), v + y), sh(c, 0.85) + (255,))

    def jacket(f, x, y, w, h):
        if long_hair and f == 'back' and y <= 5 - (x % 2):
            return sh(hair, rnd() * 0.92)  # el pelo largo cae sobre la espalda
        if spec.get('cape') and f == 'back':
            return sh(spec['cape'], rnd() * (0.9 if y > 6 else 1))
        return None
    s.fill('jacket', jacket)

    # ------------------------------------------------------------------ brazos
    sleeve = spec.get('sleeve', shirt)
    glove = spec.get('glove')

    def arm(f, x, y, w, h):
        if f == 'top':
            return sh(top or sleeve, 1.05) if spec.get('short_sleeve') is not True else sh(sleeve, 1.05)
        if f == 'bottom':
            return sh(glove or skin, 0.85)
        k = rnd()
        if y >= 10 and glove:
            return sh(glove, k)
        if y >= 10:
            return sh(skin, k)
        if spec.get('short_sleeve') and y >= 4:
            return sh(skin, k * (0.96 if f in ('back', 'right') else 1))
        if spec.get('bracer') and 6 <= y <= 9:
            return sh(spec['bracer'], k)
        c = top if (top and spec.get('vest_sleeves') and y <= 3) else sleeve
        if spec.get('pauldron') and y <= 2:
            return sh(spec['pauldron'], k * (1.15 if y == 0 else 0.95 if y == 1 else 0.7))
        shade = (0.9 if f == 'back' else 1) * (0.82 if y == 5 else 1) * (0.78 if y == 9 else 1)
        if f in ('front',) and x == 0:
            shade *= 0.9
        return sh(c, k * shade)
    s.fill('rarm', arm)
    s.fill('larm', arm)
    if spec.get('fur'):
        def furcuff(f, x, y, w, h):
            return sh(spec['fur'], rnd()) if (y == 0 and f != 'bottom') else None
        s.fill('rsleeve', furcuff); s.fill('lsleeve', furcuff)

    # ------------------------------------------------------------------ piernas
    def leg(f, x, y, w, h):
        if f == 'top':
            return sh(pants, 1)
        if f == 'bottom':
            return sh(boots, 0.6)
        k = rnd()
        bt = spec.get('boot_h', 5)
        if y >= 12 - bt:
            c = boots
            if y == 12 - bt:
                c = sh(boots, 1.2)
            if y == 11:
                c = sh(boots, 0.7)
            return sh(c, k)
        if spec.get('skirt') and y <= 4:
            return sh(spec['skirt'], k * (0.75 if y == 4 else 1.05 if y == 0 else 1) * (0.88 if x % 2 else 1))
        shade = (0.9 if f in ('back',) else 1) * (0.84 if y in (3, 6) and x in (1, 2) else 1)
        return sh(pants, k * shade)
    s.fill('rleg', leg)
    s.fill('lleg', leg)
    for part in ('rleg', 'lleg'):  # cordones de las botas
        bt = spec.get('boot_h', 5)
        for y in range(12 - bt + 1, 11, 2):
            s.px(part, 'front', 1, y, sh(boots, 1.5)); s.px(part, 'front', 2, y + 1 if y + 1 < 11 else y, sh(boots, 1.5))
    if spec.get('wraps'):
        def wraps(f, x, y, w, h):
            return sh(spec['wraps'], rnd()) if (f != 'top' and f != 'bottom' and 12 - spec.get('boot_h', 5) - 3 <= y < 12 - spec.get('boot_h', 5) and (x + y) % 2 == 0) else None
        s.fill('rpants', wraps); s.fill('lpants', wraps)
    return s.img


# Colores
SKIN_L, SKIN_M, SKIN_T = (246, 214, 190), (232, 190, 160), (200, 150, 112)
SPECS = [
    # --- bandidos
    dict(name='outlaw_m_melee', female=False, slim=False, skin=SKIN_M, hair=(60, 40, 28), eye=(120, 80, 40),
         fringe=[2, 3, 3, 2, 2, 3, 3, 2], mask=(140, 28, 30), shirt=(120, 36, 36), top=(92, 62, 40), vest_sleeves=True,
         belt=(50, 32, 20), pants=(70, 62, 52), boots=(54, 38, 26), bracer=(92, 62, 40), lace=(200, 180, 140), pouch=(110, 76, 44), dagger=True, pauldron=(70, 50, 34), scar=True),
    dict(name='outlaw_m_archer', female=False, slim=False, skin=SKIN_L, hair=(150, 110, 60), eye=(70, 120, 60),
         fringe=[2, 2, 3, 2, 1, 2, 3, 2], bandana=(176, 28, 30), shirt=(86, 110, 60), top=(110, 80, 50), vest_sleeves=True,
         belt=(60, 40, 24), pants=(92, 80, 60), boots=(70, 48, 30), strap=(60, 40, 22), glove=(90, 60, 36), cape=(64, 86, 48), pouch=(96, 70, 40)),
    dict(name='outlaw_f_melee', female=True, slim=True, long=True, skin=SKIN_L, hair=(150, 30, 40), eye=(220, 170, 60),
         fringe=[4, 3, 3, 2, 1, 2, 3, 4], ponytail=(30, 30, 34), liner=(36, 18, 24), lash=(48, 14, 22), lips=(196, 72, 84), shirt=(222, 214, 232), top=(60, 40, 34), crop=True, vneck=True,
         belt=(40, 28, 20), pants=(48, 44, 50), boots=(40, 30, 26), boot_h=6, bracer=(60, 40, 34), lace=(150, 30, 40), dagger=True, pouch=(80, 50, 40)),
    dict(name='outlaw_f_archer', female=True, slim=True, long=True, skin=SKIN_M, hair=(40, 46, 60), eye=(90, 170, 120),
         fringe=[3, 2, 3, 3, 2, 2, 3, 3], bandana=(176, 28, 30), freckles=((2, 6), (5, 6), (1, 7), (6, 7)), lips=(204, 124, 124), shirt=(150, 170, 110), top=(96, 70, 46), vest_sleeves=True,
         belt=(60, 40, 24), pants=(80, 70, 56), boots=(64, 44, 28), strap=(70, 46, 26), glove=(96, 66, 40), skirt=(96, 70, 46), pouch=(110, 80, 50)),
    # --- bestias (oso, perro, gato)
    dict(name='beast_bear_m', female=False, slim=False, skin=SKIN_T, hair=(96, 62, 36), eye=(150, 100, 40),
         fringe=[2, 2, 3, 2, 2, 3, 2, 2], shirt=(128, 58, 40), top=(96, 66, 38), vest_sleeves=True, fur=(130, 96, 60),
         belt=(70, 46, 26), pants=(96, 72, 48), boots=(70, 50, 34), wraps=(180, 160, 120), short_sleeve=True, necklace=(90, 60, 30), pauldron=(130, 96, 60), scar=True),
    dict(name='beast_bear_f', female=True, slim=True, long=True, skin=SKIN_T, hair=(110, 70, 40), eye=(170, 110, 40),
         fringe=[3, 2, 3, 2, 3, 2, 3, 2], shirt=(92, 108, 66), top=(112, 76, 44), fur=(140, 104, 66), short_sleeve=True,
         belt=(70, 46, 26), pants=(110, 84, 56), boots=(76, 54, 36), skirt=(120, 82, 48), wraps=(190, 170, 130), necklace=(90, 60, 30)),
    dict(name='beast_dog_m', female=False, slim=False, skin=SKIN_M, hair=(190, 190, 196), eye=(70, 110, 190),
         fringe=[2, 3, 2, 2, 3, 2, 3, 2], shirt=(60, 84, 130), top=(140, 140, 150), vest_sleeves=True, fur=(220, 220, 225),
         belt=(50, 40, 34), pants=(70, 70, 80), boots=(46, 40, 40), bracer=(140, 140, 150), pauldron=(150, 150, 160), pouch=(70, 60, 50)),
    dict(name='beast_dog_f', female=True, slim=True, long=True, skin=SKIN_L, hair=(216, 216, 222), eye=(90, 140, 210),
         fringe=[3, 3, 2, 3, 3, 2, 3, 3], shirt=(70, 96, 150), top=(160, 160, 170), fur=(235, 235, 240), crop=True,
         belt=(50, 40, 34), pants=(80, 80, 92), boots=(50, 44, 44), boot_h=6, skirt=(70, 96, 150), necklace=(60, 60, 70)),
    dict(name='beast_cat_m', female=False, slim=False, skin=SKIN_L, hair=(220, 130, 50), eye=(90, 180, 70),
         fringe=[2, 3, 3, 2, 3, 3, 2, 2], shirt=(214, 176, 92), top=(170, 60, 40), vest_sleeves=True,
         belt=(80, 50, 30), pants=(90, 70, 56), boots=(70, 48, 32), short_sleeve=True, wraps=(240, 220, 190), pouch=(120, 80, 40)),
    dict(name='beast_cat_f', female=True, slim=True, long=True, skin=SKIN_L, hair=(236, 150, 60), eye=(110, 200, 80),
         fringe=[3, 2, 3, 3, 2, 3, 2, 3], shirt=(70, 64, 80), top=(190, 70, 50), crop=True, vneck=True,
         belt=(80, 50, 30), pants=(100, 80, 64), boots=(80, 54, 36), skirt=(190, 70, 50), short_sleeve=True, necklace=(200, 170, 60)),
]


def front_preview(img, slim):
    """Vista de frente (cabeza, torso, brazos y piernas) para revisar."""
    aw = 3 if slim else 4
    out = Image.new('RGBA', (16, 32), (0, 0, 0, 0))
    def cp(u, v, w, h, x, y):
        out.alpha_composite(img.crop((u, v, u + w, v + h)), (x, y))
    cp(8, 8, 8, 8, 4, 0); cp(40, 8, 8, 8, 4, 0)
    cp(20, 20, 8, 12, 4, 8); cp(20, 36, 8, 12, 4, 8)
    cp(44, 20, aw, 12, 4 - aw, 8); cp(36, 52, aw, 12, 12, 8)
    cp(4, 20, 4, 12, 4, 20); cp(20, 52, 4, 12, 8, 20)
    return out


if __name__ == '__main__':
    previews = []
    for sp in SPECS:
        img = paint(sp)
        img.save(os.path.join(OUT, sp['name'] + '.png'))
        previews.append(front_preview(img, sp['slim']))
    if len(sys.argv) > 1:
        sheet = Image.new('RGBA', (len(previews) * 20, 34), (90, 140, 200, 255))
        for i, p in enumerate(previews):
            sheet.alpha_composite(p, (i * 20 + 2, 1))
        sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(sys.argv[1])
    print(len(SPECS), 'skins')

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

    # ------------------------------------------------------------------ cabeza
    def head(f, x, y, w, h):
        if f == 'top':
            return sh(hair, rnd())
        if f == 'bottom':
            return sh(skin, 0.8)
        if f == 'back':
            return sh(hair, rnd() * (0.85 if y > 5 else 1))
        if f in ('right', 'left'):
            # costados: pelo arriba; la oreja humana queda tapada (llevan orejas de bestia o pelo)
            if y <= 3 or long_hair or (x >= 4 if f == 'right' else x <= 3):
                return sh(hair, rnd() * (0.9 if y > 4 else 1))
            return sh(skin, 0.92)
        # frente: cara
        return sh(skin, 1.0)
    s.fill('head', head)
    F = lambda x, y, c: s.px('head', 'front', x, y, c)
    # flequillo
    fringe = spec.get('fringe', [2, 3, 2, 1, 2, 3, 2, 2])
    for x in range(8):
        for y in range(fringe[x]):
            F(x, y, sh(hair, 1.05 if y == 0 else 0.95))
    if long_hair or female:
        for y in range(2, 7):
            F(0, y, sh(hair, 0.9)); F(7, y, sh(hair, 0.9))
    # ojos grandes: pestana oscura arriba, iris, brillo, parte baja mas clara
    lash = sh(hair, 0.35) if not female else (30, 20, 25)
    for ex in (1, 5):
        F(ex, 3, lash); F(ex + 1, 3, lash)
        F(ex, 4, sh(eye, 0.75)); F(ex + 1, 4, (250, 250, 255))
        F(ex, 5, sh(eye, 1.15)); F(ex + 1, 5, sh(eye, 0.95))
    if female:
        F(0, 3, lash); F(7, 3, lash)
        F(1, 6, (238, 150, 150)); F(6, 6, (238, 150, 150))
    # boca
    F(3, 7 if not spec.get('mask') else 7, sh(skin, 0.7)); F(4, 7, sh(skin, 0.75))
    if spec.get('mask'):
        for x in range(8):
            for y in (6, 7):
                F(x, y, sh(spec['mask'], rnd() * (0.85 if y == 7 else 1)))
        for y in (6, 7):
            s.px('head', 'right', 7 - 0, y, sh(spec['mask'], 0.8)); s.px('head', 'left', 0, y, sh(spec['mask'], 0.8))
    # capa del sombrero: pelo largo y vincha / capucha
    def hat(f, x, y, w, h):
        if spec.get('hood'):
            hood = spec['hood']
            if f == 'front':
                return sh(hood, 0.85) if (y == 0 or x in (0, 7)) else None
            if f == 'bottom':
                return None
            return sh(hood, rnd() * (0.9 if y > 4 else 1))
        if f == 'back' and long_hair:
            return sh(hair, rnd() * 0.95)
        if f in ('right', 'left') and long_hair and y >= 3:
            return sh(hair, rnd() * 0.9)
        if spec.get('band') and y == 1 and f in ('front', 'right', 'left', 'back'):
            return spec['band']
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
            return sh(belt, k * (1.2 if (f == 'front' and x in (3, 4) and y == 8) else 1))
        c = shirt
        if top and f in ('front', 'back') and (x <= 1 or x >= 6) and y <= 7:
            c = top  # chaleco abierto adelante
        if top and f in ('right', 'left') and y <= 7:
            c = top
        if spec.get('crop') and f == 'front' and y in (6, 7):
            c = skin  # top corto
        return sh(c, k * (0.88 if y == 0 and f == 'front' else 1))
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
        return sh(c, k * (0.9 if f == 'back' else 1))
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
            return sh(spec['skirt'], k * (0.9 if y == 4 else 1))
        return sh(pants, k * (0.9 if f in ('back',) else 1))
    s.fill('rleg', leg)
    s.fill('lleg', leg)
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
         belt=(50, 32, 20), pants=(70, 62, 52), boots=(54, 38, 26), bracer=(92, 62, 40), lace=(200, 180, 140)),
    dict(name='outlaw_m_archer', female=False, slim=False, skin=SKIN_L, hair=(150, 110, 60), eye=(70, 120, 60),
         fringe=[2, 2, 3, 2, 1, 2, 3, 2], hood=(64, 86, 48), shirt=(86, 110, 60), top=(110, 80, 50), vest_sleeves=True,
         belt=(60, 40, 24), pants=(92, 80, 60), boots=(70, 48, 30), strap=(60, 40, 22), glove=(90, 60, 36), cape=(64, 86, 48)),
    dict(name='outlaw_f_melee', female=True, slim=True, long=True, skin=SKIN_L, hair=(150, 30, 40), eye=(200, 60, 70),
         fringe=[3, 3, 2, 2, 3, 3, 2, 2], band=(30, 30, 34), shirt=(222, 214, 232), top=(60, 40, 34), crop=True, vneck=True,
         belt=(40, 28, 20), pants=(48, 44, 50), boots=(40, 30, 26), boot_h=6, bracer=(60, 40, 34), lace=(150, 30, 40)),
    dict(name='outlaw_f_archer', female=True, slim=True, long=True, skin=SKIN_M, hair=(40, 46, 60), eye=(90, 170, 120),
         fringe=[2, 3, 3, 2, 2, 3, 3, 2], hood=(70, 92, 56), shirt=(150, 170, 110), top=(96, 70, 46), vest_sleeves=True,
         belt=(60, 40, 24), pants=(80, 70, 56), boots=(64, 44, 28), strap=(70, 46, 26), glove=(96, 66, 40), skirt=(96, 70, 46)),
    # --- bestias (oso, perro, gato)
    dict(name='beast_bear_m', female=False, slim=False, skin=SKIN_T, hair=(96, 62, 36), eye=(150, 100, 40),
         fringe=[2, 2, 3, 2, 2, 3, 2, 2], shirt=(128, 58, 40), top=(96, 66, 38), vest_sleeves=True, fur=(130, 96, 60),
         belt=(70, 46, 26), pants=(96, 72, 48), boots=(70, 50, 34), wraps=(180, 160, 120), short_sleeve=True),
    dict(name='beast_bear_f', female=True, slim=True, long=True, skin=SKIN_T, hair=(110, 70, 40), eye=(170, 110, 40),
         fringe=[3, 2, 3, 2, 3, 2, 3, 2], shirt=(92, 108, 66), top=(112, 76, 44), fur=(140, 104, 66), short_sleeve=True,
         belt=(70, 46, 26), pants=(110, 84, 56), boots=(76, 54, 36), skirt=(120, 82, 48), wraps=(190, 170, 130)),
    dict(name='beast_dog_m', female=False, slim=False, skin=SKIN_M, hair=(190, 190, 196), eye=(70, 110, 190),
         fringe=[2, 3, 2, 2, 3, 2, 3, 2], shirt=(60, 84, 130), top=(140, 140, 150), vest_sleeves=True, fur=(220, 220, 225),
         belt=(50, 40, 34), pants=(70, 70, 80), boots=(46, 40, 40), bracer=(140, 140, 150)),
    dict(name='beast_dog_f', female=True, slim=True, long=True, skin=SKIN_L, hair=(216, 216, 222), eye=(90, 140, 210),
         fringe=[3, 3, 2, 3, 3, 2, 3, 3], shirt=(70, 96, 150), top=(160, 160, 170), fur=(235, 235, 240), crop=True,
         belt=(50, 40, 34), pants=(80, 80, 92), boots=(50, 44, 44), boot_h=6, skirt=(70, 96, 150)),
    dict(name='beast_cat_m', female=False, slim=False, skin=SKIN_L, hair=(220, 130, 50), eye=(90, 180, 70),
         fringe=[2, 3, 3, 2, 3, 3, 2, 2], shirt=(214, 176, 92), top=(170, 60, 40), vest_sleeves=True,
         belt=(80, 50, 30), pants=(90, 70, 56), boots=(70, 48, 32), short_sleeve=True, wraps=(240, 220, 190)),
    dict(name='beast_cat_f', female=True, slim=True, long=True, skin=SKIN_L, hair=(236, 150, 60), eye=(110, 200, 80),
         fringe=[3, 2, 3, 3, 2, 3, 2, 3], shirt=(70, 64, 80), top=(190, 70, 50), crop=True, vneck=True,
         belt=(80, 50, 30), pants=(100, 80, 64), boots=(80, 54, 36), skirt=(190, 70, 50), short_sleeve=True),
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

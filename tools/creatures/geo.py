"""Mini motor para armar criaturas de GeckoLib desde Python: huesos, cubos, texturas pintadas por material y
animaciones por ciclos. Coordenadas Bedrock: Y arriba, el frente mira a -Z, pixeles (1/16 de bloque)."""
import json, math, os, random
from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
ASSETS = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'remushoku')


def clamp(v):
    return max(0, min(255, int(round(v))))


def mul(c, k):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


FACE_SHADE = {'up': 1.08, 'down': 0.72, 'north': 1.0, 'south': 0.95, 'east': 0.9, 'west': 0.9}


# ------------------------------------------------------------------ materiales (pintores por pixel)
class Mat:
    """paint(face, x, y, w, h, rnd, cube) -> (r,g,b,a) ; glow(...) -> bool."""
    glow = False

    def color(self, face, x, y, w, h, rnd, cube):
        raise NotImplementedError

    def paint(self, face, x, y, w, h, rnd, cube):
        c = self.color(face, x, y, w, h, rnd, cube)
        if c is None:
            return None
        a = c[3] if len(c) > 3 else 255
        return mul(c[:3], FACE_SHADE[face]) + (a,)


class Fur(Mat):
    """Pelaje: lomo mas oscuro, panza clara, mechones verticales y rayas opcionales."""
    def __init__(self, base, dark=None, light=None, belly=None, stripes=None, spots=None, stripe_every=4):
        self.base, self.dark = hexc(base), hexc(dark) if dark else None
        self.light = hexc(light) if light else None
        self.belly = hexc(belly) if belly else None
        self.stripes = hexc(stripes) if stripes else None
        self.spots = hexc(spots) if spots else None
        self.every = stripe_every

    def color(self, face, x, y, w, h, rnd, cube):
        dark = self.dark or mul(self.base, 0.7)
        light = self.light or mul(self.base, 1.2)
        if face == 'up':
            c = mix(self.base, dark, 0.35)
        elif face == 'down':
            c = self.belly or mul(self.base, 0.8)
        else:
            t = y / max(1, h - 1)
            c = mix(mix(self.base, dark, 0.3), self.belly or self.base, t * 0.8) if self.belly else mix(self.base, dark, t * 0.4)
        k = 1 + (0.08 if (x + (y // 2)) % 3 == 0 else -0.05 if (x * 7 + y) % 5 == 0 else 0) + rnd.uniform(-0.05, 0.05)
        c = mul(c, k)
        if self.stripes and face in ('up', 'east', 'west') and (x + y // 3) % self.every == 0 and y < h * 0.7:
            c = mix(c, self.stripes, 0.75)
        if self.spots and rnd.random() < 0.07:
            c = mix(c, self.spots, 0.8)
        if light and rnd.random() < 0.06:
            c = mix(c, light, 0.5)
        return c


class Solid(Mat):
    def __init__(self, base, var=0.06, glow=False, edge=None):
        self.base, self.var, self.glow = hexc(base), var, glow
        self.edge = hexc(edge) if edge else None

    def color(self, face, x, y, w, h, rnd, cube):
        if self.edge and (x == 0 or y == 0 or x == w - 1 or y == h - 1):
            return self.edge
        return mul(self.base, 1 + rnd.uniform(-self.var, self.var))


class Scales(Mat):
    """Escamas en rombo con manchas grandes (anaconda) o lisas (cobra)."""
    def __init__(self, base, dark, belly, blotch=None, blotch_edge=None):
        self.base, self.dark, self.belly = hexc(base), hexc(dark), hexc(belly)
        self.blotch = hexc(blotch) if blotch else None
        self.bedge = hexc(blotch_edge) if blotch_edge else None

    def color(self, face, x, y, w, h, rnd, cube):
        if face == 'down':
            return mul(self.belly, 1.0 if y % 2 else 0.9)
        c = self.base
        if (x + y) % 4 == 0 or (x - y) % 4 == 0:
            c = mix(c, self.dark, 0.35)
        if self.blotch and face in ('up', 'east', 'west'):
            cx, cy = (x + cube['seed'] % 5) % 8, (y + cube['seed'] % 3) % 6
            d = (cx - 4) ** 2 / 9 + (cy - 3) ** 2 / 4
            if d < 1:
                c = self.blotch if d < 0.55 else (self.bedge or mul(self.blotch, 0.6))
        if face in ('east', 'west') and y >= h - 1:
            c = mix(c, self.belly, 0.6)
        return mul(c, 1 + rnd.uniform(-0.04, 0.04))


class Bark(Mat):
    def __init__(self, base, dark, moss=None, moss_amt=0.0):
        self.base, self.dark = hexc(base), hexc(dark)
        self.moss = hexc(moss) if moss else None
        self.moss_amt = moss_amt

    def color(self, face, x, y, w, h, rnd, cube):
        c = self.base
        if face == 'up' or face == 'down':
            r = math.hypot(x - w / 2, y - h / 2)
            return mix(hexc('b89a6a'), mul(self.base, 0.9), 0.4) if int(r) % 2 == 0 else mul(self.base, 1.1)
        if (x + cube['seed']) % 3 == 0:
            c = mix(c, self.dark, 0.7)
        if rnd.random() < 0.04:
            c = mul(self.dark, 0.8)
        if self.moss and rnd.random() < self.moss_amt * (1.5 if y < h / 2 else 0.6):
            c = mul(self.moss, rnd.uniform(0.85, 1.15))
        return mul(c, 1 + rnd.uniform(-0.06, 0.06))


class Leaves(Mat):
    def __init__(self, base, light, holes=0.12, fruit=None):
        self.base, self.light, self.holes = hexc(base), hexc(light), holes
        self.fruit = hexc(fruit) if fruit else None

    def color(self, face, x, y, w, h, rnd, cube):
        if rnd.random() < self.holes and face != 'up':
            return (0, 0, 0, 0)
        c = mix(self.base, self.light, rnd.random() * 0.6) if (x * 3 + y * 5) % 7 < 3 else mul(self.base, rnd.uniform(0.75, 1.0))
        if self.fruit and rnd.random() < 0.025:
            c = self.fruit
        return c


class Rock(Mat):
    def __init__(self, base, dark, moss=None, moss_top=0.0, crack=None):
        self.base, self.dark = hexc(base), hexc(dark)
        self.moss = hexc(moss) if moss else None
        self.moss_top = moss_top
        self.crack = hexc(crack) if crack else mul(self.dark, 0.7)

    def color(self, face, x, y, w, h, rnd, cube):
        c = mix(self.base, self.dark, (rnd.random() ** 2) * 0.45 + 0.1 * ((x // 3 + y // 2) % 2))
        if ((x * 2 + y * 3 + cube['seed']) % 19 == 0) or ((x - 2 * y + cube['seed']) % 23 == 0):
            c = self.crack
        if self.moss and ((face == 'up' and rnd.random() < self.moss_top) or (face != 'down' and y < 2 and rnd.random() < self.moss_top * 0.6)):
            c = mul(self.moss, rnd.uniform(0.8, 1.15))
        return c


class Crystal(Mat):
    glow = True

    def __init__(self, base, light, dark):
        self.base, self.light, self.dark = hexc(base), hexc(light), hexc(dark)

    def color(self, face, x, y, w, h, rnd, cube):
        if x == 0 or x == w - 1:
            return self.dark
        t = (x + y) / max(1, w + h)
        c = mix(self.light, self.base, t)
        if (x + y) % 5 == 0:
            c = mix(c, (255, 255, 255), 0.5)
        return c


class Membrane(Mat):
    def __init__(self, base, vein):
        self.base, self.vein = hexc(base), hexc(vein)

    def color(self, face, x, y, w, h, rnd, cube):
        if face in ('up', 'down'):
            if x % 5 == 0 or (x + y) % 9 == 0:
                return self.vein
            return mul(self.base, 1 + rnd.uniform(-0.05, 0.05) - y / max(1, h) * 0.15)
        return self.vein


# ------------------------------------------------------------------ modelo
class Model:
    def __init__(self, name, atlas=128):
        self.name, self.atlas = name, atlas
        self.bones, self.order, self.cubes = {}, [], []

    def bone(self, name, parent, pivot, rot=None):
        b = dict(name=name, pivot=[round(v, 3) for v in pivot])
        if parent:
            b['parent'] = parent
        if rot:
            b['rotation'] = [round(v, 3) for v in rot]
        self.bones[name] = b
        self.order.append(name)
        return name

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, pivot=None):
        c = dict(bone=bone, origin=list(origin), size=[max(0.01, s) for s in size], mat=mat, inflate=inflate, rot=rot,
                 pivot=pivot, seed=len(self.cubes) * 7 + 3)
        self.cubes.append(c)
        return c

    def box_between(self, bone, a, b, thick, mat, inflate=0.0):
        """Cubo largo de a hasta b (eje largo = Z local, girado con X/Y)."""
        d = [b[i] - a[i] for i in range(3)]
        L = math.sqrt(sum(v * v for v in d))
        yaw = math.degrees(math.atan2(-d[0], -d[2]))
        pitch = math.degrees(math.atan2(d[1], math.hypot(d[0], d[2])))
        mid = [(a[i] + b[i]) / 2 for i in range(3)]
        o = (mid[0] - thick[0] / 2, mid[1] - thick[1] / 2, mid[2] - L / 2)
        return self.cube(bone, o, (thick[0], thick[1], L), mat, inflate, rot=(pitch, yaw, 0), pivot=mid)

    def _uv_size(self, c):
        w, h, d = [max(1, math.ceil(s - 1e-6)) for s in c['size']]
        return 2 * (w + d), h + d, (w, h, d)

    def build(self, palette=None, seed=1):
        """Empaca las UV, pinta la textura (palette = materiales de reemplazo por nombre) y arma el geo."""
        items = sorted(self.cubes, key=lambda c: -self._uv_size(c)[1])
        x = y = row_h = 0
        A = self.atlas
        for c in items:
            uw, uh, _ = self._uv_size(c)
            if x + uw > A:
                x, y, row_h = 0, y + row_h, 0
            c['uv'] = (x, y)
            x += uw
            row_h = max(row_h, uh)
        H = 1
        while H < y + row_h:
            H *= 2
        H = max(H, 16)
        img = Image.new('RGBA', (A, H), (0, 0, 0, 0))
        glow = Image.new('RGBA', (A, H), (0, 0, 0, 0))
        any_glow = False
        rnd = random.Random(seed)
        for c in self.cubes:
            mat = c['mat']
            if palette and isinstance(mat, str):
                mat = palette[mat]
            elif isinstance(mat, str):
                raise ValueError('material sin paleta: ' + mat)
            u, v = c['uv']
            _, _, (w, h, d) = self._uv_size(c)
            faces = {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                     'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}
            for f, (fu, fv, fw, fh) in faces.items():
                for yy in range(fh):
                    for xx in range(fw):
                        px = mat.paint(f, xx, yy, fw, fh, rnd, c)
                        if px is None:
                            continue
                        img.putpixel((fu + xx, fv + yy), px)
                        if mat.glow and px[3] > 0:
                            glow.putpixel((fu + xx, fv + yy), px)
                            any_glow = True
        bones = []
        for n in self.order:
            b = dict(self.bones[n])
            cs = []
            for c in self.cubes:
                if c['bone'] != n:
                    continue
                cc = dict(origin=[round(v, 3) for v in c['origin']], size=[round(v, 3) for v in c['size']], uv=list(c['uv']))
                if c['inflate']:
                    cc['inflate'] = c['inflate']
                if c['rot']:
                    cc['rotation'] = [round(v, 3) for v in c['rot']]
                    cc['pivot'] = [round(v, 3) for v in (c['pivot'] or [c['origin'][i] + c['size'][i] / 2 for i in range(3)])]
                cs.append(cc)
            if cs:
                b['cubes'] = cs
            bones.append(b)
        geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': dict(identifier='geometry.' + self.name, texture_width=A, texture_height=H,
                                visible_bounds_width=8, visible_bounds_height=6, visible_bounds_offset=[0, 2, 0]),
            'bones': bones}]}
        return geo, img, (glow if any_glow else None)

    def save(self, textures, anims):
        """textures: {nombre_textura: palette}. Guarda geo, animaciones y las texturas (con glowmask si brillan)."""
        geo = None
        for tname, pal in textures.items():
            geo, img, glow = self.build(pal, seed=hash(tname) & 0xffff)
            img.save(os.path.join(ASSETS, 'textures', 'entities', tname + '.png'))
            gm = os.path.join(ASSETS, 'textures', 'entities', tname + '_glowmask.png')
            if glow:
                glow.save(gm)
            elif os.path.exists(gm):
                os.remove(gm)
        with open(os.path.join(ASSETS, 'geo', self.name + '.geo.json'), 'w') as f:
            json.dump(geo, f)
        with open(os.path.join(ASSETS, 'animations', self.name + '.animation.json'), 'w') as f:
            json.dump({'format_version': '1.8.0', 'animations': anims}, f)


# ------------------------------------------------------------------ animaciones
def cycle(length, steps, fn, loop=True):
    """fn(t) con t en [0,1] -> {hueso: {'rotation': [x,y,z], 'position': [x,y,z]}}. Devuelve la animacion."""
    bones = {}
    for i in range(steps + 1):
        t = i / steps
        key = '%.4f' % (t * length)
        for bone, ch in fn(t).items():
            for kind, vec in ch.items():
                bones.setdefault(bone, {}).setdefault(kind, {})[key] = {
                    'vector': [round(v, 3) for v in vec], 'lerp_mode': 'catmullrom'}
    out = {'animation_length': length, 'bones': bones}
    out['loop'] = True if loop else False
    return out


def S(t, phase=0.0, k=1.0):
    return math.sin(2 * math.pi * (t * k + phase))


def C(t, phase=0.0, k=1.0):
    return math.cos(2 * math.pi * (t * k + phase))


def pulse(t, a, b):
    """0 fuera de [a,b], sube y baja suave adentro."""
    if t <= a or t >= b:
        return 0.0
    return math.sin(math.pi * (t - a) / (b - a))

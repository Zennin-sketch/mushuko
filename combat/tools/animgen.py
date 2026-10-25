"""
Generador de animaciones del addon de combate (formato Emotecraft v3, el que leen Player Animator y Better Combat).

Las animaciones se describen con poses "humanas" (hacia donde apunta el brazo, hacia donde apunta la hoja,
cuanto gira el torso...) y aca se convierten a las rotaciones exactas de cada parte del modelo de Minecraft,
muestreadas tick a tick. Asi los brazos siguen al torso cuando gira y no hay que pelear con angulos de Euler.

Ejes intuitivos: (derecha, arriba, adelante) desde el punto de vista del personaje. Angulos en grados.
Uso: python3 animgen.py <carpeta_salida>   (escribe un .json por animacion)
"""
import json, math, os, sys

D = math.pi / 180

# ---------------------------------------------------------------- matematicas basicas

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

def mat_vec(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]

def rx(a):
    c, s = math.cos(a), math.sin(a); return [[1, 0, 0], [0, c, -s], [0, s, c]]

def ry(a):
    c, s = math.cos(a), math.sin(a); return [[c, 0, s], [0, 1, 0], [-s, 0, c]]

def rz(a):
    c, s = math.cos(a), math.sin(a); return [[c, -s, 0], [s, c, 0], [0, 0, 1]]

def zyx(x, y, z):
    """Rotacion de ModelPart de Minecraft: Rz * Ry * Rx (radianes)."""
    return mat_mul(rz(z), mat_mul(ry(y), rx(x)))

def euler_zyx(m):
    """Inversa de zyx(): devuelve (x, y, z)."""
    sy = max(-1.0, min(1.0, -m[2][0]))
    y = math.asin(sy)
    if abs(sy) < 0.99999:
        x = math.atan2(m[2][1], m[2][2])
        z = math.atan2(m[1][0], m[0][0])
    else:  # bloqueo de cardan
        x = math.atan2(-m[1][2], m[1][1])
        z = 0.0
    return x, y, z

def norm(v):
    l = math.sqrt(sum(c * c for c in v)) or 1.0
    return [c / l for c in v]

def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]

def dot(a, b):
    return sum(x * y for x, y in zip(a, b))

def to_model(v):
    """(derecha, arriba, adelante) -> espacio del modelo de Minecraft (x = izquierda, y = abajo, z = atras)."""
    return [-v[0], -v[1], -v[2]]

def frame(axis_down, front):
    """Rotacion que lleva el eje del miembro (0,1,0) a axis_down y su frente (0,0,-1) a front (ortogonalizado)."""
    yv = norm(axis_down)
    f = [c - dot(front, yv) * a for c, a in zip(front, yv)]
    if math.sqrt(dot(f, f)) < 1e-4:  # frente paralelo al brazo: elegir cualquiera
        f = [0, 0, -1] if abs(yv[2]) < 0.9 else [0, 1, 0]
        f = [c - dot(f, yv) * a for c, a in zip(f, yv)]
    zneg = norm(f)
    zv = [-c for c in zneg]
    xv = cross(yv, zv)
    return [[xv[0], yv[0], zv[0]], [xv[1], yv[1], zv[1]], [xv[2], yv[2], zv[2]]]

# ---------------------------------------------------------------- curvas

EASE = {
    'linear': lambda t: t,
    'in': lambda t: t * t * t,
    'out': lambda t: 1 - (1 - t) ** 3,
    'inout': lambda t: 4 * t * t * t if t < .5 else 1 - (-2 * t + 2) ** 3 / 2,
    'sine': lambda t: -(math.cos(math.pi * t) - 1) / 2,
    'back': lambda t: 1 + 2.70158 * (t - 1) ** 3 + 1.70158 * (t - 1) ** 2,  # se pasa un poco y vuelve
    'snap': lambda t: 1 - (1 - t) ** 5,  # golpe muy rapido al principio
    'hold': lambda t: 0.0,
}

def lerp(a, b, t):
    if isinstance(a, (list, tuple)):
        return [lerp(x, y, t) for x, y in zip(a, b)]
    return a + (b - a) * t

# ---------------------------------------------------------------- pose

NEUTRAL = {
    'root': [0, 0, 0, 0, 0, 0, 1],       # desplazamiento (der, arriba, adelante) en bloques, inclinacion adelante, giro der, ladeo der
    'torso': [0, 0, 0],                  # giro der, inclinacion adelante, ladeo der
    'head': [0, 0, 0],                   # mirar der, mirar abajo, ladeo
    'rarm': [[5, -12, 0], [0, 0, 1]],    # posicion de la mano (px desde el cuello, marco del torso) y direccion de la hoja
    'larm': [[-5, -12, 0], [0, 0, 1]],
    'rleg': [[0, -1, 0], 0],             # direccion de la pierna, rodilla
    'lleg': [[0, -1, 0], 0],
    'ritem': [0, 0, 0, 0, 0, 0],         # rot x/y/z (grados) y desplazamiento (px) del objeto en la mano derecha
    'litem': [0, 0, 0, 0, 0, 0],
}

def blend(a, b, t):
    out = {}
    for k in NEUTRAL:
        va, vb = a[k], b[k]
        if k in ('rarm', 'larm'):
            out[k] = [lerp(va[0], vb[0], t), norm(lerp(va[1], vb[1], t))]
        elif k in ('rleg', 'lleg'):
            out[k] = [norm(lerp(va[0], vb[0], t)), lerp(va[1], vb[1], t)]
        else:
            out[k] = lerp(va, vb, t)
    return out

def solve(p):
    """Pose intuitiva -> valores por parte del modelo (lo que va en el JSON)."""
    o = {}
    r = p['root']
    plant = r[6] if len(r) > 6 else 1.0
    # El cuerpo entero gira alrededor de un punto a 0.7 bloques del suelo: se compensa para que los pies no se muevan.
    lean, tilt = r[3] * D * plant, r[5] * D * plant
    fwd = r[2] + 0.7 * math.sin(lean)
    right = r[0] + 0.7 * math.sin(tilt)
    up = r[1] - 0.7 * (1 - math.cos(lean)) - 0.7 * (1 - math.cos(tilt))
    o['body'] = dict(x=right, y=up, z=-fwd, pitch=-r[3] * D, yaw=-r[4] * D, roll=-r[5] * D)
    t = p['torso']
    R_t = zyx(t[1] * D, t[0] * D, -t[2] * D)
    tx, ty, tz = euler_zyx(R_t)
    o['torso'] = dict(x=0, y=0, z=0, pitch=tx, yaw=ty, roll=tz)
    h = p['head']
    o['head'] = dict(pitch=h[1] * D, yaw=h[0] * D, roll=-h[2] * D)
    wrists = {}
    for key, part, sx in (('rarm', 'rightArm', -5), ('larm', 'leftArm', 5)):
        hand_p, blade = p[key]
        o[part], wrists[part] = arm_ik(R_t, sx, hand_p, blade)
    for key, part, sx in (('rleg', 'rightLeg', -1.9), ('lleg', 'leftLeg', 1.9)):
        d, knee = p[key]
        R = mat_mul(frame(to_model(d), [0, 0, -1]), rx(-knee * D / 2))  # rodilla: el muslo va adelante, la pierna atras
        x, y, z = euler_zyx(R)
        o[part] = dict(x=sx, y=12, z=0.1, pitch=x, yaw=y, roll=z, bend=knee * D, axis=0)
    for key, part, arm in (('ritem', 'rightItem', 'rightArm'), ('litem', 'leftItem', 'leftArm')):
        i = p[key]
        w = wrists[arm]
        o[part] = dict(pitch=w[0] + i[0] * D, yaw=w[1] + i[1] * D, roll=w[2] + i[2] * D, x=i[3], y=i[4], z=i[5])
    return o

UPPER, FORE = 4.0, 6.0  # pivote del brazo -> codo (y=4 en el cubo), codo -> mano

def arm_ik(R_t, sx, hand_p, blade):
    """Hombro -> codo -> mano. El codo apunta abajo/afuera/atras como un brazo real; la hoja sale del antebrazo."""
    side = -1 if sx < 0 else 1           # en espacio del modelo: derecha = -x
    S = [sx, 2.0, 0.0]
    H = to_model(hand_p)
    H = [H[0], H[1], H[2]]
    u = [h - s_ for h, s_ in zip(H, S)]
    d = math.sqrt(dot(u, u)) or 1e-6
    u = [c / d for c in u]
    pole = norm([side * 0.55, 1.0, 0.35])  # afuera, abajo, un poco atras
    pp = [c - dot(pole, u) * a for c, a in zip(pole, u)]
    if math.sqrt(dot(pp, pp)) < 1e-4:
        pp = [0, 0, 1]
    pp = norm(pp)
    if d >= UPPER + FORE - 1e-3:
        ua = fore = u
        e = 0.0
    else:
        d = max(d, abs(FORE - UPPER) + 0.5)
        ca = (UPPER * UPPER + d * d - FORE * FORE) / (2 * UPPER * d)
        al = math.acos(max(-1.0, min(1.0, ca)))
        E = [s_ + UPPER * (math.cos(al) * a + math.sin(al) * b) for s_, a, b in zip(S, u, pp)]
        ua = norm([e_ - s_ for e_, s_ in zip(E, S)])
        fore = norm([s_ + c * d - e_ for s_, c, e_ in zip(S, u, E)])
        e = math.acos(max(-1.0, min(1.0, dot(ua, fore))))
    # todo en el marco del torso -> modelo
    ua, fore = mat_vec(R_t, ua), mat_vec(R_t, fore)
    bm = mat_vec(R_t, norm(to_model(blade)))
    Lf = frame(fore, bm)                      # antebrazo: hoja exacta
    if e > 1e-4:
        k = norm(cross(fore, ua))             # eje que lleva el antebrazo al brazo
        U = mat_mul(axis_rot(k, e), Lf)
        kl = mat_vec([list(r) for r in zip(*U)], k)  # eje en coordenadas locales del brazo
        axis = -math.atan2(kl[2], kl[0])
        bend = -e
    else:
        U, axis, bend = Lf, 0.0, 0.0
    x, y, z = euler_zyx(U)
    pos = mat_vec(R_t, [sx, 2, 0])
    # Muneca: el objeto sale perpendicular al antebrazo; si la hoja pedida no lo es, se gira el objeto en la mano.
    bl = mat_vec([list(r) for r in zip(*Lf)], bm)  # hoja en coordenadas del antebrazo
    n = [0.0, 0.0, -1.0]
    ang = math.acos(max(-1.0, min(1.0, dot(n, bl))))
    wrist = (0.0, 0.0, 0.0)
    if ang > 1e-3:
        k = norm(cross(n, bl))
        Q = axis_rot(k, ang)
        Qi = mat_mul(mat_mul(transpose(ITEM_M), Q), ITEM_M)
        wrist = euler_zyx(Qi)
    return dict(x=pos[0], y=pos[1], z=pos[2], pitch=x, yaw=y, roll=z, bend=bend, axis=axis), wrist

def transpose(m):
    return [list(r) for r in zip(*m)]

# del marco del objeto en la mano al marco del antebrazo: XP(-90) y YP(180) de ItemInHandLayer
ITEM_M = mat_mul(rx(-math.pi / 2), ry(math.pi))

def _item_m():
    return mat_mul(rx(-math.pi / 2), ry(math.pi))

def axis_rot(k, a):
    x, y, z = k; c, s = math.cos(a), math.sin(a); t = 1 - c
    return [[t*x*x + c, t*x*y - s*z, t*x*z + s*y], [t*x*y + s*z, t*y*y + c, t*y*z - s*x], [t*x*z - s*y, t*y*z + s*x, t*z*z + c]]

# ---------------------------------------------------------------- animacion

class Anim:
    def __init__(self, name, desc, end, begin=0, stop=None, loop=False, ret=0, parts=None, start=None):
        self.name, self.desc = name, desc
        self.begin, self.end = begin, end
        self.stop = stop if stop is not None else end
        self.loop, self.ret = loop, ret
        self.parts = parts  # None = todas
        self.keys = []      # (tick, pose completa, ease)
        base = dict(NEUTRAL); base.update(start or {})
        self.cur = json.loads(json.dumps(base))

    def key(self, tick, ease='inout', **changes):
        p = json.loads(json.dumps(self.cur))
        for k, v in changes.items():
            if k in ('rarm', 'larm', 'rleg', 'lleg'):
                base = p[k]
                v = list(v)
                for i, x in enumerate(v):
                    if x is not None:
                        base[i] = x
                p[k] = base
            else:
                p[k] = list(v)
        self.cur = p
        self.keys.append((tick, p, ease))
        return self

    def pose_at(self, tick):
        ks = self.keys
        if tick <= ks[0][0]:
            return ks[0][1]
        for (t0, p0, _), (t1, p1, e) in zip(ks, ks[1:]):
            if t0 <= tick <= t1:
                f = 0 if t1 == t0 else (tick - t0) / (t1 - t0)
                return blend(p0, p1, EASE[e](f))
        return ks[-1][1]

    def build(self):
        first = self.keys[0][0]
        last = self.keys[-1][0]
        series = {}
        prev = {}
        for tick in range(first, last + 1):
            vals = solve(self.pose_at(tick))
            for part, axes in vals.items():
                if self.parts and part not in self.parts:
                    continue
                for ax, v in axes.items():
                    k = (part, ax)
                    if ax in ('pitch', 'yaw', 'roll', 'axis') and k in prev:  # continuidad de angulos
                        while v - prev[k] > math.pi: v -= 2 * math.pi
                        while v - prev[k] < -math.pi: v += 2 * math.pi
                    prev[k] = v
                    series.setdefault(k, []).append((tick, v))
        by_tick = {}
        for (part, ax), pts in series.items():
            # se quitan los puntos que la interpolacion lineal ya reproduce
            keep = [pts[0]]
            for i in range(1, len(pts) - 1):
                (t0, v0), (t1, v1), (t2, v2) = keep[-1], pts[i], pts[i + 1]
                if abs(v0 + (v2 - v0) * (t1 - t0) / (t2 - t0) - v1) > 0.002:
                    keep.append(pts[i])
            if len(pts) > 1:
                keep.append(pts[-1])
            for t, v in keep:
                by_tick.setdefault(t, {}).setdefault(part, {})[ax] = round(v, 4)
        moves = []
        for t in sorted(by_tick):
            m = {'tick': t, 'easing': 'LINEAR'}
            m.update(by_tick[t])
            moves.append(m)
        return {
            'version': 3,
            'name': self.name,
            'author': 'Zennin / Mushoku Combat',
            'description': self.desc,
            'emote': {
                'beginTick': self.begin, 'endTick': self.end, 'stopTick': self.stop,
                'isLoop': 'true' if self.loop else 'false', 'returnTick': self.ret,
                'degrees': False, 'moves': moves,
            },
        }

ANIMS = []

def anim(*a, **k):
    x = Anim(*a, **k)
    ANIMS.append(x)
    return x

def write(out):
    os.makedirs(out, exist_ok=True)
    for a in ANIMS:
        with open(os.path.join(out, a.name + '.json'), 'w') as f:
            json.dump(a.build(), f, separators=(',', ':'))
    print(len(ANIMS), 'animaciones ->', out)

if __name__ == '__main__':
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import animgen as G  # mismo modulo que importa anims.py
    import anims  # noqa: F401  (registra todas las animaciones)
    G.write(sys.argv[1])

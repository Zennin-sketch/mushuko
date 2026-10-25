"""
Generador de animaciones del addon de combate (formato Emotecraft v3: lo leen Player Animator y Better Combat).

Las animaciones se describen con poses "humanas" (donde esta la mano, hacia donde apunta la hoja, cuanto gira el
torso...) y aca se convierten a las rotaciones exactas de cada parte del modelo, muestreadas tick a tick:

* Curvas suaves: entre keyframes se usa una curva cubica continua (sin frenadas en cada keyframe).
  Un keyframe 'stop' frena ahi (pose extrema, golpe sostenido); 'smooth' pasa de largo con velocidad.
* Inercia: cabeza, mano libre y piernas siguen a su objetivo con un resorte amortiguado, asi llegan un poco tarde
  y se pasan apenas (accion superpuesta / contrapeso).
* Angulos sanos: de las dos rotaciones de Euler equivalentes se elige el camino mas corto y cercano al reposo, y los
  giros completos se "rebasan" con keyframes duplicados (turn) para que al terminar no de una vuelta de mas.

Ejes intuitivos: (derecha, arriba, adelante) desde el personaje. Angulos en grados.
Uso: python3 animgen.py <carpeta_salida>
"""
import json, math, os, sys

D = math.pi / 180
TAU = 2 * math.pi

# ---------------------------------------------------------------- matematicas

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

def mat_vec(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]

def transpose(m):
    return [list(r) for r in zip(*m)]

def rx(a):
    c, s = math.cos(a), math.sin(a); return [[1, 0, 0], [0, c, -s], [0, s, c]]

def ry(a):
    c, s = math.cos(a), math.sin(a); return [[c, 0, s], [0, 1, 0], [-s, 0, c]]

def rz(a):
    c, s = math.cos(a), math.sin(a); return [[c, -s, 0], [s, c, 0], [0, 0, 1]]

def zyx(x, y, z):
    """Rotacion de ModelPart: Rz * Ry * Rx."""
    return mat_mul(rz(z), mat_mul(ry(y), rx(x)))

def wrap(a):
    return (a + math.pi) % TAU - math.pi

def euler_candidates(m):
    """Las dos soluciones (x, y, z) de R = Rz Ry Rx."""
    sy = max(-1.0, min(1.0, -m[2][0]))
    y = math.asin(sy)
    if abs(sy) < 0.99999:
        x = math.atan2(m[2][1], m[2][2])
        z = math.atan2(m[1][0], m[0][0])
    else:
        x = math.atan2(-m[1][2], m[1][1]); z = 0.0
    a = (x, y, z)
    b = (wrap(x + math.pi), wrap(math.pi - y), wrap(z + math.pi))
    return [a, b]

def norm(v):
    l = math.sqrt(sum(c * c for c in v)) or 1.0
    return [c / l for c in v]

def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]

def dot(a, b):
    return sum(x * y for x, y in zip(a, b))

def to_model(v):
    """(derecha, arriba, adelante) -> espacio del modelo (x = izquierda, y = abajo, z = atras)."""
    return [-v[0], -v[1], -v[2]]

def frame(axis_down, front):
    """Rotacion que lleva (0,1,0) a axis_down y (0,0,-1) a front (ortogonalizado)."""
    yv = norm(axis_down)
    f = [c - dot(front, yv) * a for c, a in zip(front, yv)]
    if math.sqrt(dot(f, f)) < 1e-4:
        f = [0, 0, -1] if abs(yv[2]) < 0.9 else [0, 1, 0]
        f = [c - dot(f, yv) * a for c, a in zip(f, yv)]
    zneg = norm(f)
    zv = [-c for c in zneg]
    xv = cross(yv, zv)
    return [[xv[0], yv[0], zv[0]], [xv[1], yv[1], zv[1]], [xv[2], yv[2], zv[2]]]

def axis_rot(k, a):
    x, y, z = k; c, s = math.cos(a), math.sin(a); t = 1 - c
    return [[t*x*x + c, t*x*y - s*z, t*x*z + s*y], [t*x*y + s*z, t*y*y + c, t*y*z - s*x], [t*x*z - s*y, t*y*z + s*x, t*z*z + c]]

# del marco del objeto en la mano al del antebrazo: XP(-90) y YP(180) de ItemInHandLayer
ITEM_M = mat_mul(rx(-math.pi / 2), ry(math.pi))

# ---------------------------------------------------------------- pose

NEUTRAL = {
    'root': [0, 0, 0, 0, 0, 0, 1],       # (der, arriba, adelante) en bloques, inclinacion adelante, giro der, ladeo der, pies plantados
    'torso': [0, 0, 0],                  # giro der, inclinacion adelante, ladeo der
    'head': [0, 0, 0],                   # mirar der, mirar abajo, ladeo
    'rarm': [[5, -12, 0], [0, 0, 1]],    # mano (px desde el cuello, marco del torso) y direccion de la hoja
    'larm': [[-5, -12, 0], [0, 0, 1]],
    'rleg': [[0, -1, 0], 0],             # direccion de la pierna, rodilla
    'lleg': [[0, -1, 0], 0],
    'ritem': [0, 0, 0, 0, 0, 0],         # ajuste extra del objeto en mano (grados, px)
    'litem': [0, 0, 0, 0, 0, 0],
    'curl': [0],                         # columna enrollada hacia adelante (grados), dobla por la cintura
    'grip': [0, 0],                      # agarre a dos manos: peso (0..1) y distancia de la mano izquierda sobre el mango
}
ORDER = list(NEUTRAL)

def flatten(p):
    out = []
    for k in ORDER:
        v = p[k]
        if k in ('rarm', 'larm'):
            out += list(v[0]) + list(v[1])
        elif k in ('rleg', 'lleg'):
            out += list(v[0]) + [v[1]]
        else:
            out += list(v)
    return out

def unflatten(f):
    p, i = {}, 0
    for k in ORDER:
        if k in ('rarm', 'larm'):
            p[k] = [f[i:i + 3], norm(f[i + 3:i + 6])]; i += 6
        elif k in ('rleg', 'lleg'):
            p[k] = [norm(f[i:i + 3]), f[i + 3]]; i += 4
        else:
            n = len(NEUTRAL[k]); p[k] = f[i:i + n]; i += n
    return p

def slice_of(key):
    """Indices de una parte en el vector plano."""
    i = 0
    for k in ORDER:
        n = 6 if k in ('rarm', 'larm') else 4 if k in ('rleg', 'lleg') else len(NEUTRAL[k])
        if k == key:
            return range(i, i + n)
        i += n
    raise KeyError(key)

UPPER, FORE = 4.0, 6.0  # pivote del brazo -> codo (y=4 en el cubo), codo -> mano

def arm_ik(R_t, sx, hand_p, blade, prev_front=None):
    """Hombro -> codo -> mano con el codo hacia abajo/afuera/atras. Devuelve (matriz del brazo, bend, axis, muneca)."""
    side = -1 if sx < 0 else 1
    S = [sx, 2.0, 0.0]
    H = to_model(hand_p)
    u = [h - s_ for h, s_ in zip(H, S)]
    d = math.sqrt(dot(u, u)) or 1e-6
    u = [c / d for c in u]
    pole = norm([side * 0.55, 1.0, 0.35])
    pp = [c - dot(pole, u) * a for c, a in zip(pole, u)]
    if math.sqrt(dot(pp, pp)) < 1e-4:
        pp = [0, 0, 1]
    pp = norm(pp)
    if d >= UPPER + FORE - 1e-3:
        ua = fore = u; e = 0.0
    else:
        d = max(d, abs(FORE - UPPER) + 0.5)
        ca = (UPPER * UPPER + d * d - FORE * FORE) / (2 * UPPER * d)
        al = math.acos(max(-1.0, min(1.0, ca)))
        E = [s_ + UPPER * (math.cos(al) * a + math.sin(al) * b) for s_, a, b in zip(S, u, pp)]
        ua = norm([e_ - s_ for e_, s_ in zip(E, S)])
        fore = norm([s_ + c * d - e_ for s_, c, e_ in zip(S, u, E)])
        e = math.acos(max(-1.0, min(1.0, dot(ua, fore))))
    ua, fore = mat_vec(R_t, ua), mat_vec(R_t, fore)
    bm = mat_vec(R_t, norm(to_model(blade)))
    # Giro del antebrazo (el "rolido" del arma sobre su eje): se elige el que minimiza a la vez cuanto se tuerce
    # el arma respecto del puno y cuanto gira respecto del tick anterior. Asi no hay vueltas bruscas.
    perp = [c - dot(bm, fore) * a for c, a in zip(bm, fore)]
    mag = math.sqrt(dot(perp, perp))
    pn = norm(perp) if mag > 1e-6 else None
    pf = None
    if prev_front is not None:
        pf = [c - dot(prev_front, fore) * a for c, a in zip(prev_front, fore)]
        pf = norm(pf) if math.sqrt(dot(pf, pf)) > 1e-4 else None
    if pn is None:
        pn = pf or frame(fore, [0, 0, -1])[0:3] and mat_vec(frame(fore, [0, 0, -1]), [0, 0, -1])
    front = pn
    if pf is not None:
        side = cross(fore, pn)
        best = None
        for k in range(48):
            th = k * TAU / 48
            f = [pn[j] * math.cos(th) + side[j] * math.sin(th) for j in range(3)]
            wrist_a = math.acos(max(-1.0, min(1.0, dot(f, bm))))
            twist_a = math.acos(max(-1.0, min(1.0, dot(f, pf))))
            cost = wrist_a ** 2 + 0.7 * twist_a ** 2
            if best is None or cost < best[0]:
                best = (cost, f)
        front = best[1]
    Lf = frame(fore, front if math.sqrt(dot(front, front)) > 1e-6 else bm)
    if e > 1e-4:
        k = norm(cross(fore, ua))
        U = mat_mul(axis_rot(k, e), Lf)
        kl = mat_vec(transpose(U), k)
        axis = -math.atan2(kl[2], kl[0])
        bend = -e
    else:
        U, axis, bend = Lf, None, 0.0
    front_out = mat_vec(Lf, [0.0, 0.0, -1.0])
    # muneca: el objeto sale perpendicular al antebrazo; si la hoja pedida no lo es, se gira el objeto en la mano
    bl = mat_vec(transpose(Lf), bm)
    n = [0.0, 0.0, -1.0]
    ang = math.acos(max(-1.0, min(1.0, dot(n, bl))))
    Qi = None
    if ang > 1e-3:
        Q = axis_rot(norm(cross(n, bl)), ang)
        Qi = mat_mul(mat_mul(transpose(ITEM_M), Q), ITEM_M)
    arm_ik.front = front_out
    return U, bend, axis, Qi

def solve(p, prev=None):
    """Pose intuitiva -> por parte: valores escalares y matrices de rotacion (se pasan a Euler despues).
    prev: resultado del tick anterior (para que el giro del antebrazo sea continuo)."""
    o = {}
    w, spread = p['grip']
    if w > 1e-4:  # la mano izquierda sigue al mango aunque la hoja cambie (correccion de colision)
        rp, b = p['rarm']
        hand = [rp[i] - b[i] * spread for i in range(3)]
        lp, lb = p['larm']
        p = dict(p)
        p['larm'] = [[lp[i] + (hand[i] - lp[i]) * w for i in range(3)], norm([lb[i] + (b[i] - lb[i]) * w for i in range(3)])]
    r = p['root']
    plant = r[6]
    lean, tilt = r[3] * D * plant, r[5] * D * plant
    fwd = r[2] + 0.7 * math.sin(lean)
    right = r[0] + 0.7 * math.sin(tilt)
    up = r[1] - 0.7 * (1 - math.cos(lean)) - 0.7 * (1 - math.cos(tilt))
    rot = (-r[3] * D, -r[4] * D, -r[5] * D)
    if plant < 1:
        # Rolls: girar alrededor del centro del cuerpo encogido (0.35 bloques arriba del pivote de la cadera)
        c = [0.0, 0.35 * (1 - plant), 0.0]
        Rc = zyx(*rot)
        rc = mat_vec(Rc, c)
        right += c[0] - rc[0]; up += c[1] - rc[1]; fwd -= c[2] - rc[2]
    o['body'] = dict(vals=dict(x=right, y=up, z=-fwd), rot=rot)
    t = p['torso']
    R_t = zyx(t[1] * D, t[0] * D, -t[2] * D)
    o['torso'] = dict(vals=dict(x=0, y=0, z=0), R=R_t)
    h = p['head']
    o['head'] = dict(vals={}, rot=(h[1] * D, h[0] * D, -h[2] * D))
    wrist = {}
    for key, part, sx in (('rarm', 'rightArm', -5), ('larm', 'leftArm', 5)):
        hand_p, blade = p[key]
        pf = prev[part].get('front') if prev else None
        U, bend, axis, Qi = arm_ik(R_t, sx, hand_p, blade, pf)
        pos = mat_vec(R_t, [sx, 2, 0])
        o[part] = dict(vals=dict(x=pos[0], y=pos[1], z=pos[2], bend=bend), R=U, axis=axis, front=arm_ik.front)
        wrist[part] = Qi
    curl = p['curl'][0] * D
    if abs(curl) > 1e-5:
        # Se dobla la malla del torso y se giran cabeza y brazos alrededor de la cintura (0, 6, 0)
        Rc = rx(curl)
        W = [0.0, 6.0, 0.0]
        o['torso']['vals']['bend'] = curl
        o['torso']['axis'] = 0.0
        for part in ('head', 'rightArm', 'leftArm'):
            d = o[part]
            if part == 'head':
                d['R'] = mat_mul(Rc, zyx(*d.pop('rot')))
                pos = [0.0, 0.0, 0.0]
            else:
                d['R'] = mat_mul(Rc, d['R'])
                pos = [d['vals']['x'], d['vals']['y'], d['vals']['z']]
            q = mat_vec(Rc, [pos[i] - W[i] for i in range(3)])
            d['vals']['x'], d['vals']['y'], d['vals']['z'] = q[0] + W[0], q[1] + W[1], q[2] + W[2]
    else:
        o['torso']['vals']['bend'] = 0.0
        o['torso']['axis'] = 0.0
        h = o['head'].pop('rot')
        o['head']['R'] = zyx(*h)
        o['head']['vals'].update(x=0.0, y=0.0, z=0.0)
    for key, part, sx in (('rleg', 'rightLeg', -1.9), ('lleg', 'leftLeg', 1.9)):
        d, knee = p[key]
        R = mat_mul(frame(to_model(d), [0, 0, -1]), rx(-knee * D / 2))  # el muslo va adelante y la pierna atras
        o[part] = dict(vals=dict(x=sx, y=12, z=0.1, bend=knee * D), R=R, axis=0.0)
    for key, part, arm in (('ritem', 'rightItem', 'rightArm'), ('litem', 'leftItem', 'leftArm')):
        i = p[key]
        R = zyx(i[0] * D, i[1] * D, i[2] * D)
        if wrist[arm] is not None:
            R = mat_mul(wrist[arm], R)
        o[part] = dict(vals=dict(x=i[3], y=i[4], z=i[5]), R=R)
    return o

# ---------------------------------------------------------------- curvas y resortes

def hermite(ks, t):
    """Curva cubica por keyframes (tick, vector, modo). 'stop' = velocidad cero en ese keyframe."""
    if t <= ks[0][0]:
        return list(ks[0][1])
    if t >= ks[-1][0]:
        return list(ks[-1][1])
    i = max(j for j in range(len(ks) - 1) if ks[j][0] <= t)
    (t0, p0, m0), (t1, p1, m1) = ks[i], ks[i + 1]
    def tangent(j):
        tj, pj, mj = ks[j]
        if mj == 'stop' or j == 0 or j == len(ks) - 1:
            return [0.0] * len(pj)
        ta, pa, _ = ks[j - 1]; tb, pb, _ = ks[j + 1]
        return [(b - a) / (tb - ta) for a, b in zip(pa, pb)]
    v0, v1 = tangent(i), tangent(i + 1)
    h = t1 - t0
    s = (t - t0) / h
    h00, h10, h01, h11 = 2*s**3 - 3*s**2 + 1, s**3 - 2*s**2 + s, -2*s**3 + 3*s**2, s**3 - s**2
    return [h00 * a + h10 * h * va + h01 * b + h11 * h * vb for a, b, va, vb in zip(p0, p1, v0, v1)]

def spring(series, idx, omega, zeta=0.7, sub=8):
    """Resorte amortiguado sobre las componentes idx: siguen al objetivo con algo de retraso y rebote."""
    x = [series[0][i] for i in idx]
    v = [0.0] * len(idx)
    out = [list(series[0])]
    dt = 1.0 / sub
    for n in range(1, len(series)):
        for s_ in range(sub):
            f = (s_ + 1) / sub
            tgt = [series[n - 1][i] * (1 - f) + series[n][i] * f for i in idx]
            for j in range(len(idx)):
                a = omega * omega * (tgt[j] - x[j]) - 2 * zeta * omega * v[j]
                v[j] += a * dt
                x[j] += v[j] * dt
        row = list(series[n])
        for j, i in enumerate(idx):
            row[i] = x[j]
        out.append(row)
    return out

# ---------------------------------------------------------------- piso

# Cubos del modelo del jugador (px, espacio del modelo): parte -> (min, tamanio, altura de la articulacion, centro x)
CUBES = {
    'head': ((-4, -8, -4), (8, 8, 8), None, 0),
    'torso': ((-4, 0, -2), (8, 12, 4), 6, 0),
    'rightArm': ((-3, -2, -2), (4, 12, 4), 4, -1),
    'leftArm': ((-1, -2, -2), (4, 12, 4), 4, 1),
    'rightLeg': ((-2, 0, -2), (4, 12, 4), 6, 0),
    'leftLeg': ((-2, 0, -2), (4, 12, 4), 6, 0),
}
GROUND_MARGIN = 0.03  # bloques

def model_points(s):
    """Esquinas de todos los cubos en el cuadro de la entidad (bloques, y hacia arriba, pies en 0).
    Misma cadena que el render: cuerpo (pivote a 0.7) -> espejo -> escala 0.9375 -> -1.501 -> px/16."""
    b = s['body']
    Rb = zyx(*b['rot'])
    bv = b['vals']
    pts = []
    for part, (mn, sz, joint, cx) in CUBES.items():
        d = s[part]
        if 'R' in d:
            R = d['R']
        else:
            R = zyx(*d['rot'])
        v = d['vals']
        pos = [v.get('x', 0.0), v.get('y', 0.0), v.get('z', 0.0)]
        bend = v.get('bend', 0.0) if joint is not None else 0.0
        axis = d.get('axis') or 0.0
        B = axis_rot([math.cos(-axis), 0.0, math.sin(-axis)], bend) if bend else None
        for ix in (0, 1):
            for iy in (0, 0.5, 1) if joint is not None else (0, 1):
                for iz in (0, 1):
                    loc = [mn[0] + sz[0] * ix, mn[1] + sz[1] * iy, mn[2] + sz[2] * iz]
                    moving = loc[1] < joint if part == 'torso' else (joint is not None and loc[1] > joint)
                    if B is not None and moving:
                        rel = [loc[0] - cx, loc[1] - joint, loc[2]]
                        rr = mat_vec(B, rel)
                        loc = [rr[0] + cx, rr[1] + joint, rr[2]]
                    m = [pos[i] + c for i, c in enumerate(mat_vec(R, loc))]
                    p = [m[0] / 16, m[1] / 16 - 1.501, m[2] / 16]
                    p = [-p[0] * 0.9375, -p[1] * 0.9375, p[2] * 0.9375]
                    p[1] -= 0.7
                    w = mat_vec(Rb, p)
                    pts.append((w[0] + bv['x'], w[1] + bv['y'] + 0.7, w[2] + bv['z']))
    return pts

def ground_contact(solved, mode='push'):
    """'push': si algo queda bajo el piso, sube el cuerpo lo justo. 'contact': ademas baja lo que flota (rolls y dash
    siempre apoyados). La correccion es una curva suave que nunca deja atravesar el piso."""
    low = [min(p[1] for p in model_points(s)) for s in solved]
    n = len(low)
    need = [max(0.0, GROUND_MARGIN - m) if m < GROUND_MARGIN else 0.0 for m in low]  # minimo para no atravesar
    if mode == 'contact':
        lift = [GROUND_MARGIN - m for m in low]   # apoyado exacto en cada tick
        for i in (0, n - 1):                       # empieza y termina parado como siempre
            if abs(low[i]) < 0.05:
                lift[i] = 0.0
    else:
        wide = [max(need[max(0, i - 1):i + 2]) for i in range(n)]
        lift = [max(need[i], sum(wide[max(0, i - 1):i + 2]) / len(wide[max(0, i - 1):i + 2])) for i in range(n)]
    for s, l in zip(solved, lift):
        s['body']['vals']['y'] += l

# ---------------------------------------------------------------- colision del arma con el cuerpo

def solve_all(poses, continuity=True):
    out, prev = [], None
    for p in poses:
        prev = solve(p, prev if continuity else None)
        out.append(prev)
    return out

def body_point(s, part, loc, joint=None, cx=0.0, upper_moves=False):
    """Punto local de una parte (px) -> espacio del modelo (px), con la flexion de codo/rodilla."""
    d = s[part]
    v = d['vals']
    bend = v.get('bend', 0.0)
    if joint is not None and bend:
        moving = loc[1] < joint if upper_moves else loc[1] > joint
        if moving:
            ax = d.get('axis') or 0.0
            B = axis_rot([math.cos(-ax), 0.0, math.sin(-ax)], bend)
            r = mat_vec(B, [loc[0] - cx, loc[1] - joint, loc[2]])
            loc = [r[0] + cx, r[1] + joint, r[2]]
    R = d['R']
    m = mat_vec(R, loc)
    return [v.get('x', 0.0) + m[0], v.get('y', 0.0) + m[1], v.get('z', 0.0) + m[2]]

def weapon_points(s, seg, n=10):
    """Puntos a lo largo del arma (espacio del modelo, px)."""
    d = s['rightArm']
    U, v = d['R'], d['vals']
    bend = v.get('bend', 0.0)
    ax = d.get('axis') or 0.0
    B = axis_rot([math.cos(-ax), 0.0, math.sin(-ax)], bend) if bend else None
    W = s['rightItem']['R']
    pts = []
    a, b = seg
    for i in range(n + 1):
        f = i / n
        q = [a[j] + (b[j] - a[j]) * f for j in range(3)]
        q = mat_vec(W, q)
        q = mat_vec(ITEM_M, [q[0] + 1, q[1] + 2, q[2] - 10])
        if B is not None:
            r = mat_vec(B, [q[0], q[1] - 4, q[2]])
            q = [r[0], r[1] + 4, r[2]]
        m = mat_vec(U, q)
        pts.append([v['x'] + m[0], v['y'] + m[1], v['z'] + m[2]])
    return pts

# cajas con las que choca el arma: (parte, min, tamanio, articulacion, centro x, mitad de arriba se mueve)
COLLIDERS = [
    ('torso', (-4, 0, -2), (8, 12, 4), 6, 0.0, True),
    ('head', (-4, -8, -4), (8, 8, 8), None, 0.0, False),
    ('rightLeg', (-2, 0, -2), (4, 12, 4), 6, 0.0, False),
    ('leftLeg', (-2, 0, -2), (4, 12, 4), 6, 0.0, False),
]
LEFT_ARM = ('leftArm', (-1, -2, -2), (4, 12, 4), 4, 1.0, False)
BLADE_R = 0.7  # grosor del arma (px)

def _inv_point(s, part, pt, joint, cx, upper_moves):
    """Espacio del modelo -> local de la parte (sin flexion: se prueba contra las dos mitades por separado)."""
    d = s[part]
    v = d['vals']
    rel = [pt[0] - v.get('x', 0.0), pt[1] - v.get('y', 0.0), pt[2] - v.get('z', 0.0)]
    return mat_vec(transpose(d['R']), rel)

def penetration(s, seg, with_left_arm, ground_world=None):
    """Cuanto se mete el arma en el cuerpo (px, suma de profundidades) y en el piso."""
    pts = weapon_points(s, seg)
    total = 0.0
    cols = COLLIDERS + ([LEFT_ARM] if with_left_arm else [])
    for part, mn, sz, joint, cx, up in cols:
        d = s[part]
        bend = d['vals'].get('bend', 0.0)
        halves = [(mn, sz, None)]
        if joint is not None and abs(bend) > 0.05:
            top = (mn, (sz[0], joint - mn[1], sz[2]))
            bot = ((mn[0], joint, mn[2]), (sz[0], mn[1] + sz[1] - joint, sz[2]))
            halves = [(top[0], top[1], 'top'), (bot[0], bot[1], 'bot')]
        for hmn, hsz, which in halves:
            B = None
            moving = which == ('top' if up else 'bot')
            if which and moving:
                ax = d.get('axis') or 0.0
                B = axis_rot([math.cos(-ax), 0.0, math.sin(-ax)], bend)
            for pt in pts:
                l = _inv_point(s, part, pt, joint, cx, up)
                if B is not None:  # deshacer la flexion de esa mitad
                    r = mat_vec(transpose(B), [l[0] - cx, l[1] - joint, l[2]])
                    l = [r[0] + cx, r[1] + joint, r[2]]
                depth = min(min(l[i] - (hmn[i] - BLADE_R), (hmn[i] + hsz[i] + BLADE_R) - l[i]) for i in range(3))
                if depth > 0:
                    total += depth
    if ground_world is not None:
        for pt in pts:
            y = ground_world(s, pt)
            if y < 0:
                total += -y * 16
    return total

def world_y(s, m):
    """Altura en el mundo (bloques) de un punto del espacio del modelo."""
    b = s['body']
    p = [m[0] / 16, m[1] / 16 - 1.501, m[2] / 16]
    p = [-p[0] * 0.9375, -p[1] * 0.9375, p[2] * 0.9375]
    p[1] -= 0.7
    w = mat_vec(zyx(*b['rot']), p)
    return w[1] + b['vals']['y'] + 0.7

def _rotate_toward(b, u, ang):
    return norm([b[i] * math.cos(ang) + u[i] * math.sin(ang) for i in range(3)])

def _ang(a, b):
    return math.acos(max(-1.0, min(1.0, dot(a, b))))

def resolve_weapon(poses, seg, from_tick=0, sweeps=10):
    """Recorrido del arma sin atravesar el cuerpo ni el piso, optimizando la animacion entera a la vez:
    se busca que no choque (muy caro), que se aparte poco de la pose original y que entre tick y tick
    la hoja y el giro de la muneca cambien lo menos posible (sin vueltas bruscas)."""
    poses = [json.loads(json.dumps(p)) for p in poses]
    n = len(poses)
    orig = [list(p['rarm'][1]) for p in poses]
    cur = [list(b) for b in orig]
    left = [p['grip'][0] < 0.5 for p in poses]
    W_PEN, W_DEV, W_SMOOTH, W_TWIST = 400.0, 0.6, 6.0, 0.6

    def evaluate(i, b, prev_s):
        q = dict(poses[i]); q['rarm'] = [poses[i]['rarm'][0], b]
        s_ = solve(q, prev_s)
        return s_, penetration(s_, seg, left[i], world_y)

    # estado resuelto de cada tick (para el giro de la muneca del vecino)
    def solve_seq(blades):
        out, prev = [], None
        for i in range(n):
            q = dict(poses[i]); q['rarm'] = [poses[i]['rarm'][0], blades[i]]
            prev = solve(q, prev)
            out.append(prev)
        return out

    states = solve_seq(cur)
    if all(penetration(states[i], seg, left[i], world_y) <= 0.05 for i in range(from_tick, n)):
        return poses  # nada que corregir
    radii = [6 * D, 15 * D, 30 * D]
    # el primer y el ultimo tick quedan como estan (empalman con la animacion anterior y con la pose de reposo)
    anchored = {i for i in (0, n - 1) if penetration(states[i], seg, left[i], world_y) <= 0.05}
    def around(b0, rs):
        ref = [0, 1, 0] if abs(b0[1]) < 0.9 else [1, 0, 0]
        u1 = norm(cross(b0, ref)); u2 = cross(b0, u1)
        out = []
        for r in rs:
            for k in range(12):
                az = k * TAU / 12
                u = [u1[j] * math.cos(az) + u2[j] * math.sin(az) for j in range(3)]
                out.append(_rotate_toward(b0, u, r))
        return out

    for sweep in range(sweeps):
        changed = False
        order = range(max(from_tick, 0), n) if sweep % 2 == 0 else range(n - 1, max(from_tick, 0) - 1, -1)
        for i in order:
            if i in anchored:
                continue
            b0 = cur[i]
            cands = [b0] + around(b0, radii)
            # copiar la solucion de los vecinos: asi el arma esquiva el cuerpo siempre por el mismo lado
            neigh = [cur[j] for j in (i - 1, i + 1) if 0 <= j < n]
            if len(neigh) == 2:
                neigh.append(norm([neigh[0][k] + neigh[1][k] for k in range(3)]))
            for nb in neigh:
                cands += [nb] + around(nb, [8 * D])
            prev_s = states[i - 1] if i > 0 else None
            best = None
            for b in cands:
                s_, pen = evaluate(i, b, prev_s)
                cost = W_PEN * pen + W_DEV * _ang(b, orig[i]) ** 2
                if i > 0:
                    cost += W_SMOOTH * _ang(b, cur[i - 1]) ** 2
                    cost += W_TWIST * _ang(s_['rightArm']['front'], states[i - 1]['rightArm']['front']) ** 2
                if i < n - 1:
                    cost += W_SMOOTH * _ang(b, cur[i + 1]) ** 2
                if best is None or cost < best[0] - 1e-9:
                    best = (cost, b, s_)
            if best[1] is not b0:
                changed = True
            cur[i] = best[1]
            states[i] = best[2]
        states = solve_seq(cur)
        if not changed:
            break
    for i in range(n):
        poses[i]['rarm'][1] = cur[i]
    return poses

# ---------------------------------------------------------------- animacion

ROT_AXES = ('pitch', 'yaw', 'roll')

class Anim:
    def __init__(self, name, desc, end, begin=0, stop=None, loop=False, ret=0, start=None, lag=('head',), settle=0, ground=True):
        self.name, self.desc = name, desc
        self.begin, self.end = begin, end
        self.stop = stop if stop is not None else end
        self.loop, self.ret = loop, ret
        self.lag = lag          # partes con inercia (resorte)
        self.ground = ground    # nada del modelo atraviesa el piso
        self.weapon = None      # segmento del arma (lo pone build_data segun la familia); None = sin colision de arma
        self.weapon_from = 0    # tick desde el que el arma esta en la mano (desenvainar)
        self.continuity = True  # giro de muneca continuo; sacar/guardar lo apagan para que el agarre sea identico
        self.keys = []
        base = json.loads(json.dumps(NEUTRAL)); base.update(json.loads(json.dumps(start or {})))
        self.cur = base

    def key(self, tick, mode='smooth', **changes):
        """mode: 'smooth' (pasa con velocidad) o 'stop' (frena en esta pose). Las demas palabras de antes = smooth."""
        p = json.loads(json.dumps(self.cur))
        if 'larm' in changes and 'grip' not in changes:
            p['grip'] = [0, 0]
        for k, v in changes.items():
            if k in ('rarm', 'larm', 'rleg', 'lleg'):
                v = list(v)
                for i, x in enumerate(v):
                    if x is not None:
                        p[k][i] = list(x) if isinstance(x, (list, tuple)) else x
            else:
                p[k] = list(v)
        self.cur = p
        self.keys.append((tick, flatten(p), 'stop' if mode in ('stop', 'hold') else 'smooth'))
        return self

    def series(self):
        first, last = self.keys[0][0], self.keys[-1][0]
        rows = [hermite(self.keys, t) for t in range(first, last + 1)]
        lag_cfg = {'head': (0.9, 0.62), 'larm': (1.0, 0.6), 'rarm': (1.4, 0.7), 'rleg': (1.3, 0.7), 'lleg': (1.3, 0.7),
                   'torso': (1.3, 0.72)}
        for part in self.lag:
            om, ze = lag_cfg[part]
            rows = spring(rows, list(slice_of(part)), om, ze)
        # los extremos quedan exactos (sin resorte) para empalmar bien con la pose anterior / siguiente
        if self.lag:
            rows[0] = list(self.keys[0][1])
            if not self.loop:
                rows[-1] = list(self.keys[-1][1])
        return first, [unflatten(r) for r in rows]

    def build(self):
        first, poses = self.series()
        if self.weapon is not None:
            poses = resolve_weapon(poses, self.weapon, self.weapon_from)
        solved = solve_all(poses, self.continuity)
        if self.ground:
            ground_contact(solved, self.ground if isinstance(self.ground, str) else 'push')
        ticks = list(range(first, first + len(poses)))
        out = {}  # (parte, eje) -> [(tick, valor)]
        for part in solved[0]:
            for ax in solved[0][part]['vals']:
                out[(part, ax)] = [(t, s[part]['vals'][ax]) for t, s in zip(ticks, solved)]
            if 'rot' in solved[0][part]:
                trip = [s[part]['rot'] for s in solved]
                rot_series(out, part, ticks, trip, pick=False)
            elif 'R' in solved[0][part]:
                cands = [euler_candidates(s[part]['R']) for s in solved]
                rot_series(out, part, ticks, viterbi(cands), pick=False)
            if 'axis' in solved[0][part]:
                vals, last = [], 0.0
                for s in solved:
                    a = s[part]['axis']
                    last = last if a is None else a
                    vals.append(last)
                out[(part, 'axis')] = list(zip(ticks, unwrap(vals)))
        return self.emit(out, ticks[-1])

    def emit(self, out, last_tick):
        by_tick, turns = {}, []
        for (part, ax), pts in out.items():
            angle = ax in ROT_AXES or ax == 'axis'
            pts = rebase(pts) if angle else [(t, v, 0) for t, v in pts]
            keep = [pts[0]]
            for i in range(1, len(pts) - 1):
                (t0, v0, _), (t1, v1, k1), (t2, v2, k2) = keep[-1], pts[i], pts[i + 1]
                v0e = keep[-1][1] + TAU * keep[-1][2]  # valor con el que sale el keyframe anterior
                if k1 or abs(v0e + (v2 - v0e) * (t1 - t0) / (t2 - t0) - v1) > 0.0015:
                    keep.append(pts[i])
            if len(pts) > 1:
                keep.append(pts[-1])
            for t, v, k in keep:
                if k:
                    turns.append({'tick': t, 'easing': 'EASEINOUTSINE' if t == last_tick else 'LINEAR', 'turn': k, part: {ax: round(v, 4)}})
                else:
                    by_tick.setdefault(t, {}).setdefault(part, {})[ax] = round(v, 4)
        moves = []
        for t in sorted(by_tick):
            m = {'tick': t, 'easing': 'EASEINOUTSINE' if t == last_tick else 'LINEAR'}
            m.update(by_tick[t])
            moves.append(m)
        moves += turns
        moves.sort(key=lambda m: m['tick'])
        return {
            'version': 3, 'name': self.name, 'author': 'Zennin / Mushoku Combat', 'description': self.desc,
            'emote': {'beginTick': self.begin, 'endTick': self.end, 'stopTick': self.stop,
                      'isLoop': 'true' if self.loop else 'false', 'returnTick': self.ret, 'degrees': False, 'moves': moves},
        }

def viterbi(cands):
    """Elige, tick a tick, cual de las dos soluciones de Euler usar: camino mas corto, empezando y terminando cerca del reposo."""
    dist = lambda a, b: sum(abs(wrap(x - y)) for x, y in zip(a, b))
    rest = lambda a: sum(abs(x) for x in a)
    cost = [rest(c) for c in cands[0]]
    back = []
    for n in range(1, len(cands)):
        nc, nb = [], []
        for c in cands[n]:
            opts = [cost[j] + dist(cands[n - 1][j], c) for j in range(2)]
            j = 0 if opts[0] <= opts[1] else 1
            nc.append(opts[j]); nb.append(j)
        cost, back = nc, back + [nb]
    j = 0 if cost[0] + rest(cands[-1][0]) <= cost[1] + rest(cands[-1][1]) else 1
    path = [j]
    for nb in reversed(back):
        j = nb[j]; path.append(j)
    path.reverse()
    return [cands[n][path[n]] for n in range(len(cands))]

def unwrap(vals):
    out = [vals[0]]
    for v in vals[1:]:
        while v - out[-1] > math.pi: v -= TAU
        while v - out[-1] < -math.pi: v += TAU
        out.append(v)
    return out

def rot_series(out, part, ticks, triples, pick):
    for i, ax in enumerate(ROT_AXES):
        out[(part, ax)] = list(zip(ticks, unwrap([tr[i] for tr in triples])))

def rebase(pts, limit=math.pi + 0.35):
    """Mantiene el angulo cerca de [-pi, pi] insertando un keyframe doble (turn) cuando se pasa:
    no se nota en el giro, pero al terminar la animacion no da una vuelta de mas."""
    res, off = [], 0.0
    for i, (t, v) in enumerate(pts):
        v += off
        k = 0
        if v > limit:
            k = -1
        elif v < -limit:
            k = 1
        if i == len(pts) - 1 and abs(v) > math.pi:  # el final siempre queda en su forma canonica
            k = -round(v / TAU) or k
        res.append((t, v, k))
        off += TAU * k
    return res

ANIMS = []

def anim(*a, **k):
    x = Anim(*a, **k)
    ANIMS.append(x)
    return x

def write(out):
    os.makedirs(out, exist_ok=True)
    for f in os.listdir(out):
        if f.endswith('.json'):
            os.remove(os.path.join(out, f))
    for a in ANIMS:
        with open(os.path.join(out, a.name + '.json'), 'w') as f:
            json.dump(a.build(), f, separators=(',', ':'))
    print(len(ANIMS), 'animaciones ->', out)

if __name__ == '__main__':
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import animgen as G
    import anims  # noqa: F401
    G.write(sys.argv[1])

# ---------------------------------------------------------------- armas guardadas

def quat(m):
    """Matriz de rotacion -> cuaternion (x, y, z, w) como JOML."""
    tr = m[0][0] + m[1][1] + m[2][2]
    if tr > 0:
        s = math.sqrt(tr + 1.0) * 2
        return ((m[2][1] - m[1][2]) / s, (m[0][2] - m[2][0]) / s, (m[1][0] - m[0][1]) / s, 0.25 * s)
    if m[0][0] > m[1][1] and m[0][0] > m[2][2]:
        s = math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2
        return (0.25 * s, (m[0][1] + m[1][0]) / s, (m[0][2] + m[2][0]) / s, (m[2][1] - m[1][2]) / s)
    if m[1][1] > m[2][2]:
        s = math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2
        return ((m[0][1] + m[1][0]) / s, 0.25 * s, (m[1][2] + m[2][1]) / s, (m[0][2] - m[2][0]) / s)
    s = math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2
    return ((m[0][2] + m[2][0]) / s, (m[1][2] + m[2][1]) / s, 0.25 * s, (m[1][0] - m[0][1]) / s)

def hand_slot(anim_obj, tick):
    """Donde queda el arma en la mano derecha en ese tick, relativo al torso (para dibujarla guardada).
    Se resuelve la animacion entera en orden, igual que al exportarla (el giro de la muneca depende del tick anterior)."""
    first, poses = anim_obj.series()
    s = solve_all(poses, anim_obj.continuity)[tick - first]
    R_t = s['torso']['R']
    Rti = transpose(R_t)
    F = mat_mul(Rti, s['rightArm']['R'])
    pos = mat_vec(Rti, [s['rightArm']['vals'][k] for k in ('x', 'y', 'z')])
    wrist = euler_candidates(s['rightItem']['R'])[0]
    r = lambda v: round(v, 5)
    return dict(pos=[r(v) for v in pos], rot=[r(v) for v in quat(F)], bend=r(s['rightArm']['vals']['bend']),
                axis=r(s['rightArm'].get('axis') or 0.0), wrist=[r(v) for v in wrist])

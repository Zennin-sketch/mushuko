"""Animaciones de primera persona por defecto del arco (las mismas que dibujaba BowFirstPerson a mano).

Formato (espacio de la camara, en bloques; x derecha, y arriba, -z adelante):
  {"name", "length", "loop", "keyframes": [{"tick", "stop", "pose": {
      "rArm": {"p": [x,y,z], "q": [x,y,z,w]},   brazo derecho: origen del modelo del brazo (hombro) y giro
      "lArm": {...},                             brazo izquierdo
      "item": {"p": [...], "q": [...]}           objeto, relativo al brazo que lo sostiene (arco: izquierdo)
  }}]}
El brazo es el cubo del modelo del jugador (y de -2 a 10 px hacia la mano); el centro del puno queda en (0, 8, 0) px.

Uso: python3 tools/fp/fp_defaults.py   (escribe src/main/resources/assets/remushoku/fp_animations/*.json)
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku', 'fp_animations')

S = 0.65
TARGET = (0, 0, -14.0)
L_IDLE, L_AIM = (-0.07, -0.36, -1.0), (-0.03, -0.27, -1.1)
GRIP_OFFSET = 0.11
DRAW_HAND = (0.035, -0.01, 0.0)
R_IDLE, R_QUIVER, R_FLING = (0.34, -0.5, -0.95), (0.42, 0.16, 0.25), (0.42, -0.2, -0.5)
SH_L, SH_R = (-0.45, -0.95, -0.55), (0.5, -0.85, -0.25)
BRACE, DRAW = 3.2, 10.5
LOAD_TICKS = 16


def add(a, b): return tuple(x + y for x, y in zip(a, b))
def sub(a, b): return tuple(x - y for x, y in zip(a, b))
def mul(a, k): return tuple(x * k for x in a)
def dot(a, b): return sum(x * y for x, y in zip(a, b))
def cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def norm(a):
    l = math.sqrt(dot(a, a))
    return mul(a, 1 / l)
def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)
def lerp(a, b, t):
    t = smooth(t)
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def quat_from_basis(x, y, z):
    """Cuaternion (x,y,z,w) de la matriz de giro con columnas x, y, z."""
    m00, m01, m02 = x[0], y[0], z[0]
    m10, m11, m12 = x[1], y[1], z[1]
    m20, m21, m22 = x[2], y[2], z[2]
    tr = m00 + m11 + m22
    if tr > 0:
        s = math.sqrt(tr + 1.0) * 2
        return ((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25 * s)
    if m00 > m11 and m00 > m22:
        s = math.sqrt(1.0 + m00 - m11 - m22) * 2
        return (0.25 * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
    if m11 > m22:
        s = math.sqrt(1.0 + m11 - m00 - m22) * 2
        return ((m01 + m10) / s, 0.25 * s, (m12 + m21) / s, (m02 - m20) / s)
    s = math.sqrt(1.0 + m22 - m00 - m11) * 2
    return ((m02 + m20) / s, (m12 + m21) / s, 0.25 * s, (m10 - m01) / s)


def qmul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return (aw * bx + ax * bw + ay * bz - az * by, aw * by - ax * bz + ay * bw + az * bx,
            aw * bz + ax * by - ay * bx + az * bw, aw * bw - ax * bx - ay * by - az * bz)


def qinv(q): return (-q[0], -q[1], -q[2], q[3])


def qrot(q, v):
    p = qmul(qmul(q, (v[0], v[1], v[2], 0.0)), qinv(q))
    return p[:3]


def arm_pose(sh, hand, right):
    """Igual que BowFirstPerson.arm: el brazo va del hombro a la mano y el centro del puno queda en 'hand'."""
    d = norm(sub(hand, sh))
    x = (-1.0, 0.0, 0.0)
    x = norm(sub(x, mul(d, dot(x, d))))
    z = norm(cross(x, d))
    start = sub(hand, mul(d, 8 / 16))
    p = add(start, mul(x, (1 if right else -1) / 16))
    return p, quat_from_basis(x, d, z)


def bow_frame(L, aim):
    G = None
    fwd = norm(sub(TARGET, L))
    cant = math.radians(50 - 25 * smooth(aim))
    u = (math.sin(cant), math.cos(cant), 0.0)
    up = norm(sub(u, mul(fwd, dot(u, fwd))))
    G = add(L, mul(up, GRIP_OFFSET))
    # el frente se recalcula desde el centro del arco (como BowFirstPerson.orient(G, aim))
    fwd = norm(sub(TARGET, G))
    up = norm(sub(u, mul(fwd, dot(u, fwd))))
    return G, fwd, up


def pose_at(L, R, aim):
    G, fwd, up = bow_frame(L, aim)
    rp, rq = arm_pose(SH_R, R, True)
    lp, lq = arm_pose(SH_L, L, False)
    # arco: relativo al brazo izquierdo
    bq = quat_from_basis(fwd, up, norm(cross(fwd, up)))
    ip = qrot(qinv(lq), sub(G, lp))
    iq = qmul(qinv(lq), bq)
    r = lambda v: [round(c, 5) for c in v]
    return {'rArm': {'p': r(rp), 'q': r(rq)}, 'lArm': {'p': r(lp), 'q': r(lq)}, 'item': {'p': r(ip), 'q': r(iq)}}


def breath(t): return math.sin(t * 0.08) * 0.006


def nock_of(L, aim, pull):
    G, fwd, up = bow_frame(L, aim)
    back = BRACE + DRAW * pull
    return add(add(mul(fwd, -back * S / 16), G), mul(up, -0.02))


def rest_of(L):
    G, fwd, up = bow_frame(L, 0)
    return add(mul(fwd, -BRACE * S / 16), G)


def anim_idle(loaded):
    keys = []
    for t in (0, 20, 39, 59, 78):
        b = breath(t)
        L = add(L_IDLE, (0, b, 0))
        R = lerp(R_IDLE, rest_of(L), 0.55) if loaded else R_IDLE
        R = add(R, (0, b * 0.7, 0))
        keys.append({'tick': t, 'stop': False, 'pose': pose_at(L, R, 0)})
    return {'name': 'bow_loaded' if loaded else 'bow_idle', 'length': 78, 'loop': True, 'keyframes': keys}


def anim_load():
    keys = []
    for t in range(0, LOAD_TICKS + 1, 2):
        load = t / LOAD_TICKS
        L = L_IDLE
        rest = rest_of(L)
        if load < 0.3:
            R = lerp(R_IDLE, R_QUIVER, load / 0.3)
        elif load < 0.42:
            R = R_QUIVER
        elif load < 0.8:
            R = lerp(R_QUIVER, rest, (load - 0.42) / 0.38)
        else:
            R = rest
        keys.append({'tick': t, 'stop': False, 'pose': pose_at(L, R, 0)})
    return {'name': 'bow_load', 'length': LOAD_TICKS, 'loop': False, 'keyframes': keys}


def anim_draw():
    keys = []
    for t in (0, 2, 4, 7, 10, 15, 20):
        pull = t / 20
        aim = min(1.0, pull * 3)
        L = lerp(L_IDLE, L_AIM, aim)
        R = add(nock_of(L, aim, pull), DRAW_HAND)
        keys.append({'tick': t, 'stop': t == 20, 'pose': pose_at(L, R, aim)})
    return {'name': 'bow_draw', 'length': 20, 'loop': False, 'keyframes': keys}


def anim_release():
    keys = []
    for t in (0, 1, 2, 3, 5, 7, 10):
        rel = t / 10
        aim = 1 - rel
        L = lerp(L_IDLE, L_AIM, aim)
        nock = nock_of(L, aim, 0)
        R = lerp(nock, R_FLING, rel / 0.25) if rel < 0.25 else lerp(R_FLING, R_IDLE, (rel - 0.25) / 0.75)
        keys.append({'tick': t, 'stop': t == 10, 'pose': pose_at(L, R, aim)})
    return {'name': 'bow_release', 'length': 10, 'loop': False, 'keyframes': keys}


def all_anims():
    return [anim_idle(False), anim_idle(True), anim_load(), anim_draw(), anim_release()]


def main():
    os.makedirs(OUT, exist_ok=True)
    for a in all_anims():
        with open(os.path.join(OUT, a['name'] + '.json'), 'w') as f:
            json.dump(a, f, indent=1)
    print('animaciones de primera persona ->', os.path.normpath(OUT))


if __name__ == '__main__':
    main()

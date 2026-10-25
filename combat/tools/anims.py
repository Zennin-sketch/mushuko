"""Todas las animaciones del addon.

Coordenadas intuitivas: (derecha, arriba, adelante). Las manos se ubican en pixeles desde el cuello, en el marco del
torso (si el torso gira, las manos giran con el). El hombro derecho esta en (5, -2, 0) y el brazo mide 10 px.
La cadera esta a -12. Angulos en grados.

Ataques de Better Combat: el golpe conecta a mitad de la animacion (tick 10 de 20). De 0 a beginTick (6) Better Combat
hace la transicion desde la pose actual, asi que el primer keyframe es la preparacion del golpe.
"""
from animgen import anim, NEUTRAL, norm

def R(p, blade=(0, 0, 1)):
    return (list(p), list(norm(blade)))

L = R

def grip(p, blade, spread=3.5):
    """Agarre a dos manos: la derecha en p y la izquierda sobre el mango (spread px hacia la empunadura).
    spread negativo = la izquierda va adelante (lanza)."""
    b = norm(blade)
    other = [p[i] - b[i] * spread for i in range(3)]
    return dict(rarm=R(p, b), larm=R(other, b))

def legs(stance='neutral', k=0):
    s = {
        'neutral': dict(rleg=((0, -1, 0), 0), lleg=((0, -1, 0), 0)),
        'guard':   dict(rleg=((0.12, -1, -0.18), 8 + k), lleg=((-0.08, -1, 0.22), 6 + k)),
        'lunge':   dict(rleg=((0.1, -0.8, 0.6), 45 + k), lleg=((-0.08, -0.85, -0.5), 8)),
        'wide':    dict(rleg=((0.35, -1, 0.05), 14 + k), lleg=((-0.35, -1, -0.05), 14 + k)),
        'crouch':  dict(rleg=((0.1, -0.7, 0.55), 70 + k), lleg=((-0.1, -0.7, 0.55), 70 + k)),
        'tuck':    dict(rleg=((0.08, -0.2, 1), 125), lleg=((-0.08, -0.2, 1), 125)),
        'stride':  dict(rleg=((0.05, -0.9, 0.4), 10 + k), lleg=((-0.05, -0.9, -0.4), 25 + k)),
        'kick':    dict(rleg=((0.05, -0.75, 0.65), 15 + k), lleg=((-0.05, -0.95, -0.3), 10)),
    }
    return s[stance]

def root(right=0, up=0, fwd=0, lean=0, turn=0, tilt=0, plant=1):
    """plant=1: los pies quedan en el piso al inclinarse; plant=0: gira sobre el centro del cuerpo (rolls)."""
    return dict(root=(right, up, fwd, lean, turn, tilt, plant))

def torso(turn=0, lean=0, tilt=0):
    return dict(torso=(turn, lean, tilt))

def head(yaw=0, down=0, tilt=0):
    return dict(head=(yaw, down, tilt))

def item(pitch=0, yaw=0, roll=0, x=0, y=0, z=0):
    return dict(ritem=(pitch, yaw, roll, x, y, z))

def P(*dicts):
    out = {}
    for d in dicts:
        out.update(d)
    return out

def attack(name, desc):
    return anim(name, desc, begin=6, end=20, stop=28)

NEUTRAL_P = dict(NEUTRAL)

# =============================================================== ESPADA (una mano)
GUARD_1H = P(root(lean=3), torso(turn=-10), head(yaw=8),
             dict(rarm=R((3, -8, 8), (0.05, 0.75, 0.65)), larm=L((-6, -11, 3))), legs('guard'))

a = attack('sword_slash_1', 'Espada: tajo diagonal descendente')
a.key(6, **P(root(lean=-3, turn=10), torso(turn=35, lean=-6), head(yaw=-30),
             dict(rarm=R((7, 6, -1), (0.2, 0.25, -0.95)), larm=L((-2, -5, 9))), legs('guard')))
a.key(10, 'snap', **P(root(fwd=0.15, lean=10, turn=-8), torso(turn=-38, lean=8, tilt=-5), head(yaw=32, down=8),
                      dict(rarm=R((-2, -9, 9), (-0.75, -0.5, 0.4)), larm=L((-8, -10, -4))), legs('lunge', 5)))
a.key(13, 'out', **P(root(fwd=0.17, lean=12, turn=-12), torso(turn=-48, lean=10, tilt=-7), head(yaw=38, down=10),
                     dict(rarm=R((-5, -11, 6), (-0.6, -0.65, -0.45)))))
a.key(20, 'inout', **GUARD_1H)

a = attack('sword_slash_2', 'Espada: reves ascendente')
a.key(6, **P(root(lean=6, turn=-14, up=-0.03), torso(turn=-42, lean=6), head(yaw=32),
             dict(rarm=R((-4, -11, 6), (-0.55, -0.6, -0.6)), larm=L((-8, -9, -4))), legs('guard', 6)))
a.key(10, 'snap', **P(root(fwd=0.1, lean=0, turn=14, up=0.03), torso(turn=38, lean=-6, tilt=5), head(yaw=-26, down=-6),
                      dict(rarm=R((9, 3, 6), (0.5, 0.8, -0.2)), larm=L((-4, -8, 7))), legs('stride')))
a.key(13, 'out', **P(root(fwd=0.11, lean=-2, turn=18, up=0.04), torso(turn=48, lean=-8, tilt=7),
                     dict(rarm=R((10, 5, 1), (0.3, 0.9, -0.3)))))
a.key(20, 'inout', **GUARD_1H)

a = attack('sword_thrust_3', 'Espada: estocada a fondo (remate)')
a.key(6, **P(root(lean=-3, turn=18, up=-0.07, fwd=-0.05), torso(turn=40, lean=-4), head(yaw=-36),
             dict(rarm=R((7, -9, -2), (0.02, 0.1, 1)), larm=L((-3, -4, 9), (0, 0.4, 1))), legs('crouch', -35)))
a.key(10, 'snap', **P(root(fwd=0.5, lean=14, turn=-10, up=-0.12), torso(turn=-22, lean=6), head(yaw=22, down=4),
                      dict(rarm=R((2, -2, 12), (0, 0.05, 1)), larm=L((-8, -10, -6), (0, 0, -1))), legs('lunge', 15)))
a.key(14, 'hold', **P(root(fwd=0.5, lean=14, turn=-10, up=-0.12)))
a.key(20, 'inout', **GUARD_1H)

# =============================================================== KATANA (dos manos)
GUARD_K = P(root(lean=4, turn=-6), torso(turn=-12), head(yaw=10), grip((1, -8, 8), (0.05, 0.75, 0.65)), legs('guard', 4))

a = attack('katana_iai', 'Katana: corte horizontal al desenvainar')
a.key(6, **P(root(lean=8, turn=-20, up=-0.08), torso(turn=-50, lean=8), head(yaw=48, down=4),
             grip((-6, -11, 3), (-0.2, -0.05, -1)), legs('crouch', -35)))
a.key(10, 'snap', **P(root(fwd=0.3, lean=8, turn=20, up=-0.1), torso(turn=45, lean=4), head(yaw=-30),
                      grip((6, -5, 10), (0.95, 0.05, 0.3)), legs('lunge', 5)))
a.key(13, 'out', **P(root(fwd=0.32, lean=8, turn=26, up=-0.1), torso(turn=60, lean=4),
                     grip((8, -5, 5), (0.8, 0.1, -0.6))))
a.key(20, 'inout', **GUARD_K)

a = attack('katana_overhead', 'Katana: corte vertical desde arriba')
a.key(6, **P(root(lean=-6, up=0.04), torso(lean=-8), head(down=-12),
             grip((1, 7, 2), (0, 0.25, -1)), legs('guard')))
a.key(10, 'snap', **P(root(fwd=0.25, lean=16, up=-0.12), torso(lean=12), head(down=14),
                      grip((0, -8, 11), (0, -0.55, 0.85)), legs('lunge', 10)))
a.key(13, 'out', **P(root(fwd=0.26, lean=18, up=-0.13), grip((0, -11, 8), (0, -0.85, 0.5))))
a.key(20, 'inout', **GUARD_K)

a = attack('katana_whirlwind', 'Katana: giro completo (remate)')
a.key(5, **P(root(lean=10, turn=10, up=-0.14), torso(turn=40, lean=8), head(yaw=-30),
             grip((6, -10, 2), (0.5, -0.1, -0.85)), legs('crouch', -20)))
a.key(8, 'in', **P(root(lean=10, turn=-120, up=-0.12), torso(turn=0), grip((5, -6, 10), (1, 0, 0.1))))
a.key(11, 'linear', **P(root(lean=8, turn=-360, up=-0.1, fwd=0.2), torso(turn=-30), grip((-5, -6, 10), (-1, 0, 0.2)), legs('wide')))
a.key(14, 'out', **P(root(lean=8, turn=-380, up=-0.1, fwd=0.22), torso(turn=-40), grip((-7, -7, 5), (-0.85, -0.1, -0.5))))
a.key(20, 'inout', **P(GUARD_K, root(lean=4, turn=-366)))

# =============================================================== ARMA PESADA (mandoble / hacha)
GUARD_H = P(root(lean=3, turn=-8), torso(turn=-15), head(yaw=12), grip((2, -8, 7), (0.2, 0.9, 0.35)), legs('wide'))

a = attack('heavy_sweep', 'Arma pesada: barrido horizontal')
a.key(6, **P(root(lean=5, turn=24, up=-0.05), torso(turn=55, lean=4), head(yaw=-45),
             grip((7, -3, -1), (0.6, 0.4, -0.7)), legs('wide', 10)))
a.key(10, 'snap', **P(root(fwd=0.15, lean=8, turn=-18, up=-0.08), torso(turn=-40, lean=6), head(yaw=30),
                      grip((-3, -5, 11), (-0.9, 0.05, 0.4)), legs('wide', 14)))
a.key(14, 'out', **P(root(fwd=0.16, lean=8, turn=-26, up=-0.08), torso(turn=-62, lean=6), grip((-7, -6, 5), (-0.7, -0.05, -0.7))))
a.key(20, 'inout', **GUARD_H)

a = attack('heavy_rise', 'Arma pesada: corte ascendente de vuelta')
a.key(6, **P(root(lean=10, turn=-20, up=-0.1), torso(turn=-55, lean=8), head(yaw=40),
             grip((-6, -11, 4), (-0.5, -0.6, -0.6)), legs('crouch', -30)))
a.key(10, 'snap', **P(root(fwd=0.12, lean=-4, turn=16, up=0.05), torso(turn=35, lean=-8), head(yaw=-20, down=-10),
                      grip((4, 4, 8), (0.4, 0.9, 0.1)), legs('stride')))
a.key(14, 'out', **P(root(fwd=0.12, lean=-6, turn=20, up=0.05), torso(turn=45, lean=-10), grip((4, 7, 4), (0.3, 0.9, -0.3))))
a.key(20, 'inout', **GUARD_H)

a = attack('heavy_slam', 'Arma pesada: golpe aplastante (remate)')
a.key(5, **P(root(lean=-10, up=0.06), torso(lean=-12), head(down=-18),
             grip((0, 8, 0), (0, 0.1, -1)), legs('guard')))
a.key(8, 'in', **P(root(lean=-6, up=0.25, fwd=0.1), grip((0, 9, 3), (0, 0.6, -0.8)), legs('tuck')))
a.key(11, 'snap', **P(root(fwd=0.35, lean=24, up=-0.3), torso(lean=14), head(down=10),
                      grip((0, -9, 12), (0, -0.75, 0.65)), legs('crouch', 10)))
a.key(15, 'hold', **P(root(fwd=0.35, lean=24, up=-0.3)))
a.key(20, 'inout', **GUARD_H)

# =============================================================== GUADANIA
GUARD_S = P(root(lean=3, turn=-12), torso(turn=-25), head(yaw=20), grip((4, -7, 6), (0.1, 0.95, 0.2), spread=6), legs('wide'))

a = attack('scythe_reap', 'Guadana: siega amplia hacia adentro')
a.key(6, **P(root(lean=5, turn=30, up=-0.04), torso(turn=60), head(yaw=-50),
             grip((8, -2, -2), (0.6, 0.6, -0.5), spread=6), legs('wide', 8)))
a.key(10, 'snap', **P(root(fwd=0.15, lean=10, turn=-15, up=-0.1), torso(turn=-35, lean=6), head(yaw=25),
                      grip((-1, -6, 12), (-0.6, 0.5, 0.6), spread=6), legs('wide', 16)))
a.key(14, 'out', **P(root(fwd=0.16, lean=10, turn=-28, up=-0.1), torso(turn=-65, lean=6), grip((-7, -8, 5), (-0.8, 0.4, -0.4), spread=6)))
a.key(20, 'inout', **GUARD_S)

a = attack('scythe_backswing', 'Guadana: tajo de vuelta')
a.key(6, **P(root(lean=6, turn=-28, up=-0.06), torso(turn=-60, lean=5), head(yaw=45),
             grip((-7, -6, 3), (-0.7, 0.4, -0.6), spread=6), legs('wide', 10)))
a.key(10, 'snap', **P(root(fwd=0.12, lean=6, turn=18, up=-0.06), torso(turn=40, lean=4), head(yaw=-25),
                      grip((6, -4, 10), (0.7, 0.4, 0.55), spread=6), legs('stride')))
a.key(14, 'out', **P(root(fwd=0.12, lean=6, turn=26, up=-0.06), torso(turn=62), grip((8, -4, 3), (0.8, 0.4, -0.45), spread=6)))
a.key(20, 'inout', **GUARD_S)

a = attack('scythe_cyclone', 'Guadana: torbellino (remate)')
a.key(5, **P(root(lean=8, turn=20, up=-0.12), torso(turn=45, lean=6), head(yaw=-35),
             grip((8, -4, 0), (0.8, 0.3, -0.5), spread=6), legs('crouch', -25)))
a.key(8, 'in', **P(root(lean=8, turn=-150, up=-0.1), torso(turn=0), grip((3, -5, 12), (0.2, 0.3, 1), spread=6), legs('wide')))
a.key(12, 'linear', **P(root(lean=6, turn=-400, up=-0.08, fwd=0.15), torso(turn=-30), grip((-6, -6, 10), (-0.8, 0.3, 0.5), spread=6)))
a.key(15, 'out', **P(root(lean=6, turn=-420, up=-0.08, fwd=0.15), torso(turn=-50), grip((-8, -7, 3), (-0.8, 0.3, -0.5), spread=6)))
a.key(20, 'inout', **P(GUARD_S, root(lean=3, turn=-372)))

# =============================================================== LANZA
def spear(p, d):
    """Lanza: la derecha atras sobre el asta y la izquierda 7 px adelante."""
    return grip(p, d, spread=-7)

GUARD_SP = P(root(lean=5, turn=-25), torso(turn=-30), head(yaw=25), spear((4, -9, 3), (0, 0.18, 1)), legs('guard', 6))

a = attack('spear_thrust', 'Lanza: estocada rapida')
a.key(6, **P(root(lean=2, turn=-28, fwd=-0.08), torso(turn=-36, lean=-2), head(yaw=30),
             spear((5, -8, -2), (0, 0.12, 1)), legs('guard', 10)))
a.key(10, 'snap', **P(root(fwd=0.4, lean=12, turn=-20, up=-0.08), torso(turn=-15, lean=6), head(yaw=18),
                      spear((1, -6, 10), (0, 0.05, 1)), legs('lunge', 8)))
a.key(13, 'hold', **P(root(fwd=0.4, lean=12, turn=-20, up=-0.08)))
a.key(20, 'inout', **GUARD_SP)

a = attack('spear_sweep', 'Lanza: barrido con el asta')
a.key(6, **P(root(lean=4, turn=20), torso(turn=40), head(yaw=-30),
             grip((7, -6, 3), (0.85, 0.05, 0.5), spread=-7), legs('wide', 8)))
a.key(10, 'snap', **P(root(fwd=0.12, lean=7, turn=-22, up=-0.06), torso(turn=-35, lean=5), head(yaw=25),
                      grip((-2, -6, 10), (-0.9, 0.05, 0.4), spread=-7), legs('wide', 14)))
a.key(14, 'out', **P(root(fwd=0.12, lean=7, turn=-30, up=-0.06), torso(turn=-55), grip((-6, -7, 5), (-0.7, 0, -0.7), spread=-7)))
a.key(20, 'inout', **GUARD_SP)

a = attack('spear_dragon_pierce', 'Lanza: giro y perforacion (remate)')
a.key(4, **P(root(lean=6, turn=-20, up=-0.1), torso(turn=-30), spear((4, -9, 2), (0, 0.15, 1)), legs('crouch', -25)))
a.key(8, 'linear', **P(root(lean=5, turn=-200, up=-0.04), torso(turn=0), grip((1, -5, 9), (1, 0.05, 0.1), spread=-7), legs('wide')))
a.key(10, 'linear', **P(root(lean=6, turn=-345, up=-0.08), torso(turn=-30), spear((5, -8, -3), (0, 0.1, 1)), legs('crouch', -25)))
a.key(12, 'snap', **P(root(fwd=0.75, lean=16, turn=-372, up=-0.18), torso(turn=-10, lean=8), head(yaw=12),
                      spear((1, -5, 12), (0, 0.04, 1)), legs('lunge', 20)))
a.key(16, 'hold', **P(root(fwd=0.75, lean=16, turn=-372, up=-0.18)))
a.key(20, 'inout', **P(GUARD_SP, root(lean=5, turn=-385)))

# =============================================================== BASTON (cuerpo a cuerpo)
GUARD_ST = P(root(lean=2), torso(turn=-8), head(yaw=6),
             dict(rarm=R((5, -8, 6), (0, 1, 0.15)), larm=L((-6, -11, 3))), legs('guard'))

a = attack('staff_jab', 'Baston: golpe directo con la punta')
a.key(6, **P(root(lean=-3, turn=14), torso(turn=30), head(yaw=-25),
             grip((6, -6, 0), (0, 0.25, 1), spread=-6), legs('guard', 8)))
a.key(10, 'snap', **P(root(fwd=0.3, lean=10, turn=-6, up=-0.05), torso(turn=-15, lean=5), head(yaw=8),
                      grip((1, -4, 11), (0, 0.15, 1), spread=-6), legs('lunge', 4)))
a.key(13, 'hold', **P(root(fwd=0.3, lean=10, turn=-6, up=-0.05)))
a.key(20, 'inout', **GUARD_ST)

a = attack('staff_crack', 'Baston: golpe descendente')
a.key(6, **P(root(lean=-6, up=0.03), torso(turn=15, lean=-8), head(down=-10),
             grip((2, 7, 1), (0.2, 0.2, -1), spread=6), legs('guard')))
a.key(10, 'snap', **P(root(fwd=0.2, lean=14, up=-0.1), torso(turn=-10, lean=10), head(down=10),
                      grip((0, -7, 10), (0, -0.5, 0.85), spread=6), legs('lunge', 6)))
a.key(13, 'out', **P(root(fwd=0.2, lean=16, up=-0.1), grip((-1, -9, 8), (0, -0.8, 0.55), spread=6)))
a.key(20, 'inout', **GUARD_ST)

# =============================================================== POSES DE ARMA (Better Combat las mantiene)
def pose(name, desc, p):
    x = anim(name, desc, begin=1, end=10, stop=10)
    x.key(1, **p)
    x.key(10, 'hold', **p)

pose('pose_staff', 'Pose con baston: en alto, listo para conjurar', dict(rarm=R((5, -7, 6), (0, 1, 0.1))))
pose('pose_spear', 'Pose con lanza: punta adelante', P(torso(turn=-18), spear((4, -9, 4), (0, 0.2, 1))))

# =============================================================== ESQUIVAS (las reproduce el addon)
TUCK = P(dict(rarm=R((3, -6, 6), (0, 1, 0)), larm=L((-3, -6, 6), (0, 1, 0))), legs('tuck'), head(down=30))

def roll(name, desc, lean=0, tilt=0):
    x = anim(name, desc, end=13, stop=13)
    x.key(0, 'linear', **NEUTRAL_P)
    x.key(2, 'out', **P(root(up=-0.3, lean=lean * 0.08, tilt=tilt * 0.08, plant=0), TUCK))
    x.key(9, 'linear', **P(root(up=-0.62, lean=lean * 0.92, tilt=tilt * 0.92, plant=0)))
    x.key(11, 'out', **P(root(up=-0.25, lean=lean, tilt=tilt, plant=0), legs('crouch', -10),
                        dict(rarm=R((6, -10, 5)), larm=L((-6, -10, 5))), head()))
    x.key(13, 'inout', **P(NEUTRAL_P, root(lean=lean, tilt=tilt, plant=0)))

roll('roll_forward', 'Roll hacia adelante (espadachin)', lean=360)
roll('roll_backward', 'Roll hacia atras (espadachin)', lean=-360)
roll('roll_left', 'Roll lateral a la izquierda (espadachin)', tilt=-360)
roll('roll_right', 'Roll lateral a la derecha (espadachin)', tilt=360)

def dash(name, desc, lean=0, tilt=0, arms='back'):
    x = anim(name, desc, end=10, stop=10)
    arm = {
        'back': dict(rarm=R((7, -9, -6), (0, 0, -1)), larm=L((-7, -9, -6), (0, 0, -1))),
        'front': dict(rarm=R((4, -4, 8), (0, 1, 0)), larm=L((-4, -4, 8), (0, 1, 0))),
        'left': dict(rarm=R((11, -4, 2)), larm=L((-6, -11, -3))),
        'right': dict(rarm=R((6, -11, -3)), larm=L((-11, -4, 2))),
    }[arms]
    x.key(0, 'linear', **NEUTRAL_P)
    x.key(2, 'snap', **P(root(up=-0.1, lean=lean, tilt=tilt), arm, legs('stride' if lean >= 0 else 'guard', 15), head(down=-lean * 0.4)))
    x.key(6, 'hold', **P(root(up=-0.1, lean=lean, tilt=tilt)))
    x.key(10, 'inout', **NEUTRAL_P)

dash('dash_forward', 'Dash hacia adelante (mago)', lean=26, arms='back')
dash('dash_backward', 'Dash hacia atras (mago)', lean=-16, arms='front')
dash('dash_left', 'Dash a la izquierda (mago)', tilt=-22, arms='left')
dash('dash_right', 'Dash a la derecha (mago)', tilt=22, arms='right')

# =============================================================== CONJURO (pose de Rudeus)
# Cuerpo de perfil con el hombro derecho adelante, brazo derecho extendido hacia el objetivo con el baston
# apuntando adelante, mano izquierda abierta abajo y atras, piernas abiertas.
CAST = P(root(up=-0.03, turn=-20), torso(turn=-22, lean=2), head(yaw=8, down=2),
         dict(rarm=R((3, -2, 10), (0.05, 0.25, 1)), larm=L((-10, -9, -4), (0, -0.3, -1))),
         dict(rleg=((0.2, -0.95, 0.32), 12), lleg=((-0.18, -0.95, -0.28), 6)))

a = anim('cast_pose', 'Pose de conjuro estilo Rudeus (loop mientras el modo conjuro esta activo)',
         end=44, loop=True, ret=6, stop=100000)
a.key(0, 'linear', **NEUTRAL_P)
a.key(6, 'out', **CAST)
a.key(25, 'sine', **P(CAST, root(up=-0.045, turn=-20), dict(rarm=R((3, -1.4, 10), (0.05, 0.3, 1)))))
a.key(44, 'sine', **CAST)

a = anim('cast_release', 'Lanzamiento del hechizo: empuje del baston', end=14, stop=14, start=CAST)
a.key(0, 'linear', **CAST)
a.key(3, 'inout', **P(CAST, root(up=-0.02, turn=-18, fwd=-0.04), dict(rarm=R((4, 0, 7), (0.05, 0.4, 1)))))
a.key(5, 'snap', **P(CAST, root(up=-0.05, turn=-22, fwd=0.1, lean=5), dict(rarm=R((2, -2, 12), (0.03, 0.15, 1))),
                     dict(larm=L((-10, -8, -6), (0, -0.3, -1)))))
a.key(9, 'hold', **P(CAST, root(up=-0.05, turn=-22, fwd=0.1, lean=5)))
a.key(14, 'inout', **NEUTRAL_P)

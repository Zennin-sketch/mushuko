"""Todas las animaciones del addon.

Coordenadas intuitivas: (derecha, arriba, adelante). Las manos se ubican en pixeles desde el cuello, en el marco del
torso (si el torso gira, las manos giran con el). El hombro derecho esta en (5, -2, 0) y el brazo mide 10 px.
La cadera esta a -12. Angulos en grados.

Ataques de Better Combat: el golpe conecta a mitad de la animacion (tick 10 de 20). De 0 a beginTick (6) Better Combat
hace la transicion desde la pose actual, asi que el primer keyframe es la preparacion del golpe.
"""
from animgen import anim, NEUTRAL, norm
import animgen as _G

def ANIMS_REF():
    return _G.ANIMS

def R(p, blade=(0, 0, 1)):
    return (list(p), list(norm(blade)))

L = R

def grip(p, blade, spread=3.5):
    """Agarre a dos manos: la derecha en p y la izquierda sobre el mango (spread px hacia la empunadura).
    spread negativo = la izquierda va adelante (lanza)."""
    b = norm(blade)
    other = [p[i] - b[i] * spread for i in range(3)]
    return dict(rarm=R(p, b), larm=R(other, b), grip=(1, spread))

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

def curl(deg):
    """Espalda enrollada hacia adelante (cabeza y brazos acompanian)."""
    return dict(curl=(deg,))

def P(*dicts):
    out = {}
    for d in dicts:
        out.update(d)
    return out

def attack(name, desc, lag=('head', 'larm')):
    """Ataque de Better Combat: preparacion (6) -> impacto pasando con velocidad (10) -> fin del recorrido (13)
    -> recuperacion (20). Better Combat vuelve solo a la pose normal entre 20 y 28."""
    return anim(name, desc, begin=6, end=20, stop=28, lag=lag)

def two(name, desc):
    return attack(name, desc, lag=('head',))

NEUTRAL_P = dict(NEUTRAL)

# =============================================================== ESPADA (una mano) - estilo Epic Fight
# Principios (sacados de como Epic Fight arma sus combos, no de sus archivos):
# * Better Combat reproduce los ticks 0-10 en ~3 ticks reales (el golpe conecta al 25% de la recarga) y estira 10-20.
#   Por eso: casi sin preparacion (tick 0 ya es la pose cargada), corte de 3 a 10, y pose extendida sostenida 10-14.
# * La cadera y el torso abren el golpe; el brazo llega despues y se estira al impactar. La otra mano hace contrapeso.
# * Paso hacia adelante en cada golpe y rodillas flexionadas.
# * Cada golpe termina en la pose con la que arranca el siguiente: el combo fluye sin volver al reposo.
# * La mano nunca pasa por detras del torso: la carga va al costado del hombro.

def stance(lead='l', depth=0):
    """Piernas: un pie adelante (lead), el otro atras, rodillas flexionadas."""
    f, b = ((-0.06, -0.93, 0.36), (0.1, -0.95, -0.3)) if lead == 'l' else ((0.06, -0.93, 0.36), (-0.1, -0.95, -0.3))
    d = dict(lleg=(f, 18 + depth), rleg=(b, 10 + depth // 2)) if lead == 'l' else dict(rleg=(f, 18 + depth), lleg=(b, 10 + depth // 2))
    return d

def look(turn):
    """La cabeza mira al frente aunque la cadera gire."""
    return head(yaw=-turn * 0.85, down=4)

GUARD_1H = P(root(lean=3), torso(turn=-10), head(yaw=8),
             dict(rarm=R((3, -8, 8), (0.05, 0.75, 0.65)), larm=L((-6, -11, 3))), legs('guard'))

def ef_attack(name, desc):
    # beginTick 3: transicion de ~1 tick real (Epic Fight usa 0.1 s)
    return anim(name, desc, begin=3, end=20, stop=26, lag=('head', 'larm'))

# Poses de enlace del combo (fin de un golpe = inicio del siguiente)
S1_START = P(root(turn=18, lean=-2, up=-0.03), torso(turn=28, lean=-4), look(18),
             dict(rarm=R((10, 2, 3), (0.35, 0.55, -0.75)), larm=L((-3, -5, 9), (0, 0.3, 1))), stance('l', 4))
S2_START = P(root(turn=-16, lean=8, up=-0.06, fwd=0.12), torso(turn=-40, lean=8), look(-16),
             dict(rarm=R((-3, -10, 11), (-0.6, -0.65, -0.4)), larm=L((-10, -8, -2))), stance('l', 14))
S3_START = P(root(turn=16, lean=-4, up=-0.04, fwd=0.08), torso(turn=36, lean=-6), look(16),
             dict(rarm=R((9, -8, 2), (0.05, 0.12, 1)), larm=L((-2, -4, 10), (0, 0.4, 1))), stance('r', 6))

a = ef_attack('sword_slash_1', 'Espada: tajo diagonal descendente')
a.key(0, 'stop', **S1_START)
a.key(5, 'smooth', **P(root(turn=6, lean=2, up=-0.04, fwd=0.04), torso(turn=6, lean=2), look(6),
                       dict(rarm=R((8, -1, 10), (0.15, 0.5, 0.85)))))
a.key(8, 'smooth', **P(root(turn=-8, lean=7, up=-0.06, fwd=0.1), torso(turn=-24, lean=6, tilt=-4), look(-8),
                       dict(rarm=R((3, -7, 12), (-0.7, -0.35, 0.6)), larm=L((-9, -8, 1))), stance('l', 12)))
a.key(10, 'smooth', **P(root(turn=-14, lean=8, up=-0.06, fwd=0.12), torso(turn=-36, lean=8, tilt=-5), look(-14),
                        dict(rarm=R((-2, -9, 11), (-0.75, -0.55, -0.2)), larm=L((-10, -8, -2)))))
a.key(14, 'stop', **P(S2_START, root(turn=-17, lean=9, up=-0.065, fwd=0.13)))
a.key(20, 'stop', **S2_START)

a = ef_attack('sword_slash_2', 'Espada: reves ascendente')
a.key(0, 'stop', **S2_START)
a.key(5, 'smooth', **P(root(turn=-4, lean=5, up=-0.05, fwd=0.1), torso(turn=-14, lean=4), look(-4),
                       dict(rarm=R((0, -9, 12), (-0.25, -0.2, 1)), larm=L((-10, -8, 0)))))
a.key(8, 'smooth', **P(root(turn=8, lean=1, up=-0.03, fwd=0.08), torso(turn=18, lean=-2, tilt=4), look(8),
                       dict(rarm=R((7, -2, 11), (0.65, 0.45, 0.6)), larm=L((-5, -8, 5))), stance('r', 8)))
a.key(10, 'smooth', **P(root(turn=13, lean=-2, up=-0.02, fwd=0.08), torso(turn=32, lean=-5, tilt=6), look(13),
                        dict(rarm=R((10, 3, 6), (0.55, 0.8, -0.2)))))
a.key(14, 'stop', **P(root(turn=15, lean=-4, up=-0.02, fwd=0.08), torso(turn=40, lean=-7, tilt=7), look(15),
                      dict(rarm=R((10, 5, 2), (0.3, 0.9, -0.35)), larm=L((-4, -6, 8), (0, 0.4, 1)))))
a.key(20, 'stop', **S3_START)

a = ef_attack('sword_thrust_3', 'Espada: estocada a fondo (remate)')
a.key(0, 'stop', **S3_START)
a.key(4, 'smooth', **P(root(turn=10, lean=0, up=-0.08, fwd=0.05), torso(turn=24, lean=-2), look(10),
                       dict(rarm=R((8, -8, 2), (0.03, 0.1, 1))), stance('r', 18)))
a.key(8, 'smooth', **P(root(turn=-4, lean=10, up=-0.12, fwd=0.35), torso(turn=-8, lean=5), look(-4),
                       dict(rarm=R((4, -4, 10), (0, 0.06, 1)), larm=L((-11, -7, -3), (0, 0, -1))), stance('r', 30)))
a.key(10, 'smooth', **P(root(turn=-10, lean=14, up=-0.15, fwd=0.55), torso(turn=-20, lean=6), look(-10),
                        dict(rarm=R((2, -2, 13), (0, 0.04, 1)), larm=L((-12, -7, -5), (0, 0, -1)))))
a.key(15, 'stop', **P(root(turn=-11, lean=15, up=-0.16, fwd=0.58), torso(turn=-22, lean=7), look(-11),
                      dict(rarm=R((2, -2.5, 13), (0, 0.02, 1)))))
a.key(20, 'stop', **P(GUARD_1H, root(lean=3, fwd=0.2)))

# =============================================================== KATANA (dos manos)
GUARD_K = P(root(lean=4, turn=-6), torso(turn=-12), head(yaw=10), grip((1, -8, 8), (0.05, 0.75, 0.65)), legs('guard', 4))

a = two('katana_iai', 'Katana: corte horizontal al desenvainar')
a.key(6, 'stop', **P(root(lean=8, turn=-20, up=-0.08), torso(turn=-50, lean=8), head(yaw=48, down=4),
             grip((-6, -11, 3), (-0.2, -0.05, -1)), legs('crouch', -35)))
a.key(10, 'smooth', **P(root(fwd=0.3, lean=8, turn=20, up=-0.1), torso(turn=45, lean=4), head(yaw=-30),
                      grip((6, -5, 10), (0.95, 0.05, 0.3)), legs('lunge', 5)))
a.key(13, 'stop', **P(root(fwd=0.32, lean=8, turn=26, up=-0.1), torso(turn=60, lean=4),
                     grip((8, -5, 5), (0.8, 0.1, -0.6))))
a.key(20, 'stop', **GUARD_K)

a = two('katana_overhead', 'Katana: corte vertical desde arriba')
a.key(6, 'stop', **P(root(lean=-6, up=0.04), torso(lean=-8), head(down=-12),
             grip((1, 7, 2), (0, 0.25, -1)), legs('guard')))
a.key(10, 'smooth', **P(root(fwd=0.25, lean=16, up=-0.12), torso(lean=12), head(down=14),
                      grip((0, -8, 11), (0, -0.55, 0.85)), legs('lunge', 10)))
a.key(13, 'stop', **P(root(fwd=0.26, lean=18, up=-0.13), grip((0, -11, 8), (0, -0.85, 0.5))))
a.key(20, 'stop', **GUARD_K)

a = two('katana_whirlwind', 'Katana: giro completo (remate)')
a.key(5, 'stop', **P(root(lean=10, turn=10, up=-0.14), torso(turn=40, lean=8), head(yaw=-30),
             grip((6, -10, 2), (0.5, -0.1, -0.85)), legs('crouch', -20)))
a.key(8, 'smooth', **P(root(lean=10, turn=-120, up=-0.12), torso(turn=0), grip((5, -6, 10), (1, 0, 0.1))))
a.key(11, 'smooth', **P(root(lean=8, turn=-360, up=-0.1, fwd=0.2), torso(turn=-30), grip((-5, -6, 10), (-1, 0, 0.2)), legs('wide')))
a.key(14, 'stop', **P(root(lean=8, turn=-380, up=-0.1, fwd=0.22), torso(turn=-40), grip((-7, -7, 5), (-0.85, -0.1, -0.5))))
a.key(20, 'stop', **P(GUARD_K, root(lean=4, turn=-366)))

# =============================================================== ARMA PESADA (mandoble / hacha)
GUARD_H = P(root(lean=3, turn=-8), torso(turn=-15), head(yaw=12), grip((2, -8, 7), (0.2, 0.9, 0.35)), legs('wide'))

a = two('heavy_sweep', 'Arma pesada: barrido horizontal')
a.key(6, 'stop', **P(root(lean=5, turn=24, up=-0.05), torso(turn=55, lean=4), head(yaw=-45),
             grip((7, -3, -1), (0.6, 0.4, -0.7)), legs('wide', 10)))
a.key(10, 'smooth', **P(root(fwd=0.15, lean=8, turn=-18, up=-0.08), torso(turn=-40, lean=6), head(yaw=30),
                      grip((-3, -5, 11), (-0.9, 0.05, 0.4)), legs('wide', 14)))
a.key(14, 'stop', **P(root(fwd=0.16, lean=8, turn=-26, up=-0.08), torso(turn=-62, lean=6), grip((-7, -6, 5), (-0.7, -0.05, -0.7))))
a.key(20, 'stop', **GUARD_H)

a = two('heavy_rise', 'Arma pesada: corte ascendente de vuelta')
a.key(6, 'stop', **P(root(lean=10, turn=-20, up=-0.1), torso(turn=-55, lean=8), head(yaw=40),
             grip((-6, -11, 4), (-0.5, -0.6, -0.6)), legs('crouch', -30)))
a.key(10, 'smooth', **P(root(fwd=0.12, lean=-4, turn=16, up=0.05), torso(turn=35, lean=-8), head(yaw=-20, down=-10),
                      grip((4, 4, 8), (0.4, 0.9, 0.1)), legs('stride')))
a.key(14, 'stop', **P(root(fwd=0.12, lean=-6, turn=20, up=0.05), torso(turn=45, lean=-10), grip((4, 7, 4), (0.3, 0.9, -0.3))))
a.key(20, 'stop', **GUARD_H)

a = two('heavy_slam', 'Arma pesada: golpe aplastante (remate)')
a.key(5, 'stop', **P(root(lean=-10, up=0.06), torso(lean=-12), head(down=-18),
             grip((0, 8, 0), (0, 0.1, -1)), legs('guard')))
a.key(8, 'smooth', **P(root(lean=-6, up=0.25, fwd=0.1), grip((0, 9, 3), (0, 0.6, -0.8)), legs('tuck')))
a.key(11, 'smooth', **P(root(fwd=0.35, lean=24, up=-0.3), torso(lean=14), head(down=10),
                      grip((0, -9, 12), (0, -0.75, 0.65)), legs('crouch', 10)))
a.key(15, 'stop', **P(root(fwd=0.35, lean=24, up=-0.3)))
a.key(20, 'stop', **GUARD_H)

# =============================================================== GUADANIA
GUARD_S = P(root(lean=3, turn=-12), torso(turn=-25), head(yaw=20), grip((4, -7, 6), (0.1, 0.95, 0.2), spread=6), legs('wide'))

a = two('scythe_reap', 'Guadana: siega amplia hacia adentro')
a.key(6, 'stop', **P(root(lean=5, turn=30, up=-0.04), torso(turn=60), head(yaw=-50),
             grip((8, -2, -2), (0.6, 0.6, -0.5), spread=6), legs('wide', 8)))
a.key(10, 'smooth', **P(root(fwd=0.15, lean=10, turn=-15, up=-0.1), torso(turn=-35, lean=6), head(yaw=25),
                      grip((-1, -6, 12), (-0.6, 0.5, 0.6), spread=6), legs('wide', 16)))
a.key(14, 'stop', **P(root(fwd=0.16, lean=10, turn=-28, up=-0.1), torso(turn=-65, lean=6), grip((-7, -8, 5), (-0.8, 0.4, -0.4), spread=6)))
a.key(20, 'stop', **GUARD_S)

a = two('scythe_backswing', 'Guadana: tajo de vuelta')
a.key(6, 'stop', **P(root(lean=6, turn=-28, up=-0.06), torso(turn=-60, lean=5), head(yaw=45),
             grip((-7, -6, 3), (-0.7, 0.4, -0.6), spread=6), legs('wide', 10)))
a.key(10, 'smooth', **P(root(fwd=0.12, lean=6, turn=18, up=-0.06), torso(turn=40, lean=4), head(yaw=-25),
                      grip((6, -4, 10), (0.7, 0.4, 0.55), spread=6), legs('stride')))
a.key(14, 'stop', **P(root(fwd=0.12, lean=6, turn=26, up=-0.06), torso(turn=62), grip((8, -4, 3), (0.8, 0.4, -0.45), spread=6)))
a.key(20, 'stop', **GUARD_S)

a = two('scythe_cyclone', 'Guadana: torbellino (remate)')
a.key(5, 'stop', **P(root(lean=8, turn=20, up=-0.12), torso(turn=45, lean=6), head(yaw=-35),
             grip((8, -4, 0), (0.8, 0.3, -0.5), spread=6), legs('crouch', -25)))
a.key(8, 'smooth', **P(root(lean=8, turn=-150, up=-0.1), torso(turn=0), grip((3, -5, 12), (0.2, 0.3, 1), spread=6), legs('wide')))
a.key(12, 'smooth', **P(root(lean=6, turn=-400, up=-0.08, fwd=0.15), torso(turn=-30), grip((-6, -6, 10), (-0.8, 0.3, 0.5), spread=6)))
a.key(15, 'stop', **P(root(lean=6, turn=-420, up=-0.08, fwd=0.15), torso(turn=-50), grip((-8, -7, 3), (-0.8, 0.3, -0.5), spread=6)))
a.key(20, 'stop', **P(GUARD_S, root(lean=3, turn=-372)))

# =============================================================== LANZA
def spear(p, d):
    """Lanza: la derecha atras sobre el asta y la izquierda 7 px adelante."""
    return grip(p, d, spread=-7)

GUARD_SP = P(root(lean=5, turn=-25), torso(turn=-30), head(yaw=25), spear((4, -9, 3), (0, 0.18, 1)), legs('guard', 6))

a = two('spear_thrust', 'Lanza: estocada rapida')
a.key(6, 'stop', **P(root(lean=2, turn=-28, fwd=-0.08), torso(turn=-36, lean=-2), head(yaw=30),
             spear((5, -8, -2), (0, 0.12, 1)), legs('guard', 10)))
a.key(10, 'smooth', **P(root(fwd=0.4, lean=12, turn=-20, up=-0.08), torso(turn=-15, lean=6), head(yaw=18),
                      spear((1, -6, 10), (0, 0.05, 1)), legs('lunge', 8)))
a.key(13, 'stop', **P(root(fwd=0.4, lean=12, turn=-20, up=-0.08)))
a.key(20, 'stop', **GUARD_SP)

a = two('spear_sweep', 'Lanza: barrido con el asta')
a.key(6, 'stop', **P(root(lean=4, turn=20), torso(turn=40), head(yaw=-30),
             grip((7, -6, 3), (0.85, 0.05, 0.5), spread=-7), legs('wide', 8)))
a.key(10, 'smooth', **P(root(fwd=0.12, lean=7, turn=-22, up=-0.06), torso(turn=-35, lean=5), head(yaw=25),
                      grip((-2, -6, 10), (-0.9, 0.05, 0.4), spread=-7), legs('wide', 14)))
a.key(14, 'stop', **P(root(fwd=0.12, lean=7, turn=-30, up=-0.06), torso(turn=-55), grip((-6, -7, 5), (-0.7, 0, -0.7), spread=-7)))
a.key(20, 'stop', **GUARD_SP)

a = two('spear_dragon_pierce', 'Lanza: giro y perforacion (remate)')
a.key(4, 'stop', **P(root(lean=6, turn=-20, up=-0.1), torso(turn=-30), spear((4, -9, 2), (0, 0.15, 1)), legs('crouch', -25)))
a.key(8, 'smooth', **P(root(lean=5, turn=-200, up=-0.04), torso(turn=0), grip((1, -5, 9), (1, 0.05, 0.1), spread=-7), legs('wide')))
a.key(10, 'smooth', **P(root(lean=6, turn=-345, up=-0.08), torso(turn=-30), spear((5, -8, -3), (0, 0.1, 1)), legs('crouch', -25)))
a.key(12, 'smooth', **P(root(fwd=0.75, lean=16, turn=-372, up=-0.18), torso(turn=-10, lean=8), head(yaw=12),
                      spear((1, -5, 12), (0, 0.04, 1)), legs('lunge', 20)))
a.key(16, 'stop', **P(root(fwd=0.75, lean=16, turn=-372, up=-0.18)))
a.key(20, 'stop', **P(GUARD_SP, root(lean=5, turn=-385)))

# =============================================================== BASTON (cuerpo a cuerpo)
GUARD_ST = P(root(lean=2), torso(turn=-8), head(yaw=6),
             dict(rarm=R((5, -8, 6), (0, 1, 0.15)), larm=L((-6, -11, 3))), legs('guard'))

a = two('staff_jab', 'Baston: golpe directo con la punta')
a.key(6, 'stop', **P(root(lean=-3, turn=14), torso(turn=30), head(yaw=-25),
             grip((6, -6, 0), (0, 0.25, 1), spread=-6), legs('guard', 8)))
a.key(10, 'smooth', **P(root(fwd=0.3, lean=10, turn=-6, up=-0.05), torso(turn=-15, lean=5), head(yaw=8),
                      grip((1, -4, 11), (0, 0.15, 1), spread=-6), legs('lunge', 4)))
a.key(13, 'stop', **P(root(fwd=0.3, lean=10, turn=-6, up=-0.05)))
a.key(20, 'stop', **GUARD_ST)

a = two('staff_crack', 'Baston: golpe descendente')
a.key(6, 'stop', **P(root(lean=-6, up=0.03), torso(turn=15, lean=-8), head(down=-10),
             grip((2, 7, 1), (0.2, 0.2, -1), spread=6), legs('guard')))
a.key(10, 'smooth', **P(root(fwd=0.2, lean=14, up=-0.1), torso(turn=-10, lean=10), head(down=10),
                      grip((0, -7, 10), (0, -0.5, 0.85), spread=6), legs('lunge', 6)))
a.key(13, 'stop', **P(root(fwd=0.2, lean=16, up=-0.1), grip((-1, -9, 8), (0, -0.8, 0.55), spread=6)))
a.key(20, 'stop', **GUARD_ST)

# =============================================================== POSES DE ARMA (Better Combat las mantiene)
def pose(name, desc, p):
    x = anim(name, desc, begin=1, end=10, stop=10)
    x.key(1, **p)
    x.key(10, 'stop', **p)

pose('pose_staff', 'Pose con baston: en alto, listo para conjurar', dict(rarm=R((5, -7, 6), (0, 1, 0.1))))
pose('pose_spear', 'Pose con lanza: punta adelante', P(torso(turn=-18), spear((4, -9, 4), (0, 0.2, 1))))

# =============================================================== ESQUIVAS (las reproduce el addon)
# Con peso: anticipacion (se carga hacia el lado contrario), impulso (los brazos y la cabeza quedan atras por
# inercia), deslizamiento, frenada (el torso sigue de largo y las rodillas absorben), rebote y reposo.
LIMBS_LAG = ('head', 'rarm', 'larm')

def dash(name, desc, d):
    """d = direccion: (der, adelante). Inclinacion hacia donde va, brazos al reves."""
    dx, df = d
    lean, tilt = 1 if df > 0 else -1 if df < 0 else 0, 1 if dx > 0 else -1 if dx < 0 else 0
    if dx and df:  # diagonal: la inclinacion se reparte entre los dos ejes
        lean, tilt = lean * 0.72, tilt * 0.72
    x = anim(name, desc, end=13, stop=17, lag=LIMBS_LAG, ground='contact')
    def arms(k):  # k > 0: brazos atras respecto del movimiento; k < 0: adelante
        return dict(rarm=R((6 + 2 * dx * -k, -9 + abs(k), -k * 6 * df + 1), (0, 0, 1)),
                    larm=L((-6 + 2 * dx * -k, -9 + abs(k), -k * 6 * df + 1), (0, 0, 1)))
    stance = legs('stride' if df >= 0 else 'guard', 10)
    x.key(0, 'stop', **NEUTRAL_P)
    x.key(2, 'stop', **P(root(up=-0.06, lean=-7 * lean, tilt=-6 * tilt), arms(-0.5), legs('crouch', -45), head(down=4)))
    x.key(4, 'smooth', **P(root(up=-0.12, lean=22 * lean, tilt=20 * tilt), arms(1.2), stance, head(down=-6 * lean)))
    x.key(7, 'smooth', **P(root(up=-0.13, lean=20 * lean, tilt=18 * tilt), arms(1.5)))
    x.key(9, 'stop', **P(root(up=-0.2, lean=32 * lean, tilt=28 * tilt), arms(-1.0), legs('crouch', -15), head(down=10 * lean)))
    x.key(11, 'smooth', **P(root(up=-0.05, lean=-5 * lean, tilt=-5 * tilt), arms(0.4), legs('guard'), head()))
    x.key(13, 'stop', **NEUTRAL_P)

dash('dash_forward', 'Dash hacia adelante (mago)', (0, 1))
dash('dash_backward', 'Dash hacia atras (mago)', (0, -1))
dash('dash_left', 'Dash a la izquierda (mago)', (-1, 0))
dash('dash_right', 'Dash a la derecha (mago)', (1, 0))
dash('dash_forward_left', 'Dash diagonal adelante-izquierda (mago)', (-1, 1))
dash('dash_forward_right', 'Dash diagonal adelante-derecha (mago)', (1, 1))
dash('dash_backward_left', 'Dash diagonal atras-izquierda (mago)', (-1, -1))
dash('dash_backward_right', 'Dash diagonal atras-derecha (mago)', (1, -1))

def roll(name, desc, lean=0, tilt=0, lean_in=None, turn=0):
    """Roll en 5 tiempos: 1) se inclina y da un paso hacia donde va a rodar, 2) se zambulle encogiendose,
    3) gira, 4) cae amortiguando con los brazos abiertos, 5) se levanta."""
    sl, st = (1 if lean > 0 else -1 if lean < 0 else 0), (1 if tilt > 0 else -1 if tilt < 0 else 0)
    x = anim(name, desc, end=17, stop=21, lag=LIMBS_LAG, ground='contact')
    # El arma va a lo largo del eje de giro (no gira con el cuerpo, asi no toca ni el cuerpo ni el piso):
    # hacia la derecha en los rolls adelante/atras y hacia atras en los laterales.
    side_roll = tilt != 0
    wb = (0.25, 0.15, -1) if side_roll else (1, 0.15, -0.1)
    hold = R((10, -5, -3), wb) if side_roll else R((6, -7, 4), wb)
    # al caer ya no gira: el arma vuelve de a poco hacia adelante
    balance = dict(rarm=R((10, -7, 2), (0.6, 0.3, 0.75) if side_roll else wb), larm=L((-10, -7, 2)))
    tuck = P(TUCK, dict(rarm=hold, larm=TUCK['larm']))
    x.key(0, 'stop', **NEUTRAL_P)
    half = {k: v for k, v in lean_in.items()}
    r = list(half['root']); r[1] *= 0.4; r[3] *= 0.4; r[5] *= 0.4
    half['root'] = tuple(r)
    li = dict(lean_in); li['rarm'] = R(list(lean_in['rarm'][0][:1]) + [lean_in['rarm'][0][1] + 3, lean_in['rarm'][0][2] - 2], wb)
    half['rarm'] = R(li['rarm'][0], (0.8, 0.5, 0.3) if side_roll else (0.7, 0.2, 0.7))  # transicion hacia el eje de giro
    x.key(2, 'smooth', **P(half, head(down=8)))
    x.key(4, 'stop', **P(li, head(down=18)))
    c = 70 if tilt == 0 else 62  # cuanto se enrolla la espalda
    x.key(6, 'smooth', **P(root(up=-0.3, lean=lean * 0.25 + lean_in['root'][3] * 0.3, tilt=tilt * 0.25 + lean_in['root'][5] * 0.3, plant=0.3), tuck, curl(c * 0.8)))
    x.key(10, 'smooth', **P(root(up=-0.4, lean=lean * 0.7, tilt=tilt * 0.7, plant=0), curl(c)))
    x.key(13, 'smooth', **P(root(up=-0.3, lean=lean * 0.97, tilt=tilt * 0.97, plant=0), legs('crouch', 15), curl(c * 0.45)))
    x.key(14, 'stop', **P(root(up=-0.24, lean=lean + 6 * sl, tilt=tilt + 6 * st, plant=0), balance, legs('crouch', 15), head(down=5), curl(c * 0.15)))
    x.key(17, 'stop', **P(NEUTRAL_P, root(lean=lean, tilt=tilt, plant=0)))
    if turn:
        # Diagonal: el cuerpo gira hacia donde va a rodar, rueda en esa direccion y al levantarse vuelve al frente
        ramp = {0: 0.0, 2: 0.6, 17: 0.0}
        x.keys = [(t, f[:4] + [f[4] + turn * ramp.get(t, 1.0)] + f[5:], m) for t, f, m in x.keys]

TUCK = P(dict(rarm=R((3, -6, 6), (0, 1, 0)), larm=L((-3, -6, 6), (0, 1, 0))), legs('tuck'), head(down=30))
roll('roll_forward', 'Roll hacia adelante (espadachin)', lean=360, lean_in=P(
    root(up=-0.18, lean=38), legs('lunge', 15), dict(rarm=R((4, -12, 8)), larm=L((-4, -12, 8)))))
roll('roll_backward', 'Roll hacia atras (espadachin)', lean=-360, lean_in=P(
    root(up=-0.28, lean=-22), legs('crouch', -5), dict(rarm=R((5, -6, 8)), larm=L((-5, -6, 8)))))
roll('roll_left', 'Roll lateral a la izquierda (espadachin)', tilt=-360, lean_in=P(
    root(up=-0.16, tilt=-34), dict(lleg=((-0.45, -0.9, 0.05), 30), rleg=((0.1, -1, 0), 5)),
    dict(rarm=R((0, -12, 4)), larm=L((-9, -12, 2)))))
roll('roll_right', 'Roll lateral a la derecha (espadachin)', tilt=360, lean_in=P(
    root(up=-0.16, tilt=34), dict(rleg=((0.45, -0.9, 0.05), 30), lleg=((-0.1, -1, 0), 5)),
    dict(rarm=R((9, -12, 2)), larm=L((0, -12, 4)))))

# Diagonales: el roll de adelante / atras con el cuerpo girado 45 grados hacia donde va (giro positivo = a la derecha)
ROLL_FWD_IN = P(root(up=-0.18, lean=38), legs('lunge', 15), dict(rarm=R((4, -12, 8)), larm=L((-4, -12, 8))))
ROLL_BACK_IN = P(root(up=-0.28, lean=-22), legs('crouch', -5), dict(rarm=R((5, -6, 8)), larm=L((-5, -6, 8))))
roll('roll_forward_left', 'Roll diagonal adelante-izquierda (espadachin)', lean=360, lean_in=ROLL_FWD_IN, turn=-45)
roll('roll_forward_right', 'Roll diagonal adelante-derecha (espadachin)', lean=360, lean_in=ROLL_FWD_IN, turn=45)
roll('roll_backward_left', 'Roll diagonal atras-izquierda (espadachin)', lean=-360, lean_in=ROLL_BACK_IN, turn=45)
roll('roll_backward_right', 'Roll diagonal atras-derecha (espadachin)', lean=-360, lean_in=ROLL_BACK_IN, turn=-45)

# =============================================================== DESENVAINAR / ENVAINAR
# El arma guardada se dibuja exactamente donde queda la mano en el tick de "agarre": por eso las dos poses de agarre
# (GRAB_HIP, GRAB_BACK) se comparten entre sacar y guardar, y sheaths.json se calcula de ahi.
GRAB_HIP = P(root(lean=4), torso(turn=-24, lean=4), head(yaw=-20, down=18),
             dict(rarm=R((-4.5, -11, 4), (-0.15, -0.45, -0.88)), larm=L((-6, -12, 1), (0, -0.3, -1))))
# Espalda: el arma cruza la espalda en diagonal, con el mango sobre el hombro derecho y la hoja hacia la cadera izquierda.
GRAB_BACK = P(root(lean=-2), torso(turn=10, lean=-4), head(yaw=-18, down=-4),
              dict(rarm=R((4.5, 4, -3.5), (-0.5, -0.85, -0.15)), larm=L((-6, -11, 2))))
SHEATH_SLOTS = {}  # slot -> (animacion, tick de agarre)

def draw_hip():
    x = anim('hip_draw', 'Desenvainar desde la cintura', end=14, stop=18, lag=('head', 'larm'))
    x.key(0, 'stop', **NEUTRAL_P)
    x.key(3, 'smooth', **P(root(lean=2), torso(turn=-14), head(yaw=-12, down=10),
                           dict(rarm=R((0, -10, 6), (-0.1, -0.2, -0.9)), larm=L((-6, -12, 1)))))
    x.key(5, 'stop', **GRAB_HIP)
    x.key(8, 'smooth', **P(root(lean=3, turn=6), torso(turn=16, lean=-2), head(yaw=-4, down=0),
                           dict(rarm=R((7, -1, 8), (0.5, 0.75, 0.4)), larm=L((-6, -11, -2)))))
    x.key(11, 'stop', **GUARD_1H)
    x.key(14, 'stop', **P(NEUTRAL_P, dict(rarm=R((5, -11, 4), (0, 0.5, 1)))))
    SHEATH_SLOTS['hip'] = (x, 5)

def sheathe_hip():
    x = anim('hip_sheathe', 'Envainar en la cintura', end=13, stop=17, lag=('head', 'larm'))
    x.key(0, 'stop', **P(NEUTRAL_P, dict(rarm=R((5, -11, 4), (0, 0.5, 1)))))
    x.key(4, 'smooth', **P(root(lean=2), torso(turn=-10), head(yaw=-14, down=12),
                           dict(rarm=R((2, -5, 8), (-0.45, -0.1, -0.9)), larm=L((-6, -12, 1)))))
    x.key(8, 'stop', **GRAB_HIP)
    x.key(10, 'smooth', **P(root(lean=1), torso(turn=-8), head(down=6), dict(rarm=R((2, -11, 4)))))
    x.key(13, 'stop', **NEUTRAL_P)

def draw_back():
    """Sacar de la espalda: sube la mano al hombro, agarra el mango, tira hacia arriba sacando la hoja por encima
    del hombro, la hace pasar por arriba de la cabeza y la baja adelante con el peso (rodillas) hasta la guardia."""
    x = anim('back_draw', 'Desenvainar desde la espalda', end=16, stop=20, lag=('head', 'larm'))
    x.key(0, 'stop', **NEUTRAL_P)
    x.key(3, 'smooth', **P(torso(turn=6), head(yaw=-10, down=-2), dict(rarm=R((6, 1, 3), (0, 1, 0.2)))))
    x.key(5, 'stop', **GRAB_BACK)
    x.key(8, 'smooth', **P(root(lean=-4, up=0.02), torso(turn=4, lean=-8), head(yaw=-8, down=-10),
                           dict(rarm=R((5, 7, -1), (-0.2, 0.75, -0.65)), larm=L((-6, -10, 3)))))
    x.key(10, 'smooth', **P(root(lean=2), torso(turn=-4, lean=-2), head(yaw=0, down=-4),
                            dict(rarm=R((4, 6, 5), (0, 0.85, 0.5)), larm=L((-3, 2, 7), (0, 0.85, 0.5)), grip=(1, 3.5))))
    x.key(13, 'stop', **P(root(lean=6, up=-0.06, fwd=0.04), torso(turn=-8, lean=5), head(yaw=8, down=4),
                          grip((2, -7, 9), (0.15, 0.45, 0.88)), legs('wide', 14)))
    x.key(16, 'stop', **GUARD_H)
    SHEATH_SLOTS['back'] = (x, 5)

def sheathe_back():
    """Guardar en la espalda: levanta el arma por encima del hombro derecho, la baja por detras hasta que el mango
    queda sobre el hombro, suelta y baja la mano."""
    x = anim('back_sheathe', 'Guardar en la espalda', end=16, stop=20, lag=('head', 'larm'))
    x.key(0, 'stop', **GUARD_H)
    x.key(4, 'smooth', **P(root(lean=-2), torso(turn=2, lean=-4), head(yaw=-4, down=-8),
                           dict(rarm=R((4, 6, 4), (0, 0.9, 0.2)), larm=L((-3, 2, 6), (0, 0.9, 0.2)), grip=(1, 3.5))))
    x.key(7, 'smooth', **P(root(lean=-3), torso(turn=8, lean=-6), head(yaw=-12, down=-8),
                           dict(rarm=R((5, 7, -1), (-0.3, 0.3, -0.9)), larm=L((-6, -9, 3)))))
    x.key(9, 'stop', **GRAB_BACK)
    # Soltar: la mano baja por el costado, siempre con el brazo casi estirado (si la mano pasa cerca del hombro el
    # codo se pliega del todo y el brazo se retuerce) y girando la muneca de a poco hasta la pose normal.
    x.key(11, 'smooth', **P(torso(turn=6, lean=-2), head(yaw=-8, down=-2),
                            dict(rarm=R((9, -8, -3), (0.15, -0.75, 0.6)), larm=L((-5.5, -12, 1)))))
    x.key(13, 'smooth', **P(torso(turn=2), head(yaw=-2), dict(rarm=R((6, -11, 0.5), (0.05, -0.35, 0.95)))))
    x.key(16, 'stop', **NEUTRAL_P)

# =============================================================== ARCO
# El arco va en la mano habil (derecha, como en vanilla) y la izquierda tensa la cuerda hasta la mejilla.
# bow_draw: saca una flecha del carcaj (sobre el hombro derecho), la coloca en la cuerda, tensa y se queda apuntando
# (loop). bow_release: la mano de la cuerda sale disparada hacia atras y el arco retrocede un poco.
BOW_HOLD = (0, 1, 0.15)
BOW_DRAWN = P(root(up=-0.02, turn=-18), torso(turn=-22, lean=1), head(yaw=20, down=0),
              dict(rarm=R((3, -1.5, 10), BOW_HOLD), larm=L((-1.5, 1, 2.5), (0, 0.2, 1))),
              dict(rleg=((0.16, -0.97, 0.22), 8), lleg=((-0.16, -0.97, -0.24), 6)))

def bow():
    x = anim('bow_draw', 'Arco: sacar flecha del carcaj, colocarla, tensar y apuntar (loop)', end=22, loop=True, ret=13,
             stop=100000, lag=('head',))
    x.key(0, 'smooth', **P(NEUTRAL_P, dict(rarm=R((4, -6, 7), (0, 0.8, 0.6)))))
    x.key(4, 'smooth', **P(torso(turn=8), head(yaw=-16, down=-2),
                           dict(rarm=R((4, -4, 8), (0, 0.9, 0.4)), larm=L((3, 1, -3.5), (0, 1, 0)))))
    x.key(8, 'smooth', **P(root(turn=-8), torso(turn=-10), head(yaw=8),
                           dict(rarm=R((3, -2, 10), BOW_HOLD), larm=L((2.2, -2, 8.5), (0, 0.3, 1)))))
    x.key(13, 'stop', **BOW_DRAWN)
    x.key(18, 'smooth', **P(BOW_DRAWN, dict(larm=L((-1.7, 1.1, 2.3), (0, 0.2, 1)))))
    x.key(22, 'smooth', **BOW_DRAWN)

    y = anim('bow_release', 'Arco: soltar la flecha', end=12, stop=16, start=BOW_DRAWN, lag=('head',))
    y.key(0, 'smooth', **BOW_DRAWN)
    y.key(2, 'stop', **P(BOW_DRAWN, dict(rarm=R((3, -1.8, 10.5), (0, 1, 0.05)), larm=L((-4, 1.5, -3.5), (0, 0.5, -1)))))
    y.key(6, 'smooth', **P(BOW_DRAWN, dict(larm=L((-5.5, -4, -1.5)))))
    y.key(12, 'stop', **NEUTRAL_P)

bow()

draw_hip(); sheathe_hip(); draw_back(); sheathe_back()
for _a in ANIMS_REF():
    if _a.name.endswith(('_draw', '_sheathe')):
        _a.continuity = False

# =============================================================== CONJURO (pose de Rudeus)
# Cuerpo de perfil con el hombro derecho adelante, brazo derecho extendido hacia el objetivo con el baston
# apuntando adelante, mano izquierda abierta abajo y atras, piernas abiertas.
CAST = P(root(up=-0.03, turn=-20), torso(turn=-22, lean=2), head(yaw=8, down=2),
         dict(rarm=R((3, -2, 10), (0.05, 0.25, 1)), larm=L((-11, -6, -3), (0, -0.3, -1))),
         dict(rleg=((0.2, -0.95, 0.32), 12), lleg=((-0.18, -0.95, -0.28), 6)))

a = anim('cast_pose', 'Pose de conjuro estilo Rudeus (loop mientras el modo conjuro esta activo)',
         end=44, loop=True, ret=6, stop=100000, lag=('head', 'larm'))
a.key(0, 'smooth', **NEUTRAL_P)
a.key(6, 'stop', **CAST)
a.key(25, 'smooth', **P(CAST, root(up=-0.045, turn=-20), dict(rarm=R((3, -1.4, 10), (0.05, 0.3, 1)))))
a.key(44, 'smooth', **CAST)

a = anim('cast_release', 'Lanzamiento del hechizo: empuje del baston', end=16, stop=20, start=CAST, lag=('head', 'larm'))
a.key(0, 'smooth', **CAST)
a.key(3, 'stop', **P(CAST, root(up=-0.02, turn=-18, fwd=-0.04), dict(rarm=R((4, 0, 7), (0.05, 0.4, 1)))))
a.key(5, 'smooth', **P(CAST, root(up=-0.05, turn=-22, fwd=0.1, lean=5), dict(rarm=R((2, -2, 12), (0.03, 0.15, 1))),
                     dict(larm=L((-11, -5, -5), (0, -0.3, -1)))))
a.key(9, 'stop', **P(CAST, root(up=-0.05, turn=-22, fwd=0.1, lean=5)))
a.key(16, 'stop', **NEUTRAL_P)

# =============================================================== IDEAS DE ANIMACIONES DE MAGIA (8)
# Para tomar de base en los conjuros de cada hechizo. Todas con el baston en la mano derecha.
def spell_anim(name, desc, end):
    return anim(name, desc, end=end, stop=end + 4, lag=('head', 'larm'))

# 1) Empuje doble: se agacha, junta las manos al pecho y empuja todo hacia adelante (Water Cannon, Sonic Boom)
a = spell_anim('magic_push', 'Magia: empuje doble con las dos manos', 26)
a.key(0, 'stop', **NEUTRAL_P)
a.key(6, 'stop', **P(root(up=-0.08, fwd=-0.08, lean=-4), torso(lean=-6), head(down=6),
                     dict(rarm=R((3, -6, 5), (0, 1, 0.3)), larm=L((-2, -6, 5), (0, 0.3, 1))), legs('guard', 12)))
a.key(9, 'smooth', **P(root(up=-0.1, fwd=0.2, lean=10), torso(lean=6), head(down=0),
                       dict(rarm=R((3, -3, 11), (0, 0.4, 1)), larm=L((-3, -3, 11), (0, 0.3, 1))), legs('lunge', 10)))
a.key(14, 'stop', **P(root(up=-0.1, fwd=0.22, lean=11), dict(rarm=R((3, -3.5, 11.5), (0, 0.4, 1)))))
a.key(26, 'stop', **NEUTRAL_P)

# 2) Reunir sobre la cabeza y lanzar: junta el mana arriba (la bola crece) y la arroja (Ex-Flame, Exodus Flame)
a = spell_anim('magic_gather_throw', 'Magia: reunir sobre la cabeza y lanzar', 34)
a.key(0, 'stop', **NEUTRAL_P)
a.key(8, 'stop', **P(root(up=0.02, lean=-8), torso(lean=-10), head(down=-25),
                     dict(rarm=R((3, 7.2, 2.5), (0, 1, 0)), larm=L((-3, 7.2, 2.5), (0, 0.4, 1))), legs('wide')))
a.key(18, 'smooth', **P(root(up=0.03, lean=-10), torso(lean=-12), head(down=-28),
                        dict(rarm=R((2.5, 7.5, 2), (0, 1, 0)), larm=L((-2.5, 7.5, 2), (0, 0.4, 1)))))
a.key(21, 'smooth', **P(root(up=-0.08, fwd=0.2, lean=16), torso(lean=12), head(down=5),
                        dict(rarm=R((3, -2, 11), (0, 0.6, 0.8)), larm=L((-3, -3, 10), (0, 0.2, 1))), legs('lunge', 12)))
a.key(25, 'stop', **P(root(up=-0.09, fwd=0.22, lean=17)))
a.key(34, 'stop', **NEUTRAL_P)

# 3) Mano al suelo: rodilla abajo y la palma izquierda contra el piso (Earth Lance, Earth Pillar, Quagmire)
a = spell_anim('magic_ground', 'Magia: palma contra el suelo', 30)
a.key(0, 'stop', **NEUTRAL_P)
a.key(6, 'smooth', **P(root(up=-0.25, lean=14), torso(lean=12), head(down=20),
                       dict(rarm=R((6, -4, 4), (0, 1, 0.2)), larm=L((-5, -14, 7), (0, -1, 0.3))), legs('crouch', 30)))
a.key(9, 'stop', **P(root(up=-0.42, lean=22), torso(lean=16), head(down=12),
                     dict(rarm=R((7, -2, 2), (0, 1, 0.1)), larm=L((-4, -18, 9), (0, -1, 0.2))),
                     dict(rleg=((0.1, -0.35, -0.9), 95), lleg=((-0.1, -0.75, 0.6), 85))))
a.key(18, 'stop', **P(root(up=-0.42, lean=20), head(down=-2)))
a.key(30, 'stop', **NEUTRAL_P)

# 4) Barrido lateral: el brazo cruza el cuerpo y barre de derecha a izquierda (Wind Slice, Flame Slice)
a = spell_anim('magic_sweep', 'Magia: barrido lateral con el baston', 24)
a.key(0, 'stop', **NEUTRAL_P)
a.key(5, 'stop', **P(root(turn=25, up=-0.04), torso(turn=40), head(yaw=-30),
                     dict(rarm=R((9, -2, -3), (0.7, 0.3, -0.6)), larm=L((-3, -6, 7), (0, 0.3, 1))), legs('wide', 8)))
a.key(9, 'smooth', **P(root(turn=-20, up=-0.06, fwd=0.1), torso(turn=-35, lean=4), head(yaw=20),
                       dict(rarm=R((-2, -3, 12), (-0.9, 0.1, 0.4)), larm=L((-9, -7, -2))), legs('wide', 12)))
a.key(13, 'stop', **P(root(turn=-26, up=-0.06, fwd=0.1), torso(turn=-48), dict(rarm=R((-7, -4, 8), (-0.8, 0.1, -0.6)))))
a.key(24, 'stop', **NEUTRAL_P)

# 5) Apuntar y disparar: el baston a la altura de los ojos como un rifle, la izquierda lo sostiene y hay retroceso
a = spell_anim('magic_snipe', 'Magia: apuntar y disparar (Stone Cannon)', 30)
a.key(0, 'stop', **NEUTRAL_P)
a.key(7, 'stop', **P(root(turn=-30, up=-0.04), torso(turn=-25), head(yaw=30, down=2),
                     grip((3, 0, 9), (0, 0.06, 1), spread=-6), legs('guard', 10)))
a.key(16, 'smooth', **P(root(turn=-30, up=-0.04), grip((3, 0.2, 9.2), (0, 0.06, 1), spread=-6)))
a.key(18, 'stop', **P(root(turn=-30, up=-0.03, fwd=-0.1, lean=-6), torso(turn=-25, lean=-5), head(yaw=30, down=-4),
                      grip((3, 2, 6), (0, 0.35, 0.95), spread=-6)))
a.key(22, 'smooth', **P(root(turn=-30, up=-0.04, lean=0), grip((3, 0, 8.5), (0, 0.08, 1), spread=-6)))
a.key(30, 'stop', **NEUTRAL_P)

# 6) Baston al cielo: lo levanta mirando arriba (se forma la tormenta) y lo baja apuntando (Lightning, Cumulonimbus)
a = spell_anim('magic_sky', 'Magia: baston al cielo y apuntar', 36)
a.key(0, 'stop', **NEUTRAL_P)
a.key(8, 'stop', **P(root(up=0.02, lean=-6), torso(lean=-8), head(down=-35),
                     dict(rarm=R((4, 7.4, 1.5), (0, 1, 0.05)), larm=L((-8, -7, 3), (0, -0.3, 1))), legs('wide')))
a.key(22, 'smooth', **P(root(up=0.03, lean=-7), head(down=-38), dict(rarm=R((4, 7.7, 1), (0, 1, 0.05)))))
a.key(26, 'smooth', **P(root(up=-0.05, fwd=0.1, lean=8), torso(lean=6), head(down=0),
                        dict(rarm=R((4, -1, 11), (0, 0.2, 1))), legs('guard', 10)))
a.key(29, 'stop', **P(root(up=-0.05, fwd=0.12, lean=9)))
a.key(36, 'stop', **NEUTRAL_P)

# 7) Giro: vuelta completa con el baston extendido y remate adelante (Tornado, Tornado Impact)
a = spell_anim('magic_spiral', 'Magia: giro completo con el baston extendido', 30)
a.key(0, 'stop', **NEUTRAL_P)
a.key(5, 'stop', **P(root(turn=20, up=-0.08), torso(turn=35), dict(rarm=R((9, -3, 2), (1, 0.2, 0.1))), legs('crouch', -20)))
a.key(10, 'smooth', **P(root(turn=-150, up=-0.06), torso(turn=0), dict(rarm=R((10, -2, 2), (1, 0.2, 0))), legs('wide')))
a.key(15, 'smooth', **P(root(turn=-345, up=-0.08, fwd=0.1), torso(turn=-10), dict(rarm=R((4, -2, 11), (0, 0.3, 1)))))
a.key(19, 'stop', **P(root(turn=-360, up=-0.08, fwd=0.14, lean=8), torso(turn=0, lean=4), dict(rarm=R((3, -2, 12), (0, 0.2, 1)))))
a.key(30, 'stop', **P(NEUTRAL_P, root(turn=-360)))

# 8) Baston plantado: lo clava adelante en vertical y adelanta la palma izquierda (Magic Shield, Water Shield)
a = spell_anim('magic_barrier', 'Magia: baston plantado y palma adelante', 30)
a.key(0, 'stop', **NEUTRAL_P)
a.key(6, 'smooth', **P(root(up=0.02), dict(rarm=R((5, 3, 7), (0, 1, 0.1)))))
a.key(9, 'stop', **P(root(up=-0.06, lean=6), torso(lean=4), head(down=4),
                     dict(rarm=R((5, -5, 8), (0, 1, 0.05)), larm=L((-3, -2, 11), (0, 1, 0.2))), legs('guard', 14)))
a.key(20, 'stop', **P(root(up=-0.07, lean=7), dict(larm=L((-3, -1.5, 11.5), (0, 1, 0.2)))))
a.key(30, 'stop', **NEUTRAL_P)

"""Candidatas de animacion para cada hechizo: 3 versiones por hechizo, para elegir despues en el Taller.

NO tocan el mod: se guardan en combat/tools/spell_candidates/ (fuera de resources) y se ven en una pagina del Taller
aparte. Cuando elijas la mejor de cada hechizo, se copia esa al addon y se engancha al conjuro.

Cada version sale de un "arquetipo" de movimiento (empuje, reunir y lanzar, mano al suelo, giro, etc.) con su propio
ritmo: los hechizos de rango alto tardan mas en cargar (velocidad segun el largo del combo).
Nombres: sv_<hechizo>_<1|2|3>.

Uso:  python3 spell_variants.py            (escribe los .json en spell_candidates/)
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import animgen as G  # noqa: E402
from anims import R, L, P, root, torso, head, legs, grip, NEUTRAL_P  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(HERE, 'spell_candidates')


# ------------------------------------------------------------------ utilidades
class V:
    """Una animacion candidata: los ticks se estiran segun la velocidad del hechizo."""

    def __init__(self, name, desc, end, s):
        self.s = s
        self.last = -1
        self.a = G.anim(name, desc, end=self.t(end), stop=self.t(end) + 4, lag=('head', 'larm'))

    def t(self, x):
        return max(0, round(x * self.s))

    def k(self, x, kind, *poses):
        t = max(self.t(x), self.last + 1)
        self.last = t
        self.a.key(t, kind, **P(*poses))
        return self


def staff_low():
    return dict(rarm=R((6, -9, 3), (0, 1, 0.25)))


# ------------------------------------------------------------------ arquetipos
ARCH = {}


def arch(label):
    def deco(fn):
        ARCH[fn.__name__] = (label, fn)
        return fn
    return deco


@arch('Empuje doble con las dos manos')
def push(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'stop', root(up=-0.08, fwd=-0.08, lean=-4), torso(lean=-6), head(down=6),
        dict(rarm=R((3, -6, 5), (0, 1, 0.3)), larm=L((-2, -6, 5), (0, 0.3, 1))), legs('guard', 12))
    v.k(9, 'smooth', root(up=-0.1, fwd=0.2, lean=10), torso(lean=6), head(down=0),
        dict(rarm=R((3, -3, 9.5), (0, 0.4, 1)), larm=L((-3, -3, 9.5), (0, 0.3, 1))), legs('lunge', 10))
    v.k(14, 'stop', root(up=-0.1, fwd=0.22, lean=11), torso(lean=6), legs('lunge', 10),
        dict(rarm=R((3, -3.5, 9.8), (0, 0.4, 1)), larm=L((-3, -3, 9.5), (0, 0.3, 1))))
    v.k(26, 'stop', NEUTRAL_P)


@arch('Reunir sobre la cabeza y lanzar')
def gather_throw(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(8, 'stop', root(up=0.02, lean=-8), torso(lean=-10), head(down=-25),
        dict(rarm=R((3, 7.2, 2.5), (0, 1, 0)), larm=L((-3, 7.2, 2.5), (0, 0.4, 1))), legs('wide'))
    v.k(18, 'smooth', root(up=0.03, lean=-10), torso(lean=-12), head(down=-28), legs('wide'),
        dict(rarm=R((2.5, 7.5, 2), (0, 1, 0)), larm=L((-2.5, 7.5, 2), (0, 0.4, 1))))
    v.k(21, 'smooth', root(up=-0.08, fwd=0.2, lean=16), torso(lean=12), head(down=5),
        dict(rarm=R((3, -2, 9.5), (0, 0.6, 0.8)), larm=L((-3, -3, 9.5), (0, 0.2, 1))), legs('lunge', 12))
    v.k(25, 'stop', root(up=-0.09, fwd=0.22, lean=17), torso(lean=12), legs('lunge', 12),
        dict(rarm=R((3, -2, 9.5), (0, 0.6, 0.8)), larm=L((-3, -3, 9.5), (0, 0.2, 1))))
    v.k(34, 'stop', NEUTRAL_P)


@arch('Palma contra el suelo')
def ground(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(up=-0.25, lean=14), torso(lean=12), head(down=20),
        dict(rarm=R((6, -4, 4), (0, 1, 0.2)), larm=L((-5, -11, 6), (0, -1, 0.3))), legs('crouch', 30))
    v.k(9, 'stop', root(up=-0.42, lean=22), torso(lean=16), head(down=12),
        dict(rarm=R((7, -2, 2), (0, 1, 0.1)), larm=L((-4, -11, 5), (0, -1, 0.2))),
        dict(rleg=((0.1, -0.35, -0.9), 95), lleg=((-0.1, -0.75, 0.6), 85)))
    v.k(18, 'stop', root(up=-0.42, lean=20), torso(lean=16), head(down=-2),
        dict(rarm=R((7, -2, 2), (0, 1, 0.1)), larm=L((-4, -11, 5), (0, -1, 0.2))),
        dict(rleg=((0.1, -0.35, -0.9), 95), lleg=((-0.1, -0.75, 0.6), 85)))
    v.k(30, 'stop', NEUTRAL_P)


@arch('Barrido lateral con el baston')
def sweep(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'stop', root(turn=25, up=-0.04), torso(turn=40), head(yaw=-30),
        dict(rarm=R((9, -2, -3), (0.7, 0.3, -0.6)), larm=L((-3, -6, 7), (0, 0.3, 1))), legs('wide', 8))
    v.k(9, 'smooth', root(turn=-20, up=-0.06, fwd=0.1), torso(turn=-35, lean=4), head(yaw=20),
        dict(rarm=R((-2, -3, 8.5), (-0.9, 0.1, 0.4)), larm=L((-9, -7, -2))), legs('wide', 12))
    v.k(13, 'stop', root(turn=-26, up=-0.06, fwd=0.1), torso(turn=-48), legs('wide', 12),
        dict(rarm=R((-3, -4, 6), (-0.8, 0.1, -0.6)), larm=L((-9, -7, -2))))
    v.k(24, 'stop', NEUTRAL_P)


@arch('Apuntar como un rifle y disparar')
def snipe(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(7, 'stop', root(turn=-30, up=-0.04), torso(turn=-25), head(yaw=30, down=2),
        grip((3, 0, 9), (0, 0.06, 1), spread=-6), legs('guard', 10))
    v.k(16, 'smooth', root(turn=-30, up=-0.04), torso(turn=-25), head(yaw=30, down=2), legs('guard', 10),
        grip((3, 0.2, 9.2), (0, 0.06, 1), spread=-6))
    v.k(18, 'stop', root(turn=-30, up=-0.03, fwd=-0.1, lean=-6), torso(turn=-25, lean=-5), head(yaw=30, down=-4),
        grip((3, 2, 6), (0, 0.35, 0.95), spread=-6), legs('guard', 10))
    v.k(22, 'smooth', root(turn=-30, up=-0.04), torso(turn=-25), head(yaw=30), grip((3, 0, 8.5), (0, 0.08, 1), spread=-6),
        legs('guard', 10))
    v.k(30, 'stop', NEUTRAL_P)


@arch('Baston al cielo y apuntar')
def sky(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(8, 'stop', root(up=0.02, lean=-6), torso(lean=-8), head(down=-35),
        dict(rarm=R((4, 7.4, 1.5), (0, 1, 0.05)), larm=L((-8, -7, 3), (0, -0.3, 1))), legs('wide'))
    v.k(22, 'smooth', root(up=0.03, lean=-7), torso(lean=-8), head(down=-38), legs('wide'),
        dict(rarm=R((4, 7.7, 1), (0, 1, 0.05)), larm=L((-8, -7, 3), (0, -0.3, 1))))
    v.k(26, 'smooth', root(up=-0.05, fwd=0.1, lean=8), torso(lean=6), head(down=0),
        dict(rarm=R((4, -1, 9.5), (0, 0.2, 1)), larm=L((-8, -7, 3))), legs('guard', 10))
    v.k(29, 'stop', root(up=-0.05, fwd=0.12, lean=9), torso(lean=6), legs('guard', 10),
        dict(rarm=R((4, -1, 9.5), (0, 0.2, 1)), larm=L((-8, -7, 3))))
    v.k(36, 'stop', NEUTRAL_P)


@arch('Giro completo con el baston extendido')
def spiral(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'stop', root(turn=20, up=-0.08), torso(turn=35), dict(rarm=R((9, -3, 2), (1, 0.2, 0.1))), legs('crouch', -20))
    v.k(10, 'smooth', root(turn=-150, up=-0.06), torso(turn=0), dict(rarm=R((10, -2, 2), (1, 0.2, 0))), legs('wide'))
    v.k(15, 'smooth', root(turn=-345, up=-0.08, fwd=0.1), torso(turn=-10), dict(rarm=R((4, -2, 9.5), (0, 0.3, 1))), legs('wide'))
    v.k(19, 'stop', root(turn=-360, up=-0.08, fwd=0.14, lean=8), torso(turn=0, lean=4), dict(rarm=R((3, -2, 9.8), (0, 0.2, 1))),
        legs('guard', 8))
    v.k(30, 'stop', NEUTRAL_P, root(turn=-360))


@arch('Baston plantado y palma adelante')
def barrier(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(up=0.02), dict(rarm=R((5, 3, 7), (0, 1, 0.1))))
    v.k(9, 'stop', root(up=-0.06, lean=6), torso(lean=4), head(down=4),
        dict(rarm=R((5, -5, 8), (0, 1, 0.05)), larm=L((-3, -2, 9.5), (0, 1, 0.2))), legs('guard', 14))
    v.k(20, 'stop', root(up=-0.07, lean=7), torso(lean=4), legs('guard', 14),
        dict(rarm=R((5, -5, 8), (0, 1, 0.05)), larm=L((-3, -1.5, 9.8), (0, 1, 0.2))))
    v.k(30, 'stop', NEUTRAL_P)


@arch('Golpe rapido de baston hacia adelante')
def flick(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(4, 'stop', root(up=-0.02, turn=10), torso(turn=12), head(yaw=-6),
        dict(rarm=R((6, 2, 3), (0, 1, 0.4)), larm=L((-6, -8, 3))), legs('guard', 6))
    v.k(7, 'smooth', root(up=-0.05, fwd=0.08, lean=6, turn=-8), torso(turn=-10, lean=3), head(yaw=6),
        dict(rarm=R((3, -3, 9.5), (0, 0.2, 1)), larm=L((-7, -8, -1))), legs('guard', 10))
    v.k(10, 'stop', root(up=-0.05, fwd=0.08, lean=6, turn=-8), torso(turn=-10, lean=3), legs('guard', 10),
        dict(rarm=R((3, -3, 9.5), (0, 0.2, 1)), larm=L((-7, -8, -1))))
    v.k(18, 'stop', NEUTRAL_P)


@arch('Esfera en la palma y la empuja')
def palm_orb(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(4, 'smooth', head(down=14), root(up=-0.02), legs('guard', 4),
        dict(larm=L((-1, -7, 6), (0, 1, 0.2)), rarm=R((6, -6, 4), (0, 1, 0.3))))
    v.k(10, 'stop', head(down=16), root(up=-0.03, turn=10), torso(turn=8), legs('guard', 6),
        dict(larm=L((-1, -6.5, 6.5), (0, 1, 0.2)), rarm=R((5, -6, 4), (0, 1, 0.3))))
    v.k(13, 'smooth', head(down=0), root(up=-0.06, fwd=0.12, lean=8, turn=-10), torso(turn=-12, lean=4), legs('lunge', 8),
        dict(larm=L((-3, -2, 9.5), (0, 0.3, 1)), rarm=R((7, -8, -1), (0, 1, -0.2))))
    v.k(17, 'stop', root(up=-0.06, fwd=0.12, lean=8, turn=-10), torso(turn=-12, lean=4), legs('lunge', 8),
        dict(larm=L((-3, -2, 9.5), (0, 0.3, 1)), rarm=R((7, -8, -1), (0, 1, -0.2))))
    v.k(24, 'stop', NEUTRAL_P)


@arch('Tensar como un arco y soltar')
def bow_magic(v):
    aim = P(root(turn=-25, up=-0.03), torso(turn=-30), head(yaw=30), legs('guard', 8))
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', aim, dict(larm=L((-3, -1, 9.5), (0, 1, 0.1)), rarm=R((-1, -1, 5), (0, 0.3, 1))))
    v.k(12, 'stop', aim, dict(larm=L((-3, -1, 9.5), (0, 1, 0.1)), rarm=R((2, 0, 1), (0, 0.4, 1))))
    v.k(18, 'smooth', aim, dict(larm=L((-3, -1, 9.6), (0, 1, 0.1)), rarm=R((2.5, 0, 0.5), (0, 0.4, 1))))
    v.k(20, 'smooth', aim, root(turn=-25, up=-0.03, fwd=-0.05), dict(larm=L((-3, -1, 9.5), (0, 1, 0.1)), rarm=R((6, -4, -3), (0, 0.3, -1))))
    v.k(24, 'stop', aim, dict(larm=L((-3, -1.5, 9.3), (0, 1, 0.1)), rarm=R((6, -4, -3), (0, 0.3, -1))))
    v.k(30, 'stop', NEUTRAL_P)


@arch('Levantar la rodilla y pisoton')
def stomp(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', root(up=0.05, lean=-4), head(down=-4), dict(rleg=((0.1, -0.3, 0.9), 110), lleg=((0, -1, 0), 4)),
        dict(rarm=R((6, 2, 2), (0, 1, 0)), larm=L((-8, -6, 3))))
    v.k(9, 'stop', root(up=-0.12, lean=10), torso(lean=6), head(down=15), dict(rleg=((0.15, -1, 0.15), 14), lleg=((-0.1, -1, -0.1), 14)),
        dict(rarm=R((5, -9, 4), (0, -0.2, 1)), larm=L((-8, -8, 2))))
    v.k(16, 'stop', root(up=-0.12, lean=10), torso(lean=6), head(down=10), dict(rleg=((0.15, -1, 0.15), 14), lleg=((-0.1, -1, -0.1), 14)),
        dict(rarm=R((5, -9, 4), (0, -0.2, 1)), larm=L((-8, -8, 2))))
    v.k(26, 'stop', NEUTRAL_P)


@arch('Agacharse y levantar las dos manos')
def rise(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(up=-0.2, lean=12), torso(lean=8), head(down=15), legs('crouch', 0),
        dict(rarm=R((5, -10, 4.5), (0, -0.5, 1)), larm=L((-5, -10, 4.5), (0, -1, 0.2))))
    v.k(12, 'stop', root(up=-0.22, lean=13), torso(lean=8), head(down=18), legs('crouch', 0),
        dict(rarm=R((5, -10, 4.5), (0, -0.5, 1)), larm=L((-5, -10, 4.5), (0, -1, 0.2))))
    v.k(20, 'smooth', root(up=0.03, lean=-6), torso(lean=-6), head(down=-25), legs('wide'),
        dict(rarm=R((5, 6, 3), (0, 1, 0.2)), larm=L((-5, 6, 3), (0, 1, 0.2))))
    v.k(24, 'stop', root(up=0.03, lean=-6), torso(lean=-6), head(down=-25), legs('wide'),
        dict(rarm=R((5, 6.3, 3), (0, 1, 0.2)), larm=L((-5, 6.3, 3), (0, 1, 0.2))))
    v.k(32, 'stop', NEUTRAL_P)


@arch('Abrir los brazos al cielo')
def spread_arms(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(lean=-3), head(down=-10), legs('wide'),
        dict(rarm=R((10, -4, 4), (1, 0.2, 0.3)), larm=L((-10, -4, 4), (-1, 0.2, 0.3))))
    v.k(14, 'smooth', root(up=0.03, lean=-5), torso(lean=-5), head(down=-25), legs('wide'),
        dict(rarm=R((12, 3, 2), (1, 0.6, 0.1)), larm=L((-12, 3, 2), (-1, 0.6, 0.1))))
    v.k(24, 'stop', root(up=0.035, lean=-5), torso(lean=-5), head(down=-28), legs('wide'),
        dict(rarm=R((12, 3.4, 2), (1, 0.6, 0.1)), larm=L((-12, 3.4, 2), (-1, 0.6, 0.1))))
    v.k(34, 'stop', NEUTRAL_P)


@arch('Puno al pecho y abrirse (refuerzo)')
def self_buff(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', head(down=10), dict(larm=L((-1, -5, 4), (0, 1, 0.2)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(11, 'stop', head(down=12), root(up=-0.03), dict(larm=L((-1, -5, 4), (0, 1, 0.2)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(15, 'smooth', root(up=0.02, lean=-4), torso(lean=-4), head(down=-8), legs('wide'),
        dict(larm=L((-9, -3, 3), (0, 1, 0)), rarm=R((9, -4, 3), (0, 1, 0.2))))
    v.k(20, 'stop', root(up=0.02, lean=-4), torso(lean=-4), head(down=-8), legs('wide'),
        dict(larm=L((-9, -3, 3), (0, 1, 0)), rarm=R((9, -4, 3), (0, 1, 0.2))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Inclinarse y tocar con la palma')
def touch(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(lean=18, up=-0.08), torso(lean=10), head(down=25), legs('stride'),
        dict(larm=L((-3, -8, 7), (0, -0.3, 1)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(16, 'stop', root(lean=19, up=-0.09), torso(lean=10), head(down=26), legs('stride'),
        dict(larm=L((-3, -8.3, 7.3), (0, -0.3, 1)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(26, 'stop', NEUTRAL_P)


@arch('Estocada en espiral (taladro)')
def drill(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'stop', root(turn=30, up=-0.06), torso(turn=30), head(yaw=-25), legs('guard', 10),
        dict(rarm=R((7, -3, -3), (0, 0.3, 1)), larm=L((-2, -4, 4))))
    v.k(10, 'smooth', root(turn=-40, fwd=0.2, lean=12, up=-0.08), torso(turn=-20, lean=4), head(yaw=20), legs('lunge', 12),
        dict(rarm=R((3, -2, 9.5), (0, 0.1, 1)), larm=L((-7, -6, -2))))
    v.k(14, 'smooth', root(turn=-60, fwd=0.22, lean=12, up=-0.08), torso(turn=-25, lean=4), head(yaw=30), legs('lunge', 12),
        dict(rarm=R((3, -2, 9.8), (0, 0.1, 1)), larm=L((-7, -6, -2))))
    v.k(18, 'stop', root(turn=-60, fwd=0.22, lean=12, up=-0.08), torso(turn=-25, lean=4), legs('lunge', 12),
        dict(rarm=R((3, -2, 9.8), (0, 0.1, 1)), larm=L((-7, -6, -2))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Salto y golpe al caer')
def jump_cast(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(4, 'smooth', root(up=-0.2, lean=10), legs('crouch'), head(down=5),
        dict(rarm=R((6, -9, -3), (0, 1, -0.2)), larm=L((-6, -9, -3))))
    v.k(9, 'smooth', root(up=0.35, lean=-4), legs('tuck'), head(down=-15),
        dict(rarm=R((6, 6, 3), (0, 1, 0.2)), larm=L((-6, 6, 3), (0, 1, 0.2))))
    v.k(13, 'stop', root(up=-0.22, lean=15), torso(lean=8), legs('crouch', 10), head(down=20),
        dict(rarm=R((4, -10, 5), (0, -0.3, 1)), larm=L((-4, -10, 5), (0, -1, 0.2))))
    v.k(20, 'stop', root(up=-0.2, lean=14), torso(lean=8), legs('crouch', 10), head(down=10),
        dict(rarm=R((4, -10, 5), (0, -0.3, 1)), larm=L((-4, -10, 5), (0, -1, 0.2))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Dos tajos cruzados en X')
def slash_x(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(4, 'stop', root(turn=15, up=-0.03), torso(turn=25), head(yaw=-15), legs('wide', 6),
        dict(rarm=R((9, 4, 1), (0.6, 0.8, 0)), larm=L((-6, -7, 4))))
    v.k(8, 'smooth', root(turn=-15, up=-0.06, fwd=0.08, lean=6), torso(turn=-30, lean=4), head(yaw=15), legs('wide', 10),
        dict(rarm=R((-1, -7, 6), (-0.6, -0.6, 0.5)), larm=L((-8, -8, -1))))
    v.k(11, 'stop', root(turn=-20, up=-0.04), torso(turn=-35), head(yaw=20), legs('wide', 10),
        dict(rarm=R((-2, 2, 6), (-0.7, 0.7, 0.1)), larm=L((-8, -8, -1))))
    v.k(15, 'smooth', root(turn=15, up=-0.07, fwd=0.12, lean=8), torso(turn=30, lean=4), head(yaw=-15), legs('lunge', 8),
        dict(rarm=R((10, -7, 4), (0.7, -0.5, 0.4)), larm=L((-6, -7, 5))))
    v.k(18, 'stop', root(turn=15, up=-0.07, fwd=0.12, lean=8), torso(turn=30, lean=4), legs('lunge', 8),
        dict(rarm=R((10, -7, 4), (0.7, -0.5, 0.4)), larm=L((-6, -7, 5))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Canalizar con las dos manos y estallar')
def channel(v):
    base = P(legs('wide', 10), head(down=4))
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', base, root(up=-0.06, lean=4), dict(rarm=R((2, -3, 8), (0, 0.2, 1)), larm=L((-2, -3, 8), (0, 0.2, 1))))
    for i, x in enumerate(range(9, 29, 3)):  # temblor: el poder se acumula
        j = 0.35 if i % 2 else -0.35
        v.k(x, 'smooth', base, root(up=-0.07 - 0.004 * i, lean=4 + 0.3 * i),
            dict(rarm=R((2 + j, -3 - j, 8), (0, 0.2, 1)), larm=L((-2 + j, -3 + j, 8), (0, 0.2, 1))))
    v.k(31, 'smooth', base, root(up=-0.06, fwd=-0.12, lean=-6), torso(lean=-4),
        dict(rarm=R((3, -2, 9.8), (0, 0.3, 1)), larm=L((-3, -2, 9.8), (0, 0.3, 1))))
    v.k(34, 'stop', base, root(up=-0.06, fwd=-0.12, lean=-6), torso(lean=-4),
        dict(rarm=R((3, -2, 9.8), (0, 0.3, 1)), larm=L((-3, -2, 9.8), (0, 0.3, 1))))
    v.k(42, 'stop', NEUTRAL_P)


@arch('Manos juntas en oracion y abrirlas')
def meditate(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', head(down=25), root(up=-0.02), dict(rarm=R((0.5, -5, 4.5), (0, 1, 0.1)), larm=L((-0.5, -5, 4.5), (0, 1, 0.1))))
    v.k(22, 'smooth', head(down=15), root(up=0.03), dict(rarm=R((0.5, -3, 5), (0, 1, 0.1)), larm=L((-0.5, -3, 5), (0, 1, 0.1))))
    v.k(26, 'smooth', head(down=-5), root(up=0.03, lean=-3), legs('wide'),
        dict(rarm=R((7, -4, 5), (0.5, 1, 0.3)), larm=L((-7, -4, 5), (-0.5, 1, 0.3))))
    v.k(30, 'stop', head(down=-5), root(up=0.03, lean=-3), legs('wide'),
        dict(rarm=R((7, -4, 5), (0.5, 1, 0.3)), larm=L((-7, -4, 5), (-0.5, 1, 0.3))))
    v.k(36, 'stop', NEUTRAL_P)


@arch('Agacharse y cubrirse (desvanecerse)')
def crouch_hide(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(up=-0.25, lean=10), legs('crouch'), head(down=20),
        dict(rarm=R((-1, 0, 5), (-1, 0.5, 0)), larm=L((1, -1, 5), (1, 0.5, 0))))
    v.k(20, 'stop', root(up=-0.27, lean=12), legs('crouch'), head(down=24),
        dict(rarm=R((-1, 0, 5), (-1, 0.5, 0)), larm=L((1, -1, 5), (1, 0.5, 0))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Dos dedos a los ojos')
def eye_touch(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', head(down=5), dict(larm=L((-1.5, 3, 3.5), (0, 1, 0.3)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(11, 'stop', head(down=6), dict(larm=L((-1.5, 3, 3.5), (0, 1, 0.3)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(15, 'smooth', head(yaw=-20, down=-4), dict(larm=L((-9, 2, 5), (-1, 0.2, 0.4)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(18, 'stop', head(yaw=-20, down=-4), dict(larm=L((-9, 2, 5), (-1, 0.2, 0.4)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(24, 'stop', NEUTRAL_P)


@arch('Mano a la boca: inspirar y soltar')
def breathe(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', head(down=-5), dict(larm=L((-0.5, 1, 4), (0, 1, 0.2)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(12, 'smooth', root(up=0.04, lean=-8), torso(lean=-6), head(down=-15),
        dict(larm=L((-0.5, 1.5, 4), (0, 1, 0.2)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(16, 'smooth', root(up=-0.03, lean=8), torso(lean=4), head(down=10),
        dict(larm=L((-3, -3, 8), (0, 0.3, 1)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(20, 'stop', root(up=-0.03, lean=8), torso(lean=4), head(down=10),
        dict(larm=L((-3, -3, 8), (0, 0.3, 1)), rarm=R((6, -9, 3), (0, 1, 0.25))))
    v.k(26, 'stop', NEUTRAL_P)


@arch('Martillar con el baston (forja)')
def hammer(v):
    low = P(root(lean=14), torso(lean=6), head(down=25), legs('guard', 6))
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', low, dict(larm=L((-1, -8, 6), (0, 1, 0)), rarm=R((6, 3, 3), (0, 1, 0.3))))
    v.k(9, 'stop', low, dict(larm=L((-1, -8, 6), (0, 1, 0)), rarm=R((2, -6, 7), (0, -0.3, 1))))
    v.k(13, 'smooth', low, dict(larm=L((-1, -8, 6), (0, 1, 0)), rarm=R((6, 3, 3), (0, 1, 0.3))))
    v.k(17, 'stop', low, dict(larm=L((-1, -8, 6), (0, 1, 0)), rarm=R((2, -6, 7), (0, -0.3, 1))))
    v.k(22, 'smooth', low, head(down=15), dict(larm=L((-3, -3, 7), (0, 1, 0.2)), rarm=R((3, -3, 7), (0, 1, 0.2))))
    v.k(25, 'stop', low, head(down=15), dict(larm=L((-3, -3, 7), (0, 1, 0.2)), rarm=R((3, -3, 7), (0, 1, 0.2))))
    v.k(32, 'stop', NEUTRAL_P)


@arch('Esparcir en abanico con la mano')
def sow(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(5, 'smooth', root(turn=15, lean=8), torso(turn=20), head(down=15), legs('stride'),
        dict(larm=L((-7, -9, 2), (0, -0.5, 1)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(10, 'smooth', root(turn=0, lean=8), torso(turn=-10), head(down=12), legs('stride'),
        dict(larm=L((-1, -5, 8.5), (0.2, 0, 1)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(13, 'smooth', root(turn=-10, lean=8), torso(turn=-30), head(down=12, yaw=15), legs('stride'),
        dict(larm=L((3, -5, 5), (1, 0, 0.5)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(16, 'stop', root(turn=-10, lean=8), torso(turn=-30), head(down=12, yaw=15), legs('stride'),
        dict(larm=L((3, -5, 5), (1, 0, 0.5)), rarm=R((6, -8, 1), (0, 1, 0.2))))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Baston sobre la cabeza y golpe al suelo')
def overhead_slam(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(lean=-10, up=0.02), torso(lean=-8), head(down=-20), legs('wide'), grip((3, 7, 1), (0, 1, -0.3), spread=4))
    v.k(10, 'stop', root(lean=-12, up=0.03), torso(lean=-10), head(down=-22), legs('wide'), grip((3, 7.2, 0.6), (0, 1, -0.35), spread=4))
    v.k(13, 'smooth', root(lean=18, up=-0.12, fwd=0.1), torso(lean=10), head(down=15), legs('lunge', 10),
        grip((2, -7, 8), (0, -0.5, 1), spread=4))
    v.k(18, 'stop', root(lean=18, up=-0.12, fwd=0.1), torso(lean=10), head(down=15), legs('lunge', 10),
        grip((2, -7, 8), (0, -0.5, 1), spread=4))
    v.k(28, 'stop', NEUTRAL_P)


@arch('Flexionar los brazos (fuerza)')
def flex(v):
    v.k(0, 'stop', NEUTRAL_P)
    v.k(6, 'smooth', root(up=-0.04), head(down=-5), legs('wide'),
        dict(rarm=R((9, 1, 1), (0, 1, 0)), larm=L((-9, 1, 1), (0, 1, 0))))
    v.k(14, 'stop', root(up=-0.05), head(down=-6), legs('wide'),
        dict(rarm=R((9, 1.4, 1), (0, 1, 0)), larm=L((-9, 1.4, 1), (0, 1, 0))))
    v.k(18, 'smooth', root(up=-0.1), torso(lean=6), head(down=5), legs('wide', 10),
        dict(rarm=R((6, -10, 3), (0, 1, 0.2)), larm=L((-6, -10, 3), (0, 1, 0.2))))
    v.k(22, 'stop', root(up=-0.1), torso(lean=6), head(down=5), legs('wide', 10),
        dict(rarm=R((6, -10, 3), (0, 1, 0.2)), larm=L((-6, -10, 3), (0, 1, 0.2))))
    v.k(28, 'stop', NEUTRAL_P)


# ------------------------------------------------------------------ que version va con cada hechizo
PLAN = {
    'water_ball': ['palm_orb', 'flick', 'gather_throw'],
    'water_shield': ['barrier', 'rise', 'spread_arms'],
    'water_arrow': ['bow_magic', 'snipe', 'flick'],
    'ice_smash': ['overhead_slam', 'gather_throw', 'push'],
    'ice_blade': ['slash_x', 'sweep', 'flick'],
    'water_cannon': ['push', 'channel', 'snipe'],
    'ice_pillar': ['ground', 'rise', 'stomp'],
    'flood_flush': ['spread_arms', 'push', 'sweep'],
    'icicle_lance': ['bow_magic', 'snipe', 'drill'],
    'blizzard_storm': ['sky', 'spread_arms', 'spiral'],
    'ice_fortress': ['rise', 'barrier', 'ground'],
    'cumulonimbus': ['sky', 'spread_arms', 'meditate'],
    'lightning': ['sky', 'snipe', 'overhead_slam'],
    'absolute_zero': ['channel', 'ground', 'spread_arms'],
    'fireball': ['palm_orb', 'flick', 'gather_throw'],
    'heat_hand': ['palm_orb', 'touch', 'push'],
    'burning_place': ['ground', 'sow', 'stomp'],
    'ex_flame': ['gather_throw', 'channel', 'push'],
    'explosion': ['channel', 'gather_throw', 'jump_cast'],
    'flame_slice': ['slash_x', 'sweep', 'spiral'],
    'exodus_flame': ['gather_throw', 'sky', 'channel'],
    'magma_gush': ['ground', 'stomp', 'rise'],
    'flash_over': ['channel', 'spread_arms', 'push'],
    'minefield': ['sow', 'ground', 'sky'],
    'nuclear_strike': ['sky', 'channel', 'gather_throw'],
    'rock_bullet': ['snipe', 'flick', 'palm_orb'],
    'sand_cloud': ['sweep', 'sow', 'spread_arms'],
    'dig': ['ground', 'drill', 'hammer'],
    'stone_cannon': ['snipe', 'push', 'channel'],
    'earth_pillar': ['rise', 'stomp', 'ground'],
    'earth_lance': ['ground', 'stomp', 'drill'],
    'earth_wall': ['stomp', 'rise', 'barrier'],
    'earth_fortress': ['rise', 'ground', 'spread_arms'],
    'dust_storm': ['spiral', 'spread_arms', 'sweep'],
    'sandstorm': ['spiral', 'sky', 'spread_arms'],
    'earth_drill': ['drill', 'ground', 'channel'],
    'mountain': ['rise', 'stomp', 'sky'],
    'blast': ['push', 'jump_cast', 'flick'],
    'soft_landing': ['jump_cast', 'spread_arms', 'meditate'],
    'fleet_foot': ['self_buff', 'jump_cast', 'stomp'],
    'sonic_boom': ['push', 'snipe', 'channel'],
    'wind_slice': ['slash_x', 'sweep', 'flick'],
    'gale_force': ['push', 'spread_arms', 'spiral'],
    'wind_gust': ['sweep', 'push', 'flick'],
    'sonic_blast': ['channel', 'push', 'snipe'],
    'tornado_impact': ['drill', 'spiral', 'push'],
    'violent_storm': ['spiral', 'sky', 'spread_arms'],
    'tornado': ['spiral', 'sky', 'rise'],
    'eye_of_the_storm': ['spread_arms', 'sky', 'meditate'],
    'healing': ['self_buff', 'touch', 'meditate'],
    'grow': ['sow', 'ground', 'rise'],
    'magic_shield': ['barrier', 'spread_arms', 'rise'],
    'x_healing': ['touch', 'meditate', 'spread_arms'],
    'feast': ['sow', 'self_buff', 'spread_arms'],
    'shrine_healing': ['meditate', 'sky', 'spread_arms'],
    'disturb_magic': ['sweep', 'push', 'flick'],
    'strength': ['flex', 'self_buff', 'stomp'],
    'jump_boost': ['jump_cast', 'self_buff', 'stomp'],
    'night_vision': ['eye_touch', 'self_buff', 'meditate'],
    'water_breathing': ['breathe', 'self_buff', 'meditate'],
    'fire_resistance': ['self_buff', 'barrier', 'meditate'],
    'invisibility': ['crouch_hide', 'meditate', 'self_buff'],
    'repair': ['hammer', 'touch', 'meditate'],
    'cleanse': ['meditate', 'spread_arms', 'self_buff'],
    'mana_infusion': ['touch', 'channel', 'meditate'],
    'stone_refining': ['hammer', 'channel', 'palm_orb'],
}

# velocidad segun el rango (largo del combo): 3 = Principiante ... 8 = Imperial
SPEED = {3: 0.85, 4: 1.0, 5: 1.1, 6: 1.25, 7: 1.4, 8: 1.6}


def spells():
    src = open(os.path.join(REPO, 'src', 'main', 'java', 'net', 'zennin', 'mushoku', 'magic', 'Spell.java'), encoding='utf-8').read()
    out = []
    for m in re.finditer(r'^\s+[A-Z_]+\("([a-z_]+)", "([a-z]*)", Element\.([A-Z]+)', src, re.M):
        out.append((m.group(1), len(m.group(2)), m.group(3)))
    return out


def names():
    lang = json.load(open(os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'remushoku', 'lang', 'es_ar.json'), encoding='utf-8'))
    return {k[len('spell.remushoku.'):]: v for k, v in lang.items() if k.startswith('spell.remushoku.')}


ELEMENT = {'WATER': 'Agua', 'FIRE': 'Fuego', 'EARTH': 'Tierra', 'WIND': 'Viento', 'NONE': 'Sin elemento'}


def define():
    """Crea las 3 candidatas de cada hechizo. Devuelve los grupos para el Taller (por elemento)."""
    nm = names()
    groups = []
    for sid, combo, elem in spells():
        plan = PLAN.get(sid)
        if not plan:
            continue
        s = SPEED.get(min(8, max(3, combo)), 1.0)
        items = []
        for i, a in enumerate(plan, 1):
            label, fn = ARCH[a]
            name = 'sv_%s_%d' % (sid, i)
            v = V(name, '%s v%d: %s' % (nm.get(sid, sid), i, label), 40, s)
            fn(v)
            # el largo real lo marca el ultimo keyframe
            v.a.end = v.last
            v.a.stop = v.last + 4
            items.append(name)
        groups.append(dict(group='%s - %s' % (ELEMENT[elem], nm.get(sid, sid)), items=items, weapon='aqua_heartia'))
    return groups


if __name__ == '__main__':
    G.ANIMS.clear()
    groups = define()
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.endswith('.json'):
            os.remove(os.path.join(OUT, f))
    for a in G.ANIMS:
        with open(os.path.join(OUT, a.name + '.json'), 'w') as f:
            json.dump(a.build(), f, separators=(',', ':'))
    json.dump(groups, open(os.path.join(OUT, '_grupos.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(len(G.ANIMS), 'candidatas ->', OUT)

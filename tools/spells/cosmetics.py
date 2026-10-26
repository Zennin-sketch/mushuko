"""Rasgos de raza que se dibujan sobre la cabeza del jugador (orejas de elfo, cuernos de demonio, orejas de hombre bestia,
gema de Superd). Genera assets/remushoku/cosmetics/<rasgo>.json, que leen CosmeticLayer (juego) y el Grimorio (preview).

Marco de la cabeza, en pixeles: +X = derecha del jugador, +Y = arriba, +Z = hacia donde mira. La cabeza ocupa
X -4..4, Y 0..8, Z -4..4 (el cuello esta en Y = 0). 'heights' = desplazamientos en Y de las alturas que se pueden elegir.
Colores: hex, o "skin"/"hair" (se toma un pixel de la skin del jugador: mejilla o parte de arriba de la cabeza),
con un multiplicador opcional: "skin:0.8". 'glow' = brilla en la oscuridad.
"""
import json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from spell_models import seg, box, ASSETS, lerp_hex  # noqa: E402

def mirror(b):
    """Copia de una caja del lado izquierdo (espejo en X)."""
    return dict(b, at=[-b['at'][0], b['at'][1], b['at'][2]], rot=[b['rot'][0], -b['rot'][1], -b['rot'][2]])

def both(boxes):
    return boxes + [mirror(b) for b in boxes]

def tube(points, w0, w1, t0, t1, color_fn, alpha=1.0):
    out = []
    n = len(points) - 1
    for k in range(n):
        u = (k + 0.5) / n
        out.append(seg(points[k], points[k + 1], w0 + (w1 - w0) * u, t0 + (t1 - t0) * u, color_fn(u), alpha))
    return out

# ------------------------------------------------------------ Orejas como cadena articulada
def chain(base, base_rot, segs, widths, thick, color, shade=None, inner=None, twitch=0.0, sway=0.0, lag=1.2, mirror=True):
    """Oreja hecha de tramos unidos (como los codos y rodillas con Bendy-lib): sin huecos entre tramos.
    base: donde nace (lado derecho). base_rot: orientacion inicial (grados, X luego Y luego Z); el tramo crece en +Y local,
    el ancho va en X local y el grosor en Z local. segs: [largo, doblez_x, doblez_z] de cada tramo.
    widths/thick: ancho y grosor en cada union (tramos + 1). shade: multiplicador de color en cada union.
    inner: {color, width (fraccion), rings (cuantas uniones cubre)} = cara interna de otro color.
    twitch/sway: grados del tic de la oreja (cada pocos segundos) y del balanceo suave; lag: atraso de la punta (ticks)."""
    return dict(base=list(base), base_rot=list(base_rot), segs=[list(x) for x in segs], widths=list(widths), thick=list(thick),
                color=color, shade=list(shade or [1.0] * len(widths)), inner=inner, twitch=twitch, sway=sway, lag=lag, mirror=mirror)

# ------------------------------------------------------------ Orejas de elfo: largas y en punta, hacia afuera, arriba y atras
def elf_ears():
    ear = chain(base=(4.0, 0.0, 0.6), base_rot=(-20, -15, -62),
                segs=[(1.6, 0, 0), (1.6, 0, 9), (1.5, 0, 11), (1.5, 0, 12), (1.2, 0, 10)],
                widths=[2.9, 2.7, 2.2, 1.5, 0.8, 0.15], thick=[0.8, 0.75, 0.65, 0.55, 0.45, 0.3],
                color='skin', shade=[0.97, 1.0, 1.0, 0.98, 0.96, 0.95],
                inner=dict(color='skin:0.74', width=0.5, rings=4), twitch=6, sway=0.8, lag=1.0)
    return dict(name='Orejas de elfo', heights=[1.6, 2.6, 3.6], height_names=['Baja', 'Media', 'Alta'], boxes=[], chains=[ear])

# ------------------------------------------------------------ Cuernos de demonio: en espiral (tipo carnero), como Kishirika
def demon_horns():
    c = (5.4, 6.9, -1.4)
    phi0 = math.atan2(-0.8, 2.3)
    pts = []
    N = 22
    for k in range(N + 1):
        u = k / N
        th = u * 1.75 * math.pi
        r = 2.45 * (1 - 0.5 * u)
        phi = phi0 + th
        pts.append((3.7 + 2.6 * u, c[1] + r * math.sin(phi), c[2] + r * math.cos(phi)))
    horn = []
    for k in range(N):
        u = (k + 0.5) / N
        ring = k % 2 == 0   # estrias alternadas, como los anillos de un cuerno
        col = lerp_hex('2b2533', 'b9ab93', u ** 2.2)
        if ring:
            col = lerp_hex(col, '000000', 0.25)
        horn.append(seg(pts[k], pts[k + 1], 2.3 - 1.7 * u, 2.3 - 1.7 * u, col, 1.0))
    return dict(name='Cuernos de demonio', heights=[-1.0, 0.0, 1.0], height_names=['Baja', 'Media', 'Alta'], boxes=both(horn), chains=[])

# ------------------------------------------------------------ Orejas de hombre bestia (arriba de la cabeza)
def beast_cat():
    ear = chain(base=(2.3, 7.6, -0.3), base_rot=(-8, 0, -14),
                segs=[(1.6, 0, 0), (1.5, -5, 4), (1.4, -7, 6)],
                widths=[3.6, 3.1, 2.1, 0.35], thick=[1.0, 0.9, 0.7, 0.35], color='hair', shade=[0.92, 1.0, 1.0, 1.05],
                inner=dict(color='e8a6b0', width=0.55, rings=3), twitch=16, sway=3, lag=1.0)
    return dict(name='Orejas de gato', heights=[0.0], height_names=['—'], boxes=[], chains=[ear])

def beast_dog():
    # Oreja caida: sube un poco, se dobla hacia afuera y cuelga al costado de la cabeza
    ear = chain(base=(3.2, 7.7, -0.2), base_rot=(0, 0, -25),
                segs=[(0.8, 0, 0), (1.2, 0, -75), (1.9, 0, -55), (1.9, 0, -18), (1.4, 0, -5)],
                widths=[2.8, 3.1, 3.3, 3.2, 2.8, 2.0], thick=[0.9, 0.8, 0.75, 0.7, 0.65, 0.6], color='hair',
                shade=[0.95, 1.0, 0.97, 0.93, 0.88, 0.84], twitch=16, sway=4, lag=1.6)
    return dict(name='Orejas de perro', heights=[0.0], height_names=['—'], boxes=[], chains=[ear])

def beast_bear():
    ear = chain(base=(2.9, 7.6, -0.6), base_rot=(-4, 0, -10),
                segs=[(1.0, 0, 0), (1.0, 0, 8), (0.6, 0, 14)],
                widths=[2.6, 3.1, 2.8, 1.3], thick=[1.1, 1.1, 1.0, 0.8], color='hair', shade=[0.95, 1.0, 1.0, 1.02],
                inner=dict(color='9a6a5c', width=0.55, rings=3), twitch=10, sway=2, lag=0.8)
    return dict(name='Orejas de oso', heights=[0.0], height_names=['—'], boxes=[], chains=[ear])

# ------------------------------------------------------------ Gema de Superd: punto rojo centrado en la frente
def superd_gem():
    g = box((0, 6.1, 4.12), (1.2, 1.2, 0.3), 'd8141c')
    g['glow'] = True
    hl = box((-0.25, 6.35, 4.3), (0.4, 0.4, 0.1), 'ff9a9a')
    hl['glow'] = True
    return dict(name='Gema de Superd', heights=[0.0], height_names=['—'], boxes=[g, hl], chains=[])

COSMETICS = dict(elf_ears=elf_ears, demon_horns=demon_horns, beast_cat=beast_cat, beast_dog=beast_dog,
                 beast_bear=beast_bear, superd_gem=superd_gem)

def main():
    out = os.path.join(ASSETS, 'cosmetics')
    os.makedirs(out, exist_ok=True)
    for name, fn in COSMETICS.items():
        with open(os.path.join(out, name + '.json'), 'w') as f:
            json.dump(fn(), f, indent=1)
    print(len(COSMETICS), 'rasgos ->', out)

if __name__ == '__main__':
    main()

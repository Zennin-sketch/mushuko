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

# ------------------------------------------------------------ Orejas de elfo: largas, en punta, hacia arriba y atras
def elf_ears():
    pts = []
    for k in range(9):
        u = k / 8
        pts.append((4.15 + 4.2 * u ** 1.1, 0.2 + 3.9 * u, 0.2 - 3.4 * u ** 1.3))
    outer = tube(pts, 2.6, 0.35, 0.9, 0.6, lambda u: 'skin:%.2f' % (1.0 - 0.08 * u))
    # pliegue interno (mas oscuro), del lado de adelante
    inner = tube([(p[0] + 0.15, p[1], p[2] + 0.45) for p in pts[1:6]], 1.1, 0.5, 0.35, 0.3, lambda u: 'skin:0.72')
    return dict(name='Orejas de elfo', heights=[1.5, 2.5, 3.5], height_names=['Baja', 'Media', 'Alta'], boxes=both(outer + inner))

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
    return dict(name='Cuernos de demonio', heights=[-1.0, 0.0, 1.0], height_names=['Baja', 'Media', 'Alta'], boxes=both(horn))

# ------------------------------------------------------------ Orejas de hombre bestia (arriba de la cabeza)
def ear_stack(x, z, h, w0, w1, depth, tilt_z, tilt_x, color, inner, n=5):
    """Oreja de capas que se afinan hacia la punta, inclinada hacia afuera (tilt_z) y adelante/atras (tilt_x)."""
    out = []
    tz, tx = math.radians(tilt_z), math.radians(tilt_x)
    for i in range(n):
        u = (i + 0.5) / n
        w = w0 + (w1 - w0) * u
        y = 8 + h * u
        cx = x + math.sin(tz) * h * u
        cz = z + math.sin(tx) * h * u
        out.append(box((cx, y, cz), (w, h / n * 1.05, depth), color, rot=(tilt_x, 0, -tilt_z)))
        if inner and i < n - 1:
            out.append(box((cx, y, cz + depth / 2 + 0.05), (w * 0.55, h / n * 1.05, 0.15), inner, rot=(tilt_x, 0, -tilt_z)))
    return out

def beast_cat():
    ears = ear_stack(2.4, -0.6, 4.2, 3.2, 0.5, 1.1, 14, -6, 'hair', 'e8a6b0')
    return dict(name='Orejas de gato', heights=[0.0], height_names=['—'], boxes=both(ears))

def beast_dog():
    # Base parada y la punta doblada hacia adelante
    base = ear_stack(2.6, -0.8, 3.0, 3.0, 2.2, 1.0, 10, -4, 'hair', 'c98f86', n=3)
    tip = [box((2.95, 10.9, 0.3), (2.3, 0.9, 2.4), 'hair', rot=(58, 0, -10)),
           box((3.0, 10.1, 1.3), (1.9, 0.8, 1.1), 'hair:0.88', rot=(75, 0, -10))]
    return dict(name='Orejas de perro', heights=[0.0], height_names=['—'], boxes=both(base + tip))

def beast_bear():
    ear = [box((2.9, 8.9, -0.8), (2.8, 1.8, 1.2), 'hair'),
           box((2.9, 10.0, -0.8), (2.0, 0.8, 1.2), 'hair'),
           box((2.9, 9.2, -0.15), (1.4, 1.3, 0.2), 'b07a6a')]
    return dict(name='Orejas de oso', heights=[0.0], height_names=['—'], boxes=both(ear))

# ------------------------------------------------------------ Gema de Superd: punto rojo centrado en la frente
def superd_gem():
    g = box((0, 6.1, 4.12), (1.2, 1.2, 0.3), 'd8141c')
    g['glow'] = True
    hl = box((-0.25, 6.35, 4.3), (0.4, 0.4, 0.1), 'ff9a9a')
    hl['glow'] = True
    return dict(name='Gema de Superd', heights=[0.0], height_names=['—'], boxes=[g, hl])

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

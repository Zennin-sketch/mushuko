"""Segmento (pomo -> punta) de cada arma, en el marco del objeto en la mano (despues del display de tercera persona).
Sale del modelo 3D real del mod, asi la colision del arma usa su largo y su agarre verdaderos."""
import math
import build_preview as BP
from animgen import mat_mul, mat_vec, rx, ry, rz, D

HANDHELD = {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.85, 0.85, 0.85]}

def _model_points(wid, w):
    if w.get('geo'):
        m = BP.load_geo(w['geo'], w['texture'], wid)
        pts = []
        for bone in m['bones']:
            for c in bone.get('cubes', []):
                o, sz = c['origin'], c['size']
                for x in (-(o[0] + sz[0]), -o[0]):
                    for y in (o[1], o[1] + sz[1]):
                        for z in (o[2], o[2] + sz[2]):
                            pts.append([x + 8, y + 8.16, z + 8])
        return pts, m['display']
    m = BP.load_model(wid)
    if not m['elements']:  # sprite plano de item/handheld: diagonal del dibujo
        return [[2, 2, 8], [15, 15, 8]], m['display'] or HANDHELD
    pts = []
    for el in m['elements']:
        f, t = el['from'], el['to']
        for x in (f[0], t[0]):
            for y in (f[1], t[1]):
                for z in (f[2], t[2]):
                    pts.append([x, y, z])
    return pts, m['display']

def segment(wid, w):
    pts, disp = _model_points(wid, w)
    tr = disp.get('translation', [0, 0, 0]); ro = disp.get('rotation', [0, 0, 0]); sc = disp.get('scale', [1, 1, 1])
    Rm = mat_mul(rx(ro[0] * D), mat_mul(ry(ro[1] * D), rz(ro[2] * D)))
    q = [[tr[i] + v for i, v in enumerate(mat_vec(Rm, [(p[0] - 8) * sc[0], (p[1] - 8) * sc[1], (p[2] - 8) * sc[2]]))] for p in pts]
    # eje mas largo del arma (componentes principales simplificado: el par de puntos mas alejado)
    best, pair = -1, None
    for i in range(len(q)):
        for j in range(i + 1, len(q)):
            d = sum((q[i][k] - q[j][k]) ** 2 for k in range(3))
            if d > best:
                best, pair = d, (q[i], q[j])
    a, b = pair
    # se achica un poco hacia el eje (las esquinas del modelo no son la punta real)
    c = [(a[k] + b[k]) / 2 for k in range(3)]
    return [[c[k] + (a[k] - c[k]) * 0.92 for k in range(3)], [c[k] + (b[k] - c[k]) * 0.92 for k in range(3)]]

if __name__ == '__main__':
    import catalog
    for wid, w in catalog.WEAPONS.items():
        a, b = segment(wid, w)
        print(wid.ljust(22), 'largo %.1f px' % math.dist(a, b))

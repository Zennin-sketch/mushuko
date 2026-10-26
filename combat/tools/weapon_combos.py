"""Un combo propio para cada arma, en proporcion a su tamano.

Cada arma toma 3 golpes de la biblioteca (los de cada familia) y los adapta:
 * b (cuerpo): cuanto participan la cadera, el torso, el paso y la flexion de rodillas. Las armas largas y pesadas
   necesitan mover todo el cuerpo (b > 1); las cortas se manejan con el brazo (b < 1).
 * mano: a una mano (la izquierda hace de contrapeso) o a dos (la izquierda sobre el mango, a 'spread' px).
 * enlace: el final de cada golpe es la pose con la que arranca el siguiente, asi el combo fluye.
La colision del arma usa el modelo real de esa arma (no el mas largo de la familia).
"""
import json
import animgen as G

def _scale_turn(t, b):
    k = round(t / 360.0)
    return k * 360 + (t - k * 360) * b

def adapt(pose, spec):
    p = json.loads(json.dumps(pose))
    b = spec['b']
    r = p['root']
    p['root'] = [r[0] * b, r[1] * b, r[2] * b, r[3] * b, _scale_turn(r[4], b), r[5] * b, r[6]]
    t = p['torso']
    p['torso'] = [t[0] * b, t[1] * b, t[2] * b]
    for leg in ('rleg', 'lleg'):
        if p[leg][1] > 0:
            p[leg][1] = p[leg][1] * (0.7 + 0.3 * b)
    two_template = p['grip'][0] > 0.5
    hand_pos, blade = p['rarm'][0], G.norm(p['rarm'][1])
    if spec['hand'] == 'one':
        if two_template:
            # contrapeso: la izquierda abierta al costado, del lado contrario al arma
            p['larm'] = [[-8, -9, -hand_pos[2] * 0.25], [0, -0.2, 1]]
        p['grip'] = [0, 0]
    else:
        sp = spec['spread']
        p['larm'] = [[hand_pos[i] - blade[i] * sp for i in range(3)], list(blade)]
        p['grip'] = [1, sp]
    return p

def build(anims_by_name, wid, spec):
    """Crea <arma>_1.. con los golpes de spec['moves']."""
    moves = [anims_by_name[m] for m in spec['moves']]
    firsts = [adapt(m.poses[0][1], spec) for m in moves]
    out = []
    for i, (tpl, name) in enumerate(zip(moves, spec['moves'])):
        a = G.anim('%s_%d' % (wid, i + 1), '%s: golpe %d (de %s)' % (spec['label'], i + 1, name),
                   end=tpl.end, begin=tpl.begin, stop=tpl.stop,
                   lag=('head', 'larm') if spec['hand'] == 'one' else ('head',))
        n = len(tpl.poses)
        for k, (tick, pose, mode) in enumerate(tpl.poses):
            last = k == n - 1
            q = firsts[i + 1] if (last and i + 1 < len(moves)) else adapt(pose, spec)
            a.key(tick, 'stop' if last else mode, **q)
        out.append(a)
    return out

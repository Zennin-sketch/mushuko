"""Genera todo lo del addon: animaciones, weapon_attributes de Better Combat y sheaths.json (armas guardadas).
Uso: python3 build_data.py"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import animgen as G
import anims
import catalog

def assign_weapons(G, anims, catalog, longest):
    by_name = {a.name: a for a in G.ANIMS}
    for fam, f in catalog.FAMILIES.items():
        seg = longest(fam)
        for atk in f['attacks']:
            a = by_name[atk['anim']]
            if a.weapon is None:
                a.weapon = seg
    sword, staff, spear = longest('sword'), longest('staff'), longest('spear')
    for name, a in by_name.items():
        if name.startswith(('roll_', 'dash_')):
            a.weapon = sword
        elif name.startswith('cast_') or name == 'pose_staff':
            a.weapon = staff
        elif name == 'pose_spear':
            a.weapon = spear

import weapons

RES = os.path.join(HERE, '..', 'src', 'main', 'resources')

# Colision del arma: cada animacion usa el arma mas larga de su familia (si esa no atraviesa el cuerpo, ninguna)
def longest(family):
    segs = [weapons.segment(wid, w) for wid, w in catalog.WEAPONS.items() if w['family'] == family]
    return max(segs, key=lambda s: sum((s[0][k] - s[1][k]) ** 2 for k in range(3)))

assign_weapons(G, anims, catalog, longest)

# Combos propios de cada arma (colision con el modelo de esa misma arma)
import weapon_combos
_by_name = {a.name: a for a in G.ANIMS}
for _wid, _spec in catalog.COMBOS.items():
    _spec = dict(_spec, label=catalog.WEAPONS[_wid]['label'])
    for _a in weapon_combos.build(_by_name, _wid, _spec):
        _a.weapon = weapons.segment(_wid, catalog.WEAPONS[_wid])
for _a in G.ANIMS:
    if _a.name.startswith('magic_'):
        _a.weapon = longest('staff')

if __name__ == '__main__':
    G.write(os.path.join(RES, 'assets', 'mushoku_combat', 'player_animations'))

    wa = os.path.join(RES, 'data', 'remushoku', 'weapon_attributes')
    os.makedirs(wa, exist_ok=True)
    for wid, w in catalog.WEAPONS.items():
        with open(os.path.join(wa, wid + '.json'), 'w') as fh:
            json.dump(catalog.weapon_attributes(wid), fh, indent=2)

    slots = {}
    for slot, (a, grab) in anims.SHEATH_SLOTS.items():
        draw = a.name
        sheathe = slot + '_sheathe'
        sh = next(x for x in G.ANIMS if x.name == sheathe)
        release = next(t for t, _, m in sh.keys if t > 0 and m == 'stop' and t >= 6)
        slots[slot] = dict(G.hand_slot(a, grab), draw=draw, sheathe=sheathe, grab=grab, drawLength=a.stop,
                           release=release, sheatheLength=sh.stop)
    data = dict(slots=slots, items={'remushoku:' + wid: catalog.FAMILIES[w['family']]['slot'] for wid, w in catalog.WEAPONS.items()})
    with open(os.path.join(RES, 'assets', 'mushoku_combat', 'sheaths.json'), 'w') as fh:
        json.dump(data, fh, indent=1)
    print('sheaths.json:', json.dumps(slots, indent=None)[:400])

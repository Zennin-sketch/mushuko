"""Genera todo lo del addon: animaciones, weapon_attributes de Better Combat y sheaths.json (armas guardadas).
Uso: python3 build_data.py"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import animgen as G
import anims
import catalog

RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
G.write(os.path.join(RES, 'assets', 'mushoku_combat', 'player_animations'))

wa = os.path.join(RES, 'data', 'remushoku', 'weapon_attributes')
os.makedirs(wa, exist_ok=True)
for wid, w in catalog.WEAPONS.items():
    with open(os.path.join(wa, wid + '.json'), 'w') as fh:
        json.dump(catalog.attributes(w['family']), fh, indent=2)

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

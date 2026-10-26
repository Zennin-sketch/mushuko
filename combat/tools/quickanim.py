"""Regenera solo algunas animaciones (sin borrar las demas). Uso: python3 quickanim.py bow_draw bow_release"""
import json, os, sys, time
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_data as B
names = set(sys.argv[1:])
out = os.path.join(B.RES, 'assets', 'mushoku_combat', 'player_animations')
t = time.time()
for a in B.G.ANIMS:
    if a.name in names:
        with open(os.path.join(out, a.name + '.json'), 'w') as f:
            json.dump(a.build(), f, separators=(',', ':'))
        print(a.name, round(time.time() - t, 1), 's')

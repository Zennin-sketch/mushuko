"""Baja de Modrinth los mods que hacen falta para la prueba visual (Fabric 1.21.1).
Uso: fetch_mods.py <carpeta mods> [extras] ; extras 'fresh' agrega EMF/ETF y baja los packs de Fresh Animations
(base y Fresh Moves) a <carpeta mods>/../resourcepacks y los activa en options.txt."""
import json, os, sys, urllib.parse, urllib.request

SLUGS = ['fabric-api', 'better-combat', 'playeranimator', 'cloth-config', '3dskinlayers']
EXTRA = {'fresh': (['entity-model-features', 'entitytexturefeatures'], ['fresh-animations', 'tras-fresh-player'])}
out = sys.argv[1]
extras = [e for e in (sys.argv[2] if len(sys.argv) > 2 else '').split(',') if e]
os.makedirs(out, exist_ok=True)
UA = {'User-Agent': 'mushoku-visualtest'}


def versions(slug, loaders, gv):
    q = {}
    if loaders:
        q['loaders'] = json.dumps(loaders)
    if gv:
        q['game_versions'] = json.dumps(gv)
    req = urllib.request.Request(f'https://api.modrinth.com/v2/project/{slug}/version?' + urllib.parse.urlencode(q), headers=UA)
    return json.load(urllib.request.urlopen(req))


def get(slug, dest, loaders, gvs):
    vs = []
    for gv in gvs:
        vs = versions(slug, loaders, gv)
        if vs:
            break
    if not vs:
        print('sin version para', slug)
        return None
    f = next((x for x in vs[0]['files'] if x.get('primary')), vs[0]['files'][0])
    print(slug, vs[0]['version_number'], vs[0]['game_versions'][-3:], f['filename'])
    req = urllib.request.Request(f['url'], headers=UA)
    open(os.path.join(dest, f['filename']), 'wb').write(urllib.request.urlopen(req).read())
    return f['filename']


mods = list(SLUGS)
packs = []
for e in extras:
    m, p = EXTRA[e]
    mods += m
    packs += p
for slug in mods:
    get(slug, out, ['fabric'], [['1.21.1']])
if packs:
    rp = os.path.join(out, '..', 'resourcepacks')
    os.makedirs(rp, exist_ok=True)
    names = []
    for slug in packs:
        n = get(slug, rp, None, [['1.21.1'], ['1.21'], ['1.21.4'], None])
        if n:
            names.append(n)
    opt = os.path.join(out, '..', 'options.txt')
    line = 'resourcePacks:' + json.dumps(['vanilla', 'fabric'] + ['file/' + n for n in names]) + '\n'
    open(opt, 'a').write(line)
    print(line)

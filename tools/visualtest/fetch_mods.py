"""Baja de Modrinth los mods que hacen falta para la prueba visual (Fabric 1.21.1)."""
import json, os, sys, urllib.parse, urllib.request

SLUGS = ['fabric-api', 'better-combat', 'playeranimator', 'cloth-config', '3dskinlayers']
out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for slug in SLUGS:
    q = urllib.parse.urlencode({'loaders': json.dumps(['fabric']), 'game_versions': json.dumps(['1.21.1'])})
    req = urllib.request.Request(f'https://api.modrinth.com/v2/project/{slug}/version?{q}', headers={'User-Agent': 'mushoku-visualtest'})
    versions = json.load(urllib.request.urlopen(req))
    if not versions:
        print('sin version para', slug); continue
    f = next((x for x in versions[0]['files'] if x.get('primary')), versions[0]['files'][0])
    print(slug, versions[0]['version_number'], f['filename'])
    req = urllib.request.Request(f['url'], headers={'User-Agent': 'mushoku-visualtest'})
    open(os.path.join(out, f['filename']), 'wb').write(urllib.request.urlopen(req).read())

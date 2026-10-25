"""Arma la pagina de previsualizacion: incrusta animaciones, modelos 3D de las armas y sus texturas.
Uso: python3 build_preview.py <carpeta_animaciones> <salida.html>
"""
import base64, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(REPO, 'src', 'main', 'resources', 'assets')
COMBAT_DATA = os.path.join(REPO, 'combat', 'src', 'main', 'resources', 'data')

sys.path.insert(0, HERE)
import catalog  # noqa: E402

def res(loc, folder, ext):
    ns, path = loc.split(':', 1) if ':' in loc else ('minecraft', loc)
    return os.path.join(ASSETS, ns, folder, path + ext)

def data_uri(png):
    with open(png, 'rb') as f:
        return 'data:image/png;base64,' + base64.b64encode(f.read()).decode()

def load_model(item):
    """Sigue la cadena de 'parent' y junta elementos, texturas y display."""
    path = res('remushoku:item/' + item, 'models', '.json')
    elements, textures, display = None, {}, {}
    chain = []
    while path and os.path.exists(path):
        m = json.load(open(path))
        chain.append(m)
        parent = m.get('parent')
        if not parent or parent.startswith('builtin') or parent.startswith('item/') or parent.startswith('minecraft:'):
            break
        path = res(parent, 'models', '.json')
    for m in reversed(chain):
        if 'elements' in m:
            elements = m['elements']
        textures.update(m.get('textures', {}))
        for k, v in m.get('display', {}).items():
            display[k] = v
    tex = {}
    for k, v in textures.items():
        if v.startswith('#'):
            continue
        p = res(v, 'textures', '.png')
        if os.path.exists(p):
            tex[k] = data_uri(p)
    return dict(kind='block', elements=elements or [], textures=tex,
                display=display.get('thirdperson_righthand', {}), builtin=chain[0].get('parent', '').startswith('builtin'))

def load_geo(geo, texture, item):
    g = json.load(open(res(geo, '', '')))['minecraft:geometry'][0]
    m = json.load(open(res('remushoku:item/' + item, 'models', '.json')))
    return dict(kind='geo', bones=g['bones'], texW=g['description']['texture_width'], texH=g['description']['texture_height'],
                texture=data_uri(res(texture, '', '')), display=m.get('display', {}).get('thirdperson_righthand', {}))

def main(anim_dir, out):
    anims = {}
    for f in sorted(os.listdir(anim_dir)):
        if f.endswith('.json'):
            anims[f[:-5]] = json.load(open(os.path.join(anim_dir, f)))
    weapons = {}
    for wid, w in catalog.WEAPONS.items():
        if w.get('geo'):
            model = load_geo(w['geo'], w['texture'], wid)
        else:
            model = load_model(wid)
        model['label'] = w['label']
        model['family'] = w['family']
        weapons[wid] = model
    data = dict(anims=anims, weapons=weapons, families=catalog.FAMILIES, extra=catalog.EXTRA)
    tpl = open(os.path.join(HERE, 'preview_template.html'), encoding='utf-8').read()
    html = tpl.replace('/*__DATA__*/null', json.dumps(data, separators=(',', ':')))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(html)
    print('preview ->', out, round(len(html) / 1024), 'KB')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])

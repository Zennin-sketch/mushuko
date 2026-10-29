"""Fresh Moves "solo ojos": deja intactas las animaciones de ojos, cejas, parpados y pestanas de Fresh Moves y saca las
del cuerpo (cabeza, torso, brazos, piernas, capa), para que el caminar/correr del mod (con rodillas y codos) mande.

Uso: python3 eyes_only.py <FreshMoves.zip> <salida.zip>
Es para uso personal (el pack es de su autor): no lo vuelvas a publicar.
"""
import json
import re
import sys
import zipfile

EYE = re.compile(r'^(right_eye|left_eye|r_eye|l_eye|right_brow|left_brow|brow|brows|eyes|extras|vertical_eyelash|'
                 r'horizontal_eyelash|right_eyelid|left_eyelid|right_blink|left_blink|ctrl_|right_eye_white|left_eye_white)')
BODY = {'head', 'headwear', 'body', 'jacket', 'right_arm', 'left_arm', 'right_sleeve', 'left_sleeve', 'right_leg', 'left_leg',
        'right_pants', 'left_pants', 'cloak'}


def keep(key):
    target = key.split('.')[0]
    if target in ('var', 'varb'):
        return True
    if target in BODY:
        return False
    return bool(EYE.match(target)) or target not in BODY


def filter_jem(data):
    removed = 0
    def walk(m):
        nonlocal removed
        anims = m.get('animations')
        if anims:
            out = []
            for a in anims:
                b = {k: v for k, v in a.items() if keep(k)}
                removed += len(a) - len(b)
                if b:
                    out.append(b)
            m['animations'] = out
        for s in m.get('submodels', []) or []:
            walk(s)
    for m in data.get('models', []):
        walk(m)
    return removed


def main(src, dst):
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            raw = zin.read(info.filename)
            name = info.filename
            if re.search(r'optifine/cem/player(_slim)?\.jem$', name):
                data = json.loads(raw.decode('utf-8'))
                n = filter_jem(data)
                print(name, 'animaciones del cuerpo quitadas:', n)
                raw = json.dumps(data).encode('utf-8')
            elif name.endswith('.jpm') and 'optifine/cem/player' in name:
                data = json.loads(raw.decode('utf-8'))
                filter_jem({'models': [data]})
                raw = json.dumps(data).encode('utf-8')
            elif name == 'pack.mcmeta':
                meta = json.loads(raw.decode('utf-8'))
                meta.setdefault('pack', {})['description'] = 'Fresh Moves (solo ojos) - uso personal con Mushoku'
                raw = json.dumps(meta, indent=1).encode('utf-8')
            zout.writestr(info, raw)
        # marca para el mod: este pack no anima el cuerpo, asi que el caminar del mod sigue andando
        zout.writestr('assets/minecraft/optifine/cem/mushoku_eyes_only.txt', 'Fresh Moves solo ojos\n')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])

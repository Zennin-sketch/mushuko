"""Arma la pagina 'Grimorio de Hechizos' con los modelos y texturas incrustados.
Uso: python3 build_spell_preview.py <salida.html>"""
import base64, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import spell_models  # noqa: E402

def main(out):
    models = {name: fn() for name, fn in spell_models.all_models().items()}
    tdir = os.path.join(spell_models.ASSETS, 'textures', 'entity', 'spell')
    textures = {}
    for f in sorted(os.listdir(tdir)):
        with open(os.path.join(tdir, f), 'rb') as fh:
            textures[f[:-4]] = 'data:image/png;base64,' + base64.b64encode(fh.read()).decode()
    tpl = open(os.path.join(HERE, 'preview_template.html'), encoding='utf-8').read()
    html = tpl.replace('/*__DATA__*/null', json.dumps(dict(models=models, textures=textures), separators=(',', ':')))
    with open(out, 'w', encoding='utf-8') as f:
        f.write(html)
    print('preview ->', out, round(len(html) / 1024), 'KB')

if __name__ == '__main__':
    main(sys.argv[1])

"""Pagina del Taller solo con las candidatas de los hechizos (spell_candidates/), 3 por hechizo, para elegir.
Uso: python3 build_spell_taller.py <salida.html>
"""
import json, os, shutil, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import catalog  # noqa: E402
import build_preview  # noqa: E402

CAND = os.path.join(HERE, 'spell_candidates')
ANIMS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mushoku_combat', 'player_animations')


def main(out):
    groups = json.load(open(os.path.join(CAND, '_grupos.json'), encoding='utf-8'))
    keep = [g for g in catalog.EXTRA if g['group'] in ('Conjuro', 'Caminar, correr y quieto')]
    catalog.EXTRA[:] = groups + keep
    tmp = tempfile.mkdtemp()
    for d in (ANIMS, CAND):
        for f in os.listdir(d):
            if f.endswith('.json') and not f.startswith('_'):
                shutil.copy(os.path.join(d, f), tmp)
    build_preview.main(tmp, out)
    shutil.rmtree(tmp)
    html = open(out, encoding='utf-8').read()
    html = html.replace('Taller de Combate', 'Taller de Hechizos')
    open(out, 'w', encoding='utf-8').write(html)


if __name__ == '__main__':
    main(sys.argv[1])

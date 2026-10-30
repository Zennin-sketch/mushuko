"""Dispensador de Piedras y Mesa de Investigacion: texturas, modelos, blockstates, items, recetas y botin."""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources'))
A = os.path.join(RES, 'assets', 'remushoku')
D = os.path.join(RES, 'data', 'remushoku')

COPPER = [(196, 112, 76), (170, 92, 62), (220, 140, 98), (150, 78, 52)]
LAPIS = [(38, 82, 170), (30, 64, 140), (64, 112, 208)]


def save(img, *p):
    path = os.path.join(A, 'textures', *p)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def jdump(obj, *p):
    path = os.path.join(*p)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


def copper_base(seed):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = COPPER[0] if (x // 8 + y // 8) % 2 == 0 else COPPER[1]
            if rnd.random() < 0.18: c = COPPER[2]
            if rnd.random() < 0.06: c = (88, 160, 130)  # patina
            if x in (0, 15) or y in (0, 15): c = COPPER[3]
            if (x in (2, 13) and y in (2, 13)): c = (240, 200, 150)  # remaches
            img.putpixel((x, y), c + (255,))
    return img


def dispenser_textures():
    side = copper_base(1)
    for x in range(1, 15):
        side.putpixel((x, 7), LAPIS[0] + (255,)); side.putpixel((x, 8), LAPIS[1] + (255,))
    save(side, 'block', 'stone_dispenser_side.png')
    top = copper_base(2)
    for i in range(4, 12):
        for (x, y) in ((i, 4), (i, 11), (4, i), (11, i)):
            top.putpixel((x, y), LAPIS[2] + (255,))
    for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)):
        top.putpixel((x, y), (190, 140, 255, 255))
    save(top, 'block', 'stone_dispenser_top.png')
    for name in ('stone_dispenser_front', 'stone_dispenser_front_vertical'):
        f = copper_base(3)
        # marco de lapislazuli y boca con la gema de amatista
        for y in range(3, 13):
            for x in range(3, 13):
                edge = x in (3, 12) or y in (3, 12)
                f.putpixel((x, y), (LAPIS[0] if edge else (22, 18, 34)) + (255,))
        for (x, y, c) in ((7, 6, (220, 180, 255)), (8, 6, (190, 140, 255)), (6, 7, (190, 140, 255)), (7, 7, (150, 100, 230)),
                          (8, 7, (150, 100, 230)), (9, 7, (120, 80, 200)), (6, 8, (150, 100, 230)), (7, 8, (120, 80, 200)),
                          (8, 8, (120, 80, 200)), (9, 8, (100, 60, 170)), (7, 9, (100, 60, 170)), (8, 9, (90, 50, 150))):
            f.putpixel((x, y), c + (255,))
        if name.endswith('vertical'):
            for i in range(5, 11):
                f.putpixel((i, 1), LAPIS[2] + (255,)); f.putpixel((i, 14), LAPIS[2] + (255,))
        save(f, 'block', name + '.png')


def book_texture():
    img = Image.new('RGBA', (16, 16), (236, 222, 186, 255))
    rnd = random.Random(4)
    for y in range(2, 15, 2):
        for x in range(1, 15):
            if x != 8 and rnd.random() < 0.7:
                img.putpixel((x, y), (90, 70, 50, 255))
    for y in range(16):
        img.putpixel((8, y), (150, 120, 90, 255))
    for (x, y) in ((3, 5), (4, 6), (5, 5), (11, 9), (12, 8), (13, 9)):
        img.putpixel((x, y), (60, 90, 200, 255))  # runas azules
    save(img, 'block', 'research_book.png')


def el(f, t, tex, rot=None, uv=None):
    faces = {}
    for k in ('north', 'south', 'east', 'west', 'up', 'down'):
        faces[k] = dict(texture=tex)
        if uv: faces[k]['uv'] = uv
    e = {'from': list(f), 'to': list(t), 'faces': faces}
    if rot:
        e['rotation'] = dict(axis=rot[0], angle=rot[1], origin=list(rot[2]))
    return e


def research_props():
    """Atlas de 32x32 para los objetos del escritorio (UV en unidades de 16: cada unidad = 2 texeles)."""
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    rnd = random.Random(11)
    P = lambda x, y, c: img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else tuple(c))
    def rect(u0, v0, u1, v1, fn):
        for y in range(v0 * 2, v1 * 2):
            for x in range(u0 * 2, u1 * 2):
                P(x, y, fn(x - u0 * 2, y - v0 * 2, (u1 - u0) * 2, (v1 - v0) * 2))
    paper = (238, 226, 196)
    # paginas del libro (izq u0-5, der u5-10; v0-7): renglones de texto, una runa azul y sombra hacia el lomo
    def page(right):
        def fn(x, y, w, h):
            edge = (x == 0 and right) or (x == w - 1 and not right)
            c = tuple(int(v * (0.86 if edge else 1.0)) for v in paper)
            if 2 <= y < h - 2 and y % 2 == 0 and 2 <= x < w - 2 and rnd.random() < 0.75:
                c = (92, 72, 54)
            if not right and 3 <= x <= 6 and 3 <= y <= 6 and (x - 4.5) ** 2 + (y - 4.5) ** 2 < 3.2:
                c = (60, 100, 210)
            if right and 4 <= x <= 7 and 8 <= y <= 11 and (x + y) % 3 == 0:
                c = (170, 40, 40)
            return c
        return fn
    rect(0, 0, 5, 7, page(False)); rect(5, 0, 10, 7, page(True))
    # tapa de cuero con borde dorado (u10-16 v0-2) y canto de hojas (u10-16 v2-3)
    rect(10, 0, 16, 2, lambda x, y, w, h: (200, 160, 70) if y in (0, h - 1) or x in (0, w - 1) else (110, 34, 30) if (x + y) % 5 else (96, 28, 26))
    rect(10, 2, 16, 3, lambda x, y, w, h: (228, 214, 180) if y % 2 == 0 else (204, 190, 156))
    # pergamino suelto (u0-6 v7-12): un circulo magico dibujado y notas
    def sheet(x, y, w, h):
        c = tuple(int(v * (0.94 if (x + y * 3) % 7 == 0 else 1)) for v in paper)
        d = ((x - 5.5) ** 2 + (y - 5) ** 2) ** 0.5
        if 3.3 < d < 4.3 or (d < 3.3 and (x == 5 or y == 5) and d > 1):
            c = (50, 80, 170)
        if x >= 9 and y % 2 == 1 and rnd.random() < 0.6:
            c = (100, 80, 60)
        if x == 0 or y == h - 1:
            c = (210, 196, 160)
        return c
    rect(0, 7, 6, 12, sheet)
    # pergamino enrollado (u6-12 v7-9) y cinta roja (u12-13 v7-9)
    rect(6, 7, 12, 9, lambda x, y, w, h: tuple(int(v * (1.0, 0.95, 0.86, 0.8)[y]) for v in paper))
    rect(12, 7, 13, 9, lambda x, y, w, h: (170, 30, 34) if x == 0 else (130, 20, 24))
    # tintero: vidrio oscuro con brillo y etiqueta (u12-16 v9-12)
    rect(12, 9, 16, 12, lambda x, y, w, h: (220, 230, 255) if (x == 1 and 1 <= y <= 3) else (180, 160, 120) if y in (3, 4) else (26, 22, 40) if y > 1 else (50, 44, 70))
    # frascos: azul (u0-3 v12-16) y rojo (u3-6 v12-16): vidrio arriba, liquido abajo, reflejo
    def bottle(liq):
        def fn(x, y, w, h):
            if x == 1 and 2 <= y <= 5:
                return (240, 250, 255)
            if y < 3:
                return (190, 215, 235)
            k = 1.15 - y / h * 0.4
            return tuple(max(0, min(255, int(v * k))) for v in liq)
        return fn
    rect(0, 12, 3, 16, bottle((60, 110, 240))); rect(3, 12, 6, 16, bottle((200, 40, 60)))
    rect(6, 12, 7, 13, lambda x, y, w, h: (160, 110, 64) if (x + y) % 2 else (130, 86, 48))  # corcho
    rect(6, 13, 7, 16, lambda x, y, w, h: (110, 60, 130) if y % 2 else (140, 80, 170))      # tapa de libro lila
    # pluma (u7-9 v12-16): blanca con raquis gris y punta oscura
    rect(7, 12, 9, 16, lambda x, y, w, h: (40, 40, 50) if y >= h - 2 and x in (1, 2) else (150, 150, 160) if x in (1, 2) else (246, 246, 250) if (x + y) % 4 else (220, 220, 230))
    # vela (u9-11 v12-16) con gotas, mecha (u11-12 v12-13)
    rect(9, 12, 11, 16, lambda x, y, w, h: (250, 244, 220) if not (y == 1 and x % 2) else (232, 222, 190))
    rect(11, 12, 12, 13, lambda x, y, w, h: (40, 30, 24))
    # lomos de libros (u12-16 v12-16): verde, azul y marron con lineas doradas
    cols = [(46, 90, 52), (40, 60, 120), (110, 70, 40), (120, 30, 40)]
    rect(12, 12, 16, 16, lambda x, y, w, h: (210, 170, 80) if y in (1, h - 2) else tuple(int(v * (0.9 if x % 2 else 1)) for v in cols[x // 2]))
    save(img, 'block', 'research_props.png')


def research_table():
    """Escritorio de dos bloques. Coordenadas del escritorio: x 0..32 (0..16 = mitad de al lado, 16..32 = la principal),
    el frente (donde se para el jugador) es -Z. Cada elemento va a la mitad que lo contiene."""
    research_props()
    tex = dict(plank='minecraft:block/dark_oak_planks', log='minecraft:block/dark_oak_log', p='remushoku:block/research_props',
               gem='minecraft:block/amethyst_block', drawer='remushoku:block/research_drawer', particle='minecraft:block/dark_oak_planks')
    R = dict(page_l=(0, 0, 5, 7), page_r=(5, 0, 10, 7), cover=(10, 0, 16, 2), edge=(10, 2, 16, 3), sheet=(0, 7, 6, 12),
             roll=(6, 7, 12, 9), ribbon=(12, 7, 13, 9), ink=(12, 9, 16, 12), blue=(0, 12, 3, 16), red=(3, 12, 6, 16),
             cork=(6, 12, 7, 13), feather=(7, 12, 9, 16), candle=(9, 12, 11, 16), wick=(11, 12, 12, 13), spines=(12, 12, 16, 16),
             lilac=(6, 13, 7, 16))
    def P(name, top=None):
        """Objeto con textura del atlas: todas las caras con la misma region (y otra arriba si se pide)."""
        return ('#p', R[name], R[top] if top else None)
    desk = [
        ((0, 12, 1), (32, 15, 15), '#plank', None),          # tablero
        ((1, 0, 2), (3, 12, 4), '#log', None), ((1, 0, 12), (3, 12, 14), '#log', None),
        ((29, 0, 2), (31, 12, 4), '#log', None), ((29, 0, 12), (31, 12, 14), '#log', None),
        # travesanos que unen las patas (atras y a los costados), pegados abajo del tablero
        ((3, 10, 12.5), (17, 12, 13.5), '#plank', None),
        ((1.5, 10, 4), (2.5, 12, 12), '#plank', None), ((29.5, 10, 4), (30.5, 12, 12), '#plank', None),
        # estante bajo (del lado), entre la pata y la cajonera, apoyado en un travesano
        ((3, 3, 4.5), (17, 4, 12.5), '#plank', None),
        ((3, 2, 11.5), (17, 3, 12.5), '#plank', None),
        # cajonera: caja pegada al tablero y al piso, entre las patas; los cajones solo al frente
        ((17, 1, 3), (29, 12, 13), 'drawer', None),
        # libros parados en el estante de abajo
        ((4, 4, 5), (5.5, 9, 11), P('spines'), None), ((5.5, 4, 5), (7, 8.5, 11), P('spines'), None),
        ((7, 4, 5), (8.5, 9.5, 11), P('spines'), None), ((9, 4, 5.5), (14, 5.5, 10.5), P('lilac', 'cover'), None),
        # libro abierto: tapa de cuero y dos paginas inclinadas hacia el lomo
        ((4.5, 15, 4.5), (15.5, 15.5, 11.5), P('cover'), None),
        ((5, 15.5, 5), (10, 16.3, 11), P('edge', 'page_l'), ('z', -22.5, (10, 15.5, 8))),
        ((10, 15.5, 5), (15, 16.3, 11), P('edge', 'page_r'), ('z', 22.5, (10, 15.5, 8))),
        # hoja suelta con un circulo magico, debajo del pergamino
        ((1.5, 15, 1.5), (7.5, 15.1, 6.5), P('sheet', 'sheet'), ('y', -22.5, (4.5, 15, 4))),
        # tintero (cuerpo + cuello) y pluma inclinada
        ((1.5, 15, 10.5), (4, 17, 13), P('ink'), None), ((2.25, 17, 11.25), (3.25, 17.6, 12.25), P('cork'), None),
        ((2.5, 16.5, 11.7), (3, 23, 11.8), P('feather'), ('z', 22.5, (2.75, 17, 11.75))),
        ((2.4, 16.5, 11.75), (3.1, 23, 11.75), P('feather'), ('z', 22.5, (2.75, 17, 11.75))),
        # cristal de amatista
        ((26.5, 15, 10.5), (28.5, 19, 12.5), '#gem', ('y', 45, (27.5, 15, 11.5))),
        ((25, 15, 12), (26.5, 17, 13.5), '#gem', ('x', 22.5, (25.75, 15, 12.75))),
        # vela con mecha (la llama son particulas)
        ((29, 15, 3), (30.5, 19, 4.5), P('candle'), None), ((29.6, 19, 3.6), (29.9, 19.8, 3.9), P('wick'), None),
        # pergamino enrollado con cinta
        ((18.5, 15, 3), (22.5, 16, 4), P('roll'), None), ((20.2, 14.95, 2.95), (20.8, 16.05, 4.05), P('ribbon'), None),
        # frascos chicos con corcho
        ((22, 15, 9), (23.5, 17.2, 10.5), P('blue'), None), ((22.35, 17.2, 9.35), (23.15, 17.8, 10.15), P('cork'), None),
        ((24, 15, 9.8), (25.2, 16.6, 11), P('red'), None), ((24.25, 16.6, 10.05), (24.95, 17.1, 10.75), P('cork'), None),
    ]
    def mk(ff, tt, tx, r):
        if tx == 'drawer':
            e = el(ff, tt, '#plank', r)
            e['faces']['north'] = dict(texture='#drawer')
            return e
        if isinstance(tx, tuple):
            _, reg, top = tx
            e = el(ff, tt, '#p', r)
            for k, f in e['faces'].items():
                f['uv'] = list(top if (top and k in ('up', 'down')) else reg)
            return e
        return el(ff, tt, tx, r)
    for part, x0 in (('side', 0), ('main', 16)):
        els = []
        for f, t, tx, rot in desk:
            lo, hi = max(f[0], x0), min(t[0], x0 + 16)
            if hi <= lo:
                continue
            ff, tt = (lo - x0, f[1], f[2]), (hi - x0, t[1], t[2])
            r = None
            if rot:
                r = (rot[0], rot[1], (rot[2][0] - x0, rot[2][1], rot[2][2]))
            els.append(mk(ff, tt, tx, r))
        jdump(dict(parent='block/block', textures=tex, elements=els), A, 'models', 'block', 'research_table_' + part + '.json')
    v = {}
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for side in ('false', 'true'):
            d = dict(model='remushoku:block/research_table_' + ('side' if side == 'true' else 'main'))
            if y: d['y'] = y
            v['facing=%s,side=%s' % (f, side)] = d
    jdump(dict(variants=v), A, 'blockstates', 'research_table.json')
    jdump(dict(parent='remushoku:block/research_table_main'), A, 'models', 'item', 'research_table.json')
    # frente de los cajones
    img = Image.new('RGBA', (16, 16), (66, 44, 26, 255))
    for y in range(16):
        for x in range(16):
            c = (86, 58, 34) if (y % 5 in (1, 2, 3) and 1 <= x <= 14) else (52, 34, 20)
            if y % 5 == 2 and x in (7, 8):
                c = (200, 170, 90)
            img.putpixel((x, y), c + (255,))
    save(img, 'block', 'research_drawer.png')


def dispenser_models():
    jdump(dict(parent='minecraft:block/orientable', textures=dict(top='remushoku:block/stone_dispenser_top',
               front='remushoku:block/stone_dispenser_front', side='remushoku:block/stone_dispenser_side')),
          A, 'models', 'block', 'stone_dispenser.json')
    jdump(dict(parent='minecraft:block/orientable_vertical', textures=dict(front='remushoku:block/stone_dispenser_front_vertical',
               side='remushoku:block/stone_dispenser_top')), A, 'models', 'block', 'stone_dispenser_vertical.json')
    v = {}
    for f, x, y, m in (('down', 180, 0, 'stone_dispenser_vertical'), ('up', 0, 0, 'stone_dispenser_vertical'),
                       ('north', 0, 0, 'stone_dispenser'), ('east', 0, 90, 'stone_dispenser'),
                       ('south', 0, 180, 'stone_dispenser'), ('west', 0, 270, 'stone_dispenser')):
        d = dict(model='remushoku:block/' + m)
        if x: d['x'] = x
        if y: d['y'] = y
        v['facing=%s,triggered=false' % f] = d
        v['facing=%s,triggered=true' % f] = d
    jdump(dict(variants=v), A, 'blockstates', 'stone_dispenser.json')
    jdump(dict(parent='remushoku:block/stone_dispenser'), A, 'models', 'item', 'stone_dispenser.json')


def data():
    for b in ('stone_dispenser', 'research_table'):
        jdump({'type': 'minecraft:block', 'random_sequence': 'remushoku:blocks/' + b, 'pools': [{'rolls': 1.0,
               'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': 'remushoku:' + b}]}]},
              D, 'loot_table', 'blocks', b + '.json')
    # teclado numerico: 7 8 9 lapislazuli / 4 y 6 cobre, 5 piedra de cualquier rango / 1 y 3 cobre, 2 amatista
    jdump({'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['LLL', 'CSC', 'CAC'],
           'key': {'L': {'item': 'minecraft:lapis_lazuli'}, 'C': {'item': 'minecraft:copper_ingot'},
                   'S': {'tag': 'remushoku:magic_stone'}, 'A': {'item': 'minecraft:amethyst_shard'}},
           'result': {'count': 1, 'id': 'remushoku:stone_dispenser'}}, D, 'recipe', 'stone_dispenser.json')
    jdump({'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['BKB', 'PPP', 'P P'],
           'key': {'B': {'item': 'minecraft:book'}, 'K': {'item': 'minecraft:amethyst_shard'}, 'P': {'tag': 'minecraft:planks'}},
           'result': {'count': 1, 'id': 'remushoku:research_table'}}, D, 'recipe', 'research_table.json')


def alembic():
    tex = dict(brick='minecraft:block/bricks', copper='minecraft:block/copper_block', cut='minecraft:block/cut_copper',
               patina='minecraft:block/exposed_copper', glass='minecraft:block/glass', liquid='minecraft:block/light_blue_concrete',
               dark='minecraft:block/black_concrete', particle='minecraft:block/copper_block')
    els = [
        el((2, 0, 2), (14, 5, 14), '#brick'),
        el((5, 1, 1.9), (10, 4, 2.1), '#fire'),
        el((2.5, 5, 4), (10.5, 12, 12), '#copper'),
        el((2, 7.5, 3.5), (11, 8.5, 12.5), '#cut'),
        el((4, 12, 5.5), (9, 14, 10.5), '#copper'),
        el((5.5, 14, 7), (7.5, 15.5, 9), '#patina'),
        el((6.5, 14.5, 7.5), (12.5, 15.5, 8.5), '#patina'),
        el((11.5, 9, 7.5), (12.5, 15.5, 8.5), '#patina'),
        el((10.5, 5, 5.5), (14, 9, 9), '#glass'),
        el((11, 5, 6), (13.5, 7.2, 8.5), '#liquid'),
        el((11.5, 9, 6.5), (13, 10, 8), '#glass'),
    ]
    for name, fire in (('alembic', '#dark'), ('alembic_lit', 'minecraft:block/magma')):
        t = dict(tex)
        t['fire'] = 'minecraft:block/black_concrete' if fire == '#dark' else 'minecraft:block/magma'
        jdump(dict(parent='block/block', textures=t, elements=els), A, 'models', 'block', name + '.json')
    v = {}
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for lit in ('false', 'true'):
            d = dict(model='remushoku:block/alembic' + ('_lit' if lit == 'true' else ''))
            if y: d['y'] = y
            v['facing=%s,lit=%s' % (f, lit)] = d
    jdump(dict(variants=v), A, 'blockstates', 'alembic.json')
    jdump(dict(parent='remushoku:block/alembic'), A, 'models', 'item', 'alembic.json')
    jdump({'type': 'minecraft:block', 'random_sequence': 'remushoku:blocks/alembic', 'pools': [{'rolls': 1.0,
           'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': 'remushoku:alembic'}]}]},
          D, 'loot_table', 'blocks', 'alembic.json')
    jdump({'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' G ', 'CBC', 'KKK'],
           'key': {'G': {'item': 'minecraft:glass_bottle'}, 'C': {'item': 'minecraft:copper_ingot'}, 'B': {'item': 'minecraft:copper_block'},
                   'K': {'item': 'minecraft:bricks'}},
           'result': {'count': 1, 'id': 'remushoku:alembic'}}, D, 'recipe', 'alembic.json')
    # pociones de mana estilo Terraria: frasco con contorno oscuro, liquido brillante en degrade, menisco claro, reflejo
    # blanco y una estrellita de mana adentro. Chica = frasco redondo chico, normal = redondo grande, grande = erlenmeyer.
    def flask(kind):
        inside = set()
        if kind == 'erlen':
            for y in range(6, 15):
                hw = 1 + (y - 6) * 0.72
                for x in range(16):
                    if abs(x + 0.5 - 8) <= hw:
                        inside.add((x, y))
            neck = (7, 3, 6)
        else:
            cy, r = (10.5, 3.7) if kind == 'small' else (10, 4.75)
            for y in range(16):
                for x in range(16):
                    if (x + 0.5 - 8) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                        inside.add((x, y))
            top = min(y for _, y in inside)
            neck = (7, (5 if kind == 'small' else 3), top)
        nx, ny0, ny1 = neck
        for y in range(ny0, ny1 + 1):
            inside.add((nx, y)); inside.add((nx + 1, y))
        return inside, ny0

    for name, kind, liq, lvl in (('mana_potion_small', 'small', (70, 140, 255), 0.2), ('mana_potion', 'round', (40, 90, 245), 0.2),
                                 ('mana_potion_large', 'erlen', (120, 60, 240), 0.22)):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        inside, ny0 = flask(kind)
        ys = [y for _, y in inside if y > ny0 + 2]
        y_top, y_bot = min(ys), max(ys)
        level = int(y_top + (y_bot - y_top) * lvl)
        dark = (22, 24, 58, 255)
        for (x, y) in inside:
            if y < level:
                c = (200, 224, 250, 150) if y > ny0 + 1 else (186, 212, 240, 170)
            else:
                t = (y - level) / max(1, y_bot - level)
                k = 1.25 - 0.55 * t - 0.03 * (x - 8)
                c = tuple(max(0, min(255, int(v * k))) for v in liq) + (255,)
                if y == level:
                    c = tuple(min(255, int(v * 0.5 + 140)) for v in liq) + (255,)
            img.putpixel((x, y), c)
        for (x, y) in list(inside):  # contorno: todo vecino de afuera
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if q not in inside and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    img.putpixel(q, dark)
        # corcho arriba del cuello
        for x in (6, 7, 8, 9):
            img.putpixel((x, ny0 - 1), (96, 58, 30, 255) if x in (6, 9) else (168, 112, 64, 255))
            img.putpixel((x, ny0 - 2), dark)
        img.putpixel((5, ny0 - 1), dark); img.putpixel((10, ny0 - 1), dark)
        # reflejo blanco a la izquierda y estrellita de mana
        xs = sorted(x for x, y in inside if y == level + 1)
        hx = xs[0] + 1 if xs else 6
        for y in range(level - 1, min(y_bot, level + 3)):
            if (hx, y) in inside:
                img.putpixel((hx, y), (255, 255, 255, 235))
        sx, sy = 9, min(y_bot - 1, level + 2 + (1 if kind != 'small' else 0))
        for (dx, dy) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            if (sx + dx, sy + dy) in inside:
                img.putpixel((sx + dx, sy + dy), (255, 250, 200, 255) if (dx, dy) == (0, 0) else (210, 230, 255, 255))
        save(img, 'item', name + '.png')
        jdump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + name}}, A, 'models', 'item', name + '.json')


alembic()
dispenser_textures()
book_texture()
dispenser_models()
research_table()
data()
print('estaciones listas')

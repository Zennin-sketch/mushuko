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


def research_table():
    """Escritorio de dos bloques. Coordenadas del escritorio: x 0..32 (0..16 = mitad de al lado, 16..32 = la principal),
    el frente (donde se para el jugador) es -Z. Cada elemento va a la mitad que lo contiene."""
    tex = dict(plank='minecraft:block/dark_oak_planks', log='minecraft:block/dark_oak_log', book='remushoku:block/research_book',
               gem='minecraft:block/amethyst_block', wax='minecraft:block/white_wool', ink='minecraft:block/black_concrete',
               flame='minecraft:block/shroomlight', drawer='remushoku:block/research_drawer', paper='minecraft:block/birch_planks',
               glass='minecraft:block/light_blue_stained_glass', red='minecraft:block/red_wool', particle='minecraft:block/dark_oak_planks')
    desk = [
        ((0, 12, 1), (32, 15, 15), '#plank', None),          # tablero
        ((1, 0, 2), (3, 12, 4), '#log', None), ((1, 0, 12), (3, 12, 14), '#log', None),
        ((29, 0, 2), (31, 12, 4), '#log', None), ((29, 0, 12), (31, 12, 14), '#log', None),
        ((2, 3, 3), (16, 4, 13), '#plank', None),            # estante bajo (lado)
        ((17, 2, 2), (31, 11, 14), '#drawer', None),         # cajonera (principal)
        # libro abierto
        ((5, 15, 4), (10, 15.6, 11), '#book', ('z', 22.5, (10, 15, 8))),
        ((10, 15, 4), (15, 15.6, 11), '#book', ('z', -22.5, (10, 15, 8))),
        # tintero y pluma
        ((2.5, 15, 11), (4.5, 16.8, 13), '#ink', None),
        ((3.2, 15.5, 11.6), (3.8, 21, 12.2), '#wax', ('z', 22.5, (3.5, 16, 12))),
        # cristal de amatista
        ((26.5, 15, 10.5), (29, 19.5, 13), '#gem', ('y', 45, (27.75, 15, 11.75))),
        ((25, 15, 12), (26.5, 17.5, 13.5), '#gem', ('x', 22.5, (25.75, 15, 12.75))),
        # vela
        ((29, 15, 3), (30.5, 18, 4.5), '#wax', None), ((29.5, 18, 3.5), (30, 19, 4), '#flame', None),
        # pergamino enrollado y frascos
        ((18, 15, 3), (24, 16.2, 4.4), '#paper', None),
        ((20.5, 15, 9), (22.5, 17.5, 11), '#glass', None), ((23, 15, 10), (24.5, 16.8, 11.5), '#red', None),
    ]
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
            els.append(el(ff, tt, tx, r))
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
    # pociones de mana: botella con liquido azul que brilla (chica / normal / grande)
    for name, size, liq in (('mana_potion_small', 0, (90, 150, 255)), ('mana_potion', 1, (60, 110, 240)), ('mana_potion_large', 2, (110, 70, 230))):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        w = (4, 6, 8)[size]
        x0, top = 8 - w // 2, (7, 6, 5)[size]
        glass, edge, cork = (210, 230, 255, 255), (120, 140, 170, 255), (150, 100, 60, 255)
        for y in range(top, 15):
            for x in range(x0 - 1, x0 + w + 1):
                inside = x0 <= x < x0 + w and y < 14
                if not inside:
                    img.putpixel((x, y), edge)
                elif y >= top + 2:
                    k = 1.0 - (y - top) * 0.04
                    c = tuple(max(0, min(255, int(v * k))) for v in liq)
                    img.putpixel((x, y), c + (255,))
                else:
                    img.putpixel((x, y), glass)
        for y in range(top - 3, top):
            img.putpixel((7, y), edge); img.putpixel((8, y), glass if y > top - 3 else cork); img.putpixel((9, y), edge)
        img.putpixel((8, top - 4), cork); img.putpixel((7, top - 4), cork)
        img.putpixel((x0, top + 3), (255, 255, 255, 255)); img.putpixel((x0, top + 4), (230, 240, 255, 255))
        for (sx, sy) in ((x0 + w - 2, 12), (x0 + 1, 11)):
            img.putpixel((sx, sy), (200, 240, 255, 255))
        save(img, 'item', name + '.png')
        jdump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + name}}, A, 'models', 'item', name + '.json')


alembic()
dispenser_textures()
book_texture()
dispenser_models()
research_table()
data()
print('estaciones listas')

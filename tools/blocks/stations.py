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
    tex = dict(plank='minecraft:block/dark_oak_planks', log='minecraft:block/dark_oak_log', book='remushoku:block/research_book',
               gem='minecraft:block/amethyst_block', wax='minecraft:block/white_wool', ink='minecraft:block/black_concrete',
               flame='minecraft:block/shroomlight', particle='minecraft:block/dark_oak_planks')
    els = [
        el((0, 12, 0), (16, 15, 16), '#plank'),
        el((1, 0, 1), (3, 12, 3), '#log'), el((13, 0, 1), (15, 12, 3), '#log'),
        el((1, 0, 13), (3, 12, 15), '#log'), el((13, 0, 13), (15, 12, 15), '#log'),
        el((2, 4, 2), (14, 5, 14), '#plank'),
        el((3, 15, 5), (8, 15.6, 11), '#book', ('z', 22.5, (8, 15, 8))),
        el((8, 15, 5), (13, 15.6, 11), '#book', ('z', -22.5, (8, 15, 8))),
        el((11.5, 15, 11.5), (14, 19, 14), '#gem', ('y', 45, (12.75, 15, 12.75))),
        el((10.5, 15, 12.5), (12, 17, 14), '#gem', ('x', 22.5, (11, 15, 13))),
        el((2, 15, 12), (3.5, 18, 13.5), '#wax'),
        el((2.5, 18, 12.5), (3, 19, 13), '#flame'),
        el((2.5, 15, 2.5), (4.5, 16.5, 4.5), '#ink'),
        el((5, 5, 3), (11, 9, 6), '#plank'),  # cajon con libros
    ]
    jdump(dict(parent='block/block', textures=tex, elements=els), A, 'models', 'block', 'research_table.json')
    jdump(dict(variants={'facing=%s' % f: dict(model='remushoku:block/research_table', y=y) for f, y in
                         (('north', 0), ('east', 90), ('south', 180), ('west', 270))}), A, 'blockstates', 'research_table.json')
    jdump(dict(parent='remushoku:block/research_table'), A, 'models', 'item', 'research_table.json')


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


dispenser_textures()
book_texture()
dispenser_models()
research_table()
data()
print('estaciones listas')

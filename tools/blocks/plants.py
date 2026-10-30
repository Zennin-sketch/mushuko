"""Flor de Mana: texturas de las 4 etapas del cultivo, semillas y petalo; modelos, blockstate, loot y semillas en el pasto."""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources'))
A = os.path.join(RES, 'assets', 'remushoku')
D = os.path.join(RES, 'data', 'remushoku')


def save(img, *p):
    path = os.path.join(A, 'textures', *p); os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)


def jdump(obj, *p):
    path = os.path.join(*p); os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f: json.dump(obj, f, indent=2)


STEM = [(38, 92, 86), (52, 120, 108), (70, 146, 128)]
LEAF = [(40, 110, 104), (58, 140, 126), (84, 172, 150), (120, 200, 176)]
PET = [(70, 120, 235), (96, 160, 250), (140, 200, 255), (200, 236, 255)]


def stage(age):
    """Estilo plantorcha: un solo tallo recto con hojas anchas abajo y, madura, una flor en forma de llama azul arriba."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    P = lambda x, y, c: 0 <= x < 16 and 0 <= y < 16 and img.putpixel((x, y), tuple(c) + (255,))
    # hojas de la base (como las de la plantorcha, abiertas hacia los costados)
    leaves = {0: [(6, 15), (9, 15), (5, 14), (10, 14)],
              1: [(6, 15), (9, 15), (5, 14), (10, 14), (4, 13), (11, 13), (6, 13), (9, 13)],
              2: [(6, 15), (9, 15), (5, 14), (10, 14), (4, 13), (11, 13), (3, 12), (12, 12), (6, 12), (9, 12), (5, 11), (10, 11)],
              3: [(6, 15), (9, 15), (5, 14), (10, 14), (4, 13), (11, 13), (3, 12), (12, 12), (6, 12), (9, 12), (5, 11), (10, 11),
                  (2, 11), (13, 11), (6, 9), (9, 9), (5, 8), (10, 8)]}[age]
    for i, (x, y) in enumerate(leaves):
        P(x, y, LEAF[(i + y) % 3])
    top = [13, 10, 6, 7][age]
    for y in range(15, top - 1, -1):          # tallo
        P(7, y, STEM[y % 3]); P(8, y, STEM[(y + 1) % 3])
    if age == 1:
        P(7, top - 1, LEAF[2]); P(8, top - 1, LEAF[1])
    if age == 2:                               # capullo cerrado (gota azul)
        for (x, y, c) in ((7, 5, PET[0]), (8, 5, PET[1]), (7, 4, PET[1]), (8, 4, PET[0]), (7, 3, PET[2]), (8, 3, PET[1]),
                          (6, 5, LEAF[1]), (9, 5, LEAF[1]), (7, 2, PET[2])):
            P(x, y, c)
    if age == 3:                               # flor como llama: petalos que suben y se abren, centro claro
        flame = ['...b..c...',
                 '..bc.bcb..',
                 '..bccdcb..',
                 '.abcddcba.',
                 '.abcddcba.',
                 '..abccba..',
                 '...abba...',
                 '....ll....']
        pal = {'a': PET[0], 'b': PET[1], 'c': PET[2], 'd': PET[3], 'l': LEAF[1]}
        for yy, row in enumerate(flame):
            for xx, ch in enumerate(row):
                if ch in pal: P(xx + 3, yy, pal[ch])
    return img


def seeds():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in ((5, 6), (9, 5), (7, 9), (10, 10), (4, 11)):
        for dx, dy, c in ((0, 0, (60, 140, 160)), (1, 0, (90, 180, 200)), (0, 1, (40, 100, 120)), (1, 1, (60, 140, 160))):
            img.putpixel((x + dx, y + dy), c + (255,))
        img.putpixel((x + 1, y), (170, 230, 255, 255))
    return img


def petal():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    out = (40, 70, 170)
    shape = ['.....##.......',
             '....#aa#......',
             '...#abba#.....',
             '..#abbcba#....',
             '..#abccba#....',
             '.#abccdcba#...',
             '.#abcddcba#...',
             '.#abccccba#...',
             '..#abbcba#....',
             '...#aaaa#.....',
             '....#ss#......',
             '.....ss.......']
    pal = {'#': out, 'a': PET[0], 'b': PET[1], 'c': PET[2], 'd': PET[3], 's': STEM[1]}
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch in pal: img.putpixel((x + 1, y + 2), pal[ch] + (255,))
    return img


for a in range(4):
    save(stage(a), 'block', 'mana_bloom_stage%d.png' % a)
    jdump({'parent': 'minecraft:block/crop', 'textures': {'crop': 'remushoku:block/mana_bloom_stage%d' % a}},
          A, 'models', 'block', 'mana_bloom_stage%d.json' % a)
jdump({'variants': {'age=%d' % a: {'model': 'remushoku:block/mana_bloom_stage%d' % a} for a in range(4)}}, A, 'blockstates', 'mana_bloom.json')
save(seeds(), 'item', 'mana_bloom_seeds.png'); save(petal(), 'item', 'mana_petal.png')
for n in ('mana_bloom_seeds', 'mana_petal'):
    jdump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:item/' + n}}, A, 'models', 'item', n + '.json')
mature = {'condition': 'minecraft:block_state_property', 'block': 'remushoku:mana_bloom', 'properties': {'age': '3'}}
jdump({'type': 'minecraft:block', 'random_sequence': 'remushoku:blocks/mana_bloom', 'pools': [
    {'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'name': 'remushoku:mana_petal', 'conditions': [mature],
         'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}]},
        {'type': 'minecraft:item', 'name': 'remushoku:mana_bloom_seeds'}]}]},
    {'rolls': 1, 'conditions': [mature], 'entries': [{'type': 'minecraft:item', 'name': 'remushoku:mana_bloom_seeds',
        'functions': [{'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:binomial_with_bonus_count',
                       'parameters': {'extra': 2, 'probability': 0.5714286}}]}]}],
       'functions': [{'function': 'minecraft:explosion_decay'}]}, D, 'loot_table', 'blocks', 'mana_bloom.json')
# 3 petalos = 1 polvo? no: los petalos van directo al alambique. Semillas: tambien desde 1 petalo en la mesa de crafteo
jdump({'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': 'remushoku:mana_petal'}],
       'result': {'count': 2, 'id': 'remushoku:mana_bloom_seeds'}}, D, 'recipe', 'mana_bloom_seeds.json')
# flor silvestre (se genera en el mundo): misma imagen que la madura, modelo en cruz
jdump({'parent': 'minecraft:block/cross', 'textures': {'cross': 'remushoku:block/mana_bloom_stage3'}}, A, 'models', 'block', 'wild_mana_bloom.json')
jdump({'variants': {'': {'model': 'remushoku:block/wild_mana_bloom'}}}, A, 'blockstates', 'wild_mana_bloom.json')
jdump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'remushoku:block/mana_bloom_stage3'}}, A, 'models', 'item', 'wild_mana_bloom.json')
jdump({'type': 'minecraft:block', 'random_sequence': 'remushoku:blocks/wild_mana_bloom', 'pools': [
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'remushoku:mana_petal',
        'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]},
    {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'remushoku:mana_bloom_seeds',
        'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]}],
       'functions': [{'function': 'minecraft:explosion_decay'}]}, D, 'loot_table', 'blocks', 'wild_mana_bloom.json')
jdump({'type': 'minecraft:flower', 'config': {'tries': 20, 'xz_spread': 5, 'y_spread': 2, 'feature': {
    'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:simple_state_provider',
                                                                         'state': {'Name': 'remushoku:wild_mana_bloom'}}}},
    'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}}]}}},
      D, 'worldgen', 'configured_feature', 'wild_mana_bloom.json')
jdump({'feature': 'remushoku:wild_mana_bloom', 'placement': [{'type': 'minecraft:rarity_filter', 'chance': 10}, {'type': 'minecraft:in_square'},
       {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}]},
      D, 'worldgen', 'placed_feature', 'wild_mana_bloom.json')
print('flor de mana lista')

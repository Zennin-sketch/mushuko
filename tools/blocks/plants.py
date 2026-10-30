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
    rnd = random.Random(age * 13 + 5)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    P = lambda x, y, c: 0 <= x < 16 and 0 <= y < 16 and img.putpixel((x, y), tuple(c) + (255,))
    h = [5, 8, 10, 11][age]
    stems = [(5, 0), (10, 1)] if age < 2 else [(3, 0), (7, 1), (11, 0), (13, 2)]
    for sx, off in stems:
        top = 15 - h + off + rnd.randint(0, 1)
        x = sx
        for y in range(15, top - 1, -1):
            P(x, y, STEM[(y + sx) % 3])
            if y % 3 == 0 and rnd.random() < 0.5: x += rnd.choice((-1, 1)); x = max(1, min(14, x))
            # hojas a los costados
            if y < 14 and (y + sx) % 3 == 0:
                d = 1 if (y // 3) % 2 else -1
                P(x + d, y, LEAF[1]); P(x + 2 * d, y - 1, LEAF[2]); P(x + d, y - 1, LEAF[0])
                if age >= 2: P(x + 3 * d, y - 2, LEAF[3])
        if age == 2:  # capullos cerrados
            P(x, top - 1, PET[0]); P(x, top - 2, PET[1])
        if age == 3:  # flor abierta: cinco petalos y centro claro que brilla
            cx, cy = x, top - 2
            for dx, dy in ((0, -2), (-2, 0), (2, 0), (-1, 2), (1, 2), (0, -1), (-1, 0), (1, 0), (0, 1), (-1, -1), (1, -1), (-1, 1), (1, 1)):
                P(cx + dx, cy + dy, PET[1] if abs(dx) + abs(dy) > 1 else PET[2])
            P(cx, cy, PET[3])
            P(cx - 2, cy - 1, PET[0]); P(cx + 2, cy + 1, PET[0])
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
print('flor de mana lista')

"""Que arma usa que combo. Fuente unica para los weapon_attributes de Better Combat y para la pagina de preview."""
import json, os, sys

def atk(anim, hitbox, dmg, angle, sound, upswing=0.5):
    return dict(anim=anim, hitbox=hitbox, damage=dmg, angle=angle, sound=sound, upswing=upswing)

NS = 'mushoku_combat'

FAMILIES = {
    'sword': dict(cooldown=12.5, slot='hip', label='Espada', two_handed=False, range_bonus=0.0, pose=None, attacks=[
        atk('sword_slash_1', 'HORIZONTAL_PLANE', 0.9, 140, 'bettercombat:sword_slash'),
        atk('sword_slash_2', 'HORIZONTAL_PLANE', 1.0, 140, 'bettercombat:sword_slash'),
        atk('sword_thrust_3', 'FORWARD_BOX', 1.35, 0, 'bettercombat:rapier_stab'),
    ]),
    'katana': dict(cooldown=12.5, slot='hip', label='Katana', two_handed=True, range_bonus=0.25, pose='bettercombat:pose_two_handed_katana', attacks=[
        atk('katana_iai', 'HORIZONTAL_PLANE', 0.95, 150, 'bettercombat:katana_slash'),
        atk('katana_overhead', 'VERTICAL_PLANE', 1.1, 90, 'bettercombat:katana_slash'),
        atk('katana_whirlwind', 'HORIZONTAL_PLANE', 1.3, 360, 'bettercombat:katana_slash'),
    ]),
    'heavy': dict(cooldown=20, slot='back', label='Mandoble', two_handed=True, range_bonus=0.5, pose='bettercombat:pose_two_handed_heavy', attacks=[
        atk('heavy_sweep', 'HORIZONTAL_PLANE', 1.0, 160, 'bettercombat:claymore_swing'),
        atk('heavy_rise', 'VERTICAL_PLANE', 1.05, 120, 'bettercombat:claymore_swing'),
        atk('heavy_slam', 'VERTICAL_PLANE', 1.45, 120, 'bettercombat:claymore_slam'),
    ]),
    'axe': dict(cooldown=20, slot='back', label='Hacha de guerra', two_handed=True, range_bonus=0.25, pose='bettercombat:pose_two_handed_heavy', attacks=[
        atk('heavy_rise', 'VERTICAL_PLANE', 1.0, 120, 'bettercombat:double_axe_swing'),
        atk('heavy_sweep', 'HORIZONTAL_PLANE', 1.0, 160, 'bettercombat:double_axe_swing'),
        atk('heavy_slam', 'VERTICAL_PLANE', 1.5, 120, 'bettercombat:hammer_slam'),
    ]),
    'scythe': dict(cooldown=16.7, slot='back', label='Guadana', two_handed=True, range_bonus=0.75, pose='bettercombat:pose_two_handed_scythe', attacks=[
        atk('scythe_reap', 'HORIZONTAL_PLANE', 1.0, 170, 'bettercombat:scythe_slash'),
        atk('scythe_backswing', 'HORIZONTAL_PLANE', 1.0, 170, 'bettercombat:scythe_slash'),
        atk('scythe_cyclone', 'HORIZONTAL_PLANE', 1.3, 360, 'bettercombat:scythe_slash'),
    ]),
    'spear': dict(cooldown=15.4, slot='back', label='Lanza', two_handed=True, range_bonus=1.0, pose=NS + ':pose_spear', attacks=[
        atk('spear_thrust', 'FORWARD_BOX', 0.9, 0, 'bettercombat:spear_stab'),
        atk('spear_sweep', 'HORIZONTAL_PLANE', 0.85, 160, 'bettercombat:glaive_slash_quick'),
        atk('spear_dragon_pierce', 'FORWARD_BOX', 1.5, 0, 'bettercombat:spear_stab'),
    ]),
    'staff': dict(cooldown=15.4, slot='back', label='Baston magico', two_handed=True, range_bonus=0.25, pose=NS + ':pose_staff', attacks=[
        atk('staff_jab', 'FORWARD_BOX', 0.9, 0, 'bettercombat:staff_stab'),
        atk('staff_crack', 'VERTICAL_PLANE', 1.1, 120, 'bettercombat:staff_slam'),
    ]),
    'rod': dict(cooldown=12.5, slot='hip', label='Maza', two_handed=False, range_bonus=0.0, pose=None, attacks=[
        atk('sword_slash_1', 'HORIZONTAL_PLANE', 1.0, 120, 'bettercombat:mace_slash'),
        atk('sword_slash_2', 'HORIZONTAL_PLANE', 1.0, 120, 'bettercombat:mace_slash'),
        atk('staff_crack', 'VERTICAL_PLANE', 1.3, 90, 'bettercombat:mace_slam'),
    ]),
}

WEAPONS = {
    'migurd_sword': dict(label='Espada Migurd', family='sword'),
    'pauls_sword': dict(label='Espada de Paul', family='sword'),
    'almanfi_sword': dict(label='Espada Almanfi', family='sword'),
    'hiramune': dict(label='Hiramune', family='sword'),
    'ice_blade': dict(label='Hoja de Hielo', family='sword'),
    'phoenix_dragon_sword': dict(label='Espada Dragon Fenix', family='sword'),
    'stone_blade': dict(label='Hoja de Piedra', family='sword'),
    'katana': dict(label='Katana', family='katana'),
    'kajakut': dict(label='Kajakut', family='heavy'),
    'war_axe': dict(label='Hacha de Guerra', family='axe'),
    'scythe': dict(label='Guadana', family='scythe'),
    'superd_spear': dict(label='Lanza Superd', family='spear', geo='remushoku:geo/ruijerdspear.geo.json',
                         texture='remushoku:textures/item/addon_ruijerd_spear.png'),
    'aqua_heartia': dict(label='Aqua Heartia', family='staff'),
    'dragon_staff': dict(label='Baculo Dragon', family='staff'),
    'staff_of_areos': dict(label='Baculo de Areos', family='staff'),
    'migurd_staff': dict(label='Baculo Migurd', family='staff'),
    'wand': dict(label='Varita', family='rod'),
    'metal_rod': dict(label='Varilla de Metal', family='rod'),
}

# Combo propio de cada arma (ver weapon_combos.py). b = cuanto cuerpo pone en cada golpe segun tamano y peso.
# moves = golpes de la biblioteca en orden (el ultimo es el remate); dmg = multiplicador de dano de cada golpe.
COMBOS = {
    'migurd_sword':  dict(b=1.0,  hand='one', moves=['sword_slash_1', 'sword_slash_2', 'sword_thrust_3'], dmg=[0.9, 1.0, 1.35]),
    'pauls_sword':   dict(b=1.15, hand='one', moves=['katana_overhead', 'sword_slash_2', 'katana_whirlwind'], dmg=[1.0, 1.0, 1.4]),
    'almanfi_sword': dict(b=0.85, hand='one', moves=['sword_slash_2', 'sword_slash_1', 'sword_thrust_3'], dmg=[0.85, 0.95, 1.3]),
    'hiramune':      dict(b=1.05, hand='one', moves=['katana_iai', 'sword_slash_1', 'sword_thrust_3'], dmg=[1.0, 0.95, 1.35]),
    'ice_blade':     dict(b=0.75, hand='one', moves=['sword_thrust_3', 'sword_slash_1', 'sword_slash_2'], dmg=[0.9, 0.9, 1.2]),
    'phoenix_dragon_sword': dict(b=1.0, hand='one', moves=['sword_slash_1', 'heavy_rise', 'katana_whirlwind'], dmg=[0.95, 1.05, 1.45]),
    'stone_blade':   dict(b=0.8,  hand='one', moves=['sword_slash_1', 'sword_slash_2', 'heavy_slam'], dmg=[0.9, 0.95, 1.3]),
    'katana':        dict(b=1.0,  hand='two', spread=3.5, moves=['katana_iai', 'katana_overhead', 'katana_whirlwind'], dmg=[0.95, 1.1, 1.3]),
    'kajakut':       dict(b=1.3,  hand='two', spread=3.5, moves=['heavy_sweep', 'heavy_rise', 'heavy_slam'], dmg=[1.0, 1.05, 1.5]),
    'war_axe':       dict(b=1.25, hand='two', spread=3.5, moves=['heavy_rise', 'heavy_sweep', 'heavy_slam'], dmg=[1.0, 1.0, 1.5]),
    'scythe':        dict(b=1.2,  hand='two', spread=6, moves=['scythe_reap', 'scythe_backswing', 'scythe_cyclone'], dmg=[1.0, 1.0, 1.3]),
    'superd_spear':  dict(b=1.1,  hand='two', spread=-7, moves=['spear_thrust', 'spear_sweep', 'spear_dragon_pierce'], dmg=[0.9, 0.85, 1.5]),
    'aqua_heartia':  dict(b=1.0,  hand='two', spread=-6, moves=['staff_jab', 'spear_sweep', 'staff_crack'], dmg=[0.9, 0.9, 1.1]),
    'dragon_staff':  dict(b=0.95, hand='two', spread=6, moves=['staff_crack', 'staff_jab'], dmg=[1.1, 0.9]),
    'staff_of_areos': dict(b=1.05, hand='two', spread=-6, moves=['spear_thrust', 'staff_crack'], dmg=[0.9, 1.1]),
    'migurd_staff':  dict(b=1.1,  hand='two', spread=6, moves=['staff_jab', 'scythe_backswing', 'staff_crack'], dmg=[0.9, 1.0, 1.15]),
    'wand':          dict(b=0.7,  hand='one', moves=['sword_thrust_3', 'sword_slash_2'], dmg=[0.9, 1.0]),
    'metal_rod':     dict(b=1.1,  hand='one', moves=['sword_slash_1', 'sword_slash_2', 'staff_crack'], dmg=[1.0, 1.0, 1.3]),
}

# Hitbox, angulo y sonido de cada golpe de la biblioteca (sale de las familias)
def move_info():
    info = {}
    for f in FAMILIES.values():
        for a in f['attacks']:
            info.setdefault(a['anim'], (a['hitbox'], a['angle'], a['sound']))
    return info

def weapon_attributes(wid):
    w = WEAPONS[wid]
    f = FAMILIES[w['family']]
    c = COMBOS[wid]
    info = move_info()
    out = {'attributes': {
        'category': w['family'],
        'two_handed': f['two_handed'],
        'range_bonus': f['range_bonus'],
        'attacks': [{
            'hitbox': info[m][0], 'damage_multiplier': c['dmg'][i], 'angle': info[m][1], 'upswing': 0.5,
            'animation': NS + ':%s_%d' % (wid, i + 1), 'swing_sound': {'id': info[m][2]},
        } for i, m in enumerate(c['moves'])],
    }}
    if f['pose']:
        out['attributes']['pose'] = f['pose']
    return out

# Animaciones que no son ataques (las reproduce el addon) y con que arma se muestran en la preview.
EXTRA = [
    dict(group='Esquivas (espadachin)', items=['roll_forward', 'roll_backward', 'roll_left', 'roll_right'], weapon='migurd_sword'),
    dict(group='Esquivas diagonales (espadachin)', items=['roll_forward_left', 'roll_forward_right', 'roll_backward_left', 'roll_backward_right'], weapon='migurd_sword'),
    dict(group='Esquivas (mago)', items=['dash_forward', 'dash_backward', 'dash_left', 'dash_right'], weapon='aqua_heartia'),
    dict(group='Esquivas diagonales (mago)', items=['dash_forward_left', 'dash_forward_right', 'dash_backward_left', 'dash_backward_right'], weapon='aqua_heartia'),
    dict(group='Ideas para la magia', items=['magic_push', 'magic_gather_throw', 'magic_ground', 'magic_sweep', 'magic_snipe',
                                            'magic_sky', 'magic_spiral', 'magic_barrier'], weapon='aqua_heartia'),
    dict(group='Conjuro', items=['cast_pose', 'cast_release'], weapon='aqua_heartia'),
    dict(group='Desenvainar y guardar (cintura)', items=['hip_draw', 'hip_sheathe'], weapon='migurd_sword'),
    dict(group='Desenvainar y guardar (espalda)', items=['back_draw', 'back_sheathe'], weapon='kajakut'),
    dict(group='Poses', items=['pose_staff', 'pose_spear'], weapon='aqua_heartia'),
    dict(group='Arco del mod (arco en la izquierda)', items=['bow_idle', 'bow_load', 'bow_draw', 'bow_release'], weapon='hunter_bow'),
    dict(group='Arco vanilla', items=['bow_draw_r', 'bow_release_r'], weapon=None),
    dict(group='Habilidades del arquero', items=['archer_power_shot', 'archer_deadeye', 'archer_piercing_ray', 'archer_phantom_arrow',
                                                'archer_triple_shot', 'archer_arrow_rain', 'archer_steel_storm', 'archer_barrage',
                                                'archer_disengage', 'archer_gale_arrow', 'archer_tempest_arrow', 'archer_gale_dance'], weapon='hunter_bow'),
]

def attributes(family):
    f = FAMILIES[family]
    out = {'attributes': {
        'category': family,
        'two_handed': f['two_handed'],
        'range_bonus': f['range_bonus'],
        'attacks': [{
            'hitbox': a['hitbox'], 'damage_multiplier': a['damage'], 'angle': a['angle'], 'upswing': a['upswing'],
            'animation': NS + ':' + a['anim'], 'swing_sound': {'id': a['sound']},
        } for a in f['attacks']],
    }}
    if f['pose']:
        out['attributes']['pose'] = f['pose']
    return out

if __name__ == '__main__':
    # Escribe data/remushoku/weapon_attributes/<arma>.json
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for wid, w in WEAPONS.items():
        with open(os.path.join(out, wid + '.json'), 'w') as fh:
            json.dump(attributes(w['family']), fh, indent=2)
    print(len(WEAPONS), 'armas ->', out)

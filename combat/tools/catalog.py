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

# Animaciones que no son ataques (las reproduce el addon) y con que arma se muestran en la preview.
EXTRA = [
    dict(group='Esquivas (espadachin)', items=['roll_forward', 'roll_backward', 'roll_left', 'roll_right'], weapon='migurd_sword'),
    dict(group='Esquivas (mago)', items=['dash_forward', 'dash_backward', 'dash_left', 'dash_right'], weapon='aqua_heartia'),
    dict(group='Conjuro', items=['cast_pose', 'cast_release'], weapon='aqua_heartia'),
    dict(group='Desenvainar y guardar (cintura)', items=['hip_draw', 'hip_sheathe'], weapon='migurd_sword'),
    dict(group='Desenvainar y guardar (espalda)', items=['back_draw', 'back_sheathe'], weapon='kajakut'),
    dict(group='Poses', items=['pose_staff', 'pose_spear'], weapon='aqua_heartia'),
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

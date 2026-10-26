"""Arma la pagina 'Armeria v2' (y compara la Mk-II original con la v2). Uso: python3 build_armor_preview.py <salida.html>"""
import base64, json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))

def uri(p):
    return 'data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode() if os.path.exists(p) else None

NOTES = {
    'mk2_v2': ('Armadura Mágica Mk-II v2 (ropa de Rudeus)', 'Túnica gris con capucha caída, camisa blanca, cinturón con bolsa, faldones largos, pantalón marrón y botas. El núcleo de maná queda como broche azul.'),
    'flame_cloak_v2': ('Capa Atrapallamas v2', 'Capa marrón con capucha que cubre todo el cuerpo y deja la cara en sombra (tapa orejas y rasgos de raza). El dobladillo brilla como brasas.'),
    'outfit_roxy': ('Ropa de Roxy', 'Sombrero de bruja con banda roja, manto corto, blusa blanca con lazo azul, túnica, medias oscuras y botas.'),
    'outfit_ghislaine': ('Ropa de Ghislaine', 'Parche en el ojo izquierdo, top y taparrabos de cuero negro, hombrera de metal, brazaletes y botas altas.'),
    'outfit_paul': ('Ropa de Paul', 'Túnica verdosa, peto de cuero con remaches, hombrera izquierda, cinturón de espadachín, pantalón gris y botas.'),
    'outfit_eris': ('Ropa de Eris', 'Diadema, blusa blanca con lazo marrón, peto de cuero, brazaletes, shorts marrones y botas.'),
    'curse_helmet_v2': ('Casco Supresor de Maldición v2', 'Yelmo de bronce con visera en T, banda de runas violetas que brilla y cresta dorada.'),
    'sunglasses_v2': ('Gafas de Sol Mágicas v2', 'Lentes azul oscuro con reflejo, marco dorado y patillas negras, en 3D.'),
    'enchanted_shoes_v2': ('Zapatos Encantados v2', 'Botas de cuero azul con puño blanco, alitas de pluma celeste y una franja de runas que brilla.'),
}

def main(out):
    armors = {}
    for name in NOTES:
        if name == 'mk2_original':
            geo, tex, glow = os.path.join(A, 'geo', 'mk2model.geo.json'), os.path.join(A, 'textures', 'item', 'mk2txt2.png'), None
        else:
            geo, tex = os.path.join(A, 'geo', 'armor_v2', name + '.geo.json'), os.path.join(A, 'textures', 'armor_v2', name + '.png')
            glow = os.path.join(A, 'textures', 'armor_v2', name + '_glowmask.png')
        armors[name] = dict(name=NOTES[name][0], note=NOTES[name][1], geo=json.load(open(geo)), texture=uri(tex), glow=uri(glow) if glow else None)
    html = open(os.path.join(HERE, 'preview_template.html'), encoding='utf-8').read().replace('/*__DATA__*/null', json.dumps(dict(armors=armors)))
    open(out, 'w', encoding='utf-8').write(html)
    print('preview ->', out, round(len(html) / 1024), 'KB')

if __name__ == '__main__':
    main(sys.argv[1])

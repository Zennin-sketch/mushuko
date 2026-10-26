"""Arma la pagina 'Armeria v2' (y compara la Mk-II original con la v2). Uso: python3 build_armor_preview.py <salida.html>"""
import base64, json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'remushoku'))

def uri(p):
    return 'data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode() if os.path.exists(p) else None

NOTES = {
    'mk2_v2': ('Armadura Mágica Mk-II v2 (ropa de Rudeus)', 'Abrigo gris largo abierto con forro oscuro, cuello alto, capucha caída, arnés de cuero cruzado, cinturón con bolsas, faldones con abertura, mangas anchas con puño vuelto y el núcleo de maná como broche.'),
    'flame_cloak_v2': ('Capa Atrapallamas v2', 'Capucha con visera y punta caída que deja la cara en sombra (dos brasas por ojos), esclavina con borde dorado, capa en paños con pliegues hasta los tobillos y llamas que brillan en el dobladillo.'),
    'outfit_roxy': ('Ropa de Roxy', 'Sombrero de bruja con ala caída y punta doblada, blusa blanca con volados y mangas abullonadas, corsé con cordones, falda negra acampanada con volado blanco, moño atrás, medias con franjas y botas.'),
    'outfit_ghislaine': ('Ropa de Ghislaine', 'Estola de piel con mechones, vendas de cuero, tahalí cruzado, dos cinturones, taparrabos caqui deshilachado, hombreras de piel, aros de oro, botas altas con borde de piel y parche.'),
    'outfit_paul': ('Ropa de Paul', 'Gambesón acolchado, coraza de cuero con remaches, hombrera de tres placas, tahalí con vaina, escarcelas de cuero, brazaletes, pantalón gris y botas de caña doblada.'),
    'outfit_eris': ('Ropa de Eris', 'Chaqueta granate con cuello y hombros de piel, blusa con cinta, corsé, falda tableada, guanteletes, medias granate, botas altas y diadema con moño.'),
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

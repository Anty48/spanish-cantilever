"""Genera los postes y piezas de poste espanoles y lo que los rodea.

Uso:  python tools/generar_postes.py <carpeta con el jar de PNW extraido> [png del lingote de hierro vanilla]

Son las piezas de PNW (postes de celosia plana, plana diagonal y cuadrada, poste H, soporte de linea
electrica y soporte de mensula con sus bloques vertical y junto a poste) con el metal gris espanol: los
blockstates y modelos son los de PNW con la textura cambiada. Solo hay el estado nuevo (sin encerar y
encerado); al oxidarse pasan al expuesto de PNW (ver ModPostes.java). Los cuatro postes tienen ademas una
version azul (el metal de la alta velocidad, LAV), que no se oxida.

Tambien los remates con aisladores, en azul y en gris (postes planos con una barra arriba de la que cuelgan
uno o dos aisladores para los cables de corriente de PNW): sus modelos son los de Blockbench del usuario en
`new stuff/new json/` (ver REMATES). Genera:

- blockstates, modelos de bloque y de objeto, tablas de botin, etiquetas (las mismas que la pieza de
  PNW equivalente), recetas y nombres;
- con el png del lingote: las texturas iberian_steel_ingot (lingote de hierro oscurecido) y mensula_incompleta
  (la mensula a medio montar de PNW, oscurecida).

Borra antes todo lo que genero (tambien los estados oxidados espanoles que habia antes).
"""
import colorsys
import glob
import json
import os
import sys

from PIL import Image

MOD = 'spanishcantilevers'
PNW = 'pantographsandwires'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', MOD)
DATA = os.path.join(ROOT, 'src', 'main', 'resources', 'data')
METAL_PNW = f'{PNW}:block/metal'
METAL = f'{MOD}:block/spanish_metal'
METAL_AZUL = f'{MOD}:block/blue_lav_spanish_metal'
NUEVO = os.path.join(ROOT, 'new stuff')
# version azul (LAV) de los postes: prefijo del id y carpeta de sus modelos
AZUL = 'blue_'
CARPETA_AZUL = 'lav'
LANGS = ['en_us', 'es_es', 'ca_es', 'de_de']

# pieza de PNW: (tiene objeto, nombres sin encerar, nombres encerado)
PIEZAS = {
    'flat_lattice_mast': (True,
        {'en_us': 'Iberian Flat Lattice Mast', 'es_es': 'Poste de celosía plana ibérico',
         'ca_es': 'Pal de gelosia plana ibèric', 'de_de': 'Iberischer Rahmenflachmast'},
        {'en_us': 'Waxed Iberian Flat Lattice Mast', 'es_es': 'Poste de celosía plana ibérico encerado',
         'ca_es': 'Pal de gelosia plana ibèric encerat', 'de_de': 'Gewachster Iberischer Rahmenflachmast'}),
    'flat_lattice_mast_diagonal': (True,
        {'en_us': 'Iberian Flat Diagonal Lattice Mast', 'es_es': 'Poste de celosía plana diagonal ibérico',
         'ca_es': 'Pal de gelosia plana diagonal ibèric', 'de_de': 'Iberischer Rahmenflachmast (diagonal)'},
        {'en_us': 'Waxed Iberian Flat Diagonal Lattice Mast', 'es_es': 'Poste de celosía plana diagonal ibérico encerado',
         'ca_es': 'Pal de gelosia plana diagonal ibèric encerat', 'de_de': 'Gewachster Iberischer Rahmenflachmast (diagonal)'}),
    'lattice_mast': (True,
        {'en_us': 'Iberian Lattice Mast', 'es_es': 'Poste de celosía ibérico', 'ca_es': 'Pal de gelosia ibèric',
         'de_de': 'Iberischer Gittermast'},
        {'en_us': 'Waxed Iberian Lattice Mast', 'es_es': 'Poste de celosía ibérico encerado',
         'ca_es': 'Pal de gelosia ibèric encerat', 'de_de': 'Gewachster Iberischer Gittermast'}),
    'h_beam_mast': (True,
        {'en_us': 'Iberian H-Beam Mast', 'es_es': 'Poste en H ibérico', 'ca_es': 'Pal en H ibèric',
         'de_de': 'Iberischer Peiner Mast'},
        {'en_us': 'Waxed Iberian H-Beam Mast', 'es_es': 'Poste en H ibérico encerado', 'ca_es': 'Pal en H ibèric encerat',
         'de_de': 'Gewachster Iberischer Peiner Mast'}),
    'power_line_bracket': (True,
        {'en_us': 'Spanish Power Line Bracket', 'es_es': 'Soporte de línea eléctrica español',
         'ca_es': 'Suport de línia elèctrica espanyol', 'de_de': 'Spanische Stromleitungshalterung'},
        {'en_us': 'Waxed Spanish Power Line Bracket', 'es_es': 'Soporte de línea eléctrica español encerado',
         'ca_es': 'Suport de línia elèctrica espanyol encerat', 'de_de': 'Gewachste Spanische Stromleitungshalterung'}),
    'cantilever_bracket': (True,
        {'en_us': 'Spanish Cantilever Bracket', 'es_es': 'Soporte de ménsula español',
         'ca_es': 'Suport de mènsula espanyol', 'de_de': 'Spanischer Querträger'},
        {'en_us': 'Waxed Spanish Cantilever Bracket', 'es_es': 'Soporte de ménsula español encerado',
         'ca_es': 'Suport de mènsula espanyol encerat', 'de_de': 'Gewachster Spanischer Querträger'}),
}
# bloques sin objeto: sueltan el de otra pieza y se llaman como ella
SIN_OBJETO = {'cantilever_bracket_at_post': 'cantilever_bracket', 'cantilever_bracket_vertical': 'cantilever_bracket'}

# postes con version azul (LAV): nombres. Los azules no se oxidan (la pintura de la alta velocidad los
# protege), asi que no tienen version encerada.
AZULES = {
    'flat_lattice_mast': {'en_us': 'Blue Iberian Flat Lattice Mast', 'es_es': 'Poste de celosía plana ibérico azul',
                          'ca_es': 'Pal de gelosia plana ibèric blau', 'de_de': 'Blauer Iberischer Rahmenflachmast'},
    'flat_lattice_mast_diagonal': {'en_us': 'Blue Iberian Flat Diagonal Lattice Mast',
                                   'es_es': 'Poste de celosía plana diagonal ibérico azul',
                                   'ca_es': 'Pal de gelosia plana diagonal ibèric blau',
                                   'de_de': 'Blauer Iberischer Rahmenflachmast (diagonal)'},
    'lattice_mast': {'en_us': 'Blue Iberian Lattice Mast', 'es_es': 'Poste de celosía ibérico azul',
                     'ca_es': 'Pal de gelosia ibèric blau', 'de_de': 'Blauer Iberischer Gittermast'},
    'h_beam_mast': {'en_us': 'Blue Iberian H-Beam Mast', 'es_es': 'Poste en H ibérico azul', 'ca_es': 'Pal en H ibèric blau',
                    'de_de': 'Blauer Iberischer Peiner Mast'},
}
# el azul de los postes de la alta velocidad (tono en grados, saturacion, brillo medio): la textura del
# usuario era azul puro; se le deja su dibujo con este color (sacado de LAV_reference_top.jpg, algo menos
# saturado que la foto para que se vea natural)
TONO_AZUL = (226, 0.72, 0.67)

# remates con aisladores (postes planos con una barra arriba de la que cuelgan uno o dos aisladores),
# en azul (LAV) y en gris espanol: (diagonal, doble) -> modelo de Blockbench del usuario (en azul; el gris
# cambia la textura). Tienen las etiquetas de su poste de PNW menos las de oxidarse: no se oxidan.
REMATES = {
    (False, False): 'LAV_top_oneside_insulator',
    (False, True): 'LAV_top_double_insulator',
    (True, False): 'diagonal_mast_top_oneside_insulator',
    (True, True): 'diagonal_mast_top_double_insulator',
}
# nombres: {color: {lang: (normal, diagonal)}} y {lang: (con aislador, con aislador doble)}
NOMBRE_POSTE_REMATE = {
    AZUL: {'en_us': ('LAV Mast', 'LAV Diagonal Mast'), 'es_es': ('Poste LAV', 'Poste LAV diagonal'),
           'ca_es': ('Pal LAV', 'Pal LAV diagonal'), 'de_de': ('LAV-Mast', 'LAV-Mast (diagonal)')},
    '': {'en_us': ('Iberian Mast', 'Iberian Diagonal Mast'), 'es_es': ('Poste ibérico', 'Poste ibérico diagonal'),
         'ca_es': ('Pal ibèric', 'Pal ibèric diagonal'), 'de_de': ('Iberischer Mast', 'Iberischer Mast (diagonal)')},
}
NOMBRE_AISLADOR_REMATE = {
    'en_us': ('{} Top Insulator', '{} Top Double Insulator'),
    'es_es': ('{} con aislador superior', '{} con aislador superior doble'),
    'ca_es': ('{} amb aïllador superior', '{} amb aïllador superior doble'),
    'de_de': ('{} mit Kopfisolator', '{} mit doppeltem Kopfisolator'),
}


def id_remate(color, diagonal, doble):
    """Como ModPostes.remate: [blue_]iberian_flat_lattice_mast[_diagonal][_double]_insulator."""
    return color + 'iberian_flat_lattice_mast' + ('_diagonal' if diagonal else '') + ('_double' if doble else '') + '_insulator'


# lingote de acero azul: 8 de acero iberico con tinte azul en medio
LINGOTE_AZUL = 'blue_iberian_steel_ingot'
NOMBRE_LINGOTE_AZUL = {'en_us': 'Blue Iberian Steel Ingot', 'es_es': 'Lingote de acero ibérico azul',
                       'ca_es': "Lingot d'acer ibèric blau", 'de_de': 'Blauer Iberischer Stahlbarren'}


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write('\n')


def leer(ruta):
    with open(ruta, encoding='utf-8') as f:
        return json.load(f)


def ident(pnw, encerado, color=''):
    """Como ModPostes.id: los postes son "ibericos" y las demas piezas "espanolas"; los azules llevan blue_."""
    prefijo = 'iberian_' if pnw.endswith('mast') or pnw.endswith('mast_diagonal') else 'spanish_'
    return ('waxed_' if encerado else '') + color + prefijo + pnw


def es_pieza(nombre):
    """Un id (o clave de nombre) de un poste o pieza de poste generado aqui."""
    return ('spanish_' in nombre or 'iberian_' in nombre) and ('mast' in nombre or 'bracket' in nombre)


def limpiar():
    """Borra lo generado antes (postes en todos sus estados y piezas)."""
    patrones = [os.path.join(DATA, MOD, 'recipes', 'postes', '**', '*.json'),
                os.path.join(ASSETS, 'textures', 'block', 'spanish_metal_*.png')]
    # solo postes y brackets (hay otros objetos "iberian_" o "spanish_", como el acero)
    for tipo in ['mast', 'bracket']:
        for nacion in ['spanish', 'iberian']:
            patrones += [os.path.join(ASSETS, 'blockstates', f'*{nacion}_*{tipo}*.json'),
                         os.path.join(ASSETS, 'models', 'item', f'*{nacion}_*{tipo}*.json'),
                         os.path.join(DATA, MOD, 'loot_tables', 'blocks', f'*{nacion}_*{tipo}*.json')]
    for carpeta in ['flat_lattice_mast', 'flat_diagonal_lattice_mast', 'lattice_mast', 'h_beam_mast', 'power_line_bracket',
                    'cantilever_bracket', 'cantilever_bracket_vertical', CARPETA_AZUL]:
        patrones.append(os.path.join(ASSETS, 'models', 'block', carpeta, '**', '*.json'))
    for patron in patrones:
        for f in glob.glob(patron, recursive=True):
            os.remove(f)
    for f in glob.glob(os.path.join(DATA, PNW, 'tags', 'blocks', '*.json')):
        os.remove(f)
    # en las de minecraft hay mas cosas (mensula, soporte de tunel): solo se quitan las piezas
    for f in glob.glob(os.path.join(DATA, 'minecraft', 'tags', 'blocks', '**', '*.json'), recursive=True):
        d = leer(f)
        d['values'] = [v for v in d['values'] if not (v.startswith(f'{MOD}:') and es_pieza(v))]
        escribir(f, d)


def texturas(pnw, hierro):
    objetos = os.path.join(ASSETS, 'textures', 'item')
    os.makedirs(objetos, exist_ok=True)
    # acero espanol: el lingote de hierro, mas oscuro y algo azulado
    lingote = Image.open(hierro).convert('RGBA')
    lingote.putdata([(round(p[0] * 0.5), round(p[1] * 0.52), round(p[2] * 0.6), p[3]) for p in lingote.getdata()])
    lingote.save(os.path.join(objetos, 'iberian_steel_ingot.png'))
    # mensula a medio montar: la de PNW, oscurecida como el acero espanol
    medio = Image.open(os.path.join(pnw, 'assets', PNW, 'textures', 'item', 'cantilever_incomplete.png')).convert('RGBA')
    medio.putdata([(round(p[0] * 0.6), round(p[1] * 0.6), round(p[2] * 0.65), p[3]) for p in medio.getdata()])
    medio.save(os.path.join(objetos, 'mensula_incompleta.png'))


def carpeta_color(color):
    """Donde van los modelos de cada color, dentro de models/block ('' = los espanoles grises)."""
    return CARPETA_AZUL + '/' if color == AZUL else ''


def copiar_modelo(pnw_assets, modelo, hechos, color=''):
    """Copia un modelo de bloque de PNW a nuestro espacio con el metal espanol (gris o azul)."""
    destino = carpeta_color(color) + modelo
    if destino in hechos:
        return
    hechos.add(destino)
    texto = open(os.path.join(pnw_assets, 'models', 'block', modelo + '.json'), encoding='utf-8').read()
    assert METAL_PNW in texto, f'{modelo}: no pone la textura del metal, no se puede cambiar'
    metal = METAL_AZUL if color == AZUL else METAL
    escribir(os.path.join(ASSETS, 'models', 'block', destino + '.json'), json.loads(texto.replace(METAL_PNW, metal)))


def con_modelos_nuestros(texto, pnw_assets, hechos, color=''):
    """Cambia las referencias a modelos de bloque de PNW por las nuestras (y las genera)."""
    prefijo = f'"{PNW}:block/'
    out, i = [], 0
    while True:
        j = texto.find(prefijo, i)
        if j < 0:
            out.append(texto[i:])
            return ''.join(out)
        k = texto.index('"', j + len(prefijo))
        modelo = texto[j + len(prefijo):k]
        copiar_modelo(pnw_assets, modelo, hechos, color)
        out.append(texto[i:j] + f'"{MOD}:block/' + carpeta_color(color) + modelo)
        i = k


def texturas_azules():
    """El metal azul y el cristal de los aisladores (de new stuff, en minusculas) y el lingote azul."""
    bloques = os.path.join(ASSETS, 'textures', 'block')
    Image.open(os.path.join(NUEVO, 'new textures', 'insulator_glass.png')).save(os.path.join(bloques, 'insulator_glass.png'))
    # el metal azul: el dibujo de la textura del usuario (su brillo, pixel a pixel) con el tono TONO_AZUL
    tono, saturacion, brillo = TONO_AZUL
    metal = Image.open(os.path.join(NUEVO, 'new textures', 'blue_LAV_spanish_metal.png')).convert('RGBA')
    pixeles = [metal.getpixel((x, y)) for y in range(metal.height) for x in range(metal.width)]
    medio = sum(max(p[:3]) for p in pixeles) / len(pixeles)
    metal.putdata([tuple(round(c * 255) for c in colorsys.hsv_to_rgb(tono / 360, saturacion, min(1, brillo * max(p[:3]) / medio)))
                   + (p[3],) for p in pixeles])
    metal.save(os.path.join(bloques, 'blue_lav_spanish_metal.png'))
    # lingote azul: el de acero iberico (oscuro) con el mismo tono, aclarado
    lingote = Image.open(os.path.join(ASSETS, 'textures', 'item', 'iberian_steel_ingot.png')).convert('RGBA')
    pixeles = [lingote.getpixel((x, y)) for y in range(lingote.height) for x in range(lingote.width)]
    lingote.putdata([tuple(round(c * 255) for c in colorsys.hsv_to_rgb(tono / 360, saturacion, min(1, 0.15 + 1.6 * max(p[:3]) / 255)))
                     + (p[3],) for p in pixeles])
    lingote.save(os.path.join(ASSETS, 'textures', 'item', f'{LINGOTE_AZUL}.png'))


def modelo_remate(archivo, color):
    """El modelo de Blockbench de un remate, con las texturas en nuestro espacio (el gris con el metal
    espanol en vez del azul) y lo que necesita como objeto. El cristal semitransparente de los aisladores
    lo pone en la capa translucida client/ModeloRemate (el modelo girable de PNW no lee render_type)."""
    modelo = leer(os.path.join(NUEVO, 'new json', archivo + '.json'))
    texturas = {k: v if v.startswith('#') or ':' in v else f'{MOD}:block/{v.lower()}'
                for k, v in modelo['textures'].items()}
    if color != AZUL:
        texturas = {k: METAL if v == METAL_AZUL else v for k, v in texturas.items()}
    return {'parent': 'minecraft:block/block', 'textures': texturas,
            'elements': modelo['elements'],
            # la barra mide casi dos bloques: en la mano y en el inventario, mas pequeno
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
                        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.35, 0.35, 0.35]},
                        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.2, 0.2, 0.2]},
                        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
                        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]}}}


def estados_remate(modelo):
    """Blockstate de un remate: el mismo modelo en todas las partes y giros finos, girado segun FACING
    (como los postes planos de PNW; el giro fino lo pone el modelo girable de PNW)."""
    variantes = {}
    for cara, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for parte in ('bottom', 'in_between', 'top'):
            for giro in range(4):
                v = {'model': modelo}
                if y:
                    v['y'] = y
                variantes[f'facing={cara},part={parte},rotation={giro}'] = v
    return {'variants': variantes}


def oxidacion(etiqueta):
    """Las etiquetas de PNW de los postes que se oxidan o se enceran (no para los que no se oxidan)."""
    return 'waxed' in etiqueta or 'raw' in etiqueta


def remate(diagonal, doble, archivo, color, lingote, nombres, etiquetas, del_pnw, recetas):
    """Un remate con aisladores: modelo, blockstate, objeto, botin, nombres, etiquetas y receta."""
    bid = id_remate(color, diagonal, doble)
    poste = 'flat_lattice_mast_diagonal' if diagonal else 'flat_lattice_mast'
    modelo = f'{MOD}:block/{CARPETA_AZUL}/remates/{bid}'
    escribir(os.path.join(ASSETS, 'models', 'block', CARPETA_AZUL, 'remates', f'{bid}.json'), modelo_remate(archivo, color))
    escribir(os.path.join(ASSETS, 'blockstates', f'{bid}.json'), estados_remate(modelo))
    escribir(os.path.join(ASSETS, 'models', 'item', f'{bid}.json'), {'parent': modelo})
    escribir(os.path.join(DATA, MOD, 'loot_tables', 'blocks', f'{bid}.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{bid}'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    for lang in LANGS:
        nombres[lang][f'block.{MOD}.{bid}'] = NOMBRE_AISLADOR_REMATE[lang][doble].format(NOMBRE_POSTE_REMATE[color][lang][diagonal])
    for etiqueta in del_pnw.get(f'{PNW}:{poste}', []):
        if not oxidacion(etiqueta):
            etiquetas.setdefault(etiqueta, []).append(f'{MOD}:{bid}')
    recetas[f'remates/{bid}'] = {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': f'{MOD}:{ident(poste, False, color)}'}, {'item': f'{MOD}:{lingote}'}]
                       + [{'tag': f'{PNW}:insulators'}] * (2 if doble else 1),
        'result': {'item': f'{MOD}:{bid}', 'count': 1}}


def etiquetas_pnw(pnw):
    """Para cada bloque de PNW, las etiquetas de bloque (de cualquier espacio de nombres) que lo incluyen."""
    por_bloque = {}
    for ruta in glob.glob(os.path.join(pnw, 'data', '*', 'tags', 'blocks', '**', '*.json'), recursive=True):
        partes = os.path.relpath(ruta, os.path.join(pnw, 'data')).replace('\\', '/').split('/')
        nombre = f'{partes[0]}:' + '/'.join(partes[3:])[:-5]
        for v in leer(ruta).get('values', []):
            v = v['id'] if isinstance(v, dict) else v
            por_bloque.setdefault(v, []).append(nombre)
    return por_bloque


def main():
    pnw = sys.argv[1]
    pnw_assets = os.path.join(pnw, 'assets', PNW)
    limpiar()
    if len(sys.argv) > 2:
        texturas(pnw, sys.argv[2])

    hechos = set()
    nombres = {lang: {} for lang in LANGS}
    etiquetas = {}
    del_pnw = etiquetas_pnw(pnw)
    recetas = {}

    texturas_azules()
    # (pieza de PNW, color): las grises espanolas y los postes azules
    todas = [(p, '') for p in list(PIEZAS) + list(SIN_OBJETO)] + [(p, AZUL) for p in AZULES]
    for pieza, color in todas:
        con_objeto = pieza in PIEZAS
        nombre_de = (True, AZULES[pieza]) if color else PIEZAS[pieza if con_objeto else SIN_OBJETO[pieza]]
        # los azules no se oxidan: no hay encerado
        for encerado in (False,) if color else (False, True):
            bid = ident(pieza, encerado, color)
            pnw_id = ('waxed_' if encerado else '') + pieza
            texto = open(os.path.join(pnw_assets, 'blockstates', pnw_id + '.json'), encoding='utf-8').read()
            escribir(os.path.join(ASSETS, 'blockstates', f'{bid}.json'),
                     json.loads(con_modelos_nuestros(texto, pnw_assets, hechos, color)))
            suelta = bid if con_objeto else ident(SIN_OBJETO[pieza], encerado)
            escribir(os.path.join(DATA, MOD, 'loot_tables', 'blocks', f'{bid}.json'), {
                'type': 'minecraft:block',
                'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{suelta}'}],
                           'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
            if con_objeto:
                texto = open(os.path.join(pnw_assets, 'models', 'item', pieza + '.json'), encoding='utf-8').read()
                escribir(os.path.join(ASSETS, 'models', 'item', f'{bid}.json'),
                         json.loads(con_modelos_nuestros(texto, pnw_assets, hechos, color)))
            for lang in LANGS:
                nombres[lang][f'block.{MOD}.{bid}'] = nombre_de[2 if encerado else 1][lang]
            # las mismas etiquetas que la pieza de PNW: se enganchan las mismas cosas, se pican igual...
            for etiqueta in del_pnw.get(f'{PNW}:{pnw_id}', []):
                if not (color and oxidacion(etiqueta)):
                    etiquetas.setdefault(etiqueta, []).append(f'{MOD}:{bid}')
            if con_objeto and not encerado and not color:
                # encerar con un panal (como PNW); en el mundo tambien vale, lo hace PNW con su mixin
                recetas[f'encerar/{bid}'] = {
                    'type': 'minecraft:crafting_shapeless',
                    'ingredients': [{'item': f'{MOD}:{bid}'}, {'item': 'minecraft:honeycomb'}],
                    'result': {'item': f'{MOD}:{ident(pieza, True)}', 'count': 1}}

    for encerado in (False, True):
        # plano normal <-> plano diagonal (gris y azul)
        for color in ('', AZUL) if not encerado else ('',):
            normal, diagonal = ident('flat_lattice_mast', encerado, color), ident('flat_lattice_mast_diagonal', encerado, color)
            for a, b in ((normal, diagonal), (diagonal, normal)):
                recetas[f'variantes/{a}_a_{b}'] = {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': f'{MOD}:{a}'}],
                                                    'result': {'item': f'{MOD}:{b}', 'count': 1}}
        # soporte de linea electrica: del poste H con el cortapiedras, como PNW
        recetas[f'{ident("power_line_bracket", encerado)}'] = {
            'type': 'minecraft:stonecutting', 'count': 2, 'ingredient': {'item': f'{MOD}:{ident("h_beam_mast", encerado)}'},
            'result': f'{MOD}:{ident("power_line_bracket", encerado)}'}

    # los azules, igual que los grises pero con el lingote azul
    for color, acero in (('', {'item': f'{MOD}:iberian_steel_ingot'}), (AZUL, {'item': f'{MOD}:{LINGOTE_AZUL}'})):
        # el plano: como el de PNW (6 tiras de hierro dan 6 postes) pero con acero espanol en medio
        recetas[ident('flat_lattice_mast', False, color)] = {
            'type': 'minecraft:crafting_shaped', 'pattern': ['AA', 'AB', 'AA'],
            'key': {'A': {'item': f'{PNW}:iron_strip'}, 'B': acero},
            'result': {'item': f'{MOD}:{ident("flat_lattice_mast", False, color)}', 'count': 6}}
        # el cuadrado: como el de PNW (tiras y varillas de hierro) con acero espanol en el centro
        recetas[ident('lattice_mast', False, color)] = {
            'type': 'minecraft:crafting_shaped', 'pattern': ['ABA', 'ACA', 'ABA'],
            'key': {'A': {'item': f'{PNW}:iron_strip'}, 'B': {'tag': 'forge:rods/iron'}, 'C': acero},
            'result': {'item': f'{MOD}:{ident("lattice_mast", False, color)}', 'count': 6}}
        # el poste H: compactado en caliente como el de PNW, con acero espanol en vez de hierro
        recetas[ident('h_beam_mast', False, color)] = {
            'type': 'create:compacting', 'heatRequirement': 'heated',
            'ingredients': [{'item': f'{PNW}:iron_strip'}, {'item': f'{PNW}:iron_strip'}, acero],
            'results': [{'item': f'{MOD}:{ident("h_beam_mast", False, color)}', 'count': 2}]}
    acero = {'item': f'{MOD}:iberian_steel_ingot'}
    # el soporte de mensula: compactado en caliente como el de PNW, con acero espanol en vez de hierro
    recetas[ident('cantilever_bracket', False)] = {
        'type': 'create:compacting', 'heatRequirement': 'heated', 'ingredients': [acero, acero, acero],
        'results': [{'item': f'{MOD}:{ident("cantilever_bracket", False)}', 'count': 3}]}

    # lingote azul: 8 de acero iberico alrededor de un tinte azul
    recetas[LINGOTE_AZUL] = {
        'type': 'minecraft:crafting_shaped', 'pattern': ['AAA', 'ABA', 'AAA'],
        'key': {'A': acero, 'B': {'item': 'minecraft:blue_dye'}},
        'result': {'item': f'{MOD}:{LINGOTE_AZUL}', 'count': 8}}
    escribir(os.path.join(ASSETS, 'models', 'item', f'{LINGOTE_AZUL}.json'),
             {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/{LINGOTE_AZUL}'}})

    # remates con aisladores: su poste (azul o gris), su lingote y uno o dos aisladores de PNW
    for (diagonal, doble), archivo in REMATES.items():
        for color, lingote in ((AZUL, LINGOTE_AZUL), ('', 'iberian_steel_ingot')):
            remate(diagonal, doble, archivo, color, lingote, nombres, etiquetas, del_pnw, recetas)

    for nombre, receta in recetas.items():
        escribir(os.path.join(DATA, MOD, 'recipes', 'postes', f'{nombre}.json'), receta)

    # etiquetas: las de PNW tal cual; las de minecraft se juntan con lo que ya habia (mensula, soporte de tunel...)
    for nombre, valores in etiquetas.items():
        ns, path = nombre.split(':')
        ruta = os.path.join(DATA, ns, 'tags', 'blocks', f'{path}.json')
        if ns != PNW and os.path.exists(ruta):
            viejos = [v for v in leer(ruta)['values'] if not (v.startswith(f'{MOD}:') and es_pieza(v))]
            valores = viejos + valores
        escribir(ruta, {'replace': False, 'values': valores})

    for lang in LANGS:
        ruta = os.path.join(ASSETS, 'lang', f'{lang}.json')
        d = {k: v for k, v in leer(ruta).items() if not (k.startswith(f'block.{MOD}.') and es_pieza(k))}
        d.update(nombres[lang])
        d[f'item.{MOD}.{LINGOTE_AZUL}'] = NOMBRE_LINGOTE_AZUL[lang]
        escribir(ruta, d)
    print('piezas:', sum(len(v) for v in nombres.values()) // len(LANGS), 'bloques,', len(hechos), 'modelos,', len(recetas), 'recetas')


if __name__ == '__main__':
    main()

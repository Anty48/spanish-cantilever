"""Genera los postes y piezas de poste espanoles y lo que los rodea.

Uso:  python tools/generar_postes.py <carpeta con el jar de PNW extraido> [png del lingote de hierro vanilla]

Son las piezas de PNW (postes de celosia plana, plana diagonal y cuadrada, poste H, soporte de linea
electrica y soporte de mensula con sus bloques vertical y junto a poste) con el metal gris espanol: los
blockstates y modelos son los de PNW con la textura cambiada. Solo hay el estado nuevo (sin encerar y
encerado); al oxidarse pasan al expuesto de PNW (ver ModPostes.java). Genera:

- blockstates, modelos de bloque y de objeto, tablas de botin, etiquetas (las mismas que la pieza de
  PNW equivalente), recetas y nombres;
- con el png del lingote: las texturas iberian_steel_ingot (lingote de hierro oscurecido) y mensula_incompleta
  (la mensula a medio montar de PNW, oscurecida).

Borra antes todo lo que genero (tambien los estados oxidados espanoles que habia antes).
"""
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


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write('\n')


def leer(ruta):
    with open(ruta, encoding='utf-8') as f:
        return json.load(f)


def ident(pnw, encerado):
    """Como ModPostes.id: los postes son "ibericos" y las demas piezas "espanolas"."""
    prefijo = 'iberian_' if pnw.endswith('mast') or pnw.endswith('mast_diagonal') else 'spanish_'
    return ('waxed_' if encerado else '') + prefijo + pnw


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
                    'cantilever_bracket', 'cantilever_bracket_vertical']:
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


def copiar_modelo(pnw_assets, modelo, hechos):
    """Copia un modelo de bloque de PNW a nuestro espacio con el metal espanol."""
    if modelo in hechos:
        return
    hechos.add(modelo)
    texto = open(os.path.join(pnw_assets, 'models', 'block', modelo + '.json'), encoding='utf-8').read()
    assert METAL_PNW in texto, f'{modelo}: no pone la textura del metal, no se puede cambiar'
    escribir(os.path.join(ASSETS, 'models', 'block', modelo + '.json'), json.loads(texto.replace(METAL_PNW, METAL)))


def con_modelos_nuestros(texto, pnw_assets, hechos):
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
        copiar_modelo(pnw_assets, modelo, hechos)
        out.append(texto[i:j] + f'"{MOD}:block/' + modelo)
        i = k


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

    for pieza in list(PIEZAS) + list(SIN_OBJETO):
        con_objeto = pieza in PIEZAS
        nombre_de = PIEZAS[pieza if con_objeto else SIN_OBJETO[pieza]]
        for encerado in (False, True):
            bid = ident(pieza, encerado)
            pnw_id = ('waxed_' if encerado else '') + pieza
            texto = open(os.path.join(pnw_assets, 'blockstates', pnw_id + '.json'), encoding='utf-8').read()
            escribir(os.path.join(ASSETS, 'blockstates', f'{bid}.json'), json.loads(con_modelos_nuestros(texto, pnw_assets, hechos)))
            suelta = bid if con_objeto else ident(SIN_OBJETO[pieza], encerado)
            escribir(os.path.join(DATA, MOD, 'loot_tables', 'blocks', f'{bid}.json'), {
                'type': 'minecraft:block',
                'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{suelta}'}],
                           'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
            if con_objeto:
                texto = open(os.path.join(pnw_assets, 'models', 'item', pieza + '.json'), encoding='utf-8').read()
                escribir(os.path.join(ASSETS, 'models', 'item', f'{bid}.json'), json.loads(con_modelos_nuestros(texto, pnw_assets, hechos)))
            for lang in LANGS:
                nombres[lang][f'block.{MOD}.{bid}'] = nombre_de[2 if encerado else 1][lang]
            # las mismas etiquetas que la pieza de PNW: se enganchan las mismas cosas, se pican igual...
            for etiqueta in del_pnw.get(f'{PNW}:{pnw_id}', []):
                etiquetas.setdefault(etiqueta, []).append(f'{MOD}:{bid}')
            if con_objeto and not encerado:
                # encerar con un panal (como PNW); en el mundo tambien vale, lo hace PNW con su mixin
                recetas[f'encerar/{bid}'] = {
                    'type': 'minecraft:crafting_shapeless',
                    'ingredients': [{'item': f'{MOD}:{bid}'}, {'item': 'minecraft:honeycomb'}],
                    'result': {'item': f'{MOD}:{ident(pieza, True)}', 'count': 1}}

    for encerado in (False, True):
        # plano normal <-> plano diagonal
        normal, diagonal = ident('flat_lattice_mast', encerado), ident('flat_lattice_mast_diagonal', encerado)
        for a, b in ((normal, diagonal), (diagonal, normal)):
            recetas[f'variantes/{a}_a_{b}'] = {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': f'{MOD}:{a}'}],
                                                'result': {'item': f'{MOD}:{b}', 'count': 1}}
        # soporte de linea electrica: del poste H con el cortapiedras, como PNW
        recetas[f'{ident("power_line_bracket", encerado)}'] = {
            'type': 'minecraft:stonecutting', 'count': 2, 'ingredient': {'item': f'{MOD}:{ident("h_beam_mast", encerado)}'},
            'result': f'{MOD}:{ident("power_line_bracket", encerado)}'}

    acero = {'item': f'{MOD}:iberian_steel_ingot'}
    # el plano: como el de PNW (6 tiras de hierro dan 6 postes) pero con acero espanol en medio
    recetas[ident('flat_lattice_mast', False)] = {
        'type': 'minecraft:crafting_shaped', 'pattern': ['AA', 'AB', 'AA'],
        'key': {'A': {'item': f'{PNW}:iron_strip'}, 'B': acero},
        'result': {'item': f'{MOD}:{ident("flat_lattice_mast", False)}', 'count': 6}}
    # el cuadrado: como el de PNW (tiras y varillas de hierro) con acero espanol en el centro
    recetas[ident('lattice_mast', False)] = {
        'type': 'minecraft:crafting_shaped', 'pattern': ['ABA', 'ACA', 'ABA'],
        'key': {'A': {'item': f'{PNW}:iron_strip'}, 'B': {'tag': 'forge:rods/iron'}, 'C': acero},
        'result': {'item': f'{MOD}:{ident("lattice_mast", False)}', 'count': 6}}
    # el poste H y el soporte de mensula: compactados en caliente como los de PNW, con acero espanol en vez de hierro
    recetas[ident('h_beam_mast', False)] = {
        'type': 'create:compacting', 'heatRequirement': 'heated',
        'ingredients': [{'item': f'{PNW}:iron_strip'}, {'item': f'{PNW}:iron_strip'}, acero],
        'results': [{'item': f'{MOD}:{ident("h_beam_mast", False)}', 'count': 2}]}
    recetas[ident('cantilever_bracket', False)] = {
        'type': 'create:compacting', 'heatRequirement': 'heated', 'ingredients': [acero, acero, acero],
        'results': [{'item': f'{MOD}:{ident("cantilever_bracket", False)}', 'count': 3}]}

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
        escribir(ruta, d)
    print('piezas:', sum(len(v) for v in nombres.values()) // len(LANGS), 'bloques,', len(hechos), 'modelos,', len(recetas), 'recetas')


if __name__ == '__main__':
    main()

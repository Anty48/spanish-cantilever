"""Genera los postes ibericos de celosia (plana, plana diagonal y cuadrada) y lo que los rodea.

Uso:  python tools/generar_postes.py <carpeta con el jar de PNW extraido> <png del lingote de hierro vanilla>

Los postes son los de celosia de PNW (plano, plano diagonal y cuadrado) con el metal iberico: los modelos
heredan la geometria de PNW ("base") y solo cambian la textura. Se oxidan como los de PNW (normal ->
expuesto -> erosionado -> oxidado) y se pueden encerar, pero no galvanizar. Genera:

- texturas: iberian_metal_{exposed,weathered,oxidized} (el metal iberico con el cambio de color que
  PNW aplica a su metal en cada estado), iberian_steel (lingote de hierro oscurecido) y
  mensula_incompleta (la mensula a medio montar de PNW, oscurecida);
- blockstates, modelos de bloque y de objeto, tablas de botin, etiquetas, recetas y nombres.
"""
import json
import os
import sys

from PIL import Image

MOD = 'iberiancantilever'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', MOD)
DATA = os.path.join(ROOT, 'src', 'main', 'resources', 'data')

ESTADOS = ['', 'exposed', 'weathered', 'oxidized']
NOMBRES_ESTADO = {
    'en_us': {'': '', 'exposed': 'Exposed ', 'weathered': 'Weathered ', 'oxidized': 'Oxidized '},
    'es_es': {'': '', 'exposed': ' expuesto', 'weathered': ' erosionado', 'oxidized': ' oxidado'},
    'ca_es': {'': '', 'exposed': ' exposat', 'weathered': ' erosionat', 'oxidized': ' oxidat'},
}
FORMAS = {
    # forma: (id base, modelos de PNW, nombres)
    'normal': ('iberian_flat_lattice_mast', 'flat_lattice_mast',
               {'en_us': 'Iberian Flat Lattice Mast', 'es_es': 'Poste de celosía plana ibérico',
                'ca_es': 'Pal de gelosia plana ibèric'}),
    'diagonal': ('iberian_flat_lattice_mast_diagonal', 'flat_diagonal_lattice_mast',
                 {'en_us': 'Iberian Flat Diagonal Lattice Mast', 'es_es': 'Poste de celosía plana diagonal ibérico',
                  'ca_es': 'Pal de gelosia plana diagonal ibèric'}),
    'cuadrado': ('iberian_lattice_mast', 'lattice_mast',
                 {'en_us': 'Iberian Lattice Mast', 'es_es': 'Poste de celosía ibérico', 'ca_es': 'Pal de gelosia ibèric'}),
}
# blockstate de PNW que se copia en cada forma
ESTADO_PNW = {'normal': 'flat_lattice_mast', 'diagonal': 'flat_lattice_mast_diagonal', 'cuadrado': 'lattice_mast'}
# etiquetas de PNW de cada forma (las mismas que sus postes)
ETIQUETAS = {
    'normal': ['flat_lattice_masts', 'cantilever_connectable_8px', 'support_wire_connectable'],
    'diagonal': ['flat_diagonal_lattice_masts', 'cantilever_connectable_8px', 'support_wire_connectable'],
    'cuadrado': ['lattice_masts', 'cantilever_connectable_12px', 'support_wire_connectable', 'cantilever_mast_bracket_fitting',
                 'catenary_headspan_connectable', 'tensioning_device_connectable'],
}
ENCERADO = {'en_us': 'Waxed ', 'es_es': ' encerado', 'ca_es': ' encerat'}


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write('\n')


def media(im):
    px = [p for p in im.getdata() if p[3] > 0]
    return [sum(p[i] for p in px) / len(px) for i in range(3)]


def texturas(pnw, hierro):
    bloques = os.path.join(ASSETS, 'textures', 'block')
    iberico = Image.open(os.path.join(bloques, 'iberian_metal.png')).convert('RGBA')
    base_pnw = media(Image.open(os.path.join(pnw, 'textures', 'block', 'metal.png')).convert('RGBA'))
    for estado in ESTADOS[1:]:
        oxidado = media(Image.open(os.path.join(pnw, 'textures', 'block', f'metal_{estado}.png')).convert('RGBA'))
        # el mismo cambio de color (canal a canal) que PNW aplica a su metal en ese estado
        factor = [oxidado[i] / base_pnw[i] for i in range(3)]
        out = Image.new('RGBA', iberico.size)
        out.putdata([tuple(min(255, round(p[i] * factor[i] * 1.15)) for i in range(3)) + (p[3],) for p in iberico.getdata()])
        out.save(os.path.join(bloques, f'iberian_metal_{estado}.png'))

    objetos = os.path.join(ASSETS, 'textures', 'item')
    os.makedirs(objetos, exist_ok=True)
    # acero iberico: el lingote de hierro, mas oscuro y algo azulado
    lingote = Image.open(hierro).convert('RGBA')
    lingote.putdata([(round(p[0] * 0.5), round(p[1] * 0.52), round(p[2] * 0.6), p[3]) for p in lingote.getdata()])
    lingote.save(os.path.join(objetos, 'iberian_steel.png'))
    # mensula a medio montar: la de PNW, oscurecida como el acero iberico
    medio = Image.open(os.path.join(pnw, 'textures', 'item', 'cantilever_incomplete.png')).convert('RGBA')
    medio.putdata([(round(p[0] * 0.6), round(p[1] * 0.6), round(p[2] * 0.65), p[3]) for p in medio.getdata()])
    medio.save(os.path.join(objetos, 'mensula_incompleta.png'))


def ident(forma, estado, encerado):
    base = FORMAS[forma][0]
    return ('waxed_' if encerado else '') + base + ('_' + estado if estado else '')


def postes(pnw):
    nombres = {lang: {} for lang in NOMBRES_ESTADO}
    pickaxe, piedra, recetas = [], [], {}
    etiquetas = {}

    def etiqueta(nombre, valor):
        etiquetas.setdefault(nombre, []).append(valor)

    for forma, (base, modelo_pnw, nombre_base) in FORMAS.items():
        estado_pnw = json.load(open(os.path.join(pnw, 'blockstates', ESTADO_PNW[forma] + '.json'), encoding='utf-8'))
        for estado in ESTADOS:
            carpeta = f'block/{modelo_pnw}'
            nombre_modelo = estado or 'normal'
            textura = f'{MOD}:block/iberian_metal' + ('_' + estado if estado else '')
            for parte in ['', '_bottom', '_top']:
                escribir(os.path.join(ASSETS, 'models', carpeta, f'{nombre_modelo}{parte}.json'), {
                    'parent': f'pantographsandwires:{carpeta}/base{parte}',
                    'textures': {'base': textura, 'particle': textura},
                })
            for encerado in (False, True):
                bid = ident(forma, estado, encerado)
                # mismo blockstate que PNW, con nuestros modelos
                texto = json.dumps(estado_pnw).replace(f'pantographsandwires:{carpeta}/normal', f'{MOD}:{carpeta}/{nombre_modelo}')
                escribir(os.path.join(ASSETS, 'blockstates', f'{bid}.json'), json.loads(texto))
                escribir(os.path.join(ASSETS, 'models', 'item', f'{bid}.json'), {'parent': f'{MOD}:{carpeta}/{nombre_modelo}'})
                escribir(os.path.join(DATA, MOD, 'loot_tables', 'blocks', f'{bid}.json'), {
                    'type': 'minecraft:block',
                    'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{bid}'}],
                               'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
                for lang in nombres:
                    if lang == 'en_us':
                        n = (ENCERADO[lang] if encerado else '') + NOMBRES_ESTADO[lang][estado] + nombre_base[lang]
                    else:
                        n = nombre_base[lang] + NOMBRES_ESTADO[lang][estado] + (ENCERADO[lang] if encerado else '')
                    nombres[lang][f'block.{MOD}.{bid}'] = n
                pickaxe.append(f'{MOD}:{bid}')
                piedra.append(f'{MOD}:{bid}')
                # las etiquetas de PNW: asi la mensula de PNW y la nuestra se enganchan y los cables tambien
                for nombre in ETIQUETAS[forma]:
                    etiqueta('pantographsandwires:' + nombre, f'{MOD}:{bid}')
                etiqueta('pantographsandwires:' + {'': 'raw_masts', 'exposed': 'exposed_masts', 'weathered': 'weathered_masts', 'oxidized': 'oxidized_masts'}[estado], f'{MOD}:{bid}')
                etiqueta('pantographsandwires:' + ('waxed_masts' if encerado else 'unwaxed_masts'), f'{MOD}:{bid}')
                # encerar con un panal (como PNW); en el mundo tambien vale, lo hace PNW con su mixin
                if not encerado:
                    recetas[f'encerar/{bid}'] = {
                        'type': 'minecraft:crafting_shapeless',
                        'ingredients': [{'item': f'{MOD}:{bid}'}, {'item': 'minecraft:honeycomb'}],
                        'result': {'item': f'{MOD}:{ident(forma, estado, True)}', 'count': 1}}
                # plano normal <-> plano diagonal, en cada estado
                if forma != 'cuadrado':
                    otra = 'diagonal' if forma == 'normal' else 'normal'
                    recetas[f'variantes/{bid}_a_{otra}'] = {
                        'type': 'minecraft:crafting_shapeless',
                        'ingredients': [{'item': f'{MOD}:{bid}'}],
                        'result': {'item': f'{MOD}:{ident(otra, estado, encerado)}', 'count': 1}}

    # el poste: como el de PNW (6 tiras de hierro dan 6 postes) pero con acero iberico en medio
    recetas[ident('normal', '', False)] = {
        'type': 'minecraft:crafting_shaped',
        'pattern': ['AA', 'AB', 'AA'],
        'key': {'A': {'item': 'pantographsandwires:iron_strip'}, 'B': {'item': f'{MOD}:iberian_steel'}},
        'result': {'item': f'{MOD}:{ident("normal", "", False)}', 'count': 6}}
    # el cuadrado: como el de PNW (tiras y varillas de hierro) con acero iberico en el centro
    recetas[ident('cuadrado', '', False)] = {
        'type': 'minecraft:crafting_shaped',
        'pattern': ['ABA', 'ACA', 'ABA'],
        'key': {'A': {'item': 'pantographsandwires:iron_strip'}, 'B': {'tag': 'forge:rods/iron'}, 'C': {'item': f'{MOD}:iberian_steel'}},
        'result': {'item': f'{MOD}:{ident("cuadrado", "", False)}', 'count': 6}}
    for nombre, receta in recetas.items():
        escribir(os.path.join(DATA, MOD, 'recipes', 'postes', f'{nombre}.json'), receta)

    for nombre, valores in etiquetas.items():
        ns, path = nombre.split(':')
        escribir(os.path.join(DATA, ns, 'tags', 'blocks', f'{path}.json'), {'replace': False, 'values': valores})
    return nombres, pickaxe, piedra


def main():
    pnw, hierro = sys.argv[1], sys.argv[2]
    pnw = os.path.join(pnw, 'assets', 'pantographsandwires')
    texturas(pnw, hierro)
    nombres, pickaxe, piedra = postes(pnw)

    # picos y herramientas: los postes y la mensula se pican con pico de piedra o mejor
    mensula = f'{MOD}:mensula'
    tunel = f'{MOD}:soporte_tunel'
    escribir(os.path.join(DATA, 'minecraft', 'tags', 'blocks', 'mineable', 'pickaxe.json'),
             {'replace': False, 'values': [mensula, tunel] + pickaxe})
    escribir(os.path.join(DATA, 'minecraft', 'tags', 'blocks', 'needs_stone_tool.json'),
             {'replace': False, 'values': [mensula, tunel] + piedra})

    for lang, textos in nombres.items():
        ruta = os.path.join(ASSETS, 'lang', f'{lang}.json')
        d = json.load(open(ruta, encoding='utf-8'))
        d.update(textos)
        escribir(ruta, d)
    print('postes:', len(pickaxe), 'bloques')


if __name__ == '__main__':
    main()

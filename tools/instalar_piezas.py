"""Instala las piezas de Blockbench del usuario en el mod.

Uso:  python tools/instalar_piezas.py

- Copia los .json de `new stuff/new json/` a assets/<mod>/models/block/piezas/<id>.json,
  arreglando las rutas de textura (`iberian_metal` -> `iberiancantilever:block/iberian_metal`).
- Copia los .png de `new stuff/new textures/` a assets/<mod>/textures/block/.
- Genera src/main/java/iberiancantilever/geometry/PiezasDatos.java con los centros de los cubos
  con nombre y los grupos de cada pieza. El codigo de la mensula (cliente Y servidor) usa esas
  constantes; por eso hay que volver a ejecutar este script si cambias una pieza.
"""
import json
import os
import re
import shutil

MOD = 'iberiancantilever'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_MODELS = os.path.join(ROOT, 'new stuff', 'new json')
SRC_TEXTURES = os.path.join(ROOT, 'new stuff', 'new textures')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', MOD)
OUT_MODELS = os.path.join(ASSETS, 'models', 'block', 'piezas')
OUT_TEXTURES = os.path.join(ASSETS, 'textures', 'block')
OUT_JAVA = os.path.join(ROOT, 'src', 'main', 'java', MOD, 'geometry', 'PiezasDatos.java')

# archivo de Blockbench -> id de la pieza en el mod
PIEZAS = {
    'Main_steel_bar.json': 'barra',
    'stee_bar_holes.json': 'perforada_diagonal',
    'stee_bar_holes_vertical.json': 'perforada_vertical',
    'cable_holder_short.json': 'brazo_corto',
    'cable_holder_long.json': 'brazo_largo',
    'cable_holder_inner.json': 'brazo_interior',
    'cable_holder_all.json': 'sujetador',
    'cable_holder_all_short.json': 'sujetador_corto',
    'sustainig_metal_cable.json': 'tirante',
    'horizontal_insulator.json': 'aislador_horizontal',
    'vertical_insulator.json': 'aislador_vertical',
    # catenaria rigida de tunel (estas estan en otra carpeta)
    '../../modelos_pnw/blockbench/postes/Tunnel iberian canteliver left.json': 'tunel_izquierda',
    '../../modelos_pnw/blockbench/postes/Tunnel iberian canteliver center.json': 'tunel_centro',
    '../../modelos_pnw/blockbench/postes/Tunnel iberian canteliver right.json': 'tunel_derecha',
    '../../modelos_pnw/blockbench/postes/steel catenary tunnel fixed cable segment.json': 'perfil_rigido',
}


def fix_texture(ref):
    if ref.startswith('#') or ':' in ref:
        return ref
    return f'{MOD}:block/{os.path.basename(ref).replace("spanish_", "iberian_")}'


def java_name(s):
    return re.sub(r'[^A-Za-z0-9]+', '_', s).strip('_').upper()


def group_indices(groups):
    """Blockbench exporta 'groups' como lista de indices sueltos o de grupos {name, children}."""
    out = {}

    def walk(node):
        if isinstance(node, dict):
            idx = []
            for c in node.get('children', []):
                idx += walk(c)
            out[node['name'].lower()] = idx
            return idx
        return [node]

    for g in groups or []:
        walk(g)
    return out


def main():
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)
    for f in os.listdir(SRC_TEXTURES):
        if f.endswith('.png'):
            shutil.copy(os.path.join(SRC_TEXTURES, f), os.path.join(OUT_TEXTURES, f.replace('spanish_', 'iberian_')))
            print('textura', f)

    java = []
    for src, pid in PIEZAS.items():
        with open(os.path.join(SRC_MODELS, src), encoding='utf-8') as fh:
            model = json.load(fh)
        model['textures'] = {k: fix_texture(v) for k, v in model.get('textures', {}).items()}
        model.pop('format_version', None)
        with open(os.path.join(OUT_MODELS, pid + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(model, fh, indent=1)

        joints = {}
        for i, e in enumerate(model['elements']):
            name = e.get('name')
            if not name:
                continue
            c = [(a + b) / 2 for a, b in zip(e['from'], e['to'])]
            joints.setdefault(name.lower(), (i, c))
        groups = group_indices(model.get('groups'))
        # piezas clonables marcadas por nombre de cubo (p. ej. el cable del tirante)
        for i, e in enumerate(model['elements']):
            if 'clonable' in (e.get('name') or '').lower():
                groups.setdefault('clonable', []).append(i)
        lo = [min(min(e['from'][k], e['to'][k]) for e in model['elements']) for k in range(3)]
        hi = [max(max(e['from'][k], e['to'][k]) for e in model['elements']) for k in range(3)]

        cls = java_name(pid)
        java.append(f'    /** {src} -> models/block/piezas/{pid}.json */')
        java.append(f'    public static final class {cls} {{')
        java.append(f'        public static final String ID = "{pid}";')
        java.append(f'        public static final int ELEMENTS = {len(model["elements"])};')
        java.append(f'        public static final float[] MIN = {{{", ".join(f"{round(v, 4)}f" for v in lo)}}};')
        java.append(f'        public static final float[] MAX = {{{", ".join(f"{round(v, 4)}f" for v in hi)}}};')
        for name, (i, c) in joints.items():
            java.append(f'        /** centro del cubo "{name}" (elemento {i}) */')
            java.append(f'        public static final float[] {java_name(name)} = {{{", ".join(f"{round(v, 4)}f" for v in c)}}};')
        for name, idx in groups.items():
            java.append(f'        /** grupo "{name}" */')
            java.append(f'        public static final int[] GRUPO_{java_name(name)} = {{{", ".join(map(str, idx))}}};')
        java.append('    }')
        java.append('')
        print('pieza', src, '->', pid, '| cubos con nombre:', ', '.join(joints), '| grupos:', ', '.join(groups))

    os.makedirs(os.path.dirname(OUT_JAVA), exist_ok=True)
    with open(OUT_JAVA, 'w', encoding='utf-8') as fh:
        fh.write('package iberiancantilever.geometry;\n\n')
        fh.write('// GENERADO por tools/instalar_piezas.py a partir de las piezas de Blockbench. No editar a mano.\n')
        fh.write('// Coordenadas en pixeles de Blockbench, en el sistema de cada pieza.\n')
        fh.write('public final class PiezasDatos {\n')
        fh.write('    private PiezasDatos() {}\n\n')
        fh.write('\n'.join(java))
        fh.write('}\n')
    print('generado', OUT_JAVA)


if __name__ == '__main__':
    main()

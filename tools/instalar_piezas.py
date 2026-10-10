"""Instala las piezas de Blockbench del usuario en el mod.

Uso:  python tools/instalar_piezas.py

- Copia los .json de `new stuff/new json/` a assets/<mod>/models/block/piezas/<id>.json,
  arreglando las rutas de textura (`spanish_metal` -> `spanishcantilevers:block/spanish_metal`).
  Tambien acepta el .bbmodel (lo pasa a .json como hace Blockbench) por si el .json exportado
  no es el bueno.
- Copia los .png de `new stuff/new textures/` a assets/<mod>/textures/block/.
- Genera src/main/java/spanishcantilevers/geometry/PiezasDatos.java con los centros de los cubos
  con nombre y los grupos de cada pieza (con los giros de cada cubo ya aplicados: Blockbench guarda
  los cubos girados sin girar, junto a su pivote, y pueden salir en coordenadas raras como -13). El codigo de la mensula (cliente Y servidor) usa esas
  constantes; por eso hay que volver a ejecutar este script si cambias una pieza.
"""
import json
import math
import os
import re
import shutil

MOD = 'spanishcantilevers'
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
    # el brazo en arco de medio y exterior (sustituye al cable_holder_short; el de medio es este alargado)
    'cable_holder_convex.json': 'brazo_convexo',
    'cable_holder_all.json': 'sujetador',
    'cable_holder_all_short.json': 'sujetador_corto',
    'sustainig_metal_cable.json': 'tirante',
    'horizontal_insulator.json': 'aislador_horizontal',
    'vertical_insulator.json': 'aislador_vertical',
    # catenaria rigida de tunel: soportes de techo (normal y grande), de pared (corto y largo) y el perfil
    'Tunnel spanish canteliver left.json': 'tunel_izquierda',
    'Tunnel spanish canteliver center.json': 'tunel_centro',
    'Tunnel spanish canteliver right.json': 'tunel_derecha',
    'Tunnel spanish canteliver large left.json': 'tunel_grande_izquierda',
    'Tunnel spanish canteliver large center.json': 'tunel_grande_centro',
    # el .json de este se exporto mal (le falta la mitad): se usa el .bbmodel
    'Tunnel spanish canteliver large right.bbmodel': 'tunel_grande_derecha',
    'alternative_tunnel_canteliver_short.json': 'tunel_pared_corto',
    'alternative_tunnel_canteliver_long.json': 'tunel_pared_largo',
    'steel catenary tunnel fixed cable segment.json': 'perfil_rigido',
}


def fix_texture(ref):
    if ref.startswith('#') or ':' in ref:
        return ref
    return f'{MOD}:block/{os.path.basename(ref).replace("spanish_", "spanish_")}'


def desde_bbmodel(bb):
    """Un .bbmodel (formato Java Block) como el .json que exporta Blockbench."""
    texturas = bb.get('textures', [])
    model = {'textures': {str(i): os.path.splitext(t['name'])[0] for i, t in enumerate(texturas)}}
    if texturas:
        model['textures']['particle'] = model['textures']['0']
    elements = []
    indice = {}
    for i, e in enumerate(bb['elements']):
        indice[e['uuid']] = i
        rot = e.get('rotation') or [0, 0, 0]
        ejes = [k for k in range(3) if rot[k]]
        eje = ejes[0] if ejes else 1
        el = {'name': e.get('name'), 'from': e['from'], 'to': e['to'],
              'rotation': {'angle': rot[eje], 'axis': 'xyz'[eje], 'origin': e.get('origin', [8, 8, 8])}, 'faces': {}}
        if e.get('rescale'):
            el['rotation']['rescale'] = True
        if el['name'] == 'cube':
            el.pop('name')
        for lado, f in e.get('faces', {}).items():
            if f.get('texture') is None:
                continue
            cara = {'uv': f['uv'], 'texture': '#' + str(f['texture'])}
            if f.get('rotation'):
                cara['rotation'] = f['rotation']
            el['faces'][lado] = cara
        elements.append(el)
    model['elements'] = elements

    def grupo(nodo):
        if isinstance(nodo, str):
            return indice[nodo]
        return {'name': nodo.get('name', 'group'), 'origin': nodo.get('origin', [8, 8, 8]),
                'children': [grupo(c) for c in nodo.get('children', [])]}

    model['groups'] = [grupo(n) for n in bb.get('outliner', [])]
    return model


def esquinas(e):
    """Las 8 esquinas del cubo con su giro aplicado (como lo dibuja Minecraft)."""
    f, t = e['from'], e['to']
    pts = [[x, y, z] for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])]
    r = e.get('rotation')
    if not r or not r.get('angle'):
        return pts
    a = math.radians(r['angle'])
    c, s = math.cos(a), math.sin(a)
    o = r['origin']
    out = []
    for p in pts:
        x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
        if r['axis'] == 'x':
            y, z = y * c - z * s, y * s + z * c
        elif r['axis'] == 'y':
            x, z = x * c + z * s, -x * s + z * c
        else:
            x, y = x * c - y * s, x * s + y * c
        out.append([x + o[0], y + o[1], z + o[2]])
    return out


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
            shutil.copy(os.path.join(SRC_TEXTURES, f), os.path.join(OUT_TEXTURES, f.replace('spanish_', 'spanish_')))
            print('textura', f)

    java = []
    for src, pid in PIEZAS.items():
        with open(os.path.join(SRC_MODELS, src), encoding='utf-8') as fh:
            model = json.load(fh)
        if src.endswith('.bbmodel'):
            model = desde_bbmodel(model)
        model['textures'] = {k: fix_texture(v) for k, v in model.get('textures', {}).items()}
        model.pop('format_version', None)
        with open(os.path.join(OUT_MODELS, pid + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(model, fh, indent=1)

        joints = {}
        for i, e in enumerate(model['elements']):
            name = e.get('name')
            if not name:
                continue
            pts = esquinas(e)
            c = [sum(p[k] for p in pts) / len(pts) for k in range(3)]
            joints.setdefault(name.lower(), (i, c))
        groups = group_indices(model.get('groups'))
        # piezas clonables marcadas por nombre de cubo (p. ej. el cable del tirante)
        for i, e in enumerate(model['elements']):
            if 'clonable' in (e.get('name') or '').lower():
                groups.setdefault('clonable', []).append(i)
        todas = [p for e in model['elements'] for p in esquinas(e)]
        lo = [min(p[k] for p in todas) for k in range(3)]
        hi = [max(p[k] for p in todas) for k in range(3)]

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
        fh.write('package spanishcantilevers.geometry;\n\n')
        fh.write('// GENERADO por tools/instalar_piezas.py a partir de las piezas de Blockbench. No editar a mano.\n')
        fh.write('// Coordenadas en pixeles de Blockbench, en el sistema de cada pieza.\n')
        fh.write('public final class PiezasDatos {\n')
        fh.write('    private PiezasDatos() {}\n\n')
        fh.write('\n'.join(java))
        fh.write('}\n')
    print('generado', OUT_JAVA)


if __name__ == '__main__':
    main()

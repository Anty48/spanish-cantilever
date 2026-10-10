"""Dibuja los iconos de las ventanas en textures/gui/iconos.png (14 iconos de 16x16, siluetas de un color).

Uso:  python tools/generar_iconos.py

Todo queda dentro de un margen de 1 px (de 1 a 14), como los iconos de PNW.
  0-2   aislador de la mensula: horizontal (U de metal que abraza el aislador, cable encima),
        horizontal colgante (U invertida bajo el tubo, cable en el hueco de dentro) y vertical
  3-4   cable de soporte si / no (poste y barra de 2 px, cable de 1 px)
  5-7   pinza izquierda / centro / derecha del soporte de tunel (de techo, de frente)
  8-9   version techo / pared
  10-11 tamano del de techo: normal / grande (marco de lado a lado)
  12-13 tamano del de pared: brazo corto / largo (de lado, con la pared a la derecha)
Los del soporte de tunel tienen el mismo dibujo que el sprite del objeto. El de techo se ve de frente
como lo ve quien lo pone: la pinza izquierda, a la izquierda.
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICONOS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spanishcantilevers', 'textures', 'gui', 'iconos.png')
COLOR = (68, 32, 0, 255)
MIN, MAX = 1, 14  # margen de 1 px


def rect(px, x0, y0, x1, y1):
    """Rellena de (x0, y0) a (x1, y1), ambos incluidos, recortado al margen."""
    for y in range(max(MIN, y0), min(MAX, y1) + 1):
        for x in range(max(MIN, x0), min(MAX, x1) + 1):
            px.add((x, y))


def aislador(px, y):
    """Aislador horizontal (un rollo visto de lado) de x 5 a 10, filas y..y+2, con sus faldas."""
    rect(px, 5, y, 10, y)
    for x in (5, 7, 8, 10):
        px.add((x, y + 1))
    rect(px, 5, y + 2, 10, y + 2)


def aislador_encima():
    px = set()
    rect(px, 7, 2, 8, 3)  # el cable, encima del aislador
    aislador(px, 4)
    rect(px, 3, 3, 3, 7)  # la U de metal que lo abraza
    rect(px, 12, 3, 12, 7)
    rect(px, 3, 7, 12, 7)
    rect(px, 7, 8, 8, 9)  # pie hasta el tubo
    rect(px, 1, 10, 14, 11)  # tubo
    return px


def aislador_colgante():
    px = set()
    rect(px, 1, 2, 14, 3)  # tubo
    rect(px, 7, 4, 8, 5)  # pie
    rect(px, 3, 6, 12, 6)  # la U invertida
    rect(px, 3, 6, 3, 12)
    rect(px, 12, 6, 12, 12)
    aislador(px, 7)
    rect(px, 7, 11, 8, 12)  # el cable, en el hueco de dentro de la U, bajo el aislador
    return px


def aislador_vertical():
    px = set()
    rect(px, 7, 1, 8, 2)  # cable
    for y in (5, 7, 9, 11):
        rect(px, 5, y, 10, y)  # faldas
    rect(px, 7, 4, 8, 12)  # cuerpo
    rect(px, 1, 13, 14, 14)  # tubo
    return px


def cable_de_soporte(con_cable):
    """Perfil de la mensula: poste y barra principal de 2 px y el cable de soporte (1 px) del poste al codo."""
    px = set()
    rect(px, 1, 1, 2, 14)  # poste
    rect(px, 9, 4, 14, 5)  # horizontal, del codo al final
    for i in range(7):  # diagonal de 2 px del poste (abajo) al codo (arriba)
        x, y = 3 + i, 11 - i
        rect(px, x, y - 1, x, y)
    if con_cable:
        rect(px, 3, 4, 8, 4)  # cable de soporte, horizontal a la altura de la barra
    return px


def techo(desplazamiento=0, grande=False):
    """El de techo de frente: la pinza y el perfil movidos {desplazamiento} px (negativo, a la izquierda)."""
    px = set()
    rect(px, 7, 1, 8, 5)  # varilla al techo
    x0, x1 = (1, 14) if grande else (2, 13)
    rect(px, x0, 6, x1, 7)  # travesano de arriba
    rect(px, x0, 11, x1, 12)  # travesano de abajo
    for x in (x0, x1) if grande else (x0 + 1, x1 - 1):
        rect(px, x, 8, x, 10)  # postes
    c = 8 + desplazamiento
    rect(px, c - 2, 13, c + 1, 13)  # pinza
    rect(px, c - 3, 14, c + 2, 14)  # perfil
    return px


def pared(largo=False):
    """El de pared de lado: pared a la derecha, placa, aislador y brazo hasta la pinza con el perfil."""
    px = set()
    rect(px, 13, 1, 14, 14)  # la pared
    rect(px, 11, 4, 12, 10)  # placa
    punta = 1 if largo else 4
    rect(px, punta, 7, 10, 8)  # brazo
    rect(px, 8, 5, 9, 9)  # aislador
    c = punta + 1
    rect(px, c - 2, 9, c + 1, 9)  # pinza
    rect(px, c - 3, 10, c + 2, 10)  # perfil
    return px


ICONOS_TODOS = {
    0: aislador_encima(),
    1: aislador_colgante(),
    2: aislador_vertical(),
    3: cable_de_soporte(True),
    4: cable_de_soporte(False),
    5: techo(-4),
    6: techo(0),
    7: techo(4),
    8: techo(0),
    9: pared(),
    10: techo(0),
    11: techo(0, grande=True),
    12: pared(),
    13: pared(largo=True),
}


def main():
    hoja = Image.new('RGBA', (16 * len(ICONOS_TODOS), 16), (0, 0, 0, 0))
    for indice, px in ICONOS_TODOS.items():
        for x, y in px:
            hoja.putpixel((indice * 16 + x, y), COLOR)
    hoja.save(ICONOS)
    print('generado', ICONOS)


if __name__ == '__main__':
    main()

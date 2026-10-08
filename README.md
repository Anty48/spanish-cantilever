# Iberian Cantilevers for Pantographs & Wires

Addon para **Minecraft 1.20.1 (Forge)** que añade **ménsulas de catenaria ibéricas** (estilo Renfe/ADIF CR-160)
a [Create: Pantographs & Wires](https://www.curseforge.com/minecraft/mc-mods/create-pantographs-and-wires) de MrJulsen.
PNW es dependencia obligatoria: se usan sus postes, sus cables y su sistema de colocación.

> Estado: **en desarrollo** (v0.1). Ménsula, postes ibéricos, acero ibérico y recetas funcionando; los cables de PNW se enganchan.

## La ménsula

Un solo bloque, que se coloca en la cara de un poste de PNW (16 direcciones, como la ménsula de PNW).

Se configura igual que la ménsula de PNW: **clic derecho al aire con el objeto** abre la ventana de ajustes
(la misma de PNW, con vista previa) y lo escogido se guarda en el objeto; cada ménsula que coloques sale así.
Una ménsula ya puesta se cambia con **clic derecho con la mano vacía**.

| Opción | Valores |
|---|---|
| Anchura | distancia del poste al centro de la vía, de 1,5 a 6,5 bloques (como en PNW) |
| Altura del soporte *(avanzado)* | cuánto baja la diagonal bajo la barra (en interior con anchura 1,5 no se usa) |
| Altura catenaria *(avanzado)* | hilo de contacto por debajo del bloque, hasta 0,75 (como en PNW) |
| Desplazamiento Y *(avanzado)* | baja toda la ménsula medio bloque (como en PNW) |
| Aislador del sustentador | tipo 1 (cable encima), 2 (cable colgando), 3 (vertical) |
| Hilo de contacto (zigzag) | interior / medio / exterior |
| Cable de soporte | sí / no |

**Tirante diagonal** (anchura 3 o más): con la bobina de PNW en modo *cable de soporte*, clic en la ménsula y
luego en un bloque del poste de 2 a 5 bloques por encima de ella (o al revés): cada altura da un triángulo
distinto. Es un solo hilo recto que entra en la barra horizontal, justo antes del aislador del sustentador
(así se aleja del poste cuanto más ancha es la ménsula), y en la cara del poste que mira a la ménsula. Si la ménsula se
reconfigura el tirante la sigue (y si baja de anchura 3 se quita); al romper la ménsula o ese bloque del poste
el cable vuelve a la bobina. `/mensula tirante` (operador) monta una prueba.

## Postes y materiales

- **Postes ibéricos de celosía** (plano, plano diagonal y cuadrado): los de PNW con acero ibérico. Se oxidan solos
  (normal → expuesto → erosionado → oxidado), un panal los encera y un hacha los rasca, como en PNW.
  No tienen versión galvanizada. Recetas: plano = 5 tiras de hierro de PNW + 1 acero ibérico (6 postes);
  cuadrado = 6 tiras + 2 varillas de hierro + 1 acero ibérico (6 postes).
- **Acero ibérico**: lingote de hierro + tinte negro.
- **Ménsula**: montaje secuenciado de Create, como la de PNW pero con acero ibérico: haz de varillas de
  hierro y, dos veces, acero ibérico + aislador marrón + prensa.

## Catenaria rígida de túnel

- **Soporte de túnel ibérico**: se cuelga del techo y gira en 16 direcciones (se pone mirando a lo largo de
  la vía y queda cruzado sobre ella). Su ventana escoge dónde agarra el perfil (inner / center / outer, para
  el zigzag) y cuánto cuelga (altura, de 0 a 1 bloque). Como la ménsula: **clic derecho al aire con el
  objeto** lo configura antes de ponerlo, y **clic derecho con la mano vacía** cambia uno ya puesto (los
  cables ya tendidos se recolocan solos).
- **Tamaño general** (100 %, 125 % o 150 %): hace más grande todo el soporte y el perfil rígido que sale de él.
  El de techo crece alrededor de la pinza (el hilo de contacto no se mueve y la varilla sigue llegando al
  techo); el de pared, desde la pared (el brazo se alarga). El perfil de cada tramo toma el tamaño del soporte
  donde se hizo el primer clic; se pueden mezclar tamaños.
- **Haz de perfiles de catenaria rígida**: se tiende de soporte a soporte como la bobina de PNW y se va
  gastando por metros (64 m por haz; al romper un tramo se recuperan sus metros en otro haz). Para PNW es un
  cable más de su catenaria, así que el pantógrafo lo toca. Si en un soporte se juntan dos tramos que no van
  rectos, el perfil se dibuja en trozos rectos un poco girados que hacen la curva (el hilo que toca el
  pantógrafo sigue yendo recto de soporte a soporte).
- **Transición**: la catenaria normal de PNW también se engancha al soporte de túnel (el hilo de contacto en
  la pinza y el sustentador en el techo).
- Recetas: soporte = 2 aceros ibéricos + 1 aislador marrón (2 soportes); haz de perfiles = 6 aceros ibéricos.
- `/mensula tunel [soportes]` (operador): monta un túnel de prueba (con curva al fondo) y la transición desde
  una ménsula.

## Cómo está hecho

- Las piezas se modelan en Blockbench (`new stuff/new json/`) y `tools/instalar_piezas.py` las instala en el mod
  y genera `geometry/PiezasDatos.java` con los puntos de enganche (cubos con nombre).
- `geometry/MensulaLayout` coloca las piezas y estira las barras; `client/MensulaBakedModel` las dibuja.
- `tools/generar_postes.py` genera los postes ibéricos (modelos, texturas, recetas, etiquetas y nombres).
- `/mensula linea [postes]` (operador): monta una línea de prueba con postes, ménsulas en zigzag y cable.
- `/mensula escaparate` (operador): monta las escenas de escaparate de la documentación (todas las variantes a la
  vista: línea con tirantes, anchuras, postes, vitrinas de soportes de túnel, curva, túnel con transición y pantógrafos).
- `Guia_piezas_Blockbench.pdf`: guía de modelado de las piezas.

## Compilar y probar

1. Poner los jars de dependencias en `libs/` (ver [libs/README.md](libs/README.md)).
2. `.\gradlew build` compila; `.\gradlew runClient` abre el juego.
3. Prueba automática: crear el archivo `run/autotest.flag` y lanzar `runClient`. Monta un mundo plano con todas las
   variantes, guarda capturas en `run/screenshots/autotest_*.png` y cierra el juego. Si el archivo trae texto
   (prefijos separados por comas, p. ej. `tirante,diagonal_`) solo se hacen esas capturas.

## Licencia y créditos

GPL-3.0, igual que Pantographs & Wires. Gracias a **MrJulsen** por PNW y DragonLib.

## Próximos pasos

- [ ] Probar la colocación en diagonal (16 direcciones) con cables.
- [ ] Créditos finales a MrJulsen (PNW) en la página del mod.

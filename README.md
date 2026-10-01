# Spanish Cantilevers for Pantographs & Wires

Addon para **Minecraft 1.20.1 (Forge)** que añade **ménsulas de catenaria españolas** (estilo Renfe/ADIF CR-160)
a [Create: Pantographs & Wires](https://www.curseforge.com/minecraft/mc-mods/create-pantographs-and-wires) de MrJulsen.
PNW es dependencia obligatoria: se usan sus postes, sus cables y su sistema de colocación.

> Estado: **en desarrollo** (v0.1). La ménsula ya se dibuja en el juego; quedan ajustes de geometría.

## La ménsula

Un solo bloque, que se coloca en la cara de un poste de PNW (16 direcciones, como la ménsula de PNW).

| Acción sobre el bloque | Cambia |
|---|---|
| Clic derecho con la mano vacía | Modo: interior / medio / exterior (descentramiento del hilo de contacto) |
| Agachado + clic derecho con la mano vacía | Aislador de arriba: tipo 1 (cable encima), 2 (cable colgando), 3 (vertical) |
| Clic derecho con un palo | Alcance: distancia del poste al centro de la vía (1 a 4 bloques) |
| Agachado + clic derecho con un palo | Tirante sí / no |

## Cómo está hecho

- Las piezas se modelan en Blockbench (`new stuff/new json/`) y `tools/instalar_piezas.py` las instala en el mod
  y genera `geometry/PiezasDatos.java` con los puntos de enganche (cubos con nombre).
- `geometry/MensulaLayout` coloca las piezas y estira las barras; `client/MensulaBakedModel` las dibuja.
- `Guia_piezas_Blockbench.pdf`: guía de modelado de las piezas.

## Compilar y probar

1. Poner los jars de dependencias en `libs/` (ver [libs/README.md](libs/README.md)).
2. `.\gradlew build` compila; `.\gradlew runClient` abre el juego.
3. Prueba automática: crear el archivo `run/autotest.flag` y lanzar `runClient`. Monta un mundo plano con todas las
   variantes, guarda capturas en `run/screenshots/autotest_*.png` y cierra el juego.

## Licencia y créditos

GPL-3.0, igual que Pantographs & Wires. Gracias a **MrJulsen** por PNW y DragonLib.

## Próximos pasos

- [ ] Uniones entre tramos de la barra principal: ahora se solapan y hacen *Z-fighting*.
- [ ] El tirante (varilla de soporte) siempre horizontal.
- [ ] Menos diagonal y más horizontal en la barra principal.
- [ ] Nuevo modelo para el ítem.
- [ ] Probar las 9 variantes (3 tipos × 3 modos) con la prueba automática.
- [ ] Probar los cables de PNW enganchados y la colocación en diagonal.

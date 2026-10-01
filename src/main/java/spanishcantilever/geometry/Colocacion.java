package spanishcantilever.geometry;

import org.joml.Matrix4f;

/**
 * Una pieza de Blockbench colocada en la mensula.
 *
 * @param pieza     id de la pieza (models/block/piezas/&lt;id&gt;.json)
 * @param elementos indices de los cubos de la pieza a dibujar (null = todos)
 * @param matriz    de pixeles de la pieza a pixeles de la mensula
 */
public record Colocacion(String pieza, int[] elementos, Matrix4f matriz) {
}

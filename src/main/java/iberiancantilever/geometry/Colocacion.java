package iberiancantilever.geometry;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Una pieza de Blockbench colocada en la mensula.
 *
 * @param pieza     id de la pieza (models/block/piezas/&lt;id&gt;.json)
 * @param elementos indices de los cubos de la pieza a dibujar (null = todos)
 * @param matriz    de pixeles de la pieza a pixeles de la mensula
 * @param inglete   corte a inglete de los extremos del tubo (null = sin corte)
 * @param variacion semilla para desplazar la textura a lo largo de la pieza, para que los tramos
 *                  repetidos no se vean todos iguales (0 = textura tal cual)
 */
public record Colocacion(String pieza, int[] elementos, Matrix4f matriz, @Nullable Inglete inglete, int variacion) {

    public Colocacion(String pieza, int[] elementos, Matrix4f matriz) {
        this(pieza, elementos, matriz, null, 0);
    }

    public Colocacion(String pieza, int[] elementos, Matrix4f matriz, @Nullable Inglete inglete) {
        this(pieza, elementos, matriz, inglete, 0);
    }

    /** La misma pieza movida {@code dy} px en vertical (el corte a inglete se mueve con ella). */
    public Colocacion desplazada(float dy) {
        Matrix4f t = new Matrix4f().translation(0, dy, 0);
        Inglete movido = inglete == null ? null
                : new Inglete(new Matrix4f(t).mul(inglete.linea()), inglete.largo(), inglete.tanInicio(), inglete.tanFin());
        return new Colocacion(pieza, elementos, new Matrix4f(t).mul(matriz), movido, variacion);
    }

    /** Pixeles de la pieza -> pixeles de la mensula (posicion + corte a inglete). */
    public void aMensula(Vector3f p) {
        matriz.transformPosition(p);
        if (inglete != null) {
            inglete.cortar(p);
        }
    }

    /**
     * Corte a inglete de un tubo recto: los vertices de cada extremo se mueven a lo largo del tubo
     * hasta el plano bisector de la union, asi dos tubos que se juntan en un codo encajan sin
     * solaparse (sin Z-fighting) ni dejar hueco.
     *
     * @param linea     sistema del tubo (X a lo largo del eje, Y perpendicular en el plano de la mensula)
     * @param largo     longitud del eje
     * @param tanInicio desplazamiento en X por px de Y en el extremo x = 0
     * @param tanFin    desplazamiento en X por px de Y en el extremo x = largo
     */
    public record Inglete(Matrix4f linea, Matrix4f lineaInversa, float largo, float tanInicio, float tanFin) {
        private static final float EPS = 1e-3f;

        public Inglete(Matrix4f linea, float largo, float tanInicio, float tanFin) {
            this(linea, new Matrix4f(linea).invert(), largo, tanInicio, tanFin);
        }

        void cortar(Vector3f p) {
            Vector3f q = lineaInversa.transformPosition(new Vector3f(p));
            if (Math.abs(q.x) < EPS) {
                q.x = q.y * tanInicio;
            } else if (Math.abs(q.x - largo) < EPS) {
                q.x = largo + q.y * tanFin;
            } else {
                return;
            }
            linea.transformPosition(q);
            p.set(q);
        }
    }
}

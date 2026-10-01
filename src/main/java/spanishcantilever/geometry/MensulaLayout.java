package spanishcantilever.geometry;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import spanishcantilever.block.ModoZigzag;
import spanishcantilever.block.TipoAislador;

/**
 * Coloca las piezas de Blockbench para formar la mensula y calcula por donde pasan los cables.
 * Sin dependencias del cliente: el servidor lo usa para los puntos de enganche de los cables.
 *
 * <p>Sistema "mensula" en pixeles (vista South de la guia): X = distancia desde la cara del bloque
 * que toca el poste hacia la via, Y = altura desde la base del bloque, Z = a lo largo de la via
 * (8 = centro). Todo esta en el plano Z = 8; el giro hacia la direccion real lo hace el modelo.
 */
public final class MensulaLayout {
    public static final float Z = 8f;
    /** Altura del eje de la barra horizontal. */
    public static final float BARRA_Y = 8f;
    /** Media altura de la barra principal (Main_steel_bar mide 2 px). */
    public static final float MEDIA_BARRA = 1f;
    /** Altura del hilo de contacto (igual en los tres modos, para que los postes seguidos casen). */
    public static final float CONTACTO_Y = -10f;
    public static final float ANGULO_DIAGONAL = 35f;
    /** El codo (diagonal -> horizontal) queda estos px antes del sustentador. */
    public static final float CODO_ANTES_SUSTENTADOR = 4f;
    /** Cuanto sigue la horizontal despues del sustentador en cada modo. */
    public static final float FIN_INTERIOR = 6f;
    public static final float FIN_MEDIO = 6f;
    public static final float FIN_EXTERIOR = 10f;
    /** Altura de la placa del tirante sobre la barra horizontal. */
    public static final float TIRANTE_SOBRE_BARRA = 10f;
    public static final float LARGO_BAJADA_MEDIO = 4f;
    public static final float BAJADA_MIN = 2f;
    public static final float LARGO_PERFORADA = 7f;
    /** Distancia del centro del wall_joint del brazo a su cara de arriba (la que toca el tubo). */
    public static final float GANCHO_BAJO_TOPE = 1f;

    public record Resultado(List<Colocacion> piezas, Vector3f contacto, Vector3f sustentador) {
    }

    private MensulaLayout() {
    }

    /** X del sustentador (centro de la via): la mensula ocupa su bloque y llega al centro del bloque n. */
    public static float sustentadorX(int alcance) {
        return 16f * alcance + 8f;
    }

    /**
     * @param hueco distancia en px entre la cara del bloque y la cara del poste (postes finos dejan hueco)
     */
    public static Resultado calcular(TipoAislador tipo, ModoZigzag modo, boolean tirante, int alcance, float hueco) {
        List<Colocacion> out = new ArrayList<>();
        float r = sustentadorX(alcance);
        float mastil = -hueco;
        Vector2f contacto = new Vector2f(r + modo.offsetPx(), CONTACTO_Y);

        // --- tubo principal: diagonal desde el poste hasta el codo, luego horizontal ---
        Vector2f codo = new Vector2f(r - CODO_ANTES_SUSTENTADOR, BARRA_Y);
        float tan = (float) Math.tan(Math.toRadians(ANGULO_DIAGONAL));
        Vector2f ancla = new Vector2f(mastil + 0.5f, BARRA_Y - (codo.x - mastil) * tan);
        barra(out, ancla, prolongar(ancla, codo, 0.8f));
        placa(out, mastil, ancla.y);

        float finHorizontal = r + switch (modo) {
            case INTERIOR -> FIN_INTERIOR;
            case MEDIO -> FIN_MEDIO;
            case EXTERIOR -> FIN_EXTERIOR;
        };
        float finBarra = modo == ModoZigzag.INTERIOR ? finHorizontal : finHorizontal + 1f;
        barra(out, new Vector2f(codo.x - 1f, BARRA_Y), new Vector2f(finBarra, BARRA_Y));

        Vector3f sustentador = aislador(out, tipo, r);

        // --- brazo de atirantado ---
        Vector2f cable;
        switch (modo) {
            case INTERIOR -> {
                // cuelga de la diagonal y apunta hacia la via
                float caida = MEDIA_BARRA / (float) Math.cos(Math.toRadians(ANGULO_DIAGONAL)) + GANCHO_BAJO_TOPE;
                Vector2f dir = new Vector2f(codo).sub(ancla).normalize();
                float largoDiag = codo.distance(ancla);
                float largo = largoBrazo(PiezasDatos.BRAZO_CORTO.WALL_JOINT, PiezasDatos.BRAZO_CORTO.CABLE_JOINT);
                float t = resolver(0f, largoDiag, s -> puntoGancho(ancla, dir, s, caida).distance(contacto) - largo);
                Vector2f gancho = puntoGancho(ancla, dir, t, caida);
                cable = brazo(out, false, gancho, contacto, false);
            }
            case MEDIO -> {
                // diagonal corta hacia abajo + pieza perforada; el brazo largo cuelga de la perforada
                Vector2f dir45 = new Vector2f(0.7071f, -0.7071f);
                Vector2f inicio = new Vector2f(finHorizontal, BARRA_Y);
                Vector2f finBajada = new Vector2f(dir45).mul(LARGO_BAJADA_MEDIO).add(inicio);
                barra(out, prolongar(finBajada, inicio, 0.8f), finBajada);
                float largo = largoBrazo(PiezasDatos.BRAZO_LARGO.WALL_JOINT, PiezasDatos.BRAZO_LARGO.CABLE_JOINT);
                float caida = 0.75f / 0.7071f + GANCHO_BAJO_TOPE;
                int n = 1;
                float s = Float.NaN;
                for (; n <= 5; n++) {
                    float max = LARGO_PERFORADA * n;
                    s = resolver(0f, max, x -> puntoGancho(finBajada, dir45, x, caida).distance(contacto) - largo);
                    if (s < max - 0.01f) {
                        break;
                    }
                }
                n = Math.min(n, 5);
                perforadaDiagonal(out, finBajada, -45f, n);
                cable = brazo(out, true, puntoGancho(finBajada, dir45, s, caida), contacto, true);
            }
            default -> {
                // diagonal corta + perforada diagonal + perforada vertical; brazo corto girado 180
                Vector2f dir45 = new Vector2f(0.7071f, -0.7071f);
                Vector2f inicio = new Vector2f(finHorizontal, BARRA_Y);
                float largo = largoBrazo(PiezasDatos.BRAZO_CORTO.WALL_JOINT, PiezasDatos.BRAZO_CORTO.CABLE_JOINT);
                float caida = GANCHO_BAJO_TOPE;
                int verticales = 1;
                float bajada = BAJADA_MIN;
                for (; verticales <= 4; verticales++) {
                    final int nv = verticales;
                    float b = resolver(BAJADA_MIN, 40f, x -> ganchoExterior(inicio, dir45, x, nv, caida).distance(contacto) - largo);
                    if (Math.abs(ganchoExterior(inicio, dir45, b, nv, caida).distance(contacto) - largo) < 0.5f) {
                        bajada = b;
                        break;
                    }
                }
                verticales = Math.min(verticales, 4);
                Vector2f finBajada = new Vector2f(dir45).mul(bajada).add(inicio);
                barra(out, prolongar(finBajada, inicio, 0.8f), finBajada);
                Vector2f finPerforada = perforadaDiagonal(out, finBajada, -45f, 1);
                perforadaVertical(out, finPerforada, verticales);
                cable = brazo(out, false, ganchoExterior(inicio, dir45, bajada, verticales, caida), contacto, true);
            }
        }

        if (tirante) {
            float y = BARRA_Y + TIRANTE_SOBRE_BARRA;
            placa(out, mastil, y);
            varilla(out, new Vector2f(mastil + 0.5f, y), new Vector2f(codo.x, BARRA_Y + MEDIA_BARRA));
        }

        return new Resultado(out, new Vector3f(cable.x, cable.y, Z), sustentador);
    }

    // ------------------------------------------------------------------ piezas

    /** Tubo estirable entre dos puntos de su eje: remate + tramos duplicados + remate. */
    private static void barra(List<Colocacion> out, Vector2f a, Vector2f b) {
        float len = a.distance(b);
        Matrix4f linea = linea(a, b);
        if (len <= 2f) {
            out.add(new Colocacion(PiezasDatos.BARRA.ID, null, new Matrix4f(linea).scale(len / 7f, 1, 1).translate(0, -1, -1)));
            return;
        }
        out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_ATTACH_SEGMENT_END_1, new Matrix4f(linea).translate(0, -1, -1)));
        float medio = len - 2f;
        int n = Math.max(1, Math.round(medio / 5f));
        float tramo = medio / n;
        for (int i = 0; i < n; i++) {
            out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_DUPLICATING_SEGMENT,
                    new Matrix4f(linea).translate(1f + i * tramo, 0, 0).scale(tramo / 5f, 1, 1).translate(-1, -1, -1)));
        }
        out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_ATTACH_SEGMENT_END_2, new Matrix4f(linea).translate(len - 7f, -1, -1)));
    }

    /** Varilla del tirante: el cubo clonable (4 px) repetido de a hasta b. */
    private static void varilla(List<Colocacion> out, Vector2f a, Vector2f b) {
        float len = a.distance(b);
        Matrix4f linea = linea(a, b);
        float[] c = PiezasDatos.TIRANTE.CABLE_PIEZA_CLONABLE_P_4;
        float x0 = PiezasDatos.TIRANTE.MAX[0] - 4f;
        int n = Math.max(1, Math.round(len / 4f));
        float tramo = len / n;
        for (int i = 0; i < n; i++) {
            out.add(new Colocacion(PiezasDatos.TIRANTE.ID, PiezasDatos.TIRANTE.GRUPO_CLONABLE,
                    new Matrix4f(linea).translate(i * tramo, 0, 0).scale(tramo / 4f, 1, 1).translate(-x0, -c[1], -c[2])));
        }
    }

    /** Placa atornillada al poste (la del tirante sin la varilla); su cara trasera en x = mastil. */
    private static void placa(List<Colocacion> out, float mastil, float y) {
        float[] p = PiezasDatos.TIRANTE.JOINT_WALL_POINT;
        float trasera = PiezasDatos.TIRANTE.MIN[0];
        out.add(new Colocacion(PiezasDatos.TIRANTE.ID, sinClonable(),
                new Matrix4f().translate(mastil - trasera, y - p[1], Z - p[2])));
    }

    private static Vector2f perforadaDiagonal(List<Colocacion> out, Vector2f inicio, float grados, int copias) {
        Matrix4f base = new Matrix4f().translate(inicio.x, inicio.y, Z).rotateZ((float) Math.toRadians(grados));
        float[] c = PiezasDatos.PERFORADA_DIAGONAL.ATTACHING_CENTER_SEGMENT;
        for (int i = 0; i < copias; i++) {
            out.add(new Colocacion(PiezasDatos.PERFORADA_DIAGONAL.ID, null,
                    new Matrix4f(base).translate(LARGO_PERFORADA * i, -c[1], -c[2])));
        }
        float rad = (float) Math.toRadians(grados);
        return new Vector2f((float) Math.cos(rad), (float) Math.sin(rad)).mul(LARGO_PERFORADA * copias).add(inicio);
    }

    private static void perforadaVertical(List<Colocacion> out, Vector2f arriba, int copias) {
        float[] c = PiezasDatos.PERFORADA_VERTICAL.TOP_JOINT;
        for (int i = 0; i < copias; i++) {
            out.add(new Colocacion(PiezasDatos.PERFORADA_VERTICAL.ID, null,
                    new Matrix4f().translate(arriba.x - c[0], arriba.y - LARGO_PERFORADA * i, Z - c[2])));
        }
    }

    /**
     * Brazo de atirantado colgado en {@code gancho} (centro de su wall_joint) y girado para que su
     * cable_joint caiga sobre {@code objetivo}. Devuelve donde queda de verdad el cable_joint.
     */
    private static Vector2f brazo(List<Colocacion> out, boolean largo, Vector2f gancho, Vector2f objetivo, boolean haciaPoste) {
        float[] e = largo ? PiezasDatos.BRAZO_LARGO.WALL_JOINT : PiezasDatos.BRAZO_CORTO.WALL_JOINT;
        float[] w = largo ? PiezasDatos.BRAZO_LARGO.CABLE_JOINT : PiezasDatos.BRAZO_CORTO.CABLE_JOINT;
        String id = largo ? PiezasDatos.BRAZO_LARGO.ID : PiezasDatos.BRAZO_CORTO.ID;
        float vx = (w[0] - e[0]) * (haciaPoste ? -1 : 1);
        float vy = w[1] - e[1];
        float giro = (float) (Math.atan2(objetivo.y - gancho.y, objetivo.x - gancho.x) - Math.atan2(vy, vx));
        Matrix4f m = new Matrix4f().translate(gancho.x, gancho.y, Z).rotateZ(giro);
        if (haciaPoste) {
            m.rotateY((float) Math.PI);
        }
        m.translate(-e[0], -e[1], -e[2]);
        out.add(new Colocacion(id, null, m));
        float cos = (float) Math.cos(giro), sin = (float) Math.sin(giro);
        return new Vector2f(gancho.x + vx * cos - vy * sin, gancho.y + vx * sin + vy * cos);
    }

    /** Aislador de arriba sobre la horizontal en x = r. Devuelve por donde pasa el sustentador. */
    private static Vector3f aislador(List<Colocacion> out, TipoAislador tipo, float r) {
        float arriba = BARRA_Y + MEDIA_BARRA;
        float abajo = BARRA_Y - MEDIA_BARRA;
        switch (tipo) {
            case TIPO1 -> {
                float[] c = PiezasDatos.AISLADOR_HORIZONTAL.CABLE_JOINT;
                float base = PiezasDatos.AISLADOR_HORIZONTAL.MIN[1];
                out.add(new Colocacion(PiezasDatos.AISLADOR_HORIZONTAL.ID, null,
                        new Matrix4f().translate(r - c[0], arriba - base, Z - c[2])));
                return new Vector3f(r, arriba + c[1] - base, Z);
            }
            case TIPO2 -> {
                // la misma pieza del reves (girada 180 grados sobre X), colgando bajo la barra
                float[] c = PiezasDatos.AISLADOR_HORIZONTAL.CABLE_JOINT;
                float base = PiezasDatos.AISLADOR_HORIZONTAL.MIN[1];
                out.add(new Colocacion(PiezasDatos.AISLADOR_HORIZONTAL.ID, null,
                        new Matrix4f().translate(r, abajo, Z).rotateX((float) Math.PI).translate(-c[0], -base, -c[2])));
                return new Vector3f(r, abajo - (c[1] - base), Z);
            }
            default -> {
                float[] c = PiezasDatos.AISLADOR_VERTICAL.CABLE_JOINT;
                float base = PiezasDatos.AISLADOR_VERTICAL.MIN[1];
                out.add(new Colocacion(PiezasDatos.AISLADOR_VERTICAL.ID, null,
                        new Matrix4f().translate(r - c[0], arriba - base, Z - c[2])));
                return new Vector3f(r, arriba + c[1] - base, Z);
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Matriz que lleva el eje X de una pieza sobre el segmento a-b (en el plano Z = 8). */
    private static Matrix4f linea(Vector2f a, Vector2f b) {
        return new Matrix4f().translate(a.x, a.y, Z).rotateZ((float) Math.atan2(b.y - a.y, b.x - a.x));
    }

    /** b alargado {@code extra} px en la direccion a->b (para que las uniones se solapen). */
    private static Vector2f prolongar(Vector2f a, Vector2f b, float extra) {
        Vector2f d = new Vector2f(b).sub(a).normalize();
        return d.mul(extra).add(b);
    }

    private static Vector2f puntoGancho(Vector2f origen, Vector2f dir, float s, float caida) {
        return new Vector2f(dir).mul(s).add(origen).sub(0, caida);
    }

    private static Vector2f ganchoExterior(Vector2f inicio, Vector2f dir45, float bajada, int verticales, float caida) {
        Vector2f finPerforada = new Vector2f(dir45).mul(bajada + LARGO_PERFORADA).add(inicio);
        return new Vector2f(finPerforada.x, finPerforada.y - LARGO_PERFORADA * verticales - caida);
    }

    private static float largoBrazo(float[] e, float[] w) {
        return (float) Math.hypot(w[0] - e[0], w[1] - e[1]);
    }

    private static int[] sinClonable() {
        int[] out = new int[PiezasDatos.TIRANTE.ELEMENTS - PiezasDatos.TIRANTE.GRUPO_CLONABLE.length];
        int k = 0;
        outer:
        for (int i = 0; i < PiezasDatos.TIRANTE.ELEMENTS; i++) {
            for (int c : PiezasDatos.TIRANTE.GRUPO_CLONABLE) {
                if (c == i) {
                    continue outer;
                }
            }
            out[k++] = i;
        }
        return out;
    }

    /**
     * Primera raiz de f en [a, b] (barrido + biseccion). Si no hay cambio de signo devuelve el
     * punto donde |f| es minimo, para que la mensula se dibuje igualmente.
     */
    static float resolver(float a, float b, Funcion f) {
        int pasos = 200;
        float prevX = a;
        float prev = f.en(a);
        float mejorX = a;
        float mejor = Math.abs(prev);
        for (int i = 1; i <= pasos; i++) {
            float x = a + (b - a) * i / pasos;
            float v = f.en(x);
            if (Math.abs(v) < mejor) {
                mejor = Math.abs(v);
                mejorX = x;
            }
            if (Math.signum(v) != Math.signum(prev)) {
                float lo = prevX, hi = x, flo = prev;
                for (int k = 0; k < 30; k++) {
                    float mid = (lo + hi) / 2;
                    float fm = f.en(mid);
                    if (Math.signum(fm) == Math.signum(flo)) {
                        lo = mid;
                        flo = fm;
                    } else {
                        hi = mid;
                    }
                }
                return (lo + hi) / 2;
            }
            prevX = x;
            prev = v;
        }
        return mejorX;
    }

    @FunctionalInterface
    interface Funcion {
        float en(float x);
    }
}

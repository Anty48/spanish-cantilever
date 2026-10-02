package iberiancantilever.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import iberiancantilever.block.ModoZigzag;
import iberiancantilever.block.TipoAislador;

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
    /** Modos medio y exterior: inclinacion de la diagonal corta, si cabe (lo que baja lo da {@link Ajustes#altura()}). */
    public static final float ANGULO_DIAGONAL = 30f;
    /** Modos medio y exterior: el codo queda en esta fraccion del camino poste -> sustentador, si la diagonal no lo limita antes. */
    public static final float FRACCION_CODO = 0.55f;
    /** En los aisladores horizontales el sustentador va medio px mas arriba: parece apoyado en ellos. */
    public static final float APOYO_SUSTENTADOR = 0.5f;
    /** Los aisladores horizontales (tipos 1 y 2) se dibujan mas grandes que la pieza de Blockbench. */
    public static final float ESCALA_AISLADOR_HORIZONTAL = 1.5f;
    /** Modo interior con alcance corto: inclinacion de la diagonal (la de la foto de referencia). */
    public static final float ANGULO_INTERIOR = 35f;
    /** Modo interior con la altura del soporte: la diagonal queda entre estas inclinaciones. */
    public static final float ANGULO_INTERIOR_MIN = 12f;
    public static final float ANGULO_INTERIOR_MAX = 75f;
    /** Modo interior: desde esta anchura (y hasta el puntal) la altura del soporte manda en la diagonal. */
    public static final float ANCHURA_INTERIOR_ALTURA = 2f;
    /** Modo interior: a partir de esta anchura, puntal en L en vez del triangulo. */
    public static final float ANCHURA_INTERIOR_PUNTAL = 3f;
    /** Modo interior con alcance largo: el puntal se inclina esto desde la vertical hacia el poste. */
    public static final float ANGULO_PUNTAL = 20f;
    /** Modo medio: el brazo largo baja estos grados desde la perforada hasta el hilo. */
    public static final float INCLINACION_BRAZO_MEDIO = 10f;
    /** Modo exterior: el brazo corto baja estos grados desde la perforada vertical hasta el hilo. */
    public static final float INCLINACION_BRAZO_EXTERIOR = 10f;
    /** Cuanto se mete el wall_joint del brazo dentro de la pieza de la que cuelga, para que nunca quede hueco. */
    public static final float HUNDIMIENTO_GANCHO = 0.4f;
    /** Los brazos se dibujan un pelin mas finos (en Z) que los tubos: lo que queda metido no parpadea. */
    private static final float GROSOR_Z_BRAZO = 0.9f;
    /** El codo nunca pasa de aqui (px antes del sustentador), para dejar sitio al aislador. */
    public static final float CODO_ANTES_SUSTENTADOR = 3f;
    /** La horizontal sigue al menos esto despues del sustentador. */
    public static final float FIN_MIN = 3f;
    /** Modo interior: cuanto sigue la horizontal despues del sustentador (triangulo / puntal en L). */
    public static final float FIN_INTERIOR = 13f;
    public static final float FIN_INTERIOR_PUNTAL = 6f;
    /** El gancho del brazo va en el agujero que queda a esto del final de la perforada. */
    public static final float AGUJERO_DESDE_FINAL = 1.5f;
    /** En los postes gruesos la placa abarca su cara menos esto (px). */
    public static final float MARGEN_PLACA = 2f;
    /** Grosor de la placa atornillada al poste (la del tirante): el tubo arranca en su cara. */
    public static final float GROSOR_PLACA = 2f * (PiezasDatos.TIRANTE.JOINT_WALL_POINT[0] - PiezasDatos.TIRANTE.MIN[0]);
    /** Largo del tubo a 45 grados entre la horizontal y la perforada. */
    public static final float BAJADA_MIN = 2f;
    public static final float BAJADA_MAX = 12f;
    /** Media anchura de las piezas perforadas (miden 1,5 px). */
    public static final float MEDIA_PERFORADA = 0.75f;
    public static final float LARGO_PERFORADA = 7f;
    /** Distancia del centro del wall_joint del brazo a su cara de arriba (la que toca el tubo). */
    public static final float GANCHO_BAJO_TOPE = 1f;

    /**
     * Un brazo de atirantado de Blockbench y como alargarlo: el tubo recto {@code tubo} (de
     * {@code tuboDesde} a {@code tuboHasta} en X) se repite, lo del lado del gancho se queda y lo del
     * lado del cable se desplaza.
     */
    private record Sujetador(String id, float[] gancho, float[] cable, int tubo, float tuboDesde, float tuboHasta,
                             int[] parteGancho, int[] parteCable) {
        float largo() {
            return (float) Math.hypot(cable[0] - gancho[0], cable[1] - gancho[1]);
        }

        /** Cuanto hay que alargar el tubo para que mida {@code distancia} de gancho a cable. */
        float alargar(float distancia) {
            float dy = cable[1] - gancho[1];
            float dx = (float) Math.sqrt(Math.max(0, distancia * distancia - dy * dy));
            return Math.max(0f, dx - (cable[0] - gancho[0]));
        }
    }

    private static final Sujetador SUJETADOR_CORTO = new Sujetador(PiezasDatos.BRAZO_CORTO.ID,
            PiezasDatos.BRAZO_CORTO.WALL_JOINT, PiezasDatos.BRAZO_CORTO.CABLE_JOINT, 3, 4f, 13f,
            new int[]{4, 5, 6, 7}, new int[]{0, 1, 2});
    private static final Sujetador SUJETADOR_LARGO = new Sujetador(PiezasDatos.BRAZO_LARGO.ID,
            PiezasDatos.BRAZO_LARGO.WALL_JOINT, PiezasDatos.BRAZO_LARGO.CABLE_JOINT, 3, 4f, 19f,
            new int[]{4, 5, 6, 7}, new int[]{0, 1, 2});
    /** cable_holder_inner: el sujetador recto del modo interior. */
    private static final Sujetador SUJETADOR_INTERIOR = new Sujetador(PiezasDatos.BRAZO_INTERIOR.ID,
            PiezasDatos.BRAZO_INTERIOR.WALL_JOINT, PiezasDatos.BRAZO_INTERIOR.CABLE_JOINT, 2, 12f, 19f,
            new int[]{3, 4, 5, 6}, new int[]{0, 1});

    /** cable_holder_all y cable_holder_all_short: los sujetadores nuevos (con aislador) del modo interior. */
    private static final Sujetador SUJETADOR_NUEVO = new Sujetador(PiezasDatos.SUJETADOR.ID,
            PiezasDatos.SUJETADOR.WALL_JOINT, PiezasDatos.SUJETADOR.CABLE_JOINT, 2, 14f, 21f,
            new int[]{3, 4, 5, 6, 7, 8, 9}, new int[]{0, 1});
    private static final Sujetador SUJETADOR_NUEVO_CORTO = new Sujetador(PiezasDatos.SUJETADOR_CORTO.ID,
            PiezasDatos.SUJETADOR_CORTO.WALL_JOINT, PiezasDatos.SUJETADOR_CORTO.CABLE_JOINT, 2, 6f, 14f,
            new int[]{3, 4, 5, 6, 7, 8}, new int[]{0, 1});

    /**
     * Modo interior: la geometria se calcula con el sujetador de antes y se dibuja el nuevo en el mismo
     * sitio y con el mismo angulo (el nuevo es un poco mas largo: el hilo queda un pelin mas fuera).
     */
    private static Sujetador nuevo(Sujetador s) {
        return s == SUJETADOR_CORTO ? SUJETADOR_NUEVO_CORTO : SUJETADOR_NUEVO;
    }

    public record Resultado(List<Colocacion> piezas, Vector3f contacto, Vector3f sustentador) {
    }

    /** Modo interior: como queda la diagonal larga y de donde cuelga el sujetador. */
    private record PlanInterior(Vector2f ancla, Vector2f codo, Sujetador sujetador, Vector2f gancho, float alargar) {
    }

    private static final float SEN45 = 0.7071068f;
    private static final Vector2f DIR45 = new Vector2f(SEN45, -SEN45);
    private static final Vector2f HORIZONTAL = new Vector2f(1, 0);

    private MensulaLayout() {
    }

    /**
     * @param hueco distancia en px entre la cara del bloque y la cara del poste (postes finos dejan hueco)
     */
    public static Resultado calcular(Ajustes ajustes, float hueco) {
        List<Colocacion> out = new ArrayList<>();
        float r = 16f * ajustes.anchura();
        float mastil = -hueco;
        ModoZigzag modo = ajustes.modo();
        // el hilo de contacto, mas o menos bajo el aislador de arriba (solo lo mueve el zigzag)
        Vector2f contacto = new Vector2f(r + modo.offsetPx(), -16f * ajustes.alturaCatenaria());
        float inicioTubo = mastil + GROSOR_PLACA;

        // --- tubo principal: diagonal desde el poste hasta el codo, luego horizontal ---
        Vector2f ancla;
        Vector2f codo;
        // interior con alcance corto: el triangulo de la foto; con alcance largo, puntal en L
        PlanInterior plan = null;
        if (modo == ModoZigzag.INTERIOR && ajustes.anchura() < ANCHURA_INTERIOR_PUNTAL) {
            // con anchura 2..3 la altura dice cuanto baja la diagonal; con la minima, la de la foto
            if (ajustes.anchura() >= ANCHURA_INTERIOR_ALTURA) {
                // si con esa altura el sujetador no llega bien, la diagonal baja un poco mas hasta que llegue
                for (float caida = 16f * ajustes.altura(); plan == null && caida <= 16f * Ajustes.ALTURA.max(); caida += 2f) {
                    plan = planInteriorTrianguloAltura(contacto, inicioTubo, r, caida);
                }
            }
            if (plan == null) {
                plan = planInteriorTriangulo(contacto, inicioTubo, r);
            }
        }
        if (plan != null) {
            ancla = plan.ancla();
            codo = plan.codo();
        } else {
            // diagonal corta que baja lo que diga la altura; con alcance corto sale mas inclinada
            float caidaDiagonal = 16f * ajustes.altura();
            float tan30 = (float) Math.tan(Math.toRadians(ANGULO_DIAGONAL));
            float codoX = inicioTubo + Math.min(FRACCION_CODO * (r - inicioTubo), caidaDiagonal / tan30);
            codoX = Math.max(inicioTubo + 4f, Math.min(codoX, r - CODO_ANTES_SUSTENTADOR));
            codo = new Vector2f(codoX, BARRA_Y);
            ancla = new Vector2f(inicioTubo, BARRA_Y - caidaDiagonal);
        }
        float tan = (codo.y - ancla.y) / (codo.x - ancla.x);
        Vector2f dirDiagonal = new Vector2f(codo).sub(ancla).normalize();
        float[] ingleteCodo = inglete(dirDiagonal, HORIZONTAL);
        // arranca cortada en vertical contra la placa del poste y a inglete en el codo
        barra(out, ancla, codo, tan, ingleteCodo[0]);
        placa(out, mastil, ancla.y);

        Vector3f sustentador = aislador(out, ajustes.tipo(), r);

        Vector2f cable = switch (modo) {
            case INTERIOR -> {
                if (plan != null) {
                    barra(out, codo, new Vector2f(r + FIN_INTERIOR, BARRA_Y), ingleteCodo[1], 0f);
                    yield brazo(out, plan.sujetador(), nuevo(plan.sujetador()), plan.gancho(), contacto, false, plan.alargar());
                }
                barra(out, codo, new Vector2f(r + FIN_INTERIOR_PUNTAL, BARRA_Y), ingleteCodo[1], 0f);
                yield brazoPuntal(out, contacto);
            }
            case MEDIO -> brazoMedio(out, contacto, codo, ingleteCodo[1], r);
            case EXTERIOR -> brazoExterior(out, contacto, codo, ingleteCodo[1], r);
        };

        if (ajustes.tirante()) {
            // siempre horizontal, a la altura del eje de la barra: del poste al codo, donde acaba dentro del tubo
            placa(out, mastil, BARRA_Y);
            varilla(out, new Vector2f(inicioTubo, BARRA_Y), codo);
        }

        // el "YOffset" de PNW: toda la mensula baja
        float dy = -16f * ajustes.desplazamientoY();
        List<Colocacion> piezas = dy == 0f ? out : out.stream().map(c -> c.desplazada(dy)).toList();
        return new Resultado(piezas, new Vector3f(cable.x, cable.y + dy, Z), sustentador.add(0, dy, 0));
    }

    // ------------------------------------------------------------------ brazos de atirantado

    /**
     * Interior con alcance corto, como la foto de referencia: la diagonal sube a ANGULO_INTERIOR y el
     * sujetador cuelga de ella formando un triangulo de dos lados iguales con su tramo de arriba (del
     * gancho al codo mide lo mismo que el sujetador) y base muy ancha. Si el sujetador da para ello, el
     * gancho queda en la mitad de la diagonal (y el sujetador se alarga); si no, el lado igual es el
     * sujetador tal cual.
     */
    private static PlanInterior planInteriorTriangulo(Vector2f contacto, float x0, float r) {
        double ang = Math.toRadians(ANGULO_INTERIOR);
        Vector2f u = new Vector2f((float) Math.cos(ang), (float) Math.sin(ang));
        Vector2f bajo = new Vector2f(u.y, -u.x);
        float caida = caidaGancho(u.y / u.x) - HUNDIMIENTO_GANCHO;
        // gancho en la mitad: con h = media diagonal, |mitad - contacto| = h tiene solucion directa
        float a = contacto.x - x0;
        float b = BARRA_Y - contacto.y;
        float mitad = (a * a + b * b) / (2f * (a * u.x + b * u.y));
        for (Sujetador sujetador : new Sujetador[]{SUJETADOR_INTERIOR, SUJETADOR_CORTO}) {
            float lado = Math.max(mitad, sujetador.largo());
            // largo de la diagonal para que el sujetador (desde el gancho a "lado" del codo) llegue al hilo
            float largoDiagonal = resolver(lado + 2f, 200f, d -> {
                Vector2f g = ganchoTriangulo(x0, u, bajo, d, lado, caida, contacto, sujetador);
                return g.distance(contacto) - Math.max(lado, sujetador.largo());
            });
            Vector2f ancla = new Vector2f(x0, BARRA_Y - largoDiagonal * u.y);
            Vector2f codo = new Vector2f(x0 + largoDiagonal * u.x, BARRA_Y);
            Vector2f gancho = ganchoTriangulo(x0, u, bajo, largoDiagonal, lado, caida, contacto, sujetador);
            if (codo.x > r - CODO_ANTES_SUSTENTADOR || largoDiagonal - lado < 2f
                    || Math.abs(gancho.distance(contacto) - Math.max(lado, sujetador.largo())) > 0.5f) {
                continue;
            }
            float alargar = 0f;
            for (int k = 0; k < 3; k++) {
                alargar = sujetador.alargar(gancho.distance(contacto));
                Vector2f g = new Vector2f(codo).sub(new Vector2f(u).mul(lado)).sub(0, caida);
                float cara = ancla.dot(bajo) + MEDIA_BARRA - HUNDIMIENTO_GANCHO;
                gancho = tocar(g, giroBrazo(g, contacto, sujetador, false, alargar), bajo, cara);
            }
            return new PlanInterior(ancla, codo, sujetador, gancho, alargar);
        }
        // no cabe el triangulo (alcance minimo con el hilo bajo): diagonal a ANGULO_INTERIOR hasta justo
        // antes del aislador y el sujetador corto colgado donde llegue, bajando hacia el hilo
        Sujetador corto = SUJETADOR_CORTO;
        float largoDiagonal = (r - CODO_ANTES_SUSTENTADOR - x0) / u.x;
        Vector2f ancla = new Vector2f(x0, BARRA_Y - largoDiagonal * u.y);
        Vector2f codo = new Vector2f(r - CODO_ANTES_SUSTENTADOR, BARRA_Y);
        float t = resolver(largoDiagonal, 0f,
                d -> ganchoTriangulo(x0, u, bajo, largoDiagonal, d, caida, contacto, corto).distance(contacto) - corto.largo());
        return new PlanInterior(ancla, codo, corto, ganchoTriangulo(x0, u, bajo, largoDiagonal, t, caida, contacto, corto), 0f);
    }

    /**
     * Como {@link #planInteriorTriangulo}, pero la diagonal baja {@code caidaDiagonal} px (la altura
     * del soporte) y lo que se busca es su inclinacion. El lado igual del triangulo es el sujetador.
     * Devuelve null si no hay inclinacion razonable con la que el sujetador llegue al hilo.
     */
    private static PlanInterior planInteriorTrianguloAltura(Vector2f contacto, float x0, float r, float caidaDiagonal) {
        for (Sujetador sujetador : new Sujetador[]{SUJETADOR_INTERIOR, SUJETADOR_CORTO}) {
            float lado = sujetador.largo();
            float desde = x0 + caidaDiagonal / (float) Math.tan(Math.toRadians(ANGULO_INTERIOR_MAX));
            float hasta = Math.min(r - CODO_ANTES_SUSTENTADOR, x0 + caidaDiagonal / (float) Math.tan(Math.toRadians(ANGULO_INTERIOR_MIN)));
            if (hasta <= desde) {
                continue;
            }
            // de codo lejos a codo cerca: la primera solucion es la del sujetador menos inclinado
            float codoX = resolver(hasta, desde, x -> {
                PlanInterior p = trianguloConCodo(contacto, x0, x, caidaDiagonal, sujetador, lado);
                return p == null ? -1f : p.gancho().distance(contacto) - lado;
            });
            PlanInterior plan = trianguloConCodo(contacto, x0, codoX, caidaDiagonal, sujetador, lado);
            if (plan != null && Math.abs(plan.gancho().distance(contacto) - lado) < 0.5f) {
                return plan;
            }
        }
        return null;
    }

    /** Diagonal de (x0, BARRA_Y - caida) a (codoX, BARRA_Y) con el sujetador colgado a {@code lado} px del codo. */
    private static PlanInterior trianguloConCodo(Vector2f contacto, float x0, float codoX, float caidaDiagonal,
                                                 Sujetador sujetador, float lado) {
        Vector2f ancla = new Vector2f(x0, BARRA_Y - caidaDiagonal);
        Vector2f codo = new Vector2f(codoX, BARRA_Y);
        if (codo.distance(ancla) < lado + 2f) {
            return null;
        }
        Vector2f u = new Vector2f(codo).sub(ancla).normalize();
        Vector2f bajo = new Vector2f(u.y, -u.x);
        float cara = ancla.dot(bajo) + MEDIA_BARRA - HUNDIMIENTO_GANCHO;
        Vector2f g = new Vector2f(codo).sub(new Vector2f(u).mul(lado)).sub(0, caidaGancho(u.y / u.x) - HUNDIMIENTO_GANCHO);
        return new PlanInterior(ancla, codo, sujetador, tocar(g, giroBrazo(g, contacto, sujetador, false, 0f), bajo, cara), 0f);
    }

    /** Gancho del triangulo: a {@code lado} px del codo bajando por una diagonal de {@code largoDiagonal}. */
    private static Vector2f ganchoTriangulo(float x0, Vector2f u, Vector2f bajo, float largoDiagonal, float lado, float caida,
                                            Vector2f contacto, Sujetador sujetador) {
        Vector2f ancla = new Vector2f(x0, BARRA_Y - largoDiagonal * u.y);
        Vector2f g = new Vector2f(u).mul(largoDiagonal - lado).add(ancla).sub(0, caida);
        float cara = ancla.dot(bajo) + MEDIA_BARRA - HUNDIMIENTO_GANCHO;
        return tocar(g, giroBrazo(g, contacto, sujetador, false, 0f), bajo, cara);
    }

    /**
     * Interior con alcance largo: puntal en L. Sale de la cara de abajo de la horizontal y baja
     * inclinado ANGULO_PUNTAL hacia el poste; en su punta, el sujetador corto horizontal hacia la via.
     */
    private static Vector2f brazoPuntal(List<Colocacion> out, Vector2f contacto) {
        Sujetador s = SUJETADOR_CORTO;
        // sujetador horizontal: el gancho queda justo donde hace falta para que el cable caiga en el hilo
        Vector2f gancho = new Vector2f(contacto.x - (s.cable()[0] - s.gancho()[0]), contacto.y - (s.cable()[1] - s.gancho()[1]));
        // el pie del puntal, metido un poco en el wall_joint; la cabeza, pegada bajo la horizontal
        Vector2f pie = new Vector2f(gancho.x, gancho.y + GANCHO_BAJO_TOPE - HUNDIMIENTO_GANCHO);
        float techo = BARRA_Y - MEDIA_BARRA;
        Vector2f cabeza = new Vector2f(pie.x + (techo - pie.y) * (float) Math.tan(Math.toRadians(ANGULO_PUNTAL)), techo);
        Vector2f d = new Vector2f(cabeza).sub(pie).normalize();
        // los dos extremos cortados en horizontal
        float corte = -d.x / d.y;
        barra(out, pie, cabeza, corte, corte);
        return brazo(out, s, nuevo(s), gancho, contacto, false, 0f);
    }


    /**
     * Medio: la horizontal sigue, baja a 45 grados y acaba en UNA perforada diagonal. El brazo largo
     * cuelga metido en la cara de abajo de la perforada, cerca de su final, y BAJA hacia el hilo
     * (nunca sube). Si el hilo queda muy abajo para el alcance, baja mas inclinado (hasta 45 grados).
     */
    private static Vector2f brazoMedio(List<Colocacion> out, Vector2f contacto, Vector2f codo, float tanCodo, float r) {
        float largo = SUJETADOR_LARGO.largo();
        // cara de abajo de la perforada: su normal hacia fuera (abajo a la izquierda)
        Vector2f bajo = new Vector2f(-SEN45, -SEN45);
        Vector2f gancho = new Vector2f();
        float fin = 0f;
        for (float grados = INCLINACION_BRAZO_MEDIO; grados <= 45f; grados += 1f) {
            double inc = Math.toRadians(grados);
            gancho.set(contacto.x + largo * (float) Math.cos(inc), contacto.y + largo * (float) Math.sin(inc));
            float eje = minimoGancho(gancho, giroBrazo(gancho, contacto, SUJETADOR_LARGO, true, 0f), bajo)
                    - MEDIA_PERFORADA + HUNDIMIENTO_GANCHO;
            // el eje de la perforada pasa por el final de la horizontal: fin * bajo.x + BARRA_Y * bajo.y = eje
            fin = (eje - BARRA_Y * bajo.y) / bajo.x;
            if (fin >= r + FIN_MIN) {
                break;
            }
        }
        if (fin < r + FIN_MIN) {
            // la horizontal no puede acabar antes del aislador: el gancho se mete en la perforada que queda
            fin = r + FIN_MIN;
            float cara = new Vector2f(fin, BARRA_Y).dot(bajo) + MEDIA_PERFORADA - HUNDIMIENTO_GANCHO;
            gancho = tocar(gancho, giroBrazo(gancho, contacto, SUJETADOR_LARGO, true, 0f), bajo, cara);
        }
        Vector2f inicio = new Vector2f(fin, BARRA_Y);
        // el gancho cae en un agujero cerca del final de la perforada; el tubo a 45 grados pone el resto
        float t = new Vector2f(gancho).sub(inicio).dot(DIR45);
        float bajada = Math.max(BAJADA_MIN, t - (LARGO_PERFORADA - AGUJERO_DESDE_FINAL));
        Vector2f finBajada = new Vector2f(DIR45).mul(bajada).add(inicio);

        barra(out, codo, inicio, tanCodo, inglete(HORIZONTAL, DIR45)[0]);
        bajada(out, inicio, finBajada, DIR45);
        perforadaDiagonal(out, finBajada, -45f, 1, 0f);
        return brazo(out, SUJETADOR_LARGO, gancho, contacto, true, 0f);
    }

    /**
     * Exterior: la horizontal sigue, baja a 45 grados, perforada diagonal y perforada vertical. El
     * brazo corto, girado hacia el poste, va metido en el lado de la vertical, cerca de su final, y
     * baja un poco hacia el hilo. El tubo a 45 grados se alarga para que sobre la menor vertical posible.
     */
    private static Vector2f brazoExterior(List<Colocacion> out, Vector2f contacto, Vector2f codo, float tanCodo, float r) {
        float largo = SUJETADOR_CORTO.largo();
        double inc = Math.toRadians(INCLINACION_BRAZO_EXTERIOR);
        Vector2f gancho = new Vector2f(contacto.x + largo * (float) Math.cos(inc), contacto.y + largo * (float) Math.sin(inc));
        // cara de la vertical que mira al poste: el wall_joint se mete un poco en ella
        Vector2f haciaPoste = new Vector2f(-1f, 0f);
        float vertX = MEDIA_PERFORADA - HUNDIMIENTO_GANCHO
                - minimoGancho(gancho, giroBrazo(gancho, contacto, SUJETADOR_CORTO, true, 0f), haciaPoste);
        // UNA sola perforada vertical, que acaba AGUJERO_DESDE_FINAL bajo el gancho; el tubo a 45 grados
        // pone todo lo que falte
        float bajada = Math.max(BAJADA_MIN,
                (BARRA_Y - (gancho.y - AGUJERO_DESDE_FINAL + LARGO_PERFORADA)) / SEN45 - LARGO_PERFORADA);
        float avance = (bajada + LARGO_PERFORADA) * SEN45;
        float fin = vertX - avance;
        if (fin < r + FIN_MIN) {
            fin = r + FIN_MIN;
            gancho.x += fin + avance - vertX;
            float dx = gancho.x - contacto.x;
            gancho.y = contacto.y + (float) Math.sqrt(Math.max(0f, largo * largo - dx * dx));
            float cara = -(fin + avance - MEDIA_PERFORADA) - HUNDIMIENTO_GANCHO;
            gancho = tocar(gancho, giroBrazo(gancho, contacto, SUJETADOR_CORTO, true, 0f), haciaPoste, cara);
        }
        Vector2f inicio = new Vector2f(fin, BARRA_Y);
        Vector2f finBajada = new Vector2f(DIR45).mul(bajada).add(inicio);

        barra(out, codo, inicio, tanCodo, inglete(HORIZONTAL, DIR45)[0]);
        bajada(out, inicio, finBajada, DIR45);
        float[] ingleteVertical = inglete(DIR45, new Vector2f(0, -1));
        Vector2f finPerforada = perforadaDiagonal(out, finBajada, -45f, 1, ingleteVertical[0]);
        perforadaVertical(out, finPerforada, 1, ingleteVertical[1]);
        return brazo(out, SUJETADOR_CORTO, gancho, contacto, true, 0f);
    }

    // ------------------------------------------------------------------ contacto del gancho

    /** Giro que {@link #brazo} da a la pieza para que, colgada en {@code gancho}, apunte a {@code objetivo}. */
    private static float giroBrazo(Vector2f gancho, Vector2f objetivo, Sujetador s, boolean haciaPoste, float alargar) {
        float vx = (s.cable()[0] - s.gancho()[0] + alargar) * (haciaPoste ? -1 : 1);
        float vy = s.cable()[1] - s.gancho()[1];
        return (float) (Math.atan2(objetivo.y - gancho.y, objetivo.x - gancho.x) - Math.atan2(vy, vx));
    }

    /** Lo mas que llega el wall_joint del brazo (girado {@code giro}, colgado en {@code gancho}) en la direccion -n. */
    private static float minimoGancho(Vector2f gancho, float giro, Vector2f n) {
        float hx = PiezasDatos.BRAZO_CORTO.WALL_JOINT[0] - PiezasDatos.BRAZO_CORTO.MIN[0];
        float hy = PiezasDatos.BRAZO_CORTO.WALL_JOINT[1] - PiezasDatos.BRAZO_CORTO.MIN[1];
        float cos = (float) Math.cos(giro), sin = (float) Math.sin(giro);
        float min = Float.MAX_VALUE;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                float x = gancho.x + sx * hx * cos - sy * hy * sin;
                float y = gancho.y + sx * hx * sin + sy * hy * cos;
                min = Math.min(min, x * n.x + y * n.y);
            }
        }
        return min;
    }

    /**
     * Mueve el gancho en la direccion {@code n} (la normal hacia fuera de la cara de la que cuelga)
     * hasta que lo mas metido del wall_joint queda en p.n = {@code cara}.
     */
    private static Vector2f tocar(Vector2f gancho, float giro, Vector2f n, float cara) {
        return new Vector2f(n).mul(cara - minimoGancho(gancho, giro, n)).add(gancho);
    }

    // ------------------------------------------------------------------ piezas

    /**
     * Tubo estirable entre dos puntos de su eje: remate + tramos duplicados + remate. Los extremos se
     * cortan a inglete ({@code tanInicio}/{@code tanFin}, ver {@link Colocacion.Inglete}) para que
     * los tubos que se juntan en un codo encajen sin solaparse.
     */
    private static void barra(List<Colocacion> out, Vector2f a, Vector2f b, float tanInicio, float tanFin) {
        float len = a.distance(b);
        Matrix4f linea = linea(a, b);
        Colocacion.Inglete corte = new Colocacion.Inglete(linea, len, tanInicio, tanFin);
        if (len <= 2f) {
            out.add(new Colocacion(PiezasDatos.BARRA.ID, null, new Matrix4f(linea).scale(len / 7f, 1, 1).translate(0, -1, -1), corte));
            return;
        }
        out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_ATTACH_SEGMENT_END_1,
                new Matrix4f(linea).translate(0, -1, -1), corte));
        float medio = len - 2f;
        int n = Math.max(1, Math.round(medio / 5f));
        float tramo = medio / n;
        for (int i = 0; i < n; i++) {
            // semilla fija por tramo (sale de su posicion): la textura cambia de un tramo a otro pero no parpadea
            int semilla = (Objects.hash(Math.round(a.x * 4), Math.round(a.y * 4), Math.round(b.x * 4), Math.round(b.y * 4), i) & 0x7fffffff) | 1;
            out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_DUPLICATING_SEGMENT,
                    new Matrix4f(linea).translate(1f + i * tramo, 0, 0).scale(tramo / 5f, 1, 1).translate(-1, -1, -1), null, semilla));
        }
        out.add(new Colocacion(PiezasDatos.BARRA.ID, PiezasDatos.BARRA.GRUPO_ATTACH_SEGMENT_END_2,
                new Matrix4f(linea).translate(len - 7f, -1, -1), corte));
    }

    /**
     * Tramo que baja desde el final de la horizontal hasta la pieza perforada, que empieza justo en
     * {@code fin} (tope con tope, sin solaparse ni dejar hueco).
     */
    private static void bajada(List<Colocacion> out, Vector2f inicio, Vector2f fin, Vector2f dir) {
        barra(out, inicio, fin, inglete(new Vector2f(1, 0), dir)[1], 0f);
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

    /**
     * Placa atornillada al poste (la del tirante sin la varilla); su cara trasera en x = mastil. En los
     * postes gruesos (los cuadrados, de mas de 8 px) se ensancha para abarcar casi toda su cara.
     */
    private static void placa(List<Colocacion> out, float mastil, float y) {
        float[] p = PiezasDatos.TIRANTE.JOINT_WALL_POINT;
        float trasera = PiezasDatos.TIRANTE.MIN[0];
        float grosorPoste = 16f + 2f * mastil;
        float ancho = PiezasDatos.TIRANTE.MAX[2] - PiezasDatos.TIRANTE.MIN[2];
        float escala = grosorPoste > 8f ? (grosorPoste - MARGEN_PLACA) / ancho : 1f;
        out.add(new Colocacion(PiezasDatos.TIRANTE.ID, sinClonable(),
                new Matrix4f().translate(mastil - trasera, y - p[1], Z).scale(1f, 1f, escala).translate(0, 0, -p[2])));
    }

    /** Copias de la perforada diagonal desde {@code inicio}; el final se corta a inglete con {@code tanFin}. */
    private static Vector2f perforadaDiagonal(List<Colocacion> out, Vector2f inicio, float grados, int copias, float tanFin) {
        float rad = (float) Math.toRadians(grados);
        Vector2f fin = new Vector2f((float) Math.cos(rad), (float) Math.sin(rad)).mul(LARGO_PERFORADA * copias).add(inicio);
        Matrix4f linea = linea(inicio, fin);
        Colocacion.Inglete corte = new Colocacion.Inglete(linea, LARGO_PERFORADA * copias, 0f, tanFin);
        float[] c = PiezasDatos.PERFORADA_DIAGONAL.ATTACHING_CENTER_SEGMENT;
        for (int i = 0; i < copias; i++) {
            out.add(new Colocacion(PiezasDatos.PERFORADA_DIAGONAL.ID, null,
                    new Matrix4f(linea).translate(LARGO_PERFORADA * i, -c[1], -c[2]), corte));
        }
        return fin;
    }

    /** Copias de la perforada vertical colgando de {@code arriba}; el principio se corta a inglete. */
    private static void perforadaVertical(List<Colocacion> out, Vector2f arriba, int copias, float tanInicio) {
        float largo = LARGO_PERFORADA * copias;
        Colocacion.Inglete corte = new Colocacion.Inglete(linea(arriba, new Vector2f(arriba.x, arriba.y - largo)), largo, tanInicio, 0f);
        float[] c = PiezasDatos.PERFORADA_VERTICAL.TOP_JOINT;
        float techo = PiezasDatos.PERFORADA_VERTICAL.MAX[1];
        for (int i = 0; i < copias; i++) {
            out.add(new Colocacion(PiezasDatos.PERFORADA_VERTICAL.ID, null,
                    new Matrix4f().translate(arriba.x - c[0], arriba.y - techo - LARGO_PERFORADA * i, Z - c[2]), corte));
        }
    }

    /**
     * Brazo de atirantado colgado en {@code gancho} (centro de su wall_joint) y girado para que su
     * cable_joint caiga sobre {@code objetivo}; {@code alargar} px mas largo si hace falta. Devuelve
     * donde queda de verdad el cable_joint.
     */
    private static Vector2f brazo(List<Colocacion> out, Sujetador s, Vector2f gancho, Vector2f objetivo, boolean haciaPoste,
                                  float alargar) {
        return brazo(out, s, s, gancho, objetivo, haciaPoste, alargar);
    }

    /**
     * Igual, pero el angulo sale de {@code referencia} y se dibuja {@code s} (otra pieza con el gancho en
     * el mismo sitio): el cable queda donde caiga la punta de {@code s}.
     */
    private static Vector2f brazo(List<Colocacion> out, Sujetador referencia, Sujetador s, Vector2f gancho, Vector2f objetivo,
                                  boolean haciaPoste, float alargar) {
        float[] e = s.gancho();
        float giro = giroBrazo(gancho, objetivo, referencia, haciaPoste, alargar);
        // un pelin mas fino en Z que los tubos: lo que se mete en ellos no parpadea
        Matrix4f m = new Matrix4f().translate(gancho.x, gancho.y, Z).rotateZ(giro).scale(1f, 1f, GROSOR_Z_BRAZO);
        if (haciaPoste) {
            m.rotateY((float) Math.PI);
        }
        m.translate(-e[0], -e[1], -e[2]);
        if (alargar <= 0.01f) {
            out.add(new Colocacion(s.id(), null, m));
        } else {
            // la parte del gancho igual, el tubo recto repetido en tramos y la parte del cable desplazada
            out.add(new Colocacion(s.id(), s.parteGancho(), m));
            out.add(new Colocacion(s.id(), s.parteCable(), new Matrix4f(m).translate(alargar, 0, 0)));
            float base = s.tuboHasta() - s.tuboDesde();
            float largoTubo = base + alargar;
            int n = Math.max(1, Math.round(largoTubo / base));
            float tramo = largoTubo / n;
            for (int i = 0; i < n; i++) {
                out.add(new Colocacion(s.id(), new int[]{s.tubo()}, new Matrix4f(m)
                        .translate(s.tuboDesde() + i * tramo, 0, 0)
                        .scale(tramo / base, 1, 1)
                        .translate(-s.tuboDesde(), 0, 0)));
            }
        }
        float vx = (s.cable()[0] - e[0] + alargar) * (haciaPoste ? -1 : 1);
        float vy = s.cable()[1] - e[1];
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
                float e = ESCALA_AISLADOR_HORIZONTAL;
                out.add(new Colocacion(PiezasDatos.AISLADOR_HORIZONTAL.ID, null,
                        new Matrix4f().translate(r, arriba, Z).scale(e).translate(-c[0], -base, -c[2])));
                return new Vector3f(r, arriba + (c[1] - base) * e + APOYO_SUSTENTADOR, Z);
            }
            case TIPO2 -> {
                // la misma pieza del reves (girada 180 grados sobre X), colgando bajo la barra
                float[] c = PiezasDatos.AISLADOR_HORIZONTAL.CABLE_JOINT;
                float base = PiezasDatos.AISLADOR_HORIZONTAL.MIN[1];
                float e = ESCALA_AISLADOR_HORIZONTAL;
                out.add(new Colocacion(PiezasDatos.AISLADOR_HORIZONTAL.ID, null,
                        new Matrix4f().translate(r, abajo, Z).rotateX((float) Math.PI).scale(e).translate(-c[0], -base, -c[2])));
                return new Vector3f(r, abajo - (c[1] - base) * e + APOYO_SUSTENTADOR, Z);
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

    /**
     * Inglete de la union entre un tubo con direccion d1 y el siguiente con direccion d2 (unitarias):
     * devuelve {tanFin del primero, tanInicio del segundo}. El plano de corte es el bisector.
     */
    private static float[] inglete(Vector2f d1, Vector2f d2) {
        float cos = 1f + d1.dot(d2);
        float n1d2 = -d1.y * d2.x + d1.x * d2.y;
        float n2d1 = -d2.y * d1.x + d2.x * d1.y;
        return new float[]{-n1d2 / cos, -n2d1 / cos};
    }

    /** Distancia vertical del eje de la diagonal al centro del wall_joint del brazo que cuelga de ella. */
    private static float caidaGancho(float tanDiagonal) {
        return MEDIA_BARRA * (float) Math.sqrt(1 + tanDiagonal * tanDiagonal) + GANCHO_BAJO_TOPE;
    }

    private static Vector2f puntoGancho(Vector2f origen, Vector2f dir, float s, float caida) {
        return new Vector2f(dir).mul(s).add(origen).sub(0, caida);
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

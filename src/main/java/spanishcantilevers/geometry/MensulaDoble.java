package spanishcantilevers.geometry;

import net.minecraft.util.Mth;
import spanishcantilevers.block.ModoZigzag;
import spanishcantilevers.block.TipoAislador;

/**
 * Reglas de la mensula doble (bifurcacion): dos mensulas en el mismo bloque, una al lado de la otra a lo
 * largo de la via, cada una con su hilo, para las agujas. Se hace clicando una mensula puesta con otra.
 *
 * <ul>
 *   <li>La segunda lleva el zigzag contrario: interior -> exterior; medio o exterior -> interior. La
 *       exterior se estira un poco ({@link #EXTRA_EXTERIOR}) para que los dos hilos queden mas separados.</li>
 *   <li>Los aisladores no quedan alineados: el de la del medio o la exterior se aparta
 *       {@link #DESALINEAR_AISLADOR} hacia fuera. La interior lleva la diagonal corta de las otras (misma
 *       inclinacion) con el sujetador colgado de ella bajando unos 25 grados, y su hilo justo bajo su aislador
 *       (sin el descentramiento del zigzag interior): los hilos se separan como los aisladores.</li>
 *   <li>Anchura como mucho {@link #ANCHURA_MAX} en las dos (cada una la suya).</li>
 *   <li>El aislador siempre arriba: el tipo 2 (colgado) pasa a ser el 1.</li>
 *   <li>Desplazamiento Y, cable de soporte y altura de catenaria iguales en las dos (los dos hilos van a
 *       la misma altura): si se cambian en una, la otra se cambia igual.</li>
 *   <li>Altura del soporte: la segunda copia la de la primera, y despues pueden diferir como mucho
 *       {@link #DIFERENCIA_ALTURA}.</li>
 * </ul>
 */
public final class MensulaDoble {
    /** Anchura maxima de cada una (bloques). */
    public static final float ANCHURA_MAX = 3f;
    /** Cada mensula queda a esto (px) del centro del bloque, a un lado y al otro. */
    public static final float SEPARACION = 4f;
    /** Lo que se aleja de mas el hilo de la exterior (px). */
    public static final float EXTRA_EXTERIOR = 2f;
    /** Lo que se aparta hacia fuera el aislador de la del medio o la exterior (px). */
    public static final float DESALINEAR_AISLADOR = 4f;
    /** Lo que pueden diferir las alturas del soporte de las dos (dos pasos del deslizador). */
    public static final float DIFERENCIA_ALTURA = 2f * Ajustes.ALTURA.paso();

    private MensulaDoble() {
    }

    /**
     * Lo que la ventana necesita saber de la otra mensula al cambiar una de una doble.
     *
     * @param otra los ajustes de la otra
     * @param cual la que se cambia: 0 = la que ya estaba, 1 = la que se anadio
     * @param lado hacia donde (+1/-1, Z de la mensula) quedo la que se anadio
     */
    public record Pareja(Ajustes otra, int cual, int lado) {
        /** Las dos como quedarian con {@code editada} (para la vista previa): {primera, segunda}. */
        public Ajustes[] con(Ajustes editada) {
            Ajustes[] par = ajustar(editada, editada, otra);
            return cual == 0 ? par : new Ajustes[]{par[1], par[0]};
        }
    }

    /** La anchura mas grande del deslizador que no pasa de {@link #ANCHURA_MAX}. */
    public static float anchuraMaxima() {
        Ajustes.Medida m = Ajustes.ANCHURA;
        return m.min() + (float) Math.floor((ANCHURA_MAX - m.min()) / m.paso() + 1e-4) * m.paso();
    }

    public static boolean cabe(Ajustes a) {
        return a.anchura() <= ANCHURA_MAX + 1e-4f;
    }

    /** El zigzag de la segunda segun el de la que ya estaba puesta. */
    public static ModoZigzag opuesto(ModoZigzag modo) {
        return modo == ModoZigzag.INTERIOR ? ModoZigzag.EXTERIOR : ModoZigzag.INTERIOR;
    }

    /** Lo que se aleja (px, negativo: se acerca al poste) el hilo de una mensula de una doble. */
    public static float extra(Ajustes a) {
        return switch (a.modo()) {
            case EXTERIOR -> EXTRA_EXTERIOR;
            // el hilo de la interior, bajo su aislador
            case INTERIOR -> -ModoZigzag.INTERIOR.offsetPx();
            case MEDIO -> 0f;
        };
    }

    /** Quita lo que no vale en una doble: el aislador colgado y la anchura de mas. */
    public static Ajustes limpiar(Ajustes a) {
        Ajustes r = a.validados();
        if (r.tipo() == TipoAislador.TIPO2) {
            r = r.conTipo(TipoAislador.TIPO1);
        }
        return r.conAnchura(Math.min(r.anchura(), anchuraMaxima())).validados();
    }

    /** Limites de la altura del soporte de una mensula segun la de la otra. */
    public static float alturaMinima(Ajustes otra) {
        return Math.max(Ajustes.ALTURA.min(), otra.altura() - DIFERENCIA_ALTURA);
    }

    public static float alturaMaxima(Ajustes otra) {
        return Math.min(Ajustes.ALTURA.max(), otra.altura() + DIFERENCIA_ALTURA);
    }

    /** La mensula que se anade a {@code existente}: lo del objeto, con el zigzag contrario y lo comun copiado. */
    public static Ajustes nueva(Ajustes existente, Ajustes objeto) {
        return limpiar(objeto.conModo(opuesto(existente.modo())).conAltura(existente.altura())
                .conAlturaCatenaria(existente.alturaCatenaria()).conDesplazamientoY(existente.desplazamientoY())
                .conTirante(existente.tirante()));
    }

    /**
     * Despues de cambiar una en su ventana: {editada, otra}. La editada se queda con su zigzag y con la altura
     * del soporte cerca de la de la otra; la otra copia lo que va igual en las dos.
     */
    public static Ajustes[] ajustar(Ajustes editada, Ajustes antes, Ajustes otra) {
        Ajustes e = limpiar(editada.conModo(antes.modo()));
        e = e.conAltura(Mth.clamp(e.altura(), alturaMinima(otra), alturaMaxima(otra))).validados();
        Ajustes o = limpiar(otra.conAlturaCatenaria(e.alturaCatenaria()).conDesplazamientoY(e.desplazamientoY())
                .conTirante(e.tirante()));
        return new Ajustes[]{e, o};
    }
}

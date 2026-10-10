package spanishcantilevers.geometry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import spanishcantilevers.block.ModoZigzag;
import spanishcantilevers.block.TipoAislador;

/**
 * Todo lo que se puede escoger de una mensula. Las medidas van en bloques y siguen el criterio de
 * la mensula de PNW, para que las dos se puedan mezclar en una misma linea:
 *
 * @param anchura         distancia del poste al centro de la via (sustentador), como "Width" de PNW
 * @param altura          cuanto baja la diagonal bajo la barra horizontal (su "Height")
 * @param alturaCatenaria hilo de contacto por debajo de la base del bloque ("CatenaryHeight")
 * @param desplazamientoY toda la mensula baja esto (0 o medio bloque, "YOffset")
 */
public record Ajustes(TipoAislador tipo, ModoZigzag modo, boolean tirante,
                      float anchura, float altura, float alturaCatenaria, float desplazamientoY) {

    public static final Medida ANCHURA = new Medida(1.5f, 6.5f, 1f, 2.5f);
    public static final Medida ALTURA = new Medida(0.25f, 1.5f, 0.25f, 0.5f);
    /** Mas de 0,75 bloques de catenaria queda mal en todos los modos (y no hay ejemplos reales). */
    public static final Medida ALTURA_CATENARIA = new Medida(0.25f, 0.75f, 0.25f, 0.5f);
    public static final Medida DESPLAZAMIENTO_Y = new Medida(0f, 0.5f, 0.5f, 0f);

    /**
     * Lo que sale si no se ha tocado nada: interior, anchura 2,5, altura del soporte 1, catenaria 0,5,
     * sin desplazamiento, cable encima del aislador y con cable de soporte. Cada jugador puede guardar
     * otra con el boton de la ventana (eso solo cambia lo que la ventana propone).
     */
    public static final Ajustes DEFECTO = new Ajustes(TipoAislador.TIPO1, ModoZigzag.INTERIOR, true,
            2.5f, 1f, 0.5f, 0f);

    /**
     * Altura del soporte que se usa sin opciones avanzadas: 1 en el medio y el exterior. En el interior,
     * con alcance corto la diagonal quedaria muy plana, asi que baja un poco mas; con anchura 2..3 manda
     * en el triangulo de la foto de referencia.
     */
    public static float alturaAutomatica(float anchura, ModoZigzag modo) {
        if (modo != ModoZigzag.INTERIOR || anchura >= 2f && anchura < 3f) {
            return 1f;
        }
        return anchura < 2f ? 0.75f : ALTURA.defecto();
    }

    /** En el modo interior con anchura minima la diagonal la manda el sujetador: la altura no cuenta. */
    public static boolean usaAltura(float anchura, ModoZigzag modo) {
        return modo != ModoZigzag.INTERIOR || anchura >= 2f;
    }

    /**
     * Con el aislador tipo 2 el sustentador cuelga bajo la barra: el hilo de contacto tiene que ir mas
     * abajo para que los dos cables no queden casi juntos.
     */
    public static float alturaCatenariaMinima(TipoAislador tipo) {
        return tipo == TipoAislador.TIPO2 ? 0.75f : ALTURA_CATENARIA.min();
    }

    /** Limites de un deslizador: minimo, maximo, paso y valor por defecto. */
    public record Medida(float min, float max, float paso, float defecto) {
        public float ajustar(float v) {
            if (!Float.isFinite(v)) {
                return defecto;
            }
            float pasos = Math.round((Mth.clamp(v, min, max) - min) / paso);
            return Math.min(max, min + pasos * paso);
        }
    }

    /** Los mismos ajustes con los valores dentro de sus limites (lo que llega por red no es de fiar). */
    public Ajustes validados() {
        return new Ajustes(tipo, modo, tirante, ANCHURA.ajustar(anchura), ALTURA.ajustar(altura),
                Math.max(ALTURA_CATENARIA.ajustar(alturaCatenaria), alturaCatenariaMinima(tipo)),
                DESPLAZAMIENTO_Y.ajustar(desplazamientoY));
    }

    public Ajustes conTipo(TipoAislador v) {
        return new Ajustes(v, modo, tirante, anchura, altura, alturaCatenaria, desplazamientoY);
    }

    public Ajustes conModo(ModoZigzag v) {
        return new Ajustes(tipo, v, tirante, anchura, altura, alturaCatenaria, desplazamientoY);
    }

    public Ajustes conTirante(boolean v) {
        return new Ajustes(tipo, modo, v, anchura, altura, alturaCatenaria, desplazamientoY);
    }

    public Ajustes conAnchura(float v) {
        return new Ajustes(tipo, modo, tirante, v, altura, alturaCatenaria, desplazamientoY);
    }

    public Ajustes conAltura(float v) {
        return new Ajustes(tipo, modo, tirante, anchura, v, alturaCatenaria, desplazamientoY);
    }

    public Ajustes conAlturaCatenaria(float v) {
        return new Ajustes(tipo, modo, tirante, anchura, altura, v, desplazamientoY);
    }

    public Ajustes conDesplazamientoY(float v) {
        return new Ajustes(tipo, modo, tirante, anchura, altura, alturaCatenaria, v);
    }

    // ------------------------------------------------------------------ NBT y red

    private static final String TIPO = "Tipo";
    private static final String MODO = "Modo";
    private static final String TIRANTE = "Tirante";
    private static final String ANCHURA_NBT = "Width";
    private static final String ALTURA_NBT = "Height";
    private static final String ALTURA_CATENARIA_NBT = "CatenaryHeight";
    private static final String DESPLAZAMIENTO_Y_NBT = "YOffset";

    public CompoundTag escribir(CompoundTag nbt) {
        nbt.putString(TIPO, tipo.getSerializedName());
        nbt.putString(MODO, modo.getSerializedName());
        nbt.putBoolean(TIRANTE, tirante);
        nbt.putFloat(ANCHURA_NBT, anchura);
        nbt.putFloat(ALTURA_NBT, altura);
        nbt.putFloat(ALTURA_CATENARIA_NBT, alturaCatenaria);
        nbt.putFloat(DESPLAZAMIENTO_Y_NBT, desplazamientoY);
        return nbt;
    }

    /** Lee lo que haya; lo que falte se queda por defecto. */
    public static Ajustes leer(CompoundTag nbt) {
        Ajustes d = DEFECTO;
        return new Ajustes(
                nbt.contains(TIPO) ? TipoAislador.porNombre(nbt.getString(TIPO)) : d.tipo(),
                nbt.contains(MODO) ? ModoZigzag.porNombre(nbt.getString(MODO)) : d.modo(),
                nbt.contains(TIRANTE) ? nbt.getBoolean(TIRANTE) : d.tirante(),
                nbt.contains(ANCHURA_NBT) ? nbt.getFloat(ANCHURA_NBT) : d.anchura(),
                nbt.contains(ALTURA_NBT) ? nbt.getFloat(ALTURA_NBT) : d.altura(),
                nbt.contains(ALTURA_CATENARIA_NBT) ? nbt.getFloat(ALTURA_CATENARIA_NBT) : d.alturaCatenaria(),
                nbt.contains(DESPLAZAMIENTO_Y_NBT) ? nbt.getFloat(DESPLAZAMIENTO_Y_NBT) : d.desplazamientoY())
                .validados();
    }

    public void escribir(FriendlyByteBuf buf) {
        buf.writeNbt(escribir(new CompoundTag()));
    }

    public static Ajustes leer(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();
        return nbt == null ? DEFECTO : leer(nbt);
    }
}

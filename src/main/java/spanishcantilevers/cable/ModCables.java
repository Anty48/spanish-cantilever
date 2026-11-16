package spanishcantilevers.cable;

import de.mrjulsen.wires.WireTypeRegistry;
import spanishcantilevers.SpanishCantilevers;

/** Tipos de cable propios, registrados en el sistema de cables de PNW. */
public final class ModCables {
    public static final CatenariaRigida RIGIDA = WireTypeRegistry.register(SpanishCantilevers.MOD_ID, "rigid_catenary", CatenariaRigida::new);
    /** El hilo del tirante diagonal de la mensula (se tiende con la bobina de PNW en modo cable de soporte). */
    public static final CableTirante TIRANTE = WireTypeRegistry.register(SpanishCantilevers.MOD_ID, "diagonal_stay", CableTirante::new);
    /** El cable de corriente de PNW con un hilo a cada aislador de los remates dobles (bobina en modo corriente). */
    public static final CableAisladores AISLADORES = WireTypeRegistry.register(SpanishCantilevers.MOD_ID, "double_insulator_wire", CableAisladores::new);

    private ModCables() {
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }
}

package iberiancantilever.cable;

import de.mrjulsen.wires.WireTypeRegistry;
import iberiancantilever.IberianCantilever;

/** Tipos de cable propios, registrados en el sistema de cables de PNW. */
public final class ModCables {
    public static final CatenariaRigida RIGIDA = WireTypeRegistry.register(IberianCantilever.MOD_ID, "rigid_catenary", CatenariaRigida::new);
    /** El hilo del tirante diagonal de la mensula (se tiende con la bobina de PNW en modo cable de soporte). */
    public static final CableTirante TIRANTE = WireTypeRegistry.register(IberianCantilever.MOD_ID, "diagonal_stay", CableTirante::new);

    private ModCables() {
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }
}

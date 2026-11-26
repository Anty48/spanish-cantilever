package spanishcantilevers.block;

import net.minecraft.util.StringRepresentable;

/**
 * Aislador del sustentador de la mensula colgante: el rollo horizontal o el vertical, encima de la viga
 * de la que cuelga o colgado debajo de ella (como en la foto de referencia, el de por defecto).
 */
public enum AisladorColgante implements StringRepresentable {
    /** Rollo horizontal sobre la viga, el cable apoyado encima (el tipo 1 de la mensula). */
    ENCIMA("encima"),
    /** Rollo horizontal colgado bajo la viga, el cable en el hueco (el tipo 2 de la mensula). */
    COLGADO("colgado"),
    /** Aislador vertical sobre la viga, hacia arriba (el tipo 3 de la mensula). */
    VERTICAL_ENCIMA("vertical_encima"),
    /** Aislador vertical colgado bajo la viga, hacia abajo. */
    VERTICAL_COLGADO("vertical_colgado");

    private final String name;

    AisladorColgante(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** El valor con ese nombre (el de getSerializedName), o COLGADO si no existe. */
    public static AisladorColgante porNombre(String nombre) {
        for (AisladorColgante v : values()) {
            if (v.name.equals(nombre)) {
                return v;
            }
        }
        return COLGADO;
    }
}

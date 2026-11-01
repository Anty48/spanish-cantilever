package spanishcantilevers.block;

import net.minecraft.util.StringRepresentable;

/**
 * Tamano del soporte de tunel. En el de techo, travesanos normales o grandes (de lado a lado del
 * bloque, con la pinza mas separada del centro); en el de pared, brazo corto o largo.
 */
public enum TamanoTunel implements StringRepresentable {
    NORMAL("normal"),
    GRANDE("grande");

    private final String nombre;

    TamanoTunel(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getSerializedName() {
        return nombre;
    }
}

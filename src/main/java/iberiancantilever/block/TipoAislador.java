package iberiancantilever.block;

import net.minecraft.util.StringRepresentable;

/** Aislador de arriba (el que sujeta el sustentador). */
public enum TipoAislador implements StringRepresentable {
    /** Tipo 1: rollo horizontal, el cable apoyado encima. */
    TIPO1("tipo1"),
    /** Tipo 2: el mismo rollo del reves, colgando bajo la barra. */
    TIPO2("tipo2"),
    /** Tipo 3: aislador vertical hacia arriba. */
    TIPO3("tipo3");

    private final String name;

    TipoAislador(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** El valor con ese nombre (el de getSerializedName), o TIPO1 si no existe. */
    public static TipoAislador porNombre(String nombre) {
        for (TipoAislador v : values()) {
            if (v.name.equals(nombre)) {
                return v;
            }
        }
        return TIPO1;
    }
}

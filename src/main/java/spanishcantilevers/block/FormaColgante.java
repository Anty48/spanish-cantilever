package spanishcantilevers.block;

import net.minecraft.util.StringRepresentable;

/** Forma del tubo que baja de la viga en la mensula colgante. */
public enum FormaColgante implements StringRepresentable {
    /** Recto e inclinado, como el puntal de la mensula interior ancha. */
    DIAGONAL("diagonal"),
    /** Un tramo corto a 45 grados bajo la viga y luego vertical (la de la foto de referencia). */
    CURVADA("curvada");

    private final String name;

    FormaColgante(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** El valor con ese nombre (el de getSerializedName), o DIAGONAL si no existe. */
    public static FormaColgante porNombre(String nombre) {
        for (FormaColgante v : values()) {
            if (v.name.equals(nombre)) {
                return v;
            }
        }
        return DIAGONAL;
    }
}

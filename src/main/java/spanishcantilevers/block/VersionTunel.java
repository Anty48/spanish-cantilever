package spanishcantilevers.block;

import net.minecraft.util.StringRepresentable;

/**
 * Como va sujeto el soporte de tunel: colgado del techo con su varilla, o empotrado en la pared (o en
 * la cara de un poste de PNW, como la mensula) con un brazo horizontal.
 */
public enum VersionTunel implements StringRepresentable {
    TECHO("techo"),
    PARED("pared");

    private final String nombre;

    VersionTunel(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getSerializedName() {
        return nombre;
    }
}

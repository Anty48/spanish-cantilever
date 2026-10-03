package iberiancantilever.block;

import net.minecraft.util.StringRepresentable;

/**
 * Donde agarra el soporte de tunel de techo el perfil rigido: a un lado, en el centro o al otro (el
 * zigzag del hilo de contacto). Cada una es su pieza de Blockbench ({@link SoporteTunelBlock#pieza}).
 * El de pared tiene una sola pinza: no la usa.
 */
public enum PosicionTunel implements StringRepresentable {
    IZQUIERDA("izquierda"),
    CENTRO("centro"),
    DERECHA("derecha");

    private final String nombre;

    PosicionTunel(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getSerializedName() {
        return nombre;
    }
}

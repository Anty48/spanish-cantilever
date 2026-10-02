package iberiancantilever.block;

import iberiancantilever.geometry.PiezasDatos;
import net.minecraft.util.StringRepresentable;

/**
 * Donde agarra el soporte de tunel el perfil rigido: a un lado, en el centro o al otro (el zigzag del
 * hilo de contacto). Cada una es su pieza de Blockbench; solo cambia el cubo de enganche, 2 px movido.
 */
public enum PosicionTunel implements StringRepresentable {
    IZQUIERDA("izquierda", PiezasDatos.TUNEL_IZQUIERDA.ID, 10f),
    CENTRO("centro", PiezasDatos.TUNEL_CENTRO.ID, 8f),
    DERECHA("derecha", PiezasDatos.TUNEL_DERECHA.ID, PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[2]);

    private final String nombre;
    private final String pieza;
    /** Z (px de Blockbench) del centro del cubo donde se engancha el perfil. */
    private final float enganche;

    PosicionTunel(String nombre, String pieza, float enganche) {
        this.nombre = nombre;
        this.pieza = pieza;
        this.enganche = enganche;
    }

    @Override
    public String getSerializedName() {
        return nombre;
    }

    public String pieza() {
        return pieza;
    }

    public float enganche() {
        return enganche;
    }
}

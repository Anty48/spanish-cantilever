package spanishcantilevers.block;

import net.minecraft.util.StringRepresentable;

/**
 * Descentramiento del hilo de contacto respecto al sustentador (que siempre queda sobre el eje).
 * Los mismos +-0,25 bloques que usa PNW.
 */
public enum ModoZigzag implements StringRepresentable {
    INTERIOR("interior", -4f),
    MEDIO("medio", 0f),
    EXTERIOR("exterior", 4f);

    private final String name;
    /** Desplazamiento del hilo de contacto en pixeles, positivo = alejandose del poste. */
    private final float offsetPx;

    ModoZigzag(String name, float offsetPx) {
        this.name = name;
        this.offsetPx = offsetPx;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public float offsetPx() {
        return offsetPx;
    }

    /** El valor con ese nombre (el de getSerializedName), o MEDIO si no existe. */
    public static ModoZigzag porNombre(String nombre) {
        for (ModoZigzag v : values()) {
            if (v.name.equals(nombre)) {
                return v;
            }
        }
        return MEDIO;
    }
}

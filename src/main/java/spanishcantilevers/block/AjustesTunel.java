package spanishcantilevers.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lo que se escoge en la ventana del soporte de tunel (todo son propiedades del bloque). Cada version
 * tiene su altura: {@code altura} la del de techo y {@code alturaPared} la del de pared. {@code escala}: el
 * tamano general, en pasos de {@link SoporteTunelBlock#PASO_ESCALA}.
 */
public record AjustesTunel(VersionTunel version, TamanoTunel tamano, PosicionTunel posicion, int altura, int alturaPared, int escala) {
    /** Como sale el bloque sin configurar: de techo, normal, pinza en el centro y alturas por defecto. */
    public static final AjustesTunel DEFECTO = new AjustesTunel(VersionTunel.TECHO, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0,
            SoporteTunelBlock.ALTURA_PARED_CENTRO, 0);

    public static AjustesTunel de(BlockState state) {
        return new AjustesTunel(state.getValue(SoporteTunelBlock.VERSION), state.getValue(SoporteTunelBlock.TAMANO),
                state.getValue(SoporteTunelBlock.POSICION), state.getValue(SoporteTunelBlock.ALTURA),
                state.getValue(SoporteTunelBlock.ALTURA_PARED), state.getValue(SoporteTunelBlock.ESCALA));
    }

    public AjustesTunel conVersion(VersionTunel v) {
        return new AjustesTunel(v, tamano, posicion, altura, alturaPared, escala);
    }

    public BlockState aplicar(BlockState state) {
        return state.setValue(SoporteTunelBlock.VERSION, version)
                .setValue(SoporteTunelBlock.TAMANO, tamano)
                .setValue(SoporteTunelBlock.POSICION, posicion)
                .setValue(SoporteTunelBlock.ALTURA, Mth.clamp(altura, 0, SoporteTunelBlock.ALTURA_MAX))
                .setValue(SoporteTunelBlock.ALTURA_PARED, Mth.clamp(alturaPared, 0, SoporteTunelBlock.ALTURA_PARED_MAX))
                .setValue(SoporteTunelBlock.ESCALA, Mth.clamp(escala, 0, SoporteTunelBlock.ESCALA_MAX));
    }

    public void escribir(FriendlyByteBuf buf) {
        buf.writeEnum(version);
        buf.writeEnum(tamano);
        buf.writeEnum(posicion);
        buf.writeVarInt(altura);
        buf.writeVarInt(alturaPared);
        buf.writeVarInt(escala);
    }

    public static AjustesTunel leer(FriendlyByteBuf buf) {
        return new AjustesTunel(buf.readEnum(VersionTunel.class), buf.readEnum(TamanoTunel.class),
                buf.readEnum(PosicionTunel.class), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    /** Para guardar la configuracion por defecto del jugador. */
    public CompoundTag escribir(CompoundTag tag) {
        tag.putString("Version", version.getSerializedName());
        tag.putString("Tamano", tamano.getSerializedName());
        tag.putString("Posicion", posicion.getSerializedName());
        tag.putInt("Altura", altura);
        tag.putInt("AlturaPared", alturaPared);
        tag.putInt("Escala", escala);
        return tag;
    }

    public static AjustesTunel leer(CompoundTag tag) {
        return new AjustesTunel(
                buscar(VersionTunel.values(), tag.getString("Version"), DEFECTO.version()),
                buscar(TamanoTunel.values(), tag.getString("Tamano"), DEFECTO.tamano()),
                buscar(PosicionTunel.values(), tag.getString("Posicion"), DEFECTO.posicion()),
                tag.contains("Altura") ? tag.getInt("Altura") : DEFECTO.altura(),
                tag.contains("AlturaPared") ? tag.getInt("AlturaPared") : DEFECTO.alturaPared(),
                tag.contains("Escala") ? tag.getInt("Escala") : DEFECTO.escala());
    }

    private static <T extends StringRepresentable> T buscar(T[] valores, String nombre, T otro) {
        for (T v : valores) {
            if (v.getSerializedName().equals(nombre)) {
                return v;
            }
        }
        return otro;
    }
}

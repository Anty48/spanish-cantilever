package spanishcantilevers.geometry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import spanishcantilevers.block.AisladorColgante;
import spanishcantilevers.block.FormaColgante;

/**
 * Lo que se puede escoger de una mensula colgante (la que cuelga bajo una viga). Medidas en bloques.
 *
 * @param anchura         distancia del hilo (bajo el bloque) a donde el tubo cuelga de la viga: el
 *                        sujetador se alarga o se acorta
 * @param alturaCatenaria lo que cuelga el hilo de contacto por debajo de la cara de abajo de la viga (asi no
 *                        depende de su grosor); el minimo deja el hilo justo bajo el aislador colgado
 */
public record AjustesColgante(AisladorColgante aislador, FormaColgante forma, float anchura, float alturaCatenaria) {

    public static final Ajustes.Medida ANCHURA = new Ajustes.Medida(1f, 3f, 0.5f, 1f);
    public static final Ajustes.Medida ALTURA_CATENARIA = new Ajustes.Medida(0.75f, 2.5f, 0.25f, 1.25f);

    /** Aislador colgado bajo la viga y tubo recto, como la mensula interior ancha de la que sale. */
    public static final AjustesColgante DEFECTO = new AjustesColgante(AisladorColgante.COLGADO, FormaColgante.DIAGONAL,
            ANCHURA.defecto(), ALTURA_CATENARIA.defecto());

    /** Los mismos ajustes con los valores dentro de sus limites (lo que llega por red no es de fiar). */
    public AjustesColgante validados() {
        return new AjustesColgante(aislador, forma, ANCHURA.ajustar(anchura), ALTURA_CATENARIA.ajustar(alturaCatenaria));
    }

    public AjustesColgante conAislador(AisladorColgante v) {
        return new AjustesColgante(v, forma, anchura, alturaCatenaria);
    }

    public AjustesColgante conForma(FormaColgante v) {
        return new AjustesColgante(aislador, v, anchura, alturaCatenaria);
    }

    public AjustesColgante conAnchura(float v) {
        return new AjustesColgante(aislador, forma, v, alturaCatenaria);
    }

    public AjustesColgante conAlturaCatenaria(float v) {
        return new AjustesColgante(aislador, forma, anchura, v);
    }

    // ------------------------------------------------------------------ NBT y red

    private static final String AISLADOR = "Aislador";
    private static final String FORMA = "Forma";
    private static final String ANCHURA_NBT = "Width";
    private static final String ALTURA_CATENARIA_NBT = "CatenaryHeight";

    public CompoundTag escribir(CompoundTag nbt) {
        nbt.putString(AISLADOR, aislador.getSerializedName());
        nbt.putString(FORMA, forma.getSerializedName());
        nbt.putFloat(ANCHURA_NBT, anchura);
        nbt.putFloat(ALTURA_CATENARIA_NBT, alturaCatenaria);
        return nbt;
    }

    /** Lee lo que haya; lo que falte se queda por defecto. */
    public static AjustesColgante leer(CompoundTag nbt) {
        AjustesColgante d = DEFECTO;
        return new AjustesColgante(
                nbt.contains(AISLADOR) ? AisladorColgante.porNombre(nbt.getString(AISLADOR)) : d.aislador(),
                nbt.contains(FORMA) ? FormaColgante.porNombre(nbt.getString(FORMA)) : d.forma(),
                nbt.contains(ANCHURA_NBT) ? nbt.getFloat(ANCHURA_NBT) : d.anchura(),
                nbt.contains(ALTURA_CATENARIA_NBT) ? nbt.getFloat(ALTURA_CATENARIA_NBT) : d.alturaCatenaria())
                .validados();
    }

    public void escribir(FriendlyByteBuf buf) {
        buf.writeNbt(escribir(new CompoundTag()));
    }

    public static AjustesColgante leer(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();
        return nbt == null ? DEFECTO : leer(nbt);
    }
}

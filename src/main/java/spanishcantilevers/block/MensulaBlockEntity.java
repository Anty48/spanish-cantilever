package spanishcantilevers.block;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.geometry.MensulaDoble;

/**
 * Guarda el nodo de cables de PNW y los ajustes de la mensula. Como en PNW, el objeto los trae en
 * su "BlockEntityTag" y al colocarlo pasan aqui solos.
 *
 * <p>Si es una mensula doble ({@link MensulaDoble}) guarda tambien la segunda y hacia que lado de la via
 * (en el sistema de la mensula, Z) quedo: la primera queda al otro lado.
 */
public class MensulaBlockEntity extends WireConnectorBlockEntity {
    private static final String SEGUNDA = "Second";
    private static final String LADO = "SecondSide";

    private Ajustes ajustes = Ajustes.DEFECTO;
    @Nullable
    private Ajustes segunda;
    /** +1 o -1: la segunda mensula queda hacia +Z o -Z de la mensula. */
    private int lado = 1;

    public MensulaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MENSULA_BE.get(), pos, state);
    }

    public Ajustes getAjustes() {
        return ajustes;
    }

    public boolean doble() {
        return segunda != null;
    }

    @Nullable
    public Ajustes getSegunda() {
        return segunda;
    }

    public int getLado() {
        return lado;
    }

    /** Los ajustes de la primera (0) o de la segunda (1); en una sencilla, siempre los de la unica. */
    public Ajustes getAjustes(int cual) {
        return cual == 1 && segunda != null ? segunda : ajustes;
    }

    /** Cambia los ajustes, los guarda y los manda a los clientes (que vuelven a dibujar la mensula). */
    public void setAjustes(Ajustes nuevos) {
        setAjustes(0, nuevos);
    }

    /**
     * Cambia la primera (0) o la segunda (1). En una doble se aplican sus reglas: la otra copia lo que va
     * igual en las dos y la altura del soporte no se aleja de la suya.
     */
    public void setAjustes(int cual, Ajustes nuevos) {
        nuevos = nuevos.validados();
        if (segunda == null) {
            if (!nuevos.equals(ajustes)) {
                ajustes = nuevos;
                cambiado();
            }
            return;
        }
        Ajustes antes = getAjustes(cual);
        Ajustes[] par = MensulaDoble.ajustar(nuevos, antes, getAjustes(1 - cual));
        Ajustes primera = cual == 0 ? par[0] : par[1];
        Ajustes otra = cual == 0 ? par[1] : par[0];
        if (!primera.equals(ajustes) || !otra.equals(segunda)) {
            ajustes = primera;
            segunda = otra;
            cambiado();
        }
    }

    /**
     * Anade la segunda mensula hacia {@code lado} (+1/-1 en Z de la mensula). Lo del objeto se adapta a la
     * que ya estaba ({@link MensulaDoble#nueva}), y la que ya estaba pierde lo que no vale en una doble.
     */
    public void bifurcar(Ajustes delObjeto, int lado) {
        ajustes = MensulaDoble.limpiar(ajustes);
        segunda = MensulaDoble.nueva(ajustes, delObjeto);
        this.lado = lado >= 0 ? 1 : -1;
        cambiado();
    }

    private void cambiado() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ajustes.escribir(tag);
        if (segunda != null) {
            tag.put(SEGUNDA, segunda.escribir(new CompoundTag()));
            tag.putInt(LADO, lado);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        Ajustes antes = ajustes;
        Ajustes segundaAntes = segunda;
        int ladoAntes = lado;
        ajustes = Ajustes.leer(tag);
        segunda = tag.contains(SEGUNDA) ? Ajustes.leer(tag.getCompound(SEGUNDA)) : null;
        lado = tag.getInt(LADO) < 0 ? -1 : 1;
        boolean igual = antes.equals(ajustes) && Objects.equals(segundaAntes, segunda) && ladoAntes == lado;
        if (level != null && level.isClientSide && !igual) {
            // el modelo depende de los ajustes: pedirlo de nuevo y volver a dibujar el bloque
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}

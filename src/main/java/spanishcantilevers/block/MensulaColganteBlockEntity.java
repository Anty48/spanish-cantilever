package spanishcantilevers.block;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.geometry.AjustesColgante;

/** Guarda el nodo de cables de PNW y los ajustes de la mensula colgante (como {@link MensulaBlockEntity}). */
public class MensulaColganteBlockEntity extends WireConnectorBlockEntity {
    private AjustesColgante ajustes = AjustesColgante.DEFECTO;

    public MensulaColganteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MENSULA_COLGANTE_BE.get(), pos, state);
    }

    public AjustesColgante getAjustes() {
        return ajustes;
    }

    /** Cambia los ajustes, los guarda y los manda a los clientes (que vuelven a dibujarla). */
    public void setAjustes(AjustesColgante nuevos) {
        nuevos = nuevos.validados();
        if (nuevos.equals(ajustes)) {
            return;
        }
        ajustes = nuevos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ajustes.escribir(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        AjustesColgante antes = ajustes;
        ajustes = AjustesColgante.leer(tag);
        if (level != null && level.isClientSide && !antes.equals(ajustes)) {
            // el modelo depende de los ajustes: pedirlo de nuevo y volver a dibujar el bloque
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}

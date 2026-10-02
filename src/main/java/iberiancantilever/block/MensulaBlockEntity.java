package iberiancantilever.block;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import iberiancantilever.ModBlocks;
import iberiancantilever.geometry.Ajustes;

/**
 * Guarda el nodo de cables de PNW y los ajustes de la mensula. Como en PNW, el objeto los trae en
 * su "BlockEntityTag" y al colocarlo pasan aqui solos.
 */
public class MensulaBlockEntity extends WireConnectorBlockEntity {
    private Ajustes ajustes = Ajustes.DEFECTO;

    public MensulaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MENSULA_BE.get(), pos, state);
    }

    public Ajustes getAjustes() {
        return ajustes;
    }

    /** Cambia los ajustes, los guarda y los manda a los clientes (que vuelven a dibujar la mensula). */
    public void setAjustes(Ajustes nuevos) {
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
        Ajustes antes = ajustes;
        ajustes = Ajustes.leer(tag);
        if (level != null && level.isClientSide && !antes.equals(ajustes)) {
            // el modelo depende de los ajustes: pedirlo de nuevo y volver a dibujar el bloque
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}

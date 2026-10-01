package spanishcantilever.block;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilever.ModBlocks;

/** Solo guarda el nodo de cables de PNW; los ajustes van en el estado del bloque. */
public class MensulaBlockEntity extends WireConnectorBlockEntity {
    public MensulaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MENSULA_BE.get(), pos, state);
    }
}

package iberiancantilever.block;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import iberiancantilever.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Solo guarda el nodo de cables de PNW del soporte de tunel. */
public class SoporteTunelBlockEntity extends WireConnectorBlockEntity {
    public SoporteTunelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SOPORTE_TUNEL_BE.get(), pos, state);
    }
}

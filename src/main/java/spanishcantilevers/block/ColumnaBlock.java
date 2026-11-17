package spanishcantilevers.block;

import de.mrjulsen.paw.block.ConcretePillarBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.property.EPostPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Columna cilindrica de hormigon: el poste de hormigon fino de PNW (concrete_post) con el modelo del usuario.
 * Como en PNW, el de abajo de una columna lleva la base y los de encima solo el fuste (lo decide la parte,
 * PART, que pone PNW). No se oxida. Las mensulas y los soportes se enganchan a ella (etiqueta de 6 px).
 */
public class ColumnaBlock extends ConcretePillarBlock {
    /** El fuste: una cruz de 6 px con el centro de 4 px (el cilindro del modelo). */
    protected static final VoxelShape FUSTE = Shapes.or(Block.box(6, 0, 6, 10, 16, 10), Block.box(7, 0, 5, 9, 16, 11),
            Block.box(5, 0, 7, 11, 16, 9));
    private static final VoxelShape CON_BASE = Shapes.or(FUSTE, Block.box(4, 0, 4, 12, 3, 12));

    public ColumnaBlock(Properties properties, IWeatheringBlock.WeatherData<ConcretePillarBlock> datos) {
        // false: el poste fino de PNW, no el pilar grueso
        super(properties, datos, false);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == EPostPart.BOTTOM ? CON_BASE : FUSTE;
    }
}

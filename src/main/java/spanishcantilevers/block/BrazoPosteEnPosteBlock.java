package spanishcantilevers.block;

import de.mrjulsen.paw.block.CantileverBracketPostConnectionBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilevers.ModPostes;

/** Brazo de poste espanol junto a un poste de celosia: si el poste se quita vuelve a ser el suelto espanol. */
public class BrazoPosteEnPosteBlock extends CantileverBracketPostConnectionBlock {
    public BrazoPosteEnPosteBlock(BlockBehaviour.Properties properties, IWeatheringBlock.WeatherData<CantileverBracketPostConnectionBlock> datos) {
        super(properties, datos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : ModPostes.aEspanol(state, getWeatheringData().isWaxed());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        return ModPostes.aEspanol(super.updateShape(state, direction, neighborState, level, currentPos, neighborPos),
                getWeatheringData().isWaxed());
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ModPostes.objetoBrazo(getWeatheringData().isWaxed());
    }
}

package spanishcantilevers.block;

import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilevers.ModPostes;

/** Tramo vertical del brazo de poste espanol (encima o debajo de un brazo). */
public class BrazoPosteVerticalBlock extends CantileverBracketVerticalBlock {
    public BrazoPosteVerticalBlock(BlockBehaviour.Properties properties, IWeatheringBlock.WeatherData<CantileverBracketVerticalBlock> datos) {
        super(properties, datos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ModPostes.objetoBrazo(getWeatheringData().isWaxed());
    }
}

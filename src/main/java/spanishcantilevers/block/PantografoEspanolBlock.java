package spanishcantilevers.block;

import de.mrjulsen.paw.block.PantographBlock;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import spanishcantilevers.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pantografo espanol: el de PNW tal cual (mismo modelo, animacion y funcionamiento, tambien montado en
 * trenes de Create), pero con el cuerpo rojo como los de aqui. Solo cambian su block entity (para que
 * se dibuje con su textura) y el objeto que da.
 */
public class PantografoEspanolBlock extends PantographBlock {
    public PantografoEspanolBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.PANTOGRAFO_ITEM.get());
    }

    @Override
    public BlockEntityType<? extends PantographBlockEntity> getBlockEntityType() {
        return ModBlocks.PANTOGRAFO_BE.get();
    }
}

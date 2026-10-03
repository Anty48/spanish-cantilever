package iberiancantilever.item;

import java.util.function.Consumer;

import de.mrjulsen.paw.item.PantographItem;
import de.mrjulsen.paw.item.forge.PantographItemImpl;
import iberiancantilever.client.PantografoIbericoRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** El objeto del pantografo iberico: el de PNW (con su animacion en la mano), dibujado con la textura roja. */
public class PantografoIbericoItem extends PantographItemImpl {
    public PantografoIbericoItem(Block block, Properties properties) {
        super(block, properties, false);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<PantographItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(PantografoIbericoRenderer.modeloObjeto());
                }
                return renderer;
            }
        });
    }
}

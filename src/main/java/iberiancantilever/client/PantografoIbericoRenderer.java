package iberiancantilever.client;

import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import de.mrjulsen.paw.blockentity.client.PantographBlockModel;
import de.mrjulsen.paw.item.PantographItem;
import iberiancantilever.IberianCantilever;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Dibuja el pantografo iberico: el modelo y la animacion de PNW (PantographBlockRenderer) con la
 * textura de cuerpo rojo (textures/block/pantografo_iberico.png).
 */
public class PantografoIbericoRenderer extends GeoBlockRenderer<PantographBlockEntity> {
    public static final ResourceLocation TEXTURA = new ResourceLocation(IberianCantilever.MOD_ID, "textures/block/pantografo_iberico.png");

    public PantografoIbericoRenderer(BlockEntityRendererProvider.Context context) {
        super(new PantographBlockModel() {
            @Override
            public ResourceLocation getTextureResource(PantographBlockEntity animatable) {
                return TEXTURA;
            }
        });
    }

    /** El del objeto en la mano, como el de PNW (PantographItemImpl) pero con la textura roja. */
    public static GeoModel<PantographItem> modeloObjeto() {
        return new DefaultedBlockGeoModel<PantographItem>(new ResourceLocation("pantographsandwires", "pantograph")) {
            @Override
            public ResourceLocation getTextureResource(PantographItem animatable) {
                return TEXTURA;
            }
        };
    }
}

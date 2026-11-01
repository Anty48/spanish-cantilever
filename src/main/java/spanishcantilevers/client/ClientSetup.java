package spanishcantilevers.client;

import java.util.Map;

import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.ModPostes;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import spanishcantilevers.SpanishCantilevers;

@EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    /**
     * Los postes espanoles se dibujan con el mismo modelo girable de PNW que sus postes (16 direcciones);
     * se registra en DragonLib al construir el mod, como hace PNW.
     */
    public static void registrarModelos(IEventBus bus) {
        for (DeferredHolder<Block, ? extends Block> poste : ModPostes.POSTES.values()) {
            DLBlockModelRegistry.registerForBlock(() -> poste.get(), BasicRotatableBlockModel::new, BasicRotatableBlockModel::new);
        }
    }

    /** El pantografo espanol se dibuja como el de PNW (GeckoLib), con su textura. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.PANTOGRAFO_BE.get(), PantografoEspanolRenderer::new);
    }

    /** Sustituye el modelo vacio de todos los estados de la mensula por el modelo montado. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Pieza.clearCache();
        PerfilRigidoRenderer.limpiar();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ModelResourceLocation mrl = entry.getKey();
            if (mrl.id().getNamespace().equals(SpanishCantilevers.MOD_ID)
                    && !mrl.variant().equals("inventory")) {
                // la mensula y el soporte de tunel se montan con las piezas de Blockbench
                if (mrl.id().getPath().equals("mensula")) {
                    entry.setValue(new MensulaBakedModel(entry.getValue()));
                } else if (mrl.id().getPath().equals("soporte_tunel")) {
                    entry.setValue(new SoporteTunelBakedModel(entry.getValue()));
                }
            }
        }
    }
}

package spanishcantilevers.client;

import java.util.Map;

import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.ModPostes;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import spanishcantilevers.SpanishCantilevers;

@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    /**
     * Los postes espanoles se dibujan con el mismo modelo girable de PNW que sus postes (16 direcciones);
     * se registra en DragonLib al construir el mod, como hace PNW.
     */
    public static void registrarModelos() {
        for (RegistryObject<? extends Block> poste : ModPostes.POSTES.values()) {
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
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation mrl
                    && mrl.getNamespace().equals(SpanishCantilevers.MOD_ID)
                    && !mrl.getVariant().equals("inventory")) {
                // la mensula y el soporte de tunel se montan con las piezas de Blockbench
                if (mrl.getPath().equals("mensula")) {
                    entry.setValue(new MensulaBakedModel(entry.getValue()));
                } else if (mrl.getPath().equals("soporte_tunel")) {
                    entry.setValue(new SoporteTunelBakedModel(entry.getValue()));
                }
            }
        }
    }
}

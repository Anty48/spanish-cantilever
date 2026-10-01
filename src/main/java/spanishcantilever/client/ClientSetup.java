package spanishcantilever.client;

import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import spanishcantilever.SpanishCantilever;

@Mod.EventBusSubscriber(modid = SpanishCantilever.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    /** Sustituye el modelo vacio de todos los estados de la mensula por el modelo montado. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Pieza.clearCache();
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation mrl
                    && mrl.getNamespace().equals(SpanishCantilever.MOD_ID)
                    && mrl.getPath().equals("mensula")
                    && !mrl.getVariant().equals("inventory")) {
                entry.setValue(new MensulaBakedModel(entry.getValue()));
            }
        }
    }
}

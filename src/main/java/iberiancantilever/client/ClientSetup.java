package iberiancantilever.client;

import java.util.Map;

import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import iberiancantilever.ModPostes;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import iberiancantilever.IberianCantilever;

@Mod.EventBusSubscriber(modid = IberianCantilever.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    /**
     * Los postes ibericos se dibujan con el mismo modelo girable de PNW que sus postes (16 direcciones);
     * se registra en DragonLib al construir el mod, como hace PNW.
     */
    public static void registrarModelos() {
        for (RegistryObject<? extends Block> poste : ModPostes.POSTES.values()) {
            DLBlockModelRegistry.registerForBlock(() -> poste.get(), BasicRotatableBlockModel::new, BasicRotatableBlockModel::new);
        }
    }

    /** Sustituye el modelo vacio de todos los estados de la mensula por el modelo montado. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Pieza.clearCache();
        PerfilRigidoRenderer.limpiar();
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation mrl
                    && mrl.getNamespace().equals(IberianCantilever.MOD_ID)
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

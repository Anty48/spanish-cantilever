package spanishcantilevers;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import spanishcantilevers.cable.ModCables;
import spanishcantilevers.cable.TiranteDiagonal;
import spanishcantilevers.client.ClientSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(SpanishCantilevers.MOD_ID)
public class SpanishCantilevers {
    public static final String MOD_ID = "spanishcantilevers";

    public SpanishCantilevers(IEventBus bus) {
        ModPostes.init();
        ModCables.init();
        TiranteDiagonal.init();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModBlocks.CREATIVE_TABS.register(bus);
        bus.addListener(SpanishCantilevers::alPreparar);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.registrarModelos(bus);
        }
    }

    /** El pantografo espanol funciona en los trenes de Create como el de PNW: con sus mismos comportamientos. */
    private static void alPreparar(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MovementBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographMovementBehaviour());
            MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographInteractionBehaviour());
        });
    }
}

package spanishcantilevers;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import spanishcantilevers.cable.ModCables;
import spanishcantilevers.cable.TiranteDiagonal;
import spanishcantilevers.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import spanishcantilevers.network.ModRed;

@Mod(SpanishCantilevers.MOD_ID)
public class SpanishCantilevers {
    public static final String MOD_ID = "spanishcantilevers";

    public SpanishCantilevers() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModPostes.init();
        ModCables.init();
        TiranteDiagonal.init();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModBlocks.CREATIVE_TABS.register(bus);
        ModRed.registrar();
        bus.addListener(SpanishCantilevers::alPreparar);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::registrarModelos);
    }

    /** El pantografo espanol funciona en los trenes de Create como el de PNW: con sus mismos comportamientos. */
    private static void alPreparar(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MovementBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographMovementBehaviour());
            MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographInteractionBehaviour());
        });
    }
}

package iberiancantilever;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import iberiancantilever.cable.ModCables;
import iberiancantilever.cable.TiranteDiagonal;
import iberiancantilever.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import iberiancantilever.network.ModRed;

@Mod(IberianCantilever.MOD_ID)
public class IberianCantilever {
    public static final String MOD_ID = "iberiancantilever";

    public IberianCantilever() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModPostes.init();
        ModCables.init();
        TiranteDiagonal.init();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModBlocks.CREATIVE_TABS.register(bus);
        ModRed.registrar();
        bus.addListener(IberianCantilever::alPreparar);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::registrarModelos);
    }

    /** El pantografo iberico funciona en los trenes de Create como el de PNW: con sus mismos comportamientos. */
    private static void alPreparar(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MovementBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographMovementBehaviour());
            MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PANTOGRAFO.get(), new PantographInteractionBehaviour());
        });
    }
}

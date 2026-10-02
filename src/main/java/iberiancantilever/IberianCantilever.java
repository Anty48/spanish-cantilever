package iberiancantilever;

import iberiancantilever.cable.ModCables;
import iberiancantilever.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import iberiancantilever.network.ModRed;

@Mod(IberianCantilever.MOD_ID)
public class IberianCantilever {
    public static final String MOD_ID = "iberiancantilever";

    public IberianCantilever() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModPostes.init();
        ModCables.init();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModBlocks.CREATIVE_TABS.register(bus);
        ModRed.registrar();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::registrarModelos);
    }
}

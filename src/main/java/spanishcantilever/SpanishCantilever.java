package spanishcantilever;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpanishCantilever.MOD_ID)
public class SpanishCantilever {
    public static final String MOD_ID = "spanishcantilever";

    public SpanishCantilever() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModBlocks.CREATIVE_TABS.register(bus);
    }
}

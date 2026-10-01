package spanishcantilever;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import spanishcantilever.block.MensulaBlock;
import spanishcantilever.block.MensulaBlockEntity;

public final class ModBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SpanishCantilever.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SpanishCantilever.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SpanishCantilever.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpanishCantilever.MOD_ID);

    public static final RegistryObject<MensulaBlock> MENSULA = BLOCKS.register("mensula",
            () -> new MensulaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> MENSULA_ITEM = ITEMS.register("mensula",
            () -> new BlockItem(MENSULA.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MensulaBlockEntity>> MENSULA_BE = BLOCK_ENTITIES.register("mensula",
            () -> BlockEntityType.Builder.of(MensulaBlockEntity::new, MENSULA.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spanishcantilever"))
                    .icon(() -> MENSULA_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> output.accept(MENSULA_ITEM.get()))
                    .build());

    private ModBlocks() {
    }
}

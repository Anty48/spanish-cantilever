package spanishcantilevers;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.block.PantografoEspanolBlock;
import spanishcantilevers.block.MensulaBlockEntity;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.block.SoporteTunelBlockEntity;
import spanishcantilevers.item.MensulaItem;
import spanishcantilevers.item.PantografoEspanolItem;
import spanishcantilevers.item.PerfilRigidoItem;
import spanishcantilevers.item.SoporteTunelItem;

public final class ModBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, SpanishCantilevers.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SpanishCantilevers.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SpanishCantilevers.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpanishCantilevers.MOD_ID);

    /** Tan dura como la mensula de PNW: se pica con pico de piedra o mejor. */
    public static final DeferredHolder<net.minecraft.world.level.block.Block, MensulaBlock> MENSULA = BLOCKS.register("mensula",
            () -> new MensulaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, Item> MENSULA_ITEM = ITEMS.register("mensula",
            () -> new MensulaItem(MENSULA.get(), new Item.Properties()));

    /** Acero espanol: hierro y tinte negro. Material de los postes y de la mensula. */
    public static final DeferredHolder<Item, Item> ACERO_IBERICO = ITEMS.register("iberian_steel_ingot", () -> new Item(new Item.Properties()));

    /** La mensula a medio montar (paso intermedio del montaje secuenciado de Create, como la de PNW). */
    public static final DeferredHolder<Item, Item> MENSULA_INCOMPLETA = ITEMS.register("mensula_incompleta",
            () -> new SequencedAssemblyItem(new Item.Properties()));

    /** Soporte de catenaria rigida de tunel: cuelga del techo. */
    public static final DeferredHolder<net.minecraft.world.level.block.Block, SoporteTunelBlock> SOPORTE_TUNEL = BLOCKS.register("soporte_tunel",
            () -> new SoporteTunelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, Item> SOPORTE_TUNEL_ITEM = ITEMS.register("soporte_tunel",
            () -> new SoporteTunelItem(SOPORTE_TUNEL.get(), new Item.Properties()));

    /** Haz de perfil rigido: se tiende entre soportes como un cable y se gasta por metros. */
    public static final DeferredHolder<Item, Item> PERFIL_RIGIDO = ITEMS.register("perfil_rigido",
            () -> new PerfilRigidoItem(new Item.Properties().stacksTo(1)));

    /** Pantografo espanol: el de PNW con el cuerpo rojo. */
    public static final DeferredHolder<net.minecraft.world.level.block.Block, PantografoEspanolBlock> PANTOGRAFO = BLOCKS.register("pantografo_espanol",
            () -> new PantografoEspanolBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredHolder<Item, Item> PANTOGRAFO_ITEM = ITEMS.register("pantografo_espanol",
            () -> new PantografoEspanolItem(PANTOGRAFO.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PantographBlockEntity>> PANTOGRAFO_BE = BLOCK_ENTITIES.register("pantografo_espanol",
            () -> BlockEntityType.Builder.<PantographBlockEntity>of((pos, state) -> new PantographBlockEntity(ModBlocks.PANTOGRAFO_BE.get(), pos, state),
                    PANTOGRAFO.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoporteTunelBlockEntity>> SOPORTE_TUNEL_BE = BLOCK_ENTITIES.register("soporte_tunel",
            () -> BlockEntityType.Builder.of(SoporteTunelBlockEntity::new, SOPORTE_TUNEL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MensulaBlockEntity>> MENSULA_BE = BLOCK_ENTITIES.register("mensula",
            () -> BlockEntityType.Builder.of(MensulaBlockEntity::new, MENSULA.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spanishcantilevers"))
                    .icon(() -> MENSULA_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(MENSULA_ITEM.get());
                        output.accept(ACERO_IBERICO.get());
                        output.accept(SOPORTE_TUNEL_ITEM.get());
                        output.accept(PERFIL_RIGIDO.get());
                        output.accept(PANTOGRAFO_ITEM.get());
                        ModPostes.conObjeto().forEach(output::accept);
                    })
                    .build());

    private ModBlocks() {
    }
}

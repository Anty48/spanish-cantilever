package iberiancantilever;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import iberiancantilever.block.MensulaBlock;
import iberiancantilever.block.PantografoIbericoBlock;
import iberiancantilever.block.MensulaBlockEntity;
import iberiancantilever.block.SoporteTunelBlock;
import iberiancantilever.block.SoporteTunelBlockEntity;
import iberiancantilever.item.MensulaItem;
import iberiancantilever.item.PantografoIbericoItem;
import iberiancantilever.item.PerfilRigidoItem;
import iberiancantilever.item.SoporteTunelItem;

public final class ModBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IberianCantilever.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IberianCantilever.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IberianCantilever.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IberianCantilever.MOD_ID);

    /** Tan dura como la mensula de PNW: se pica con pico de piedra o mejor. */
    public static final RegistryObject<MensulaBlock> MENSULA = BLOCKS.register("mensula",
            () -> new MensulaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> MENSULA_ITEM = ITEMS.register("mensula",
            () -> new MensulaItem(MENSULA.get(), new Item.Properties()));

    /** Acero iberico: hierro y tinte negro. Material de los postes y de la mensula. */
    public static final RegistryObject<Item> ACERO_IBERICO = ITEMS.register("iberian_steel", () -> new Item(new Item.Properties()));

    /** La mensula a medio montar (paso intermedio del montaje secuenciado de Create, como la de PNW). */
    public static final RegistryObject<Item> MENSULA_INCOMPLETA = ITEMS.register("mensula_incompleta",
            () -> new SequencedAssemblyItem(new Item.Properties()));

    /** Soporte de catenaria rigida de tunel: cuelga del techo. */
    public static final RegistryObject<SoporteTunelBlock> SOPORTE_TUNEL = BLOCKS.register("soporte_tunel",
            () -> new SoporteTunelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> SOPORTE_TUNEL_ITEM = ITEMS.register("soporte_tunel",
            () -> new SoporteTunelItem(SOPORTE_TUNEL.get(), new Item.Properties()));

    /** Haz de perfil rigido: se tiende entre soportes como un cable y se gasta por metros. */
    public static final RegistryObject<Item> PERFIL_RIGIDO = ITEMS.register("perfil_rigido",
            () -> new PerfilRigidoItem(new Item.Properties().stacksTo(1)));

    /** Pantografo iberico: el de PNW con el cuerpo rojo. */
    public static final RegistryObject<PantografoIbericoBlock> PANTOGRAFO = BLOCKS.register("pantografo_iberico",
            () -> new PantografoIbericoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> PANTOGRAFO_ITEM = ITEMS.register("pantografo_iberico",
            () -> new PantografoIbericoItem(PANTOGRAFO.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PantographBlockEntity>> PANTOGRAFO_BE = BLOCK_ENTITIES.register("pantografo_iberico",
            () -> BlockEntityType.Builder.<PantographBlockEntity>of((pos, state) -> new PantographBlockEntity(ModBlocks.PANTOGRAFO_BE.get(), pos, state),
                    PANTOGRAFO.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SoporteTunelBlockEntity>> SOPORTE_TUNEL_BE = BLOCK_ENTITIES.register("soporte_tunel",
            () -> BlockEntityType.Builder.of(SoporteTunelBlockEntity::new, SOPORTE_TUNEL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MensulaBlockEntity>> MENSULA_BE = BLOCK_ENTITIES.register("mensula",
            () -> BlockEntityType.Builder.of(MensulaBlockEntity::new, MENSULA.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.iberiancantilever"))
                    .icon(() -> MENSULA_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(MENSULA_ITEM.get());
                        output.accept(ACERO_IBERICO.get());
                        output.accept(SOPORTE_TUNEL_ITEM.get());
                        output.accept(PERFIL_RIGIDO.get());
                        output.accept(PANTOGRAFO_ITEM.get());
                        ModPostes.POSTES.values().forEach(b -> output.accept(b.get()));
                    })
                    .build());

    private ModBlocks() {
    }
}

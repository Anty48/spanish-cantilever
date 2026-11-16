package spanishcantilevers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.paw.block.CantileverBracketBlock;
import de.mrjulsen.paw.block.CantileverBracketPostConnectionBlock;
import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.FlatLatticeMastBlock;
import de.mrjulsen.paw.block.HBeamMastBlock;
import de.mrjulsen.paw.block.LatticeMastBlock;
import de.mrjulsen.paw.block.PowerLineBracketBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import spanishcantilevers.block.BrazoPosteBlock;
import spanishcantilevers.block.BrazoPosteEnPosteBlock;
import spanishcantilevers.block.BrazoPosteVerticalBlock;
import spanishcantilevers.block.RemateAisladoresBlock;

/**
 * Postes y piezas de poste espanoles: los de PNW (celosia plana, plana diagonal y cuadrada, poste H,
 * brazo de poste y soporte de linea) con el metal gris espanol. Usan las clases de PNW, asi que se
 * comportan igual. Solo hay el estado nuevo, sin encerar y encerado: al oxidarse pasan al expuesto de
 * PNW y desde ahi siguen la cadena de PNW (expuesto -> erosionado -> oxidado). Los cuatro postes tienen
 * tambien una version azul (el metal de la alta velocidad, LAV), que no se oxida (sin version encerada), y
 * hay remates azules y grises con aisladores para los cables de corriente ({@link RemateAisladoresBlock}).
 * Assets y datos: tools/generar_postes.py.
 */
public final class ModPostes {
    /** Todos los bloques por id, en el orden de la pestana creativa. */
    public static final Map<String, DeferredHolder<Block, ? extends Block>> POSTES = new LinkedHashMap<>();
    /** Prefijo de los postes azules (LAV). */
    public static final String AZUL = "blue_";
    /** Los remates con aisladores (para su tipo de bloque-entidad y su modelo). */
    public static final Map<String, DeferredHolder<Block, RemateAisladoresBlock>> REMATES = new LinkedHashMap<>();

    static {
        pieza("flat_lattice_mast", FlatLatticeMastBlock::new, true);
        pieza("flat_lattice_mast_diagonal", FlatLatticeMastBlock::new, true);
        pieza("lattice_mast", LatticeMastBlock::new, true);
        pieza("h_beam_mast", HBeamMastBlock::new, true);
        pieza("power_line_bracket", PowerLineBracketBlock::new, true);
        // el brazo de poste son tres bloques (suelto, junto a un poste de celosia y vertical); solo el
        // primero tiene objeto, los otros dos sueltan ese
        ModPostes.<CantileverBracketBlock>pieza("cantilever_bracket", BrazoPosteBlock::new, true);
        ModPostes.<CantileverBracketPostConnectionBlock>pieza("cantilever_bracket_at_post", BrazoPosteEnPosteBlock::new, false);
        ModPostes.<CantileverBracketVerticalBlock>pieza("cantilever_bracket_vertical", BrazoPosteVerticalBlock::new, false);
        // los postes azules de la alta velocidad
        pieza("flat_lattice_mast", FlatLatticeMastBlock::new, true, AZUL);
        pieza("flat_lattice_mast_diagonal", FlatLatticeMastBlock::new, true, AZUL);
        pieza("lattice_mast", LatticeMastBlock::new, true, AZUL);
        pieza("h_beam_mast", HBeamMastBlock::new, true, AZUL);
        // remates con aisladores: azules y grises, plano y plano diagonal, con uno o dos aisladores
        for (String color : new String[]{AZUL, ""}) {
            for (boolean diagonal : new boolean[]{false, true}) {
                for (boolean doble : new boolean[]{false, true}) {
                    remate(color, diagonal, doble);
                }
            }
        }
    }

    /** El nodo de cables de los remates con aisladores (el tipo de PNW solo vale para sus bloques). */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireConnectorBlockEntity>> REMATE_BE = ModBlocks.BLOCK_ENTITIES.register(
            "mast_top_insulator", () -> BlockEntityType.Builder.<WireConnectorBlockEntity>of(
                    (pos, state) -> new WireConnectorBlockEntity(ModPostes.REMATE_BE.get(), pos, state),
                    REMATES.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    private ModPostes() {
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }

    /** Las clases de PNW se construyen con sus propiedades y sus datos de oxidacion. */
    public interface Fabrica<T extends Block & IWeatheringBlock<T>> {
        T crear(BlockBehaviour.Properties propiedades, IWeatheringBlock.WeatherData<T> datos);
    }

    /** Los postes son "ibericos" y las demas piezas "espanolas" (un poco de variedad en los nombres). */
    public static String id(String pnw, boolean encerado) {
        return id(pnw, encerado, "");
    }

    /** Como {@link #id(String, boolean)}, con el prefijo de color ({@link #AZUL} o nada). */
    public static String id(String pnw, boolean encerado, String color) {
        return (encerado ? "waxed_" : "") + color + (pnw.endsWith("mast") || pnw.endsWith("mast_diagonal") ? "iberian_" : "spanish_") + pnw;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block> T bloque(String id) {
        return (T) POSTES.get(id).get();
    }

    /** El bloque de PNW con ese id (el expuesto al que pasa el espanol al oxidarse). */
    @SuppressWarnings("unchecked")
    private static <T extends Block> Supplier<T> dePnw(String id) {
        return () -> (T) BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("pantographsandwires", id));
    }

    private static <T extends Block & IWeatheringBlock<T>> void pieza(String pnw, Fabrica<T> fabrica, boolean conObjeto) {
        pieza(pnw, fabrica, conObjeto, "");
    }

    private static <T extends Block & IWeatheringBlock<T>> void pieza(String pnw, Fabrica<T> fabrica, boolean conObjeto, String color) {
        // los azules no se oxidan (la pintura de la alta velocidad los protege): no hay encerado
        boolean seOxida = color.isEmpty();
        for (boolean encerado : seOxida ? new boolean[]{false, true} : new boolean[]{false}) {
            String id = id(pnw, encerado, color);
            String gemelo = id(pnw, !encerado, color);
            // sin encerar se oxida al expuesto de PNW; encerado no cambia
            IWeatheringBlock.WeatherData<T> datos = new IWeatheringBlock.WeatherData<>(IWeatheringBlock.WeatherState.UNAFFECTED,
                    seOxida ? new IAgingBlock.BlockTransform<T, IWeatheringBlock.WeatherState>(null,
                            encerado ? null : dePnw(pnw + "_exposed"), () -> bloque(gemelo))
                            : new IAgingBlock.BlockTransform<T, IWeatheringBlock.WeatherState>(null, null, null),
                    encerado);
            DeferredHolder<Block, T> bloque = ModBlocks.BLOCKS.register(id, () -> {
                // acero: tan duro como un bloque de hierro
                BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK);
                if (seOxida && !encerado) {
                    p.randomTicks();
                }
                return fabrica.crear(p, datos);
            });
            POSTES.put(id, bloque);
            if (conObjeto) {
                ModBlocks.ITEMS.register(id, () -> new BlockItem(bloque.get(), new Item.Properties()));
            }
        }
    }

    /**
     * Un remate con aisladores, azul o gris; no se oxida (no hay version encerada). Ids como
     * [blue_]iberian_flat_lattice_mast[_diagonal][_double]_insulator.
     */
    private static void remate(String color, boolean diagonal, boolean doble) {
        String id = color + "iberian_flat_lattice_mast" + (diagonal ? "_diagonal" : "") + (doble ? "_double" : "") + "_insulator";
        IWeatheringBlock.WeatherData<FlatLatticeMastBlock> datos = new IWeatheringBlock.WeatherData<>(IWeatheringBlock.WeatherState.UNAFFECTED,
                new IAgingBlock.BlockTransform<FlatLatticeMastBlock, IWeatheringBlock.WeatherState>(null, null, null), false);
        DeferredHolder<Block, RemateAisladoresBlock> bloque = ModBlocks.BLOCKS.register(id,
                () -> new RemateAisladoresBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), datos, diagonal, doble));
        POSTES.put(id, bloque);
        REMATES.put(id, bloque);
        ModBlocks.ITEMS.register(id, () -> new BlockItem(bloque.get(), new Item.Properties()));
    }

    /** Los bloques con objeto, para la pestana creativa. */
    public static Iterable<Block> conObjeto() {
        return POSTES.values().stream().<Block>map(DeferredHolder::get)
                .filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR).toList();
    }

    // ------------------------------------------------------------------ brazo de poste

    /**
     * PNW cambia el brazo de poste entre sus tres bloques (suelto, junto a un poste, vertical) buscando
     * los suyos; esto pone el espanol equivalente, encerado o no como {@code encerado}.
     */
    public static BlockState aEspanol(BlockState state, boolean encerado) {
        Block b = state.getBlock();
        String pnw;
        if (b instanceof BrazoPosteBlock || b instanceof BrazoPosteEnPosteBlock || b instanceof BrazoPosteVerticalBlock) {
            return state;
        } else if (b instanceof CantileverBracketVerticalBlock) {
            pnw = "cantilever_bracket_vertical";
        } else if (b instanceof CantileverBracketPostConnectionBlock) {
            pnw = "cantilever_bracket_at_post";
        } else if (b instanceof CantileverBracketBlock) {
            pnw = "cantilever_bracket";
        } else {
            return state;
        }
        return POSTES.get(id(pnw, encerado)).get().withPropertiesOf(state);
    }

    /** El objeto del brazo de poste espanol (el que sueltan y dan con la rueda los tres bloques). */
    public static ItemStack objetoBrazo(boolean encerado) {
        return new ItemStack(POSTES.get(id("cantilever_bracket", encerado)).get());
    }
}

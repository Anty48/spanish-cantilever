package spanishcantilevers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.paw.block.CantileverBracketBlock;
import de.mrjulsen.paw.block.CantileverBracketPostConnectionBlock;
import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.ConcretePillarBlock;
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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import spanishcantilevers.block.BrazoPosteBlock;
import spanishcantilevers.block.BrazoPosteEnPosteBlock;
import spanishcantilevers.block.BrazoPosteVerticalBlock;
import spanishcantilevers.block.ColumnaAisladorBlock;
import spanishcantilevers.block.ColumnaBlock;
import spanishcantilevers.block.RemateAisladoresBlock;

/**
 * Postes y piezas de poste espanoles: los de PNW (celosia plana, plana diagonal y cuadrada, poste H,
 * brazo de poste y soporte de linea) con el metal gris espanol. Usan las clases de PNW, asi que se
 * comportan igual. Solo hay el estado nuevo, sin encerar y encerado: al oxidarse pasan al expuesto de
 * PNW y desde ahi siguen la cadena de PNW (expuesto -> erosionado -> oxidado). Los cuatro postes tienen
 * tambien versiones de la alta velocidad (LAV) que no se oxidan (sin version encerada): azul (GIF) y verde
 * (ADIF). Hay remates grises, azules y verdes con aisladores para los cables de corriente
 * ({@link RemateAisladoresBlock}) y la columna cilindrica de hormigon, sola o con aislador
 * ({@link ColumnaBlock}, {@link ColumnaAisladorBlock}).
 * Assets y datos: tools/generar_postes.py.
 */
public final class ModPostes {
    /** Todos los bloques por id, en el orden de la pestana creativa. */
    public static final Map<String, RegistryObject<? extends Block>> POSTES = new LinkedHashMap<>();
    /** Prefijo de los postes azules de la alta velocidad (GIF). */
    public static final String AZUL = "blue_";
    /** Prefijo de los postes verdes de la alta velocidad (ADIF). */
    public static final String VERDE = "green_";
    /** Los colores de la alta velocidad. */
    public static final String[] COLORES_LAV = {AZUL, VERDE};
    /** Los cuatro postes (los que tienen version de color), en el orden de la pestana creativa. */
    private static final String[] FORMAS = {"flat_lattice_mast", "flat_lattice_mast_diagonal", "lattice_mast", "h_beam_mast"};
    /** Los remates con aisladores y la columna con aislador (para su tipo de bloque-entidad y su modelo). */
    public static final Map<String, RegistryObject<? extends Block>> REMATES = new LinkedHashMap<>();
    /** La columna cilindrica de hormigon y la columna con aislador. */
    public static final String COLUMNA = "iberian_concrete_column_mast";
    public static final String COLUMNA_AISLADOR = "iberian_concrete_column_mast_insulator";

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
        // los postes de la alta velocidad: azules (GIF) y verdes (ADIF)
        for (String color : COLORES_LAV) {
            pieza("flat_lattice_mast", FlatLatticeMastBlock::new, true, color);
            pieza("flat_lattice_mast_diagonal", FlatLatticeMastBlock::new, true, color);
            pieza("lattice_mast", LatticeMastBlock::new, true, color);
            pieza("h_beam_mast", HBeamMastBlock::new, true, color);
        }
        // remates con aisladores: azules, verdes y grises, plano y plano diagonal, con uno o dos aisladores
        for (String color : new String[]{AZUL, VERDE, ""}) {
            for (boolean diagonal : new boolean[]{false, true}) {
                for (boolean doble : new boolean[]{false, true}) {
                    remate(color, diagonal, doble);
                }
            }
        }
        columnas();
    }

    /** El nodo de cables de los remates con aisladores (el tipo de PNW solo vale para sus bloques). */
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<WireConnectorBlockEntity>> REMATE_BE = ModBlocks.BLOCK_ENTITIES.register(
            "mast_top_insulator", () -> BlockEntityType.Builder.<WireConnectorBlockEntity>of(
                    (pos, state) -> new WireConnectorBlockEntity(ModPostes.REMATE_BE.get(), pos, state),
                    REMATES.values().stream().map(r -> (Block) r.get()).toArray(Block[]::new)).build(null));

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

    /** Como {@link #id(String, boolean)}, con el prefijo de color ({@link #AZUL}, {@link #VERDE} o nada). */
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
        return () -> (T) ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", id));
    }

    private static <T extends Block & IWeatheringBlock<T>> void pieza(String pnw, Fabrica<T> fabrica, boolean conObjeto) {
        pieza(pnw, fabrica, conObjeto, "");
    }

    private static <T extends Block & IWeatheringBlock<T>> void pieza(String pnw, Fabrica<T> fabrica, boolean conObjeto, String color) {
        // los de color no se oxidan (la pintura de la alta velocidad los protege): no hay encerado
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
            RegistryObject<T> bloque = ModBlocks.BLOCKS.register(id, () -> {
                // acero: tan duro como un bloque de hierro
                BlockBehaviour.Properties p = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK);
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
     * Un remate con aisladores, gris, azul o verde; no se oxida (no hay version encerada). Ids como
     * [blue_|green_]iberian_flat_lattice_mast[_diagonal][_double]_insulator.
     */
    private static void remate(String color, boolean diagonal, boolean doble) {
        String id = idRemate(color, diagonal, doble);
        IWeatheringBlock.WeatherData<FlatLatticeMastBlock> datos = new IWeatheringBlock.WeatherData<>(IWeatheringBlock.WeatherState.UNAFFECTED,
                new IAgingBlock.BlockTransform<FlatLatticeMastBlock, IWeatheringBlock.WeatherState>(null, null, null), false);
        RegistryObject<RemateAisladoresBlock> bloque = ModBlocks.BLOCKS.register(id,
                () -> new RemateAisladoresBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion(), datos, diagonal, doble));
        POSTES.put(id, bloque);
        REMATES.put(id, bloque);
        ModBlocks.ITEMS.register(id, () -> new BlockItem(bloque.get(), new Item.Properties()));
    }

    /** La columna de hormigon (de piedra: dura como la piedra lisa) y la columna con aislador; no se oxidan. */
    private static void columnas() {
        IWeatheringBlock.WeatherData<ConcretePillarBlock> datos = new IWeatheringBlock.WeatherData<>(IWeatheringBlock.WeatherState.UNAFFECTED,
                new IAgingBlock.BlockTransform<ConcretePillarBlock, IWeatheringBlock.WeatherState>(null, null, null), false);
        RegistryObject<ColumnaBlock> columna = ModBlocks.BLOCKS.register(COLUMNA,
                () -> new ColumnaBlock(BlockBehaviour.Properties.copy(Blocks.SMOOTH_STONE), datos));
        RegistryObject<ColumnaAisladorBlock> aislador = ModBlocks.BLOCKS.register(COLUMNA_AISLADOR,
                () -> new ColumnaAisladorBlock(BlockBehaviour.Properties.copy(Blocks.SMOOTH_STONE).noOcclusion(), datos));
        POSTES.put(COLUMNA, columna);
        POSTES.put(COLUMNA_AISLADOR, aislador);
        REMATES.put(COLUMNA_AISLADOR, aislador);
        ModBlocks.ITEMS.register(COLUMNA, () -> new BlockItem(columna.get(), new Item.Properties()));
        ModBlocks.ITEMS.register(COLUMNA_AISLADOR, () -> new BlockItem(aislador.get(), new Item.Properties()));
    }

    private static String idRemate(String color, boolean diagonal, boolean doble) {
        return color + "iberian_flat_lattice_mast" + (diagonal ? "_diagonal" : "") + (doble ? "_double" : "") + "_insulator";
    }

    /**
     * Los bloques con objeto en el orden de la pestana creativa: por familias de color (gris, azul GIF,
     * verde ADIF) y en cada una los postes y sus remates con aisladores; la gris sigue con las piezas de
     * poste, todo lo encerado y la columna de hormigon.
     */
    public static List<Block> ordenCreativo() {
        List<String> ids = new ArrayList<>();
        for (String color : new String[]{"", AZUL, VERDE}) {
            for (String forma : FORMAS) {
                ids.add(id(forma, false, color));
            }
            for (boolean diagonal : new boolean[]{false, true}) {
                for (boolean doble : new boolean[]{false, true}) {
                    ids.add(idRemate(color, diagonal, doble));
                }
            }
            if (color.isEmpty()) {
                ids.add(id("power_line_bracket", false));
                ids.add(id("cantilever_bracket", false));
                for (String forma : FORMAS) {
                    ids.add(id(forma, true));
                }
                ids.add(id("power_line_bracket", true));
                ids.add(id("cantilever_bracket", true));
                ids.add(COLUMNA);
                ids.add(COLUMNA_AISLADOR);
            }
        }
        List<Block> bloques = new ArrayList<>(ids.stream().map(i -> (Block) POSTES.get(i).get()).toList());
        // lo que se registre y no este en la lista va al final, para que no falte
        POSTES.values().stream().map(r -> (Block) r.get())
                .filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR && !bloques.contains(b)).forEach(bloques::add);
        return bloques;
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

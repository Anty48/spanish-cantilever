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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import spanishcantilevers.block.BrazoPosteBlock;
import spanishcantilevers.block.BrazoPosteEnPosteBlock;
import spanishcantilevers.block.BrazoPosteVerticalBlock;

/**
 * Postes y piezas de poste espanoles: los de PNW (celosia plana, plana diagonal y cuadrada, poste H,
 * brazo de poste y soporte de linea) con el metal gris espanol. Usan las clases de PNW, asi que se
 * comportan igual. Solo hay el estado nuevo, sin encerar y encerado: al oxidarse pasan al expuesto de
 * PNW y desde ahi siguen la cadena de PNW (expuesto -> erosionado -> oxidado). Assets y datos:
 * tools/generar_postes.py.
 */
public final class ModPostes {
    /** Todos los bloques por id, en el orden de la pestana creativa. */
    public static final Map<String, RegistryObject<? extends Block>> POSTES = new LinkedHashMap<>();

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
    }

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
        return (encerado ? "waxed_" : "") + (pnw.endsWith("mast") || pnw.endsWith("mast_diagonal") ? "iberian_" : "spanish_") + pnw;
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
        for (boolean encerado : new boolean[]{false, true}) {
            String id = id(pnw, encerado);
            String gemelo = id(pnw, !encerado);
            // sin encerar se oxida al expuesto de PNW; encerado no cambia
            IWeatheringBlock.WeatherData<T> datos = new IWeatheringBlock.WeatherData<>(IWeatheringBlock.WeatherState.UNAFFECTED,
                    new IAgingBlock.BlockTransform<T, IWeatheringBlock.WeatherState>(null,
                            encerado ? null : dePnw(pnw + "_exposed"), () -> bloque(gemelo)),
                    encerado);
            RegistryObject<T> bloque = ModBlocks.BLOCKS.register(id, () -> {
                // acero: tan duro como un bloque de hierro
                BlockBehaviour.Properties p = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK);
                if (!encerado) {
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

    /** Los bloques con objeto, para la pestana creativa. */
    public static Iterable<Block> conObjeto() {
        return POSTES.values().stream().map(r -> (Block) r.get()).filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR).toList();
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

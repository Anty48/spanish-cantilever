package iberiancantilever;

import java.util.LinkedHashMap;
import java.util.Map;

import de.mrjulsen.paw.block.FlatLatticeMastBlock;
import de.mrjulsen.paw.block.LatticeMastBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/**
 * Postes ibericos de celosia (plano, plano diagonal y cuadrado): los de PNW con el metal iberico. Usan
 * las clases de PNW, asi que se comportan igual (se les enganchan las mensulas, se oxidan con el tiempo
 * normal -> expuesto -> erosionado -> oxidado, el panal los encera y el hacha los rasca, con los
 * mixins de PNW). No hay version galvanizada. Assets y datos: tools/generar_postes.py.
 */
public final class ModPostes {
    /** Estados de oxidacion, en orden. */
    private static final IWeatheringBlock.WeatherState[] ESTADOS = IWeatheringBlock.WeatherState.oxidationStates();
    /** Todos los postes por id, en el orden de la pestana creativa. */
    public static final Map<String, RegistryObject<? extends Block>> POSTES = new LinkedHashMap<>();

    static {
        forma("iberian_flat_lattice_mast", FlatLatticeMastBlock::new);
        forma("iberian_flat_lattice_mast_diagonal", FlatLatticeMastBlock::new);
        forma("iberian_lattice_mast", LatticeMastBlock::new);
    }

    /** Las clases de poste de PNW se construyen con sus propiedades y sus datos de oxidacion. */
    private interface Fabrica<T extends Block & IWeatheringBlock<T>> {
        T crear(BlockBehaviour.Properties propiedades, IWeatheringBlock.WeatherData<T> datos);
    }

    private static <T extends Block & IWeatheringBlock<T>> void forma(String forma, Fabrica<T> fabrica) {
        for (boolean encerado : new boolean[]{false, true}) {
            for (int i = 0; i < ESTADOS.length; i++) {
                registrar(forma, i, encerado, fabrica);
            }
        }
    }

    private ModPostes() {
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }

    private static String id(String forma, int estado, boolean encerado) {
        String nombre = ESTADOS[estado].getName();
        return (encerado ? "waxed_" : "") + forma + (nombre.isBlank() ? "" : "_" + nombre);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block & IWeatheringBlock<T>> T poste(String id) {
        return (T) POSTES.get(id).get();
    }

    private static <T extends Block & IWeatheringBlock<T>> void registrar(String forma, int estado, boolean encerado, Fabrica<T> fabrica) {
        String id = id(forma, estado, encerado);
        IWeatheringBlock.WeatherState s = ESTADOS[estado];
        // como PNW: el anterior y el siguiente estado del mismo tipo (encerado o no) y su gemelo encerado/sin encerar
        String anterior = estado > 0 ? id(forma, estado - 1, encerado) : null;
        String siguiente = estado < ESTADOS.length - 1 ? id(forma, estado + 1, encerado) : null;
        String gemelo = id(forma, estado, !encerado);
        IWeatheringBlock.WeatherData<T> datos = new IWeatheringBlock.WeatherData<>(s,
                new IAgingBlock.BlockTransform<T, IWeatheringBlock.WeatherState>(
                        anterior == null ? null : () -> poste(anterior),
                        siguiente == null ? null : () -> poste(siguiente),
                        () -> poste(gemelo)),
                encerado);
        RegistryObject<T> bloque = ModBlocks.BLOCKS.register(id, () -> {
            // acero: tan duro como un bloque de hierro; solo se oxida sin encerar y si aun no esta oxidado
            BlockBehaviour.Properties p = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK);
            if (!encerado && s != IWeatheringBlock.WeatherState.OXIDIZED) {
                p.randomTicks();
            }
            return fabrica.crear(p, datos);
        });
        POSTES.put(id, bloque);
        ModBlocks.ITEMS.register(id, () -> new BlockItem(bloque.get(), new Item.Properties()));
    }
}

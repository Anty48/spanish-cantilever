package spanishcantilevers.cable;

import org.joml.Vector3d;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import spanishcantilevers.SpanishCantilevers;

/**
 * Datos de enganche de un remate con dos aisladores: el primero como un conector normal de PNW (si algun
 * cable de PNW lo lee, va a ese) y el segundo aparte. {@link CableAisladores} tiende un hilo a cada uno.
 */
public class ConectorAisladores extends BasicConnectorDataProvider {
    public static final DLRegistryObject<ConectorAisladores> TIPO = WiresApi.CONNECTOR_DATA_PROVIDER_REGISTRY.register(
            ResourceLocation.fromNamespaceAndPath(SpanishCantilevers.MOD_ID, "double_insulator_connector"), () -> new ConectorAisladores(null, null));
    private static final String NBT_SEGUNDO = "SecondWireAttachPoint";

    private Vector3d segundo;

    public ConectorAisladores(Vector3d primero, Vector3d segundo) {
        super(primero);
        this.segundo = segundo;
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }

    /** El enganche del segundo aislador, relativo a la esquina del bloque. */
    public Vector3d getSecondAttachOffset() {
        return new Vector3d(segundo);
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = super.serializeNbt();
        Utils.putNbtVector3d(nbt, NBT_SEGUNDO, segundo);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        segundo = Utils.getNbtVector3d(nbt, NBT_SEGUNDO);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
        return (DLRegistryObject) TIPO;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConectorAisladores o && super.equals(o) && segundo.equals(o.segundo);
    }
}

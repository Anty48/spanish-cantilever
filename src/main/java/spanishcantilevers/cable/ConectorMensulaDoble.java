package spanishcantilevers.cable;

import org.joml.Vector3d;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import spanishcantilevers.SpanishCantilevers;

/**
 * Enganches de las dos mensulas de una doble a la vez (el cable se clico en la cara de arriba): la primera
 * como un conector de mensula normal de PNW (si algun cable de PNW lo lee, va a esa) y la segunda aparte.
 * {@link CatenariaBifurcada} tiende una catenaria a cada una.
 */
public class ConectorMensulaDoble extends CantileverConnectorDataProvider {
    public static final DLRegistryObject<ConectorMensulaDoble> TIPO = WiresApi.CONNECTOR_DATA_PROVIDER_REGISTRY.register(
            new ResourceLocation(SpanishCantilevers.MOD_ID, "double_cantilever_connector"),
            () -> new ConectorMensulaDoble((Vector3d) null, null, null, null));
    private static final String NBT_CONTACTO = "SecondWireAttachPoint";
    private static final String NBT_SUSTENTADOR = "SecondTensionWireAttachPoint";

    private Vector3d contacto2;
    private Vector3d sustentador2;

    public ConectorMensulaDoble(Vector3d contacto, Vector3d sustentador, Vector3d contacto2, Vector3d sustentador2) {
        super(contacto, sustentador);
        this.contacto2 = contacto2;
        this.sustentador2 = sustentador2;
    }

    /** Fuerza la carga de la clase (y con ella el registro) desde el constructor del mod. */
    public static void init() {
    }

    /** Los enganches de una sola de las dos (0 o 1), como un conector de mensula normal. */
    public CantileverConnectorDataProvider de(int cual) {
        return cual == 1 ? new CantileverConnectorDataProvider(new Vector3d(contacto2), new Vector3d(sustentador2))
                : new CantileverConnectorDataProvider(getAttachOffset(), getTensionWireAttachOffset());
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = super.serializeNbt();
        Utils.putNbtVector3d(nbt, NBT_CONTACTO, contacto2);
        Utils.putNbtVector3d(nbt, NBT_SUSTENTADOR, sustentador2);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        contacto2 = Utils.getNbtVector3d(nbt, NBT_CONTACTO);
        sustentador2 = Utils.getNbtVector3d(nbt, NBT_SUSTENTADOR);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
        return (DLRegistryObject) TIPO;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConectorMensulaDoble o && super.equals(o) && contacto2.equals(o.contacto2)
                && sustentador2.equals(o.sustentador2);
    }
}

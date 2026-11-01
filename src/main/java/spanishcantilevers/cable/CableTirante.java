package spanishcantilevers.cable;

import de.mrjulsen.paw.item.SupportWireType;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Vector3d;

/**
 * El cable del tirante diagonal ({@link TiranteDiagonal}): el cable de soporte de PNW (mismo largo maximo,
 * mismo gasto de bobina, se corta igual), pero de un solo hilo recto de nodo a nodo. El de PNW son dos
 * hilos con un travesano en cada punta, que en la mensula y en el poste quedaba como un trocito
 * horizontal; asi el hilo entra directo en el hierro (los nodos ya estan metidos dentro).
 */
public class CableTirante extends SupportWireType {
    /** Grueso del hilo (bloques): el mismo que los del cable de soporte de PNW. */
    private static final double GRUESO = 0.0625;

    public CableTirante(ResourceLocation id) {
        super(id);
    }

    @Override
    public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge,
                               WireNode nodeA, WireNode nodeB) {
        return WireBatch.of(WireBuilder.createWire("main", context, new Vector3d(nodeA.getPos()), new Vector3d(nodeB.getPos()),
                WireBuilder.CableType.TIGHT, GRUESO, 0.0, SegmentControl.createAuto()));
    }
}

package iberiancantilever.cable;

import java.util.ArrayList;
import java.util.UUID;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Cuando un conector cambia de forma (la pinza del soporte de tunel se mueve o baja), los cables ya
 * tendidos se quedan con los enganches de antes: PNW los guarda al tender. Esto los vuelve a pedir al
 * bloque y rehace los cables, como hace PNW al actualizar un nodo (WireGraph.updateNodeData).
 */
public final class RecolocarCables {
    private RecolocarCables() {
    }

    /** Solo servidor: rehace los cables de catenaria enganchados al bloque de {@code pos}. */
    public static void enBloque(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        WireGraph grafo = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (grafo == null) {
            return;
        }
        for (WireNode nodo : new ArrayList<>(grafo.getNodes())) {
            if (!(nodo.getData() instanceof BlockConnectorNodeData datos) || !pos.equals(datos.getPos())) {
                continue;
            }
            for (UUID id : new ArrayList<>(nodo.getConnections())) {
                WireEdge edge = grafo.getEdge(id);
                if (edge == null) {
                    continue;
                }
                WireNode a = grafo.getNode(edge.getNodeAId());
                WireNode b = grafo.getNode(edge.getNodeBId());
                if (a == null || b == null) {
                    continue;
                }
                WireConnectionData antes = edge.getWireConnectionData();
                CustomData custom = antes.customData();
                WireConnectionData nuevos = new WireConnectionData(custom,
                        a.getData().getConnectorCustomData(grafo, custom, 0).orElse(antes.connectorA()),
                        b.getData().getConnectorCustomData(grafo, custom, 1).orElse(antes.connectorB()));
                if (!antes.equals(nuevos)) {
                    edge.setWireConnectionData(nuevos);
                    grafo.updateEdge(edge, true);
                }
            }
        }
    }
}

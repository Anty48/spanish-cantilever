package spanishcantilevers.cable;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3d;

import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * La catenaria de PNW que, cuando un extremo es una mensula doble enganchada por arriba
 * ({@link ConectorMensulaDoble}), tiende dos: una a cada mensula de la doble. Asi, en el tramo de antes de
 * la bifurcacion, los dos hilos salen de la misma mensula normal (PNW solo deja un cable entre dos bloques).
 * Si los dos extremos son dobles, cada una va con la de enfrente (el emparejamiento mas corto). Por lo
 * demas es la catenaria de PNW tal cual: cuelga igual, la toca el pantografo igual y se rompe igual.
 */
public class CatenariaBifurcada extends CatenaryWireType {
    /** Los hilos de la segunda catenaria se llaman como los de PNW con esto detras (los nombres no se repiten). */
    private static final String SEGUNDA = "_second";

    public CatenariaBifurcada(ResourceLocation id) {
        super(id);
    }

    @Override
    public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData datos, WireEdge edge,
                               WireNode nodoA, WireNode nodoB) {
        ConnectorDataProvider a = datos.connectorA();
        ConnectorDataProvider b = datos.connectorB();
        if (!(a instanceof ConectorMensulaDoble) && !(b instanceof ConectorMensulaDoble)) {
            return super.buildWire(context, level, datos, edge, nodoA, nodoB);
        }
        // de doble a doble, cada una con la de enfrente: el emparejamiento que deja los hilos mas cortos
        boolean cruzar = a instanceof ConectorMensulaDoble da && b instanceof ConectorMensulaDoble db
                && largo(nodoA, da.de(0), nodoB, db.de(1)) + largo(nodoA, da.de(1), nodoB, db.de(0))
                < largo(nodoA, da.de(0), nodoB, db.de(0)) + largo(nodoA, da.de(1), nodoB, db.de(1));
        WireBatch primera = super.buildWire(context, level, new WireConnectionData(datos.customData(), de(a, 0), de(b, cruzar ? 1 : 0)),
                edge, nodoA, nodoB);
        WireBatch segunda = super.buildWire(context, level, new WireConnectionData(datos.customData(), de(a, 1), de(b, cruzar ? 0 : 1)),
                edge, nodoA, nodoB);
        List<Wire> hilos = new ArrayList<>(primera.getWires().values());
        for (Wire w : segunda.getWires().values()) {
            hilos.add(new Wire(w.name() + SEGUNDA, w.pos(), w.collisionData(), w.renderData()));
        }
        return WireBatch.of(hilos.toArray(Wire[]::new));
    }

    /** Los enganches de una de las dos si el conector es de una doble; si no, el conector tal cual. */
    private static ConnectorDataProvider de(ConnectorDataProvider conector, int cual) {
        return conector instanceof ConectorMensulaDoble doble ? doble.de(cual) : conector;
    }

    private static double largo(WireNode nodoA, CantileverConnectorDataProvider a, WireNode nodoB, CantileverConnectorDataProvider b) {
        return new Vector3d(nodoA.getPos()).add(a.getAttachOffset()).distance(new Vector3d(nodoB.getPos()).add(b.getAttachOffset()));
    }
}

package iberiancantilever.cable;

import java.util.Optional;

import org.joml.Vector3d;

import de.mrjulsen.paw.item.AbstractWireType;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.util.GraphId;
import iberiancantilever.item.PerfilRigidoItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

/**
 * Catenaria rigida (tunel): para PNW es un cable mas, del mismo grafo que su catenaria (asi el
 * pantografo la toca y se puede mezclar con el cable normal), pero recto y sin colgar. PNW solo dibuja
 * un hilo fino que queda escondido dentro del perfil; el perfil de verdad (la pieza de Blockbench) lo
 * dibuja {@link iberiancantilever.client.PerfilRigidoRenderer}.
 */
public class CatenariaRigida extends AbstractWireType {
    /** El hilo que dibuja PNW: casi nada, para que no asome del perfil cuando este hace curva. */
    private static final double GROSOR = 0.008;
    /** Nombre del hilo: el mismo que el hilo de contacto de PNW. */
    private static final String NOMBRE = "contact";
    private static final int LARGO_MAXIMO = 24;

    public CatenariaRigida(net.minecraft.resources.ResourceLocation id) {
        super(id);
    }

    /** Donde engancha un extremo: el punto de contacto del conector (la pinza del soporte). */
    public static Vector3d enganche(WireNode nodo, ConnectorDataProvider datos) {
        Vector3d offset = datos.getAsTypeIfMatching(BasicConnectorDataProvider.class)
                .map(BasicConnectorDataProvider::getAttachOffset).orElse(new Vector3d());
        return new Vector3d(nodo.getPos()).add(offset);
    }

    @Override
    public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData datos, WireEdge edge, WireNode a, WireNode b) {
        Vector3d inicio = enganche(a, datos.connectorA());
        Vector3d fin = enganche(b, datos.connectorB());
        int tramos = Math.max(1, (int) (inicio.distance(fin) / 2.0));
        Wire hilo = WireBuilder.createWire(NOMBRE, context, inicio, fin, WireBuilder.CableType.TIGHT, GROSOR, 0.0,
                SegmentControl.create(SegmentControl.Config.fixed(tramos), SegmentControl.Config.fixed(2)));
        return WireBatch.of(hilo);
    }

    @Override
    public int getMaxLength() {
        return LARGO_MAXIMO;
    }

    @Override
    public GraphId getGraphId(CompoundTag itemData) {
        return WiresApi.PAW_CATENARY_WIRES;
    }

    /** Al romperlo se recuperan sus metros de perfil en un haz (salvo en creativo). */
    @Override
    public void onBreak(Level level, Vector3d donde, Optional<Player> player, IWireGraph graph, WireEdge edge) {
        if (player.map(p -> p.isCreative() || p.isSpectator()).orElse(false)) {
            return;
        }
        ItemEntity item = new ItemEntity(level, donde.x, donde.y, donde.z, PerfilRigidoItem.conMetros(edge.length()));
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}

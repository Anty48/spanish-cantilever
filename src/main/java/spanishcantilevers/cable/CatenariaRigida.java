package spanishcantilevers.cable;

import java.util.Optional;

import org.joml.Vector3d;

import de.mrjulsen.paw.item.AbstractWireType;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WirePoints;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.util.GraphId;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.geometry.PiezasDatos;
import spanishcantilevers.item.PerfilRigidoItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Catenaria rigida (tunel): para PNW es un cable mas, del mismo grafo que su catenaria (asi el
 * pantografo la toca y se puede mezclar con el cable normal), pero recto y sin colgar. A PNW solo se le
 * da la colision del hilo (la que toca el pantografo y la que se apunta para romperlo), nada que dibujar:
 * el perfil de verdad (la pieza de Blockbench) lo dibuja {@link spanishcantilevers.client.PerfilRigidoRenderer}.
 */
public class CatenariaRigida extends AbstractWireType {
    /** Nombre del hilo: el mismo que el hilo de contacto de PNW. */
    private static final String NOMBRE = "contact";
    private static final int LARGO_MAXIMO = 24;
    /**
     * Cuanto baja la colision (la que toca el pantografo) desde el eje del perfil, en px de la pieza: el
     * perfil se dibuja centrado en las pinzas, asi que con la colision en el eje el pantografo se metia
     * media seccion dentro. Con la colision en el fondo del perfil aun se hundia (el cabezal de PNW sube
     * algo mas que la colision; con su hilo de 1 px no se nota), asi que baja 1/4 de px mas (ajustado
     * mirandolo en el juego: con 3/4 ya no llegaba a tocar).
     */
    private static final float BAJADA_COLISION = (PiezasDatos.PERFIL_RIGIDO.MAX[1] - PiezasDatos.PERFIL_RIGIDO.MIN[1]) / 2f + 0.25f;

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
        // el perfil de un tramo tiene el tamano del soporte de su primer extremo (ver PerfilRigidoRenderer)
        double bajada = BAJADA_COLISION * escala(level, a) / 16.0;
        Vector3d inicio = enganche(a, datos.connectorA()).sub(0, bajada, 0);
        Vector3d fin = enganche(b, datos.connectorB()).sub(0, bajada, 0);
        int tramos = Math.max(1, (int) (inicio.distance(fin) / 2.0));
        // sin datos de dibujo: un hilo recto, por fino que fuera, asomaba negro en las curvas del perfil
        WirePoints colision = context.collisionRequired() ? WireBuilder.createWirePoints(inicio, fin, WireBuilder.CableType.TIGHT, 0.0,
                SegmentControl.create(SegmentControl.Config.fixed(tramos), SegmentControl.Config.fixed(2))) : null;
        return WireBatch.of(new Wire(NOMBRE, new Vector3d(inicio).add(fin).mul(0.5), colision, null));
    }

    /** El tamano general del soporte de tunel de ese nodo (1 si no lo es). */
    private static float escala(BlockAndTintGetter level, WireNode nodo) {
        if (nodo.getData() instanceof BlockConnectorNodeData datos) {
            BlockState state = level.getBlockState(datos.getPos());
            if (state.getBlock() instanceof SoporteTunelBlock) {
                return SoporteTunelBlock.escala(state);
            }
        }
        return 1f;
    }

    @Override
    public int getMaxLength() {
        return LARGO_MAXIMO;
    }

    @Override
    public GraphId getGraphId(CompoundTag itemData) {
        return WiresApi.PAW_CATENARY_WIRES;
    }

    /**
     * Al romperlo se recuperan sus metros de perfil (salvo en creativo), como el cable de PNW: vuelven a
     * los haces que lleva el jugador y lo que no cabe cae en UN haz.
     */
    @Override
    public void onBreak(Level level, Vector3d donde, Optional<Player> player, IWireGraph graph, WireEdge edge) {
        if (player.map(p -> p.isCreative() || p.isSpectator()).orElse(false)) {
            return;
        }
        int sobran = player.map(p -> PerfilRigidoItem.devolver(p, edge.length())).orElse(edge.length());
        if (sobran <= 0) {
            return;
        }
        ItemEntity item = new ItemEntity(level, donde.x, donde.y, donde.z, PerfilRigidoItem.conMetros(sobran));
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}

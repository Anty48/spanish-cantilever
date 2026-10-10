package spanishcantilevers.cable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3d;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import de.mrjulsen.paw.item.SupportWireItem;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.accessor.GenericBlockNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.INodeDataBlock;
import de.mrjulsen.wires.graph.data.node.MastNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.geometry.MensulaLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tirante diagonal: con anchura >= 3 la barra horizontal de la mensula se puede colgar de un poste mas
 * arriba con el cable de soporte de PNW. Bobina de PNW en modo "cable de soporte", clic en la mensula y
 * luego en un bloque del poste de 2 a 5 bloques mas arriba (o al reves): el hilo va de dentro de la
 * horizontal, antes del aislador del sustentador, a dentro de la cara del poste que mira a la mensula.
 *
 * <p>PNW solo engancha ese cable al centro de los bloques de poste, asi que aqui hay dos nodos propios
 * para su grafo de cables: el de la mensula y el de la cara del poste, que guardan el punto exacto. El
 * clic se intercepta antes que el de la bobina y se hace con {@link #ACTOR}, que es el cable de soporte de
 * PNW con esos nodos.
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID)
public final class TiranteDiagonal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final NodeDataRegistryObject<NodoMensula, GenericBlockNodeAccessor<NodoMensula>> NODO_MENSULA = WiresApi.NODE_DATA_REGISTRY.register(
            new ResourceLocation(SpanishCantilevers.MOD_ID, "diagonal_stay_cantilever"), NodoMensula::new, GenericBlockNodeAccessor::new);
    public static final NodeDataRegistryObject<NodoPoste, GenericBlockNodeAccessor<NodoPoste>> NODO_POSTE = WiresApi.NODE_DATA_REGISTRY.register(
            new ResourceLocation(SpanishCantilevers.MOD_ID, "diagonal_stay_mast"), NodoPoste::new, GenericBlockNodeAccessor::new);

    /** El bloque del poste tiene que estar entre estos bloques por encima de la mensula (asi sale un triangulo). */
    public static final int ALTURA_POSTE_MIN = 2;
    public static final int ALTURA_POSTE_MAX = 5;
    /** Lo que el hilo se mete en el poste desde su cara (bloques), para que se vea enganchado. */
    private static final double DENTRO_POSTE = 1.5 / 16.0;

    /**
     * El cable de soporte de PNW, pero con nuestros nodos en la mensula y en el poste y nuestro hilo
     * ({@link ModCables#TIRANTE}): uno solo, sin los travesanos de las puntas.
     */
    private static final SupportWireItem ACTOR = new SupportWireItem() {
        @Override
        public NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {
            return nodo(level, player, player.getItemInHand(hand), hit);
        }

        @Override
        public IWireType getWireType(ItemStack stack) {
            return ModCables.TIRANTE;
        }
    };

    private TiranteDiagonal() {
    }

    /** Fuerza la carga de la clase (y con ella el registro de los nodos) desde el constructor del mod. */
    public static void init() {
    }

    // ------------------------------------------------------------------ clic con la bobina

    @SubscribeEvent
    public static void alClicar(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof MultiWireItem bobina) || !(bobina.getSubType(stack) instanceof SupportWireItem)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean enMensula = state.getBlock() instanceof MensulaBlock;
        // el poste solo es nuestro si se viene de una mensula; si no, que lo haga PNW como siempre
        if (!enMensula && !(state.is(ModBlockTags.SUPPORT_WIRE_CONNECTABLE) && NODO_MENSULA.id().toString().equals(ultimoPunto(stack)))) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) {
            return;
        }
        Player player = event.getEntity();
        if (enMensula && MensulaBlock.ajustes(level, pos).anchura() < MensulaLayout.ANCHURA_TIRANTE_DIAGONAL) {
            aviso(player, "anchura");
            return;
        }
        ACTOR.placeWire(level, player, event.getHand(), event.getHitVec(), null);
    }

    /** Id del tipo del ultimo punto que la bobina tiene apuntado (sin tocar el NBT), o null. */
    private static String ultimoPunto(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null) {
            return null;
        }
        ListTag puntos = nbt.getCompound(IWireItemBase.NBT_ROOT).getList(IWireItemBase.NBT_POINTS, Tag.TAG_COMPOUND);
        return puntos.isEmpty() ? null : puntos.getCompound(puntos.size() - 1).getString(NodeDataRegistryObject.NBT_ID);
    }

    /**
     * El nodo del clic: en la mensula, el de encima de la horizontal; en un poste viniendo de una mensula,
     * el de su cara. Si se empezo por el poste (PNW lo apunto en su centro) se cambia ese punto por el de
     * la cara que mira a la mensula: la bobina guarda los puntos en su NBT y PNW usa esos mismos objetos.
     */
    private static NodeData nodo(Level level, Player player, ItemStack stack, HitResult hit) {
        if (!(hit instanceof BlockHitResult bloque)) {
            return null;
        }
        BlockPos pos = bloque.getBlockPos();
        BlockState state = level.getBlockState(pos);
        ListTag puntos = IWireItemBase.getNbt(stack).getList(IWireItemBase.NBT_POINTS, Tag.TAG_COMPOUND);
        CompoundTag anterior = puntos.isEmpty() ? null : puntos.getCompound(puntos.size() - 1);
        NodeData antes = anterior == null ? null : WiresApi.NODE_DATA_REGISTRY.load(anterior);
        if (state.getBlock() instanceof MensulaBlock mensula) {
            if (antes instanceof NodoMensula) {
                return aviso(player, "poste");
            }
            NodoMensula nodo = new NodoMensula(pos, mensula.puntoTiranteDiagonal(level, pos, state));
            if (antes instanceof MastNodeData poste) {
                if (!alturaValida(poste.getBlockPos(), pos)) {
                    return aviso(player, "poste");
                }
                NodoPoste cara = NodoPoste.crear(level, poste.getBlockPos(), nodo.punto);
                if (cara == null) {
                    return null;
                }
                anterior.putString(NodeDataRegistryObject.NBT_ID, NODO_POSTE.id().toString());
                anterior.put(NodeDataRegistryObject.NBT_DATA, cara.serializeNbt());
            }
            return nodo;
        }
        if (antes instanceof NodoMensula desde) {
            // el tirante cuelga la horizontal: el poste tiene que quedar por encima de la mensula
            return alturaValida(pos, desde.pos) ? NodoPoste.crear(level, pos, desde.punto) : aviso(player, "poste");
        }
        return null;
    }

    /** Si el bloque del poste queda entre ALTURA_POSTE_MIN y ALTURA_POSTE_MAX bloques por encima de la mensula. */
    private static boolean alturaValida(BlockPos poste, BlockPos mensula) {
        int dy = poste.getY() - mensula.getY();
        return dy >= ALTURA_POSTE_MIN && dy <= ALTURA_POSTE_MAX;
    }

    private static NodeData aviso(Player player, String clave) {
        player.displayClientMessage(Component.translatable("message.spanishcantilevers.tirante_diagonal." + clave,
                ALTURA_POSTE_MIN, ALTURA_POSTE_MAX), true);
        return null;
    }


    // ------------------------------------------------------------------ mantenimiento

    /** Un jugador rompe la mensula o el poste: el tirante se quita y el cable vuelve a su bobina. */
    @SubscribeEvent
    public static void alRomper(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            quitar(level, event.getPos(), Optional.of(event.getPlayer()));
        }
    }

    /** Solo servidor: quita los tirantes enganchados en {@code pos} (mensula o poste). */
    public static void quitar(Level level, BlockPos pos, Optional<Player> player) {
        WireGraph grafo = grafo(level);
        if (grafo == null) {
            return;
        }
        Vector3d donde = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (WireNode nodo : nodosEn(grafo, pos)) {
            grafo.removeNode(nodo.getId(), donde, player);
        }
    }

    /**
     * Solo servidor: la mensula de {@code pos} ha cambiado de ajustes. Si ya no llega a la anchura minima
     * el tirante se quita; si no, se vuelve a tender a donde ha quedado ahora la horizontal.
     */
    public static void recolocar(Level level, BlockPos pos, Player player) {
        WireGraph grafo = grafo(level);
        if (grafo == null || !(level.getBlockState(pos).getBlock() instanceof MensulaBlock mensula)) {
            return;
        }
        if (MensulaBlock.ajustes(level, pos).anchura() < MensulaLayout.ANCHURA_TIRANTE_DIAGONAL) {
            quitar(level, pos, Optional.of(player));
            return;
        }
        Vec3 punto = mensula.puntoTiranteDiagonal(level, pos, level.getBlockState(pos));
        Vector3d hacia = new Vector3d(punto.x, punto.y, punto.z);
        for (WireNode nodo : nodosEn(grafo, pos)) {
            if (!(nodo.getData() instanceof NodoMensula)) {
                continue;
            }
            for (UUID id : new ArrayList<>(nodo.getConnections())) {
                WireEdge edge = grafo.getEdge(id);
                if (edge == null) {
                    continue;
                }
                boolean esA = edge.getNodeAId().equals(nodo.getId());
                WireNode otro = grafo.getNode(esA ? edge.getNodeBId() : edge.getNodeAId());
                NodeData nuevoMensula = new NodoMensula(pos, punto);
                NodeData nuevoPoste = otro != null && otro.getData() instanceof NodoPoste p ? NodoPoste.crear(level, p.pos, hacia) : null;
                WireConnectionData datos = edge.getWireConnectionData();
                if (nuevoPoste == null) {
                    grafo.removeEdge(id, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Optional.of(player));
                    continue;
                }
                // sin soltar cable: se quita sin mas y se tiende otra vez con los puntos nuevos
                grafo.removeEdge(id, null, null);
                grafo.createEdge(edge.getType(), datos.customData(), esA ? nuevoMensula : nuevoPoste, esA ? nuevoPoste : nuevoMensula,
                        new MutableInt(), true);
            }
        }
    }

    /** Para las pruebas: cuantos tirantes hay tendidos (y en el log, de donde a donde va cada uno). */
    public static int contar(Level level) {
        WireGraph grafo = grafo(level);
        if (grafo == null) {
            return 0;
        }
        int n = 0;
        for (WireEdge edge : grafo.getEdges()) {
            WireNode a = grafo.getNode(edge.getNodeAId());
            WireNode b = grafo.getNode(edge.getNodeBId());
            if (a != null && b != null && (a.getData() instanceof Nodo || b.getData() instanceof Nodo)) {
                LOGGER.info("[tirante] {} {} -> {} {}", a.getData().getClass().getSimpleName(), a.getPos(),
                        b.getData().getClass().getSimpleName(), b.getPos());
                n++;
            }
        }
        return n;
    }

    private static WireGraph grafo(Level level) {
        return level.isClientSide ? null : WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
    }

    private static List<WireNode> nodosEn(WireGraph grafo, BlockPos pos) {
        List<WireNode> nodos = new ArrayList<>();
        for (WireNode nodo : grafo.getNodes()) {
            if (nodo.getData() instanceof Nodo n && pos.equals(n.pos)) {
                nodos.add(nodo);
            }
        }
        return nodos;
    }

    // ------------------------------------------------------------------ nodos

    /** Un extremo del tirante: el bloque al que va y el punto exacto (en el mundo) donde se engancha. */
    private abstract static class Nodo extends NodeData implements INodeDataBlock {
        BlockPos pos;
        Vector3d punto;

        Nodo() {
        }

        Nodo(BlockPos pos, Vec3 punto) {
            this.pos = pos;
            this.punto = new Vector3d(punto.x, punto.y, punto.z);
        }

        @Override
        public BlockPos getBlockPos() {
            return pos;
        }

        /** Si el bloque de {@code pos} sigue sirviendo para este extremo. */
        abstract boolean vale(BlockState state, Level level);

        @Override
        protected WireNode getOrCreateNode(WireGraph graph) {
            Level level = graph.getLevel();
            if (!level.isLoaded(pos) || !vale(level.getBlockState(pos), level)) {
                return null;
            }
            return graph.createNode(this, new Vector3d(punto));
        }

        @Override
        public Vector3d toWorldPos(IWireGraph graph) {
            return new Vector3d(punto);
        }

        @Override
        public Optional<ConnectorDataProvider> getConnectorCustomData(IWireGraph graph, CustomData customData, int index) {
            // el cable de soporte de PNW se tiende entre las posiciones de los nodos, sin desplazamientos
            return Optional.of(new ConnectorDataProvider.Empty());
        }

        @Override
        public boolean validate(WireGraph graph, CompoundTag itemData, int index) {
            Level level = graph.getLevel();
            return !level.isLoaded(pos) || vale(level.getBlockState(pos), level);
        }

        @Override
        public CompoundTag serializeNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put("Pos", NbtUtils.writeBlockPos(pos));
            nbt.putDouble("X", punto.x);
            nbt.putDouble("Y", punto.y);
            nbt.putDouble("Z", punto.z);
            return nbt;
        }

        @Override
        public void deserializeNbt(CompoundTag nbt) {
            pos = NbtUtils.readBlockPos(nbt.getCompound("Pos"));
            punto = new Vector3d(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass() && pos.equals(((Nodo) obj).pos)
                    && punto.distance(((Nodo) obj).punto) < 0.01;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), pos);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> tipo(NodeDataRegistryObject<?, ?> tipo) {
            return (NodeDataRegistryObject) tipo;
        }
    }

    /** Extremo de la mensula: encima de la barra horizontal, antes del aislador del sustentador. */
    public static final class NodoMensula extends Nodo {
        public NodoMensula() {
        }

        NodoMensula(BlockPos pos, Vec3 punto) {
            super(pos, punto);
        }

        @Override
        boolean vale(BlockState state, Level level) {
            return state.getBlock() instanceof MensulaBlock
                    && MensulaBlock.ajustes(level, pos).anchura() >= MensulaLayout.ANCHURA_TIRANTE_DIAGONAL;
        }

        @Override
        public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
            return tipo(NODO_MENSULA);
        }
    }

    /** Extremo del poste: metido un poco por la cara de su forma que mira hacia la mensula, a media altura del bloque. */
    public static final class NodoPoste extends Nodo {
        public NodoPoste() {
        }

        NodoPoste(BlockPos pos, Vec3 punto) {
            super(pos, punto);
        }

        /**
         * El punto del poste de {@code pos} que mira hacia {@code hacia}: se sale del centro de su forma en
         * horizontal hacia la mensula hasta poco antes de su borde (el hilo entra en el hierro). Null si no es
         * un poste de los del cable de soporte.
         */
        static NodoPoste crear(Level level, BlockPos pos, Vector3d hacia) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlockTags.SUPPORT_WIRE_CONNECTABLE)) {
                return null;
            }
            VoxelShape forma = state.getShape(level, pos);
            AABB caja = forma.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : forma.bounds();
            double cx = pos.getX() + (caja.minX + caja.maxX) / 2;
            double cz = pos.getZ() + (caja.minZ + caja.maxZ) / 2;
            double dx = hacia.x - cx;
            double dz = hacia.z - cz;
            double largo = Math.sqrt(dx * dx + dz * dz);
            double t = 0;
            if (largo > 1e-6) {
                dx /= largo;
                dz /= largo;
                double tx = Math.abs(dx) < 1e-6 ? Double.MAX_VALUE : (caja.maxX - caja.minX) / 2 / Math.abs(dx);
                double tz = Math.abs(dz) < 1e-6 ? Double.MAX_VALUE : (caja.maxZ - caja.minZ) / 2 / Math.abs(dz);
                t = Math.max(0, Math.min(tx, tz) - DENTRO_POSTE);
            }
            return new NodoPoste(pos, new Vec3(cx + dx * t, pos.getY() + 0.5, cz + dz * t));
        }

        @Override
        boolean vale(BlockState state, Level level) {
            return state.is(ModBlockTags.SUPPORT_WIRE_CONNECTABLE);
        }

        @Override
        public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
            return tipo(NODO_POSTE);
        }
    }
}

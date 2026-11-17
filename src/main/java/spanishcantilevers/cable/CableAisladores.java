package spanishcantilevers.cable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3d;

import de.mrjulsen.paw.item.FeederWireItem;
import de.mrjulsen.paw.item.PowerWireType;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.ColumnaAisladorBlock;
import spanishcantilevers.block.RemateAisladoresBlock;

/**
 * El cable de corriente de PNW cuando un extremo es un remate con dos aisladores: un hilo a cada aislador.
 * Si el otro extremo tiene un solo enganche (un aislador de PNW, un remate de un aislador...) los dos hilos
 * van a ese punto; si es otro remate doble van uno a uno (cada aislador con el de enfrente, sin cruzarse),
 * como los feeders de la alta velocidad. Por lo demas es el de PNW: cuelga igual, gasta igual y se rompe igual.
 *
 * <p>La bobina de PNW tenderia su propio cable (un solo hilo), asi que si se clica un remate doble, o se
 * viene de uno, el clic se intercepta y se hace con {@link #ACTOR}: la bobina de PNW en modo corriente,
 * pero con este cable.
 *
 * <p>Tambien se intercepta el clic en una columna con aislador ({@link ColumnaAisladorBlock}), para guardar
 * en el punto de la bobina el lado clicado (cada cable va a una punta de su varilla).
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID)
public class CableAisladores extends PowerWireType {
    /** Lo que cuelga cada hilo por bloque de largo: lo mismo que el cable de corriente de PNW. */
    private static final double CAIDA = 0.025;
    private static final double GRUESO = 0.0625;

    /** La bobina de PNW en modo cable de corriente, pero tendiendo este cable. */
    private static final FeederWireItem ACTOR = new FeederWireItem() {
        @Override
        public IWireType getWireType(ItemStack stack) {
            return ModCables.AISLADORES;
        }
    };

    public CableAisladores(ResourceLocation id) {
        super(id);
    }

    @Override
    public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData datos, WireEdge edge,
                               WireNode nodoA, WireNode nodoB) {
        List<Vector3d> a = enganches(nodoA, datos.connectorA());
        List<Vector3d> b = enganches(nodoB, datos.connectorB());
        if (a.isEmpty() || b.isEmpty()) {
            return WireBatch.of();
        }
        if (a.size() == 2 && b.size() == 2) {
            // de doble a doble, uno a uno: el emparejamiento mas corto (los hilos no se cruzan)
            if (a.get(0).distance(b.get(0)) + a.get(1).distance(b.get(1)) > a.get(0).distance(b.get(1)) + a.get(1).distance(b.get(0))) {
                b = List.of(b.get(1), b.get(0));
            }
        } else if (a.size() == 1 && b.size() == 2) {
            a = List.of(a.get(0), a.get(0));
        } else if (a.size() == 2) {
            b = List.of(b.get(0), b.get(0));
        }
        Wire[] hilos = new Wire[a.size()];
        for (int i = 0; i < hilos.length; i++) {
            double largo = a.get(i).distance(b.get(i));
            // el primero se llama como el de PNW ("main"): es al que se le ponen los aisladores de cable
            hilos[i] = WireBuilder.createWire(i == 0 ? "main" : "main" + (i + 1), context, a.get(i), b.get(i),
                    WireBuilder.CableType.HANGING, GRUESO, (float) (CAIDA * largo),
                    SegmentControl.create(SegmentControl.Config.auto(), SegmentControl.Config.fixed(1)));
        }
        return WireBatch.of(hilos);
    }

    /** Los puntos (mundo) donde se engancha un extremo: dos en un remate doble, uno en cualquier otro conector. */
    private static List<Vector3d> enganches(WireNode nodo, ConnectorDataProvider datos) {
        List<Vector3d> puntos = new ArrayList<>(2);
        if (datos instanceof BasicConnectorDataProvider basico) {
            puntos.add(new Vector3d(nodo.getPos()).add(basico.getAttachOffset()));
            if (datos instanceof ConectorAisladores doble) {
                puntos.add(new Vector3d(nodo.getPos()).add(doble.getSecondAttachOffset()));
            }
        }
        return puntos;
    }

    // ------------------------------------------------------------------ clic con la bobina

    @SubscribeEvent
    public static void alClicar(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof MultiWireItem bobina) || !(bobina.getSubType(stack) instanceof FeederWireItem)) {
            return;
        }
        Level level = event.getLevel();
        boolean doble = esDoble(level, event.getPos()) || vieneDeDoble(level, stack);
        BiConsumer<CompoundTag, CompoundTag> lado = ColumnaAisladorBlock.datosClic(level, event.getPos(), event.getHitVec().getLocation());
        if (!doble && lado == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide) {
            // con un remate doble, este cable; si no, el de corriente de PNW tal cual (con el lado de la columna)
            FeederWireItem actor = doble ? ACTOR : (FeederWireItem) bobina.getSubType(stack);
            actor.placeWire(level, event.getEntity(), event.getHand(), event.getHitVec(), lado);
        }
    }

    private static boolean esDoble(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RemateAisladoresBlock remate && remate.doble();
    }

    /** Si el punto que la bobina tiene apuntado (el primer clic) es un remate doble. */
    private static boolean vieneDeDoble(Level level, ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null) {
            return false;
        }
        ListTag puntos = nbt.getCompound(IWireItemBase.NBT_ROOT).getList(IWireItemBase.NBT_POINTS, Tag.TAG_COMPOUND);
        if (puntos.isEmpty()) {
            return false;
        }
        NodeData punto = WiresApi.NODE_DATA_REGISTRY.load(puntos.getCompound(puntos.size() - 1));
        return punto instanceof BlockConnectorNodeData bloque && esDoble(level, bloque.getPos());
    }
}

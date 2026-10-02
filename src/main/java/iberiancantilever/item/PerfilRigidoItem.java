package iberiancantilever.item;

import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.item.AbstractWireItemBase;
import iberiancantilever.ModBlocks;
import iberiancantilever.cable.ModCables;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Haz de perfiles de catenaria rigida: se tiende como la bobina de PNW (clic en un soporte y luego en
 * otro) y, como ella, se va gastando por metros; cuando se acaba desaparece. Se engancha a los mismos
 * sitios que la catenaria de PNW (soportes de tunel, mensulas...).
 */
public class PerfilRigidoItem extends AbstractWireItemBase {
    /** Metros de perfil de un haz nuevo. */
    public static final int CAPACIDAD = 64;
    private static final String NBT_METROS = "Metros";
    private static final int COLOR_BARRA = 0x9AA3B0;

    public PerfilRigidoItem(Properties properties) {
        super(properties);
    }

    /** Un haz con estos metros de perfil. */
    public static ItemStack conMetros(int metros) {
        ItemStack stack = new ItemStack(ModBlocks.PERFIL_RIGIDO.get());
        if (metros < CAPACIDAD) {
            stack.getOrCreateTag().putInt(NBT_METROS, Math.max(1, metros));
        }
        return stack;
    }

    public static int metros(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_METROS) ? nbt.getInt(NBT_METROS) : CAPACIDAD;
    }

    private static boolean gratis(@Nullable Player player) {
        return player != null && (player.isCreative() || player.isSpectator());
    }

    @Override
    public IWireType getWireType(ItemStack stack) {
        return ModCables.RIGIDA;
    }

    /** Como CatenaryWireItem de PNW: el nodo es el bloque conector de catenaria que se ha pinchado. */
    @Override
    public NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {
        if (!(hit instanceof BlockHitResult bloque)) {
            return null;
        }
        BlockPos pos = bloque.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof WireConnectorBlockEntity
                && state.getBlock() instanceof ICatenaryWireConnector conector
                && conector.canConnectWire(level, pos, state)) {
            return new BlockConnectorNodeData(pos);
        }
        return null;
    }

    /** Ademas de lo de PNW: que quede perfil para llegar hasta ahi. */
    @Override
    public DLStatus testPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata,
                              ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points, NodeData nodeData) {
        DLStatus resultado = super.testPoint(level, player, hand, hit, metadata, stack, itemData, customDataNbt, points, nodeData);
        if (resultado.flag() != 0 || points.isEmpty() || gratis(player)) {
            return resultado;
        }
        WireGraph grafo = WireGraphManager.get(level, getWireType(stack).getGraphId(itemData));
        NodeData anterior = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
        if ((int) anterior.toWorldPos(grafo).distance(nodeData.toWorldPos(grafo)) > metros(stack)) {
            return new DLStatus((byte) -128, 0, "item.iberiancantilever.perfil_rigido.no_alcanza");
        }
        return resultado;
    }

    /** Gasta los metros del tramo tendido. */
    @Override
    public void removeWireItem(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, int length) {
        if (gratis(player)) {
            return;
        }
        int quedan = metros(stack) - Math.max(1, length);
        if (quedan <= 0) {
            stack.shrink(1);
        } else {
            stack.getOrCreateTag().putInt(NBT_METROS, quedan);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.iberiancantilever.perfil_rigido.cantidad", metros(stack)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return metros(stack) < CAPACIDAD;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13f * metros(stack) / CAPACIDAD), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return COLOR_BARRA;
    }
}

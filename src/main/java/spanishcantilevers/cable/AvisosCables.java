package spanishcantilevers.cable;

import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.item.IWireItemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.item.PerfilRigidoItem;
import spanishcantilevers.network.Aviso;

/**
 * Avisos en rojo (sobre la barra rapida, en varias lineas: ver {@link spanishcantilevers.client.AvisosCliente})
 * cuando se mezcla la catenaria rigida con la flexible de una forma que no es la prevista. No se prohibe
 * nada: el cable se tiende igual.
 *
 * <ul>
 *   <li>el perfil rigido enganchado a algo que no es un soporte de tunel (una mensula, la de PNW...);</li>
 *   <li>el cable de catenaria normal de PNW tendido entre dos soportes de tunel.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID)
public final class AvisosCables {
    private AvisosCables() {
    }

    /** Cada vez que el perfil rigido se engancha a un conector que no es un soporte de tunel. */
    public static void perfilFueraDeTunel(Level level, Player player, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof SoporteTunelBlock)) {
            avisar(player, "aviso.spanishcantilevers.perfil_fuera_de_tunel");
        }
    }

    /** El cable normal de PNW: avisa al pinchar el segundo soporte de tunel seguido. */
    @SubscribeEvent
    public static void alPinchar(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        if (level.isClientSide || !(stack.getItem() instanceof IWireItemBase cable) || stack.getItem() instanceof PerfilRigidoItem
                || !(cable.getWireType(stack) instanceof CatenaryWireType)
                || !(level.getBlockState(event.getPos()).getBlock() instanceof SoporteTunelBlock)) {
            return;
        }
        BlockPos anterior = ultimoPunto(stack);
        if (anterior != null && !anterior.equals(event.getPos()) && level.getBlockState(anterior).getBlock() instanceof SoporteTunelBlock) {
            avisar(event.getEntity(), "aviso.spanishcantilevers.cable_entre_tuneles");
        }
    }

    /** El conector del ultimo punto que lleva apuntado el objeto (el primer clic de un tramo), si es un bloque. */
    private static BlockPos ultimoPunto(ItemStack stack) {
        CompoundTag raiz = stack.getTag();
        if (raiz == null || !raiz.contains(IWireItemBase.NBT_ROOT)) {
            return null;
        }
        ListTag puntos = raiz.getCompound(IWireItemBase.NBT_ROOT).getList(IWireItemBase.NBT_POINTS, Tag.TAG_COMPOUND);
        if (puntos.isEmpty()) {
            return null;
        }
        NodeData nodo = WiresApi.NODE_DATA_REGISTRY.load(puntos.getCompound(puntos.size() - 1));
        return nodo instanceof BlockConnectorNodeData bloque ? bloque.getPos() : null;
    }

    private static void avisar(Player player, String clave) {
        if (player instanceof ServerPlayer jugador) {
            Aviso.enviar(jugador, clave);
        }
    }
}

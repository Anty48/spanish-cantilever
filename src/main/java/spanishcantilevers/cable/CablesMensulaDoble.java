package spanishcantilevers.cable;

import java.util.function.BiConsumer;

import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.item.IWireItemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.block.MensulaBlockEntity;
import spanishcantilevers.item.PerfilRigidoItem;
import spanishcantilevers.network.Aviso;

/**
 * El cable de catenaria de PNW en una mensula doble. Segun la cara por la que se clica el bloque:
 * <ul>
 *   <li>una de las dos caras grandes de los lados: va a la mensula de ese lado de la via;</li>
 *   <li>la de arriba: va a las dos a la vez ({@link CatenariaBifurcada}: dos catenarias en un solo cable,
 *       para el tramo de antes de la bifurcacion, desde una mensula normal);</li>
 *   <li>abajo o un canto estrecho: no se tiende nada y sale un aviso en rojo.</li>
 * </ul>
 * La eleccion se guarda en los datos del punto de la bobina, como en la columna con aislador, y PNW nos la
 * devuelve al pedir el enganche. Los cables que empiezan o acaban en una doble se tienden con la catenaria
 * bifurcada (con una sola mensula a cada lado es igual que la de PNW).
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID)
public final class CablesMensulaDoble {
    /** La bobina de PNW en modo catenaria, pero tendiendo la catenaria bifurcada. */
    private static final CatenaryWireItem ACTOR = new CatenaryWireItem() {
        @Override
        public IWireType getWireType(ItemStack stack) {
            return ModCables.BIFURCADA;
        }
    };

    private CablesMensulaDoble() {
    }

    @SubscribeEvent
    public static void alClicar(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        // el perfil rigido gasta metros a su manera: ese se deja como esta
        if (!(stack.getItem() instanceof IWireItemBase cable) || stack.getItem() instanceof PerfilRigidoItem
                || !(cable.getWireType(stack) instanceof CatenaryWireType)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BiConsumer<CompoundTag, CompoundTag> datos = null;
        if (state.getBlock() instanceof MensulaBlock && level.getBlockEntity(pos) instanceof MensulaBlockEntity be && be.doble()) {
            Direction cara = event.getHitVec().getDirection();
            Integer cual = cara == Direction.UP ? Integer.valueOf(MensulaBlock.AMBAS) : MensulaBlock.cualPorCara(state, be, cara);
            if (cual == null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                if (event.getEntity() instanceof ServerPlayer jugador) {
                    Aviso.enviar(jugador, "aviso.spanishcantilevers.mensula_doble_lado");
                }
                return;
            }
            datos = (comunes, punto) -> punto.putInt(MensulaBlock.NBT_CUAL, cual);
        } else if (!(state.getBlock() instanceof IWireConnector) || !vieneDeDoble(level, stack)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide) {
            ACTOR.placeWire(level, event.getEntity(), event.getHand(), event.getHitVec(), datos);
        }
    }

    private static boolean esDoble(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof MensulaBlock
                && level.getBlockEntity(pos) instanceof MensulaBlockEntity be && be.doble();
    }

    /** Si el punto que la bobina tiene apuntado (el primer clic) es una mensula doble. */
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

package spanishcantilevers.network;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import spanishcantilevers.block.MensulaColganteBlockEntity;
import spanishcantilevers.cable.RecolocarCables;
import spanishcantilevers.geometry.AjustesColgante;
import spanishcantilevers.item.MensulaColganteItem;

/**
 * Cliente -> servidor: la ventana de la mensula colgante se ha cerrado con estos ajustes. Van a la ya
 * puesta en {@code pos} o, si no hay pos, al objeto que el jugador tiene en la mano.
 */
public record ConfigurarColgante(@Nullable BlockPos pos, AjustesColgante ajustes) {
    /** Distancia maxima (al cuadrado, en bloques) a la que se puede cambiar una puesta. */
    private static final double DISTANCIA_MAX_SQ = 8 * 8;

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(pos, FriendlyByteBuf::writeBlockPos);
        ajustes.escribir(buf);
    }

    public static ConfigurarColgante decode(FriendlyByteBuf buf) {
        return new ConfigurarColgante(buf.readNullable(FriendlyByteBuf::readBlockPos), AjustesColgante.leer(buf));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            AjustesColgante validos = ajustes.validados();
            if (pos == null) {
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof MensulaColganteItem) {
                        MensulaColganteItem.setAjustes(stack, validos);
                        return;
                    }
                }
                return;
            }
            Level level = player.level();
            if (level.isLoaded(pos) && level.mayInteract(player, pos)
                    && player.distanceToSqr(pos.getCenter()) <= DISTANCIA_MAX_SQ
                    && level.getBlockEntity(pos) instanceof MensulaColganteBlockEntity be) {
                be.setAjustes(validos);
                // PNW guarda los enganches al tender: los cables ya puestos se rehacen con los nuevos
                RecolocarCables.enBloque(level, pos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

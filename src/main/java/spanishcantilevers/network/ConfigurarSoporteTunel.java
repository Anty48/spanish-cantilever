package spanishcantilevers.network;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.cable.RecolocarCables;
import spanishcantilevers.item.SoporteTunelItem;
import spanishcantilevers.block.SoporteTunelBlock;

/**
 * Cliente -> servidor: la ventana del soporte de tunel se ha cerrado con estos ajustes. Van al soporte
 * puesto en {@code pos} o, si no hay pos, al objeto que el jugador tiene en la mano.
 */
public record ConfigurarSoporteTunel(@Nullable BlockPos pos, AjustesTunel ajustes) {
    /** Distancia maxima (al cuadrado, en bloques) a la que se puede cambiar un soporte. */
    private static final double DISTANCIA_MAX_SQ = 8 * 8;

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(pos, FriendlyByteBuf::writeBlockPos);
        ajustes.escribir(buf);
    }

    public static ConfigurarSoporteTunel decode(FriendlyByteBuf buf) {
        return new ConfigurarSoporteTunel(buf.readNullable(FriendlyByteBuf::readBlockPos), AjustesTunel.leer(buf));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (pos == null) {
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof SoporteTunelItem) {
                        SoporteTunelItem.setAjustes(stack, ajustes);
                        return;
                    }
                }
                return;
            }
            Level level = player.level();
            if (!level.isLoaded(pos) || !level.mayInteract(player, pos) || player.distanceToSqr(pos.getCenter()) > DISTANCIA_MAX_SQ) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SoporteTunelBlock)) {
                return;
            }
            BlockState nuevo = ajustes.aplicar(state);
            if (!nuevo.canSurvive(level, pos)) {
                // de techo a pared (o al reves) solo si tiene donde agarrarse: si no, se queda como estaba
                nuevo = nuevo.setValue(SoporteTunelBlock.VERSION, state.getValue(SoporteTunelBlock.VERSION));
                player.displayClientMessage(Component.translatable("gui.spanishcantilevers.soporte_tunel.no_cabe"), true);
            }
            level.setBlock(pos, nuevo, Block.UPDATE_ALL);
            RecolocarCables.enBloque(level, pos);
        });
        ctx.get().setPacketHandled(true);
    }
}

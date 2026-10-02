package iberiancantilever.network;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import iberiancantilever.block.PosicionTunel;
import iberiancantilever.cable.RecolocarCables;
import iberiancantilever.item.SoporteTunelItem;
import iberiancantilever.block.SoporteTunelBlock;

/**
 * Cliente -> servidor: la ventana del soporte de tunel se ha cerrado con esta posicion y altura. Van al
 * soporte puesto en {@code pos} o, si no hay pos, al objeto que el jugador tiene en la mano.
 */
public record ConfigurarSoporteTunel(@Nullable BlockPos pos, PosicionTunel posicion, int altura) {
    /** Distancia maxima (al cuadrado, en bloques) a la que se puede cambiar un soporte. */
    private static final double DISTANCIA_MAX_SQ = 8 * 8;

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(pos, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(posicion);
        buf.writeVarInt(altura);
    }

    public static ConfigurarSoporteTunel decode(FriendlyByteBuf buf) {
        return new ConfigurarSoporteTunel(buf.readNullable(FriendlyByteBuf::readBlockPos), buf.readEnum(PosicionTunel.class), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            int valida = Mth.clamp(altura, 0, 4);
            if (pos == null) {
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof SoporteTunelItem) {
                        SoporteTunelItem.setAjustes(stack, posicion, valida);
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
            if (state.getBlock() instanceof SoporteTunelBlock) {
                level.setBlock(pos, state.setValue(SoporteTunelBlock.POSICION, posicion)
                        .setValue(SoporteTunelBlock.ALTURA, valida), Block.UPDATE_ALL);
                RecolocarCables.enBloque(level, pos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

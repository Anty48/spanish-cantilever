package spanishcantilevers.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import spanishcantilevers.cable.RecolocarCables;
import spanishcantilevers.block.MensulaColganteBlockEntity;
import spanishcantilevers.geometry.AjustesColgante;
import spanishcantilevers.item.MensulaColganteItem;

/**
 * Cliente -> servidor: la ventana de la mensula colgante se ha cerrado con estos ajustes. Van a la ya
 * puesta en {@code pos} o, si no hay pos, al objeto que el jugador tiene en la mano.
 */
public record ConfigurarColgante(@Nullable BlockPos pos, AjustesColgante ajustes) implements CustomPacketPayload {
    public static final Type<ConfigurarColgante> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("spanishcantilevers", "configurar_colgante"));
    public static final StreamCodec<FriendlyByteBuf, ConfigurarColgante> STREAM_CODEC =
            StreamCodec.ofMember(ConfigurarColgante::encode, ConfigurarColgante::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Distancia maxima (al cuadrado, en bloques) a la que se puede cambiar una puesta. */
    private static final double DISTANCIA_MAX_SQ = 8 * 8;

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(pos, BlockPos.STREAM_CODEC);
        ajustes.escribir(buf);
    }

    public static ConfigurarColgante decode(FriendlyByteBuf buf) {
        return new ConfigurarColgante(buf.readNullable(BlockPos.STREAM_CODEC), AjustesColgante.leer(buf));
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
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
    }
}

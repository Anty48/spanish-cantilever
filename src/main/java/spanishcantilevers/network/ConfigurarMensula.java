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
import spanishcantilevers.cable.TiranteDiagonal;
import spanishcantilevers.block.MensulaBlockEntity;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.item.MensulaItem;

/**
 * Cliente -> servidor: la ventana de la mensula se ha cerrado con estos ajustes. Van a la mensula
 * ya puesta en {@code pos} o, si no hay pos, al objeto que el jugador tiene en la mano (como PNW).
 * En una mensula doble, {@code cual} dice a cual de las dos (0 = la que ya estaba, 1 = la anadida).
 */
public record ConfigurarMensula(@Nullable BlockPos pos, int cual, Ajustes ajustes) implements CustomPacketPayload {
    public static final Type<ConfigurarMensula> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("spanishcantilevers", "configurar_mensula"));
    public static final StreamCodec<FriendlyByteBuf, ConfigurarMensula> STREAM_CODEC =
            StreamCodec.ofMember(ConfigurarMensula::encode, ConfigurarMensula::decode);

    public ConfigurarMensula(@Nullable BlockPos pos, Ajustes ajustes) {
        this(pos, 0, ajustes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Distancia maxima (al cuadrado, en bloques) a la que se puede cambiar una mensula puesta. */
    private static final double DISTANCIA_MAX_SQ = 8 * 8;

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(pos, BlockPos.STREAM_CODEC);
        buf.writeByte(cual);
        ajustes.escribir(buf);
    }

    public static ConfigurarMensula decode(FriendlyByteBuf buf) {
        return new ConfigurarMensula(buf.readNullable(BlockPos.STREAM_CODEC), buf.readByte() == 1 ? 1 : 0, Ajustes.leer(buf));
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            Ajustes validos = ajustes.validados();
            if (pos == null) {
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof MensulaItem) {
                        MensulaItem.setAjustes(stack, validos);
                        return;
                    }
                }
                return;
            }
            Level level = player.level();
            if (level.isLoaded(pos) && level.mayInteract(player, pos)
                    && player.distanceToSqr(pos.getCenter()) <= DISTANCIA_MAX_SQ
                    && level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
                be.setAjustes(cual, validos);
                // PNW guarda los enganches al tender: los cables ya puestos se rehacen con los nuevos
                RecolocarCables.enBloque(level, pos);
                TiranteDiagonal.recolocar(level, pos, player);
            }
        });
    }
}

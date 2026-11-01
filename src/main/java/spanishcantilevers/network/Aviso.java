package spanishcantilevers.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import spanishcantilevers.client.AvisosCliente;

/**
 * Servidor -> cliente: un aviso en rojo (clave de traduccion). El cliente lo dibuja sobre la barra
 * rapida partido en varias lineas, porque en la barra de accion de vanilla los largos se salen.
 */
public record Aviso(String clave) implements CustomPacketPayload {
    public static final Type<Aviso> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("spanishcantilevers", "aviso"));
    public static final StreamCodec<FriendlyByteBuf, Aviso> STREAM_CODEC =
            StreamCodec.ofMember(Aviso::encode, Aviso::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void enviar(ServerPlayer player, String clave) {
        PacketDistributor.sendToPlayer(player, new Aviso(clave));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(clave);
    }

    public static Aviso decode(FriendlyByteBuf buf) {
        return new Aviso(buf.readUtf());
    }

    public void handle(IPayloadContext ctx) {
        // Solo se registra hacia el cliente: el handler corre siempre en el cliente.
        ctx.enqueueWork(() -> AvisosCliente.mostrar(clave));
    }
}

package spanishcantilevers.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import spanishcantilevers.client.AvisosCliente;

/**
 * Servidor -> cliente: un aviso en rojo (clave de traduccion). El cliente lo dibuja sobre la barra
 * rapida partido en varias lineas, porque en la barra de accion de vanilla los largos se salen.
 */
public record Aviso(String clave) {
    public static void enviar(ServerPlayer player, String clave) {
        ModRed.CANAL.send(PacketDistributor.PLAYER.with(() -> player), new Aviso(clave));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(clave);
    }

    public static Aviso decode(FriendlyByteBuf buf) {
        return new Aviso(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AvisosCliente.mostrar(clave)));
        ctx.get().setPacketHandled(true);
    }
}

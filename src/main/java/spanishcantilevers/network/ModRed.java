package spanishcantilevers.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import spanishcantilevers.SpanishCantilevers;

/**
 * Red del mod (las ventanas de la mensula y del soporte de tunel, y los avisos en rojo).
 *
 * Port a 1.21: ya no hay SimpleChannel; cada mensaje es un {@code CustomPacketPayload} que se registra
 * aqui y se envia con {@code PacketDistributor}.
 */
@EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModRed {
    private static final String VERSION = "3";

    private ModRed() {
    }

    @SubscribeEvent
    public static void registrar(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(ConfigurarMensula.TYPE, ConfigurarMensula.STREAM_CODEC, (p, ctx) -> p.handle(ctx));
        registrar.playToServer(ConfigurarSoporteTunel.TYPE, ConfigurarSoporteTunel.STREAM_CODEC, (p, ctx) -> p.handle(ctx));
        registrar.playToClient(Aviso.TYPE, Aviso.STREAM_CODEC, (p, ctx) -> p.handle(ctx));
    }
}

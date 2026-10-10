package spanishcantilevers.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import spanishcantilevers.SpanishCantilevers;

/** Canal de red del mod (las ventanas de la mensula y del soporte de tunel, y los avisos en rojo). */
public final class ModRed {
    private static final String VERSION = "3";
    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SpanishCantilevers.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModRed() {
    }

    public static void registrar() {
        CANAL.messageBuilder(ConfigurarMensula.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigurarMensula::encode)
                .decoder(ConfigurarMensula::decode)
                .consumerNetworkThread(ConfigurarMensula::handle)
                .add();
        CANAL.messageBuilder(ConfigurarSoporteTunel.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigurarSoporteTunel::encode)
                .decoder(ConfigurarSoporteTunel::decode)
                .consumerNetworkThread(ConfigurarSoporteTunel::handle)
                .add();
        CANAL.messageBuilder(Aviso.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Aviso::encode)
                .decoder(Aviso::decode)
                .consumerNetworkThread(Aviso::handle)
                .add();
    }
}

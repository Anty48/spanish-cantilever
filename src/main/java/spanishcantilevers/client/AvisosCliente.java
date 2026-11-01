package spanishcantilevers.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import spanishcantilevers.SpanishCantilevers;

/**
 * Dibuja el ultimo aviso en rojo donde va la barra de accion, pero partido en lineas y con margenes
 * amplios a los lados; se desvanece al final como el de vanilla.
 */
@EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AvisosCliente {
    /** Cuanto se ve (ms) y cuanto dura el desvanecido al final. */
    private static final long DURACION = 3500, DESVANECIDO = 1000;
    /** El texto ocupa como mucho esta fraccion del ancho de la pantalla (y nunca mas de ANCHO_MAX px). */
    private static final float FRACCION_ANCHO = 0.5f;
    private static final int ANCHO_MAX = 240;
    /** La ultima linea queda aqui por encima del borde de abajo (justo encima de la barra rapida y la vida). */
    private static final int SOBRE_BORDE = 72;

    private static Component texto;
    private static long desde;

    private AvisosCliente() {
    }

    public static void mostrar(String clave) {
        texto = Component.translatable(clave).withStyle(ChatFormatting.RED);
        desde = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void registrar(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE,
                ResourceLocation.fromNamespaceAndPath(SpanishCantilevers.MOD_ID, "aviso"), (graphics, deltaTracker) -> {
            int ancho = graphics.guiWidth();
            int alto = graphics.guiHeight();
            if (texto == null) {
                return;
            }
            long pasado = System.currentTimeMillis() - desde;
            if (pasado > DURACION) {
                texto = null;
                return;
            }
            float opacidad = Mth.clamp((DURACION - pasado) / (float) DESVANECIDO, 0f, 1f);
            int alfa = Math.max(8, (int) (opacidad * 255)) << 24;
            Font font = Minecraft.getInstance().font;
            List<FormattedCharSequence> lineas = font.split(texto, Math.min(ANCHO_MAX, (int) (ancho * FRACCION_ANCHO)));
            int y = alto - SOBRE_BORDE - (lineas.size() - 1) * (font.lineHeight + 1);
            for (FormattedCharSequence linea : lineas) {
                graphics.drawString(font, linea, (ancho - font.width(linea)) / 2, y, 0xFFFFFF | alfa, true);
                y += font.lineHeight + 1;
            }
        });
    }
}

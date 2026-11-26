package spanishcantilevers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateSlider;
import de.mrjulsen.paw.client.gui.widgets.IIconRepresentable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.AisladorColgante;
import spanishcantilevers.block.FormaColgante;
import spanishcantilevers.block.MensulaColganteBlock;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.geometry.AjustesColgante;

/**
 * Ventana de la mensula colgante, como la de la mensula (textura de PNW, deslizadores de Create y vista
 * previa en el centro, con un trozo de la viga de la que cuelga): anchura, altura de catenaria, aislador
 * (encima o colgado de la viga) y forma del tubo.
 */
public class VentanaColgante extends DLWindow {
    private static final DLTexture TEXTURA = new DLTexture(new ResourceLocation("pantographsandwires", "textures/gui/cantilever_settings.png"), 256, 256);
    private static final int ANCHO = 251;
    private static final int ALTO = 231;
    private static final int FILA_DESLIZADORES = 155;
    private static final int FILA_SELECTORES = 177;
    private static final int VISTA_X = 27;
    private static final int VISTA_Y = 27;
    private static final int VISTA_ANCHO = 197;
    private static final int VISTA_ALTO = 124;

    private final Component titulo = Component.translatable("gui.spanishcantilevers.colgante.titulo");
    private final MensulaColganteBlock.Viga viga;

    private AjustesColgante ajustes;
    private List<BakedQuad> vista;
    private float centroX;
    private float centroY;
    private float escala;

    public VentanaColgante(DLWindowManager manager, AjustesColgante inicial, MensulaColganteBlock.Viga viga, Consumer<AjustesColgante> alCerrar) {
        super(manager);
        this.ajustes = inicial.validados();
        this.viga = viga;
        setSize(ANCHO, ALTO);
        centrar();
        addEventListener(DLGuiStandardEvents.ScreenLayoutUpdatedEvent.class, (s, e) -> {
            centrar();
            return false;
        });
        addEventListener(DLGuiStandardEvents.CloseEvent.class, (s, e) -> {
            alCerrar.accept(ajustes);
            return false;
        });
        addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
            if (e.keyCode() == 256) {
                getWindowManager().closeWindow(this);
            }
            return false;
        });

        CreateSlider anchura = deslizador(AjustesColgante.ANCHURA, "gui.spanishcantilevers.mensula.anchura", ajustes.anchura(),
                v -> ajustes = ajustes.conAnchura(v));
        CreateSlider catenaria = deslizador(AjustesColgante.ALTURA_CATENARIA, "gui.spanishcantilevers.mensula.altura_catenaria",
                ajustes.alturaCatenaria(), v -> ajustes = ajustes.conAlturaCatenaria(v));

        VentanaMensula.SelectorIconos<OpcionAislador> aislador = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES,
                OpcionAislador.class, VentanaMensula.ICONO_AISLADOR_COLGANTE);
        aislador.value.set((double) ajustes.aislador().ordinal());
        aislador.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            ajustes = ajustes.conAislador(OpcionAislador.values()[(int) e.value()].valor);
            actualizarVista();
            return false;
        });
        addComponent(aislador);

        VentanaMensula.SelectorIconos<OpcionForma> forma = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES,
                OpcionForma.class, VentanaMensula.ICONO_FORMA_COLGANTE);
        forma.value.set((double) ajustes.forma().ordinal());
        forma.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            ajustes = ajustes.conForma(OpcionForma.values()[(int) e.value()].valor);
            actualizarVista();
            return false;
        });
        addComponent(forma);

        CreateButton hecho = new CreateButton(ANCHO - 7 - 18, ALTO - 6 - 18, AllIcons.I_CONFIRM);
        hecho.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(hecho);

        // guarda lo que hay ahora como configuracion por defecto (la que propone la ventana a objetos nuevos)
        CreateButton predeterminar = new CreateButton(7, ALTO - 6 - 18, AllIcons.I_CONFIG_SAVE);
        predeterminar.tooltip.set(new DLTooltip(List.of(Component.translatable("gui.spanishcantilevers.mensula.predeterminar")), 200));
        predeterminar.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            boolean ok = ClienteMensula.guardarPredeterminadoColgante(ajustes);
            predeterminar.tooltip.set(new DLTooltip(List.of(Component.translatable(ok ? "gui.spanishcantilevers.mensula.predeterminado_guardado"
                    : "gui.spanishcantilevers.mensula.predeterminado_error")), 200));
            return false;
        });
        addComponent(predeterminar);

        repartir(new DLGuiComponent[]{anchura, catenaria}, ANCHO / 2);
        repartir(new DLGuiComponent[]{aislador, forma}, ANCHO / 2);
        actualizarVista();
    }

    private CreateSlider deslizador(Ajustes.Medida medida, String clave, float valor, Consumer<Float> cambiar) {
        CreateSlider s = new CreateSlider(0, FILA_DESLIZADORES, 45, 14, Component.translatable(clave));
        s.min.set((double) medida.min());
        s.max.set((double) medida.max());
        s.step.set((double) medida.paso());
        s.value.set((double) valor);
        s.addEventListener(DLSlider.ValueChangedEvent.class, (src, e) -> {
            cambiar.accept((float) e.value());
            actualizarVista();
            return false;
        });
        addComponent(s);
        return s;
    }

    private void centrar() {
        setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);
    }

    /** Como en la ventana de la mensula: pone en fila, centrados, los componentes. */
    private static void repartir(DLGuiComponent[] componentes, int centroX) {
        List<DLGuiComponent> visibles = new ArrayList<>(List.of(componentes));
        int w = visibles.get(0).width();
        int hueco = 4;
        double x = centroX - (visibles.size() * w + (visibles.size() - 1) * hueco) / 2.0;
        for (DLGuiComponent c : visibles) {
            c.setX((int) Math.round(x));
            x += w + hueco;
        }
    }

    /** Vuelve a montar la vista previa y la encuadra con lo que ocupan sus vertices (px). */
    private void actualizarVista() {
        if (ajustes == null) {
            return;
        }
        vista = ColganteBakedModel.vistaPrevia(ajustes.validados(), viga);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int paso = DefaultVertexFormat.BLOCK.getIntegerSize();
        for (BakedQuad quad : vista) {
            int[] v = quad.getVertices();
            for (int i = 0; i < v.length; i += paso) {
                float x = Float.intBitsToFloat(v[i]) * 16f;
                float y = Float.intBitsToFloat(v[i + 1]) * 16f;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        minX -= 2f;
        maxX += 2f;
        minY -= 2f;
        maxY += 2f;
        centroX = (minX + maxX) / 2f;
        centroY = (minY + maxY) / 2f;
        escala = Math.min(40f, Math.min((VISTA_ANCHO - 16) / (maxX - minX), (VISTA_ALTO - 12) / (maxY - minY)) * 16f);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawTexture(TEXTURA, graphics, 0, 0, ANCHO, ALTO, 0, 0);
        if (vista != null) {
            PoseStack pose = graphics.poseStack();
            Lighting.setupFor3DItems();
            pose.pushPose();
            pose.setIdentity();
            pose.translate(x() + VISTA_X + VISTA_ANCHO / 2f, y() + VISTA_Y + VISTA_ALTO / 2f, 200f);
            // como la de la mensula: de perfil, balanceandose un poco
            pose.scale(escala, escala, -escala);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            pose.mulPose(Axis.YP.rotationDegrees(180f + 25f * (float) Math.sin(System.nanoTime() / 1.5e9)));
            pose.translate(-centroX / 16f, -centroY / 16f, -0.5f);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
            for (BakedQuad quad : vista) {
                vc.putBulkData(pose.last(), quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            buffer.endBatch();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            pose.popPose();
            Lighting.setupForFlatItems();
        }
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(0f, 0f, 500f);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 4, titulo, DragonLib.VANILLA_UI_FONT_COLOR,
                ETextAlignment.CENTER, false);
        graphics.poseStack().popPose();
    }

    /** Aislador (mismo orden que {@link AisladorColgante}); sus iconos los dibuja SelectorIconos. */
    public enum OpcionAislador implements ITranslatableEnum, IIconRepresentable {
        ENCIMA(AisladorColgante.ENCIMA),
        COLGADO(AisladorColgante.COLGADO),
        VERTICAL_ENCIMA(AisladorColgante.VERTICAL_ENCIMA),
        VERTICAL_COLGADO(AisladorColgante.VERTICAL_COLGADO);

        private final AisladorColgante valor;

        OpcionAislador(AisladorColgante valor) {
            this.valor = valor;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "aislador_colgante", valor.getSerializedName());
        }
    }

    /** Forma del tubo (mismo orden que {@link FormaColgante}). */
    public enum OpcionForma implements ITranslatableEnum, IIconRepresentable {
        DIAGONAL(FormaColgante.DIAGONAL),
        CURVADA(FormaColgante.CURVADA);

        private final FormaColgante valor;

        OpcionForma(FormaColgante valor) {
            this.valor = valor;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "forma_colgante", valor.getSerializedName());
        }
    }
}

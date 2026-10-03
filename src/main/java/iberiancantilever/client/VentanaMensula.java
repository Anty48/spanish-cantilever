package iberiancantilever.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLToggleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateEnumSlider;
import de.mrjulsen.paw.client.gui.widgets.CreateSlider;
import de.mrjulsen.paw.client.gui.widgets.IIconRepresentable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import iberiancantilever.IberianCantilever;
import iberiancantilever.block.ModoZigzag;
import iberiancantilever.block.TipoAislador;
import iberiancantilever.geometry.Ajustes;
import iberiancantilever.geometry.MensulaLayout;

/**
 * Ventana de ajustes de la mensula, hecha igual que la de la mensula de PNW (su textura, sus
 * deslizadores de Create y la vista previa en el centro), para que se sienta parte del mismo mod.
 * Basada en CantileverSettingsScreen de Pantographs &amp; Wires (MrJulsen, GPL-3.0).
 */
public class VentanaMensula extends DLWindow {
    /** Como en PNW, las opciones avanzadas se recuerdan mientras dure la partida. */
    private static boolean avanzado = false;

    private static final DLTexture TEXTURA = new DLTexture(new ResourceLocation("pantographsandwires", "textures/gui/cantilever_settings.png"), 256, 256);
    private static final DLTexture ICONOS = new DLTexture(new ResourceLocation(IberianCantilever.MOD_ID, "textures/gui/iconos.png"), 224, 16);
    /** Primer icono de cada selector en textures/gui/iconos.png (uno de 16x16 por opcion, en orden). */
    private static final int ICONO_AISLADOR = 0;
    private static final int ICONO_CABLE_SOPORTE = 3;
    static final int ICONO_POSICION_TUNEL = 5;
    static final int ICONO_VERSION_TUNEL = 8;
    static final int ICONO_TAMANO_TECHO = 10;
    static final int ICONO_TAMANO_PARED = 12;
    private static final int ANCHO = 251;
    private static final int ALTO = 231;
    private static final int FILA_DESLIZADORES = 155;
    private static final int FILA_SELECTORES = 177;
    /** Zona azul de la textura donde se dibuja la vista previa. */
    private static final int VISTA_X = 27;
    private static final int VISTA_Y = 27;
    private static final int VISTA_ANCHO = 197;
    private static final int VISTA_ALTO = 124;

    private final Component titulo = Component.translatable("gui.iberiancantilever.mensula.titulo");
    private final Component txtAvanzado = Component.translatable("gui.iberiancantilever.mensula.avanzado");

    private Ajustes ajustes;
    private List<BakedQuad> vista;
    private float centroX;
    private float centroY;
    private float escala;

    public VentanaMensula(DLWindowManager manager, Ajustes inicial, Consumer<Ajustes> alCerrar) {
        super(manager);
        this.ajustes = inicial.validados();
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

        // se rellena al final: los cambios de anchura y de zigzag recolocan los deslizadores
        Runnable[] recolocar = {() -> {
        }};

        // --- fila de arriba: medidas (como width / height / catenary height / y offset de PNW) ---
        CreateSlider anchura = deslizador(Ajustes.ANCHURA, "anchura", ajustes.anchura(), v -> {
            ajustes = ajustes.conAnchura(v);
            recolocar[0].run();
        });
        CreateSlider altura = deslizador(Ajustes.ALTURA, "altura", ajustes.altura(), v -> ajustes = ajustes.conAltura(v));
        CreateSlider catenaria = deslizador(Ajustes.ALTURA_CATENARIA, "altura_catenaria", ajustes.alturaCatenaria(),
                v -> ajustes = ajustes.conAlturaCatenaria(v));
        CreateSlider desplazamiento = deslizador(Ajustes.DESPLAZAMIENTO_Y, "desplazamiento_y", ajustes.desplazamientoY(),
                v -> ajustes = ajustes.conDesplazamientoY(v));

        // --- fila de abajo: lo que se escoge con iconos ---
        SelectorIconos<OpcionAislador> aislador = new SelectorIconos<>(0, FILA_SELECTORES, OpcionAislador.class, ICONO_AISLADOR);
        aislador.value.set((double) ajustes.tipo().ordinal());
        catenaria.min.set((double) Ajustes.alturaCatenariaMinima(ajustes.tipo()));
        aislador.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            TipoAislador tipo = OpcionAislador.values()[(int) e.value()].tipo;
            ajustes = ajustes.conTipo(tipo);
            // con el tipo 2 el hilo de contacto no puede subir tanto (el deslizador se ajusta solo)
            catenaria.min.set((double) Ajustes.alturaCatenariaMinima(tipo));
            catenaria.value.set(Math.max(catenaria.value.get(), Ajustes.alturaCatenariaMinima(tipo)));
            ajustes = ajustes.conAlturaCatenaria(catenaria.value.get().floatValue());
            actualizarVista();
            return false;
        });
        addComponent(aislador);

        CreateEnumSlider<OpcionZigzag> zigzag = new CreateEnumSlider<>(0, FILA_SELECTORES, 50, 20, OpcionZigzag.class);
        zigzag.value.set((double) ajustes.modo().ordinal());
        zigzag.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            ajustes = ajustes.conModo(OpcionZigzag.values()[(int) e.value()].modo);
            recolocar[0].run();
            actualizarVista();
            return false;
        });
        addComponent(zigzag);

        SelectorIconos<Tirante> tirante = new SelectorIconos<>(0, FILA_SELECTORES, Tirante.class, ICONO_CABLE_SOPORTE);
        tirante.value.set((double) (ajustes.tirante() ? Tirante.SI : Tirante.NO).ordinal());
        tirante.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            ajustes = ajustes.conTirante(Tirante.values()[(int) e.value()].valor);
            actualizarVista();
            return false;
        });
        addComponent(tirante);

        // --- opciones avanzadas y boton de hecho, en el mismo sitio que PNW ---
        Font font = Minecraft.getInstance().font;
        int cbW = 16 + font.width(txtAvanzado);
        int cbH = font.lineHeight;
        DLCheckBox casilla = new DLCheckBox(220 - cbW, 148 - cbH, cbW, cbH) {
            @Override
            public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
                GuiUtils.drawTexture(TEXTURA, graphics, 0, height() / 2 - 4, 12, 7, 0, checked.get() ? 241 : 249);
                GuiUtils.drawString(graphics, font, 16, height() / 2 - font.lineHeight / 2, text.get(),
                        DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
            }
        };
        CreateButton hecho = new CreateButton(ANCHO - 7 - 18, ALTO - 6 - 18, AllIcons.I_CONFIRM);
        hecho.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(hecho);

        // guarda lo que hay ahora como configuracion por defecto (la que propone la ventana a objetos nuevos)
        // (boton de Create con icono, como el de hecho; lo que hace va en su tooltip)
        CreateButton predeterminar = new CreateButton(7, ALTO - 6 - 18, AllIcons.I_CONFIG_SAVE);
        predeterminar.tooltip.set(new DLTooltip(List.of(Component.translatable("gui.iberiancantilever.mensula.predeterminar")), 200));
        predeterminar.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            boolean ok = ClienteMensula.guardarPredeterminado(ajustes);
            predeterminar.tooltip.set(new DLTooltip(List.of(Component.translatable(ok ? "gui.iberiancantilever.mensula.predeterminado_guardado"
                    : "gui.iberiancantilever.mensula.predeterminado_error")), 200));
            return false;
        });
        addComponent(predeterminar);

        recolocar[0] = () -> {
            boolean av = casilla.checked.get();
            // la altura del soporte no pinta nada en el interior con la anchura minima
            altura.visible.set(av && Ajustes.usaAltura(ajustes.anchura(), ajustes.modo()));
            catenaria.visible.set(av);
            desplazamiento.visible.set(av);
            if (!av) {
                // sin opciones avanzadas, las alturas son las de una CR-160 normal (PNW hace lo mismo)
                altura.value.set((double) Ajustes.alturaAutomatica(ajustes.anchura(), ajustes.modo()));
                catenaria.value.set((double) Math.max(Ajustes.ALTURA_CATENARIA.defecto(), Ajustes.alturaCatenariaMinima(ajustes.tipo())));
                desplazamiento.value.set((double) Ajustes.DESPLAZAMIENTO_Y.defecto());
            }
            repartir(new DLGuiComponent[]{anchura, altura, catenaria, desplazamiento}, ANCHO / 2);
            repartir(new DLGuiComponent[]{aislador, zigzag, tirante}, ANCHO / 2);
        };
        casilla.text.set(txtAvanzado);
        casilla.cursor.set(CursorType.HAND);
        casilla.checked.set(avanzado);
        casilla.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            avanzado = e.checked();
            recolocar[0].run();
            return false;
        });
        addComponent(casilla);
        recolocar[0].run();
        actualizarVista();
    }

    private CreateSlider deslizador(Ajustes.Medida medida, String clave, float valor, Consumer<Float> cambiar) {
        CreateSlider s = new CreateSlider(0, FILA_DESLIZADORES, 45, 14,
                Component.translatable("gui.iberiancantilever.mensula." + clave));
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

    /** Copia de PNW: pone en fila, centrados, los componentes visibles. */
    private static void repartir(DLGuiComponent[] componentes, int centroX) {
        List<DLGuiComponent> visibles = new ArrayList<>();
        for (DLGuiComponent c : componentes) {
            if (c != null && c.visible.get()) {
                visibles.add(c);
            }
        }
        if (visibles.isEmpty()) {
            return;
        }
        int w = visibles.get(0).width();
        int hueco = 4;
        double x = centroX - (visibles.size() * w + (visibles.size() - 1) * hueco) / 2.0;
        for (DLGuiComponent c : visibles) {
            c.setX((int) Math.round(x));
            x += w + hueco;
        }
    }

    /** Vuelve a montar la mensula de la vista previa y calcula como encuadrarla. */
    private void actualizarVista() {
        if (ajustes == null) {
            return;
        }
        Ajustes a = ajustes.validados();
        vista = MensulaBakedModel.vistaPrevia(a);
        MensulaLayout.Resultado r = MensulaLayout.calcular(a, 0f);
        float minY = Math.min(r.contacto().y(), 0f) - 2f;
        float maxY = Math.max(r.sustentador().y(), MensulaLayout.BARRA_Y) + 3f;
        float maxX = r.contacto().x() + 20f;
        centroX = maxX / 2f;
        centroY = (minY + maxY) / 2f;
        // px de la mensula -> px de la pantalla, sin pasarse de la zona azul
        escala = Math.min(40f, Math.min((VISTA_ANCHO - 16) / maxX, (VISTA_ALTO - 12) / (maxY - minY)) * 16f);
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
            // mismos giros que PNW (escala con Z negativa y media vuelta en Z) para verla de perfil
            pose.scale(escala, escala, -escala);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            // se balancea un poco para que se vea en 3D sin perder el perfil
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

    /**
     * Selector de PNW, pero con nuestros iconos (PNW no tiene de estos aisladores, ni del cable de soporte,
     * ni de la pinza del soporte de tunel).
     */
    static class SelectorIconos<T extends Enum<T> & ITranslatableEnum & IIconRepresentable> extends CreateEnumSlider<T> {
        private final int primerIcono;

        SelectorIconos(int x, int y, Class<T> clase, int primerIcono) {
            super(x, y, 50, 20, clase);
            this.primerIcono = primerIcono;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            int ancho = sliderWidth.get();
            int k = ancho / 2 - 3;
            int i = (height() - 8) / 2;
            GuiUtils.drawTexture(TEXTURE, graphics, k, i, 7, 8);
            GuiUtils.drawTexture(TEXTURE, graphics, width() - 7 - k, i, 7, 8);
            GuiUtils.drawTexture(TEXTURE, graphics, k + 7, i, width() - 14 - k * 2, 8, 7, 0);
            int sliderX = (int) ((width() - ancho) / (max.get() - min.get()) * (value.get() - min.get()));
            GuiUtils.drawTexture(TEXTURE, graphics, sliderX, height() / 2 - 11, 22, 22, 0, 43);
            GuiUtils.drawTexture(ICONOS, graphics, sliderX + 3, 3, 16, 16, (primerIcono + value.get().intValue()) * 16, 0);
        }
    }

    /** Opciones de aislador (mismo orden que {@link TipoAislador}); sus iconos los dibuja SelectorAislador. */
    public enum OpcionAislador implements ITranslatableEnum, IIconRepresentable {
        TIPO1(TipoAislador.TIPO1),
        TIPO2(TipoAislador.TIPO2),
        TIPO3(TipoAislador.TIPO3);

        private final TipoAislador tipo;

        OpcionAislador(TipoAislador tipo) {
            this.tipo = tipo;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(IberianCantilever.MOD_ID, "aislador", tipo.getSerializedName());
        }
    }

    /** Zigzag del hilo con los iconos del brazo de atirantado de PNW (mismo orden que {@link ModoZigzag}). */
    public enum OpcionZigzag implements ITranslatableEnum, IIconRepresentable {
        INTERIOR(ModoZigzag.INTERIOR, ModGuiIcons.CANTILEVER_INNER),
        MEDIO(ModoZigzag.MEDIO, ModGuiIcons.CANTILEVER_CENTER),
        EXTERIOR(ModoZigzag.EXTERIOR, ModGuiIcons.CANTILEVER_OUTER);

        private final ModoZigzag modo;
        private final ModGuiIcons icono;

        OpcionZigzag(ModoZigzag modo, ModGuiIcons icono) {
            this.modo = modo;
            this.icono = icono;
        }

        @Override
        public ModGuiIcons getIcon() {
            return icono;
        }

        @Override
        public Data getTranslationData() {
            return new Data(IberianCantilever.MOD_ID, "zigzag", modo.getSerializedName());
        }
    }

    /** Cable de soporte (el tirante) si/no; sus iconos los dibuja SelectorIconos. */
    public enum Tirante implements ITranslatableEnum, IIconRepresentable {
        SI(true),
        NO(false);

        private final boolean valor;

        Tirante(boolean valor) {
            this.valor = valor;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(IberianCantilever.MOD_ID, "tirante", valor ? "si" : "no");
        }
    }
}

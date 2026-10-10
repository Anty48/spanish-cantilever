package spanishcantilevers.client;

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
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.block.PosicionTunel;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.block.TamanoTunel;
import spanishcantilevers.block.VersionTunel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Ventana del soporte de tunel (el unico menu de todas sus variantes): version (techo / pared),
 * tamano (normal / grande en el de techo, brazo corto / largo en el de pared), donde agarra el perfil
 * (izquierda / centro / derecha, solo el de techo), su altura (cada version la suya) y el tamano general
 * (todo el soporte y el perfil que sale de el, mas grande). Los selectores
 * no se mueven al cambiar de version: en el de pared el hueco de la pinza queda vacio. Misma textura y
 * vista previa que la de la mensula ({@link VentanaMensula}), con un bloque de referencia (el techo o
 * la pared donde va; una linterna de mar, que se distingue bien al girar).
 */
public class VentanaSoporteTunel extends DLWindow {
    private static final DLTexture TEXTURA = new DLTexture(new ResourceLocation("pantographsandwires", "textures/gui/cantilever_settings.png"), 256, 256);
    private static final int ANCHO = 251;
    private static final int ALTO = 231;
    private static final int FILA_DESLIZADORES = 155;
    private static final int FILA_SELECTORES = 177;
    /** Ancho de cada selector y separacion entre ellos. */
    private static final int ANCHO_SELECTOR = 50;
    private static final int PASO_SELECTORES = 58;
    private static final int VISTA_X = 27;
    private static final int VISTA_Y = 27;
    private static final int VISTA_ANCHO = 197;
    private static final int VISTA_ALTO = 124;
    /** X de los dos deslizadores de la fila: la altura (la de la version escogida) y el tamano general. */
    private static final int X_ALTURA = ANCHO / 2 - 45 - 5;
    private static final int X_ESCALA = ANCHO / 2 + 5;
    /** Pasos de altura por bloque (el deslizador va en bloques, como los de PNW). */
    private static final float PASOS_POR_BLOQUE = 16f / SoporteTunelBlock.PX_POR_ALTURA;

    private final Component titulo = Component.translatable("gui.spanishcantilevers.soporte_tunel.titulo");

    private VersionTunel version;
    private TamanoTunel tamano;
    private PosicionTunel posicion;
    private int altura;
    private int alturaPared;
    private int tamanoGeneral;
    private List<BakedQuad> vista;
    /** Centro (bloques) de lo que se ve en la vista previa y cuanto se amplia. */
    private float centroX;
    private float centroY;
    private float centroZ;
    private float escala;

    private final VentanaMensula.SelectorIconos<OpcionVersion> selectorVersion;
    private final VentanaMensula.SelectorIconos<OpcionTamanoTecho> selectorTamanoTecho;
    private final VentanaMensula.SelectorIconos<OpcionTamanoPared> selectorTamanoPared;
    private final VentanaMensula.SelectorIconos<OpcionPosicion> selectorPosicion;
    private final CreateSlider deslizadorTecho;
    private final CreateSlider deslizadorPared;
    private final CreateSlider deslizadorEscala;

    public VentanaSoporteTunel(DLWindowManager manager, AjustesTunel ajustes, Consumer<AjustesTunel> alCerrar) {
        super(manager);
        this.version = ajustes.version();
        this.tamano = ajustes.tamano();
        this.posicion = ajustes.posicion();
        this.altura = ajustes.altura();
        this.alturaPared = ajustes.alturaPared();
        this.tamanoGeneral = ajustes.escala();
        setSize(ANCHO, ALTO);
        centrar();
        addEventListener(DLGuiStandardEvents.ScreenLayoutUpdatedEvent.class, (s, e) -> {
            centrar();
            return false;
        });
        addEventListener(DLGuiStandardEvents.CloseEvent.class, (s, e) -> {
            alCerrar.accept(ajustes());
            return false;
        });
        addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
            if (e.keyCode() == 256) {
                getWindowManager().closeWindow(this);
            }
            return false;
        });

        // la altura: el de techo en bloques que cuelga de mas; el de pared en px arriba o abajo del centro
        // del bloque (sin salirse de el). Los dos en el mismo sitio, a la izquierda; se ve el de la version
        // escogida. A la derecha, el tamano general (las dos versiones)
        deslizadorTecho = new CreateSlider(X_ALTURA, FILA_DESLIZADORES, 45, 14,
                Component.translatable("gui.spanishcantilevers.soporte_tunel.altura"));
        deslizadorTecho.min.set(0.0);
        deslizadorTecho.max.set(SoporteTunelBlock.ALTURA_MAX / (double) PASOS_POR_BLOQUE);
        deslizadorTecho.step.set(1 / (double) PASOS_POR_BLOQUE);
        deslizadorTecho.value.set(altura / (double) PASOS_POR_BLOQUE);
        deslizadorTecho.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            altura = Math.round((float) e.value() * PASOS_POR_BLOQUE);
            actualizarVista();
            return false;
        });
        addComponent(deslizadorTecho);
        double medioPared = SoporteTunelBlock.ALTURA_PARED_CENTRO * SoporteTunelBlock.PX_POR_ALTURA_PARED;
        deslizadorPared = new CreateSlider(X_ALTURA, FILA_DESLIZADORES, 45, 14,
                Component.translatable("gui.spanishcantilevers.soporte_tunel.altura_pared"));
        deslizadorPared.min.set(-medioPared);
        deslizadorPared.max.set(medioPared);
        deslizadorPared.step.set((double) SoporteTunelBlock.PX_POR_ALTURA_PARED);
        deslizadorPared.value.set((alturaPared - SoporteTunelBlock.ALTURA_PARED_CENTRO) * (double) SoporteTunelBlock.PX_POR_ALTURA_PARED);
        deslizadorPared.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            alturaPared = SoporteTunelBlock.ALTURA_PARED_CENTRO + Math.round((float) e.value() / SoporteTunelBlock.PX_POR_ALTURA_PARED);
            actualizarVista();
            return false;
        });
        addComponent(deslizadorPared);
        deslizadorEscala = new CreateSlider(X_ESCALA, FILA_DESLIZADORES, 45, 14,
                Component.translatable("gui.spanishcantilevers.soporte_tunel.escala"));
        deslizadorEscala.min.set(1.0);
        deslizadorEscala.max.set(1.0 + SoporteTunelBlock.ESCALA_MAX * (double) SoporteTunelBlock.PASO_ESCALA);
        deslizadorEscala.step.set((double) SoporteTunelBlock.PASO_ESCALA);
        deslizadorEscala.value.set(1.0 + tamanoGeneral * (double) SoporteTunelBlock.PASO_ESCALA);
        deslizadorEscala.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            tamanoGeneral = Math.round(((float) e.value() - 1f) / SoporteTunelBlock.PASO_ESCALA);
            actualizarVista();
            return false;
        });
        addComponent(deslizadorEscala);

        selectorVersion = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES, OpcionVersion.class, VentanaMensula.ICONO_VERSION_TUNEL);
        selectorVersion.value.set((double) version.ordinal());
        selectorVersion.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            version = OpcionVersion.values()[(int) e.value()].version;
            recolocar();
            actualizarVista();
            return false;
        });
        addComponent(selectorVersion);

        // el tamano son dos selectores (con sus iconos y nombres) y se ve el de la version escogida
        selectorTamanoTecho = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES, OpcionTamanoTecho.class, VentanaMensula.ICONO_TAMANO_TECHO);
        selectorTamanoPared = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES, OpcionTamanoPared.class, VentanaMensula.ICONO_TAMANO_PARED);
        for (VentanaMensula.SelectorIconos<?> selector : List.of(selectorTamanoTecho, selectorTamanoPared)) {
            selector.value.set((double) tamano.ordinal());
            selector.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
                tamano = TamanoTunel.values()[(int) e.value()];
                actualizarVista();
                return false;
            });
            addComponent(selector);
        }

        selectorPosicion = new VentanaMensula.SelectorIconos<>(0, FILA_SELECTORES, OpcionPosicion.class, VentanaMensula.ICONO_POSICION_TUNEL);
        selectorPosicion.value.set((double) posicion.ordinal());
        selectorPosicion.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            posicion = OpcionPosicion.values()[(int) e.value()].posicion;
            actualizarVista();
            return false;
        });
        addComponent(selectorPosicion);

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
            boolean ok = ClienteMensula.guardarPredeterminadoTunel(ajustes());
            predeterminar.tooltip.set(new DLTooltip(List.of(Component.translatable(ok ? "gui.spanishcantilevers.mensula.predeterminado_guardado"
                    : "gui.spanishcantilevers.mensula.predeterminado_error")), 200));
            return false;
        });
        addComponent(predeterminar);
        recolocar();
        actualizarVista();
    }

    private AjustesTunel ajustes() {
        return new AjustesTunel(version, tamano, posicion, altura, alturaPared, tamanoGeneral);
    }

    private void centrar() {
        setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);
    }

    /**
     * Tres huecos fijos en la fila (version, tamano, pinza): al cambiar de version no se mueve nada, solo
     * cambia lo que hay en cada hueco (en el de pared el de la pinza queda vacio) y que altura se ve.
     */
    private void recolocar() {
        boolean techo = version == VersionTunel.TECHO;
        selectorTamanoTecho.visible.set(techo);
        selectorTamanoPared.visible.set(!techo);
        selectorPosicion.visible.set(techo);
        deslizadorTecho.visible.set(techo);
        deslizadorPared.visible.set(!techo);
        double x = ANCHO / 2.0 - (2 * PASO_SELECTORES + ANCHO_SELECTOR) / 2.0;
        selectorVersion.setPosition(x, FILA_SELECTORES);
        selectorTamanoTecho.setPosition(x + PASO_SELECTORES, FILA_SELECTORES);
        selectorTamanoPared.setPosition(x + PASO_SELECTORES, FILA_SELECTORES);
        selectorPosicion.setPosition(x + 2 * PASO_SELECTORES, FILA_SELECTORES);
    }

    private void actualizarVista() {
        BlockState state = ajustes().aplicar(ModBlocks.SOPORTE_TUNEL.get().defaultBlockState());
        vista = SoporteTunelBakedModel.hornear(state, 0f);
        // lo que ocupa la pieza (bloques), para centrarla y que quepa entera
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        int paso = DefaultVertexFormat.BLOCK.getIntegerSize();
        for (BakedQuad quad : vista) {
            int[] v = quad.getVertices();
            for (int i = 0; i + 2 < v.length; i += paso) {
                for (int k = 0; k < 3; k++) {
                    float c = Float.intBitsToFloat(v[i + k]);
                    min[k] = Math.min(min[k], c);
                    max[k] = Math.max(max[k], c);
                }
            }
        }
        // el bloque de referencia tambien tiene que verse entero
        float[] ref = referencia();
        for (int k = 0; k < 3; k++) {
            min[k] = Math.min(min[k], ref[k]);
            max[k] = Math.max(max[k], ref[k] + 1f);
        }
        centroX = (min[0] + max[0]) / 2f;
        centroY = (min[1] + max[1]) / 2f;
        centroZ = (min[2] + max[2]) / 2f;
        // al balancearse lo ancho puede quedar de frente: cuenta la diagonal en planta
        float ancho = (float) Math.hypot(max[0] - min[0], max[2] - min[2]);
        escala = Math.min(90f, Math.min((VISTA_ALTO - 16) / (max[1] - min[1]), (VISTA_ANCHO - 16) / ancho));
    }

    /** Esquina (bloques) del bloque de referencia: el techo encima o la pared detras (+Z, donde PNW pone el poste). */
    private float[] referencia() {
        return version == VersionTunel.PARED ? new float[]{0f, 0f, 1f} : new float[]{0f, 1f, 0f};
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
            pose.scale(escala, escala, -escala);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            // el de techo de frente, visto como lo ve quien lo pone (la pinza izquierda a la izquierda); el de
            // pared de lado, con la pared a la derecha; balanceandose un poco para que se vea en 3D
            float giro = version == VersionTunel.PARED ? 270f : 0f;
            pose.mulPose(Axis.YP.rotationDegrees(giro + 30f * (float) Math.sin(System.nanoTime() / 1.5e9)));
            pose.translate(-centroX, -centroY, -centroZ);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
            for (BakedQuad quad : vista) {
                vc.putBulkData(pose.last(), quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            // el techo (encima) o la pared (detras) de referencia: se ve hasta donde llega el bloque
            float[] ref = referencia();
            pose.pushPose();
            pose.translate(ref[0], ref[1], ref[2]);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.SEA_LANTERN.defaultBlockState(), pose, buffer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            pose.popPose();
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

    /** Techo o pared (mismo orden que {@link VersionTunel}); sus iconos los dibuja SelectorIconos. */
    public enum OpcionVersion implements ITranslatableEnum, IIconRepresentable {
        TECHO(VersionTunel.TECHO),
        PARED(VersionTunel.PARED);

        private final VersionTunel version;

        OpcionVersion(VersionTunel version) {
            this.version = version;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "version_tunel", version.getSerializedName());
        }
    }

    /** Tamano del de techo: travesanos normales o grandes (mismo orden que {@link TamanoTunel}). */
    public enum OpcionTamanoTecho implements ITranslatableEnum, IIconRepresentable {
        NORMAL(TamanoTunel.NORMAL),
        GRANDE(TamanoTunel.GRANDE);

        private final TamanoTunel tamano;

        OpcionTamanoTecho(TamanoTunel tamano) {
            this.tamano = tamano;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "tamano_tunel_techo", tamano.getSerializedName());
        }
    }

    /** Tamano del de pared: brazo corto o largo (mismo orden que {@link TamanoTunel}). */
    public enum OpcionTamanoPared implements ITranslatableEnum, IIconRepresentable {
        NORMAL(TamanoTunel.NORMAL),
        GRANDE(TamanoTunel.GRANDE);

        private final TamanoTunel tamano;

        OpcionTamanoPared(TamanoTunel tamano) {
            this.tamano = tamano;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "tamano_tunel_pared", tamano.getSerializedName());
        }
    }

    /** Donde agarra el perfil el de techo (mismo orden que {@link PosicionTunel}). */
    public enum OpcionPosicion implements ITranslatableEnum, IIconRepresentable {
        IZQUIERDA(PosicionTunel.IZQUIERDA),
        CENTRO(PosicionTunel.CENTRO),
        DERECHA(PosicionTunel.DERECHA);

        private final PosicionTunel posicion;

        OpcionPosicion(PosicionTunel posicion) {
            this.posicion = posicion;
        }

        @Override
        public ModGuiIcons getIcon() {
            return ModGuiIcons.EMPTY;
        }

        @Override
        public Data getTranslationData() {
            return new Data(SpanishCantilevers.MOD_ID, "posicion_tunel", posicion.getSerializedName());
        }
    }
}

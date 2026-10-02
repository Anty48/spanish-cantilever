package iberiancantilever.client;

import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLSlider;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateEnumSlider;
import de.mrjulsen.paw.client.gui.widgets.CreateSlider;
import iberiancantilever.ModBlocks;
import iberiancantilever.block.PosicionTunel;
import iberiancantilever.block.SoporteTunelBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ventana del soporte de tunel: solo donde agarra el perfil (interior / centro / exterior, con los
 * iconos del brazo de atirantado de PNW) y cuanto cuelga. Misma textura y vista previa que la de la
 * mensula ({@link VentanaMensula}).
 */
public class VentanaSoporteTunel extends DLWindow {
    private static final DLTexture TEXTURA = new DLTexture(new ResourceLocation("pantographsandwires", "textures/gui/cantilever_settings.png"), 256, 256);
    private static final int ANCHO = 251;
    private static final int ALTO = 231;
    private static final int FILA_DESLIZADORES = 155;
    private static final int FILA_SELECTORES = 177;
    private static final int VISTA_X = 27;
    private static final int VISTA_Y = 27;
    private static final int VISTA_ANCHO = 197;
    private static final int VISTA_ALTO = 124;
    /** Pasos de altura por bloque (el deslizador va en bloques, como los de PNW). */
    private static final float PASOS_POR_BLOQUE = 16f / SoporteTunelBlock.PX_POR_ALTURA;

    private final Component titulo = Component.translatable("gui.iberiancantilever.soporte_tunel.titulo");

    private PosicionTunel posicion;
    private int altura;
    private List<BakedQuad> vista;
    private float centroY;
    private float escala;

    public VentanaSoporteTunel(DLWindowManager manager, BlockState inicial, BiConsumer<PosicionTunel, Integer> alCerrar) {
        super(manager);
        this.posicion = inicial.getValue(SoporteTunelBlock.POSICION);
        this.altura = inicial.getValue(SoporteTunelBlock.ALTURA);
        setSize(ANCHO, ALTO);
        centrar();
        addEventListener(DLGuiStandardEvents.ScreenLayoutUpdatedEvent.class, (s, e) -> {
            centrar();
            return false;
        });
        addEventListener(DLGuiStandardEvents.CloseEvent.class, (s, e) -> {
            alCerrar.accept(posicion, altura);
            return false;
        });
        addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
            if (e.keyCode() == 256) {
                getWindowManager().closeWindow(this);
            }
            return false;
        });

        CreateSlider deslizador = new CreateSlider(ANCHO / 2 - 45 / 2, FILA_DESLIZADORES, 45, 14,
                Component.translatable("gui.iberiancantilever.soporte_tunel.altura"));
        deslizador.min.set(0.0);
        deslizador.max.set(4 / (double) PASOS_POR_BLOQUE);
        deslizador.step.set(1 / (double) PASOS_POR_BLOQUE);
        deslizador.value.set(altura / (double) PASOS_POR_BLOQUE);
        deslizador.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            altura = Math.round((float) e.value() * PASOS_POR_BLOQUE);
            actualizarVista();
            return false;
        });
        addComponent(deslizador);

        // mismos iconos y nombres que el zigzag de la mensula; el orden coincide con PosicionTunel
        CreateEnumSlider<VentanaMensula.OpcionZigzag> zigzag = new CreateEnumSlider<>(ANCHO / 2 - 25, FILA_SELECTORES, 50, 20,
                VentanaMensula.OpcionZigzag.class);
        zigzag.value.set((double) posicion.ordinal());
        zigzag.addEventListener(DLSlider.ValueChangedEvent.class, (s, e) -> {
            posicion = PosicionTunel.values()[(int) e.value()];
            actualizarVista();
            return false;
        });
        addComponent(zigzag);

        CreateButton hecho = new CreateButton(ANCHO - 7 - 18, ALTO - 6 - 18, AllIcons.I_CONFIRM);
        hecho.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(hecho);
        actualizarVista();
    }

    private void centrar() {
        setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);
    }

    private void actualizarVista() {
        BlockState state = ModBlocks.SOPORTE_TUNEL.get().defaultBlockState()
                .setValue(SoporteTunelBlock.POSICION, posicion)
                .setValue(SoporteTunelBlock.ALTURA, altura);
        vista = SoporteTunelBakedModel.hornear(state);
        // de la punta de la varilla (18 px) a lo mas bajo de la pinza, en bloques
        float minY = (2f - SoporteTunelBlock.bajada(state)) / 16f;
        float maxY = 18f / 16f;
        centroY = (minY + maxY) / 2f;
        escala = Math.min(90f, (VISTA_ALTO - 16) / (maxY - minY));
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
            // de frente (los travesanos cruzados), balanceandose un poco para que se vea en 3D
            pose.mulPose(Axis.YP.rotationDegrees(180f + 30f * (float) Math.sin(System.nanoTime() / 1.5e9)));
            pose.translate(-0.5f, -centroY, -0.5f);
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
}

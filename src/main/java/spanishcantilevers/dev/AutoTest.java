package spanishcantilevers.dev;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.ModPostes;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.MensulaBlockEntity;
import spanishcantilevers.block.ModoZigzag;
import spanishcantilevers.block.TipoAislador;
import spanishcantilevers.geometry.Ajustes;

/**
 * Prueba automatica (gradlew runClient -Pautotest, o con el archivo run/autotest.flag):
 * crea un mundo plano, monta postes de PNW con mensulas en todas las variantes, hace capturas en
 * run/screenshots/autotest_*.png y cierra el juego.
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, value = Dist.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Si run/autotest.flag trae texto: prefijos de las vistas que se quieren, separados por comas (las
     * demas no se fotografian). Va antes que ENABLED, que es quien lo lee.
     */
    private static String filtro;
    /** Se activa con -Pautotest en gradle o dejando un archivo run/autotest.flag (se borra al usarlo). */
    private static final boolean ENABLED = Boolean.getBoolean("spanishcantilevers.autotest") || consumirFlag();
    private static final String WORLD = "mensula_test";
    /** Superficie del mundo plano por defecto (la hierba esta en y = -61). */
    private static final int SUELO = -60;
    /** Tamano de la ventana (y de las capturas) durante la prueba. */
    private static final int ANCHO_CAPTURA = 1600;
    private static final int ALTO_CAPTURA = 900;
    /** Altura del bloque de la mensula sobre el suelo: hilo de contacto a ~5,4 m del carril. */
    private static final int ALTURA_MENSULA = 6;
    private static final int ALTO_POSTE = 8;
    private static final String OBJETO = "objeto";
    private static final String VENTANA_TUNEL = "ventana_tunel";
    private static final String OBJETO_TUNEL = "objeto_tunel";
    private static final String VENTANA_MENSULA = "ventana_mensula";
    /** El aviso rojo mas largo, con la barra rapida, para ver que se parte en lineas. */
    private static final String AVISO = "aviso";
    /** Linea con postes espanoles (para las capturas de la documentacion): empieza en z = LINEA_ESPANOLA_Z. */
    private static final int LINEA_ESPANOLA_Z = 120;
    /** X de los postes de la linea con cables (lejos de las otras filas). */
    private static final int LINEA_X = -40;
    /** X de la fila de postes espanoles. */
    private static final int POSTES_X = -80;
    /** X del eje de la via del tunel. */
    private static final int TUNEL_X = -120;
    /** X del primer poste de la escena de las 16 direcciones. */
    private static final int DIAGONAL_X = -200;
    /** X del eje de la via del tunel con las otras variantes del soporte. */
    private static final int TUNEL2_X = -160;
    /** X de los postes de la escena de los tirantes diagonales (lejos de la fila B, que llega a x = 160). */
    private static final int TIRANTE_X = 200;
    /** Origen de las escenas de escaparate ({@link Escaparate}) para las capturas de la documentacion. */
    private static final int ESCAPARATE_X = 600;
    /** X del pantografo de PNW (el espanol, 3 bloques al este). */
    private static final int PANTOGRAFOS_X = 20;

    /** Una camara; {@code fov} el campo de vision (las de la documentacion lo cierran para que no se deforme). */
    private record Vista(String nombre, double x, double y, double z, double mirarX, double mirarY, double mirarZ, int fov) {
        Vista(String nombre, double x, double y, double z, double mirarX, double mirarY, double mirarZ) {
            this(nombre, x, y, z, mirarX, mirarY, mirarZ, 70);
        }
    }

    private static final List<Vista> VISTAS = new ArrayList<>();

    private static boolean started;
    private static int ticks;
    private static int step = -1;
    private static int wait;

    private AutoTest() {
    }

    private static boolean consumirFlag() {
        java.io.File flag = new java.io.File("autotest.flag");
        if (!flag.isFile()) {
            return false;
        }
        try {
            filtro = java.nio.file.Files.readString(flag.toPath()).trim();
        } catch (java.io.IOException e) {
            filtro = "";
        }
        return flag.delete();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!ENABLED || started
                || !(event.getScreen() instanceof TitleScreen || event.getScreen() instanceof AccessibilityOnboardingScreen)) {
            return;
        }
        started = true;
        Minecraft mc = Minecraft.getInstance();
        // todas las capturas del mismo tamano (y no el de la ventana maximizada o no)
        org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());
        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), ANCHO_CAPTURA, ALTO_CAPTURA);
        // la primera vez sale la pantalla de accesibilidad en vez del menu principal
        mc.options.onboardAccessibility = false;
        // si la ventana pierde el foco (alguien usando el PC) no tiene que salir el menu de pausa en las capturas
        mc.options.pauseOnLostFocus = false;
        mc.options.save();
        try (var access = mc.getLevelSource().createAccess(WORLD)) {
            access.deleteLevel();
        } catch (Exception e) {
            LOGGER.info("[autotest] no habia mundo previo que borrar ({})", e.toString());
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        ticks++;
        if (step == -1) {
            if (ticks > 40) {
                prepararVistas();
                server.execute(() -> montarEscena(server));
                mc.options.hideGui = true;
                step = 0;
                wait = 0;
            }
            return;
        }
        if (step < VISTAS.size()) {
            Vista v = VISTAS.get(step);
            if (wait == 0) {
                server.execute(() -> mover(server, v));
            }
            wait++;
            if (VENTANA_TUNEL.equals(v.nombre()) && wait == 30) {
                BlockPos soporte = new BlockPos(TUNEL_X, SUELO + ALTURA_MENSULA - 1, -7);
                LOGGER.info("[autotest] clic en {} ({}), mano: {}", soporte, mc.level.getBlockState(soporte), mc.player.getMainHandItem());
                InteractionResult r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(soporte), Direction.SOUTH, soporte, false));
                LOGGER.info("[autotest] resultado {}, pantalla {}", r, mc.screen);
            }
            if (AVISO.equals(v.nombre()) && wait == 20) {
                spanishcantilevers.client.AvisosCliente.mostrar("aviso.spanishcantilevers.cable_entre_tuneles");
            }
            if (VENTANA_MENSULA.equals(v.nombre()) && wait == 30) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            if (VENTANA_MENSULA.equals(v.nombre()) && wait == 72) {
                de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow.closeWindow();
                mc.setScreen(null);
            }
            if (OBJETO_TUNEL.equals(v.nombre()) && wait == 30) {
                InteractionResult r = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                LOGGER.info("[autotest] objeto {}: {}, pantalla {}", mc.player.getMainHandItem(), r, mc.screen);
            }
            if (OBJETO_TUNEL.equals(v.nombre()) && wait == 72) {
                de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow.closeWindow();
                mc.setScreen(null);
            }
            if (OBJETO_TUNEL.equals(v.nombre()) && wait == 73) {
                // escogido "pared": se pone en la cara este del muro del soporte largo del tunel 2, mas abajo
                LOGGER.info("[autotest] objeto tras cerrar: {}", mc.player.getMainHandItem().getTag());
                BlockPos muro = new BlockPos(TUNEL2_X - 3, SUELO + 3, -13);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(muro).add(0.5, 0, 0), Direction.EAST, muro, false));
            }
            if (OBJETO_TUNEL.equals(v.nombre()) && wait == 74) {
                BlockPos puesto = new BlockPos(TUNEL2_X - 2, SUELO + 3, -13);
                LOGGER.info("[autotest] soporte puesto: {}", mc.level.getBlockState(puesto));
            }
            if ((VENTANA_TUNEL.equals(v.nombre()) || OBJETO_TUNEL.equals(v.nombre())) && mc.screen != null && (wait == 40 || wait == 50)) {
                // como un jugador: clic en el extremo derecho de un selector (con el soporte puesto, el de
                // tamano; con el objeto, el de version: pasa a pared) y del de altura (el de la izquierda)
                double cx = mc.getWindow().getGuiScaledWidth() / 2.0;
                double cy = mc.getWindow().getGuiScaledHeight() / 2.0;
                double x = wait == 40 ? (OBJETO_TUNEL.equals(v.nombre()) ? cx - 36 : cx + 22) : cx - 8;
                double y = wait == 40 ? cy - 115 + 187 : cy - 115 + 162;
                boolean ok = mc.screen.mouseClicked(x, y, 0);
                mc.screen.mouseReleased(x, y, 0);
                LOGGER.info("[autotest] clic ventana ({}, {}) -> {}", x, y, ok);
            }
            if ((VENTANA_TUNEL.equals(v.nombre()) || OBJETO_TUNEL.equals(v.nombre())) && mc.screen != null && wait == 55) {
                // y el tamano general (el deslizador de la derecha) hasta arriba
                double x = mc.getWindow().getGuiScaledWidth() / 2.0 + 47;
                double y = mc.getWindow().getGuiScaledHeight() / 2.0 - 115 + 162;
                mc.screen.mouseClicked(x, y, 0);
                mc.screen.mouseReleased(x, y, 0);
            }
            if ((VENTANA_TUNEL.equals(v.nombre()) || OBJETO_TUNEL.equals(v.nombre()) || VENTANA_MENSULA.equals(v.nombre())) && wait == 60) {
                // el raton fuera de la ventana: si se queda encima de un deslizador, su tooltip tapa la vista previa
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                if (mc.screen != null) {
                    mc.screen.mouseMoved(0, 0);
                }
            }
            if (VENTANA_TUNEL.equals(v.nombre()) && wait == 72) {
                de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow.closeWindow();
            }
            if (VENTANA_TUNEL.equals(v.nombre()) && wait == 74) {
                BlockPos soporte = new BlockPos(TUNEL_X, SUELO + ALTURA_MENSULA - 1, -7);
                LOGGER.info("[autotest] tras cerrar: servidor {} / cliente {}", server.overworld().getBlockState(soporte), mc.level.getBlockState(soporte));
                mc.setScreen(null);
            }
            if (wait == (step == 0 ? 160 : 70)) {
                Screenshot.grab(mc.gameDirectory, "autotest_" + v.nombre() + ".png", mc.getMainRenderTarget(),
                        msg -> LOGGER.info("[autotest] {}", msg.getString()));
            }
            if (wait >= (step == 0 ? 165 : 75)) {
                step++;
                wait = 0;
            }
            return;
        }
        if (step == VISTAS.size()) {
            step++;
            LOGGER.info("[autotest] terminado, cerrando");
            mc.stop();
        }
    }

    // ------------------------------------------------------------------ escena

    private static void montarEscena(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);

        // Fila A: los tres modos (tipo 1, con tirante), postes separados 12 bloques en X.
        ModoZigzag[] modos = ModoZigzag.values();
        for (int i = 0; i < modos.length; i++) {
            BlockPos poste = new BlockPos(i * 12, SUELO, 0);
            poner(level, poste, TipoAislador.TIPO1, modos[i], true, 2);
        }
        // Fila B: las 9 variantes (tipo x modo), 60 bloques mas al este; el tipo 2 sin tirante.
        TipoAislador[] tipos = TipoAislador.values();
        for (int i = 0; i < tipos.length; i++) {
            for (int j = 0; j < modos.length; j++) {
                BlockPos poste = new BlockPos(60 + (i * 3 + j) * 12, SUELO, 0);
                poner(level, poste, tipos[i], modos[j], i != 1, 2);
            }
        }
        // Fila C: una linea con cables de PNW tendidos entre las mensulas (zigzag interior / exterior)
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        LineaPrueba.visionNocturna(player);
        int cables = LineaPrueba.tenderCables(player, LineaPrueba.montar(level, new BlockPos(LINEA_X, SUELO, 0), 4));
        // Fila D: los postes ibericos (plano, plano diagonal, cuadrado y H) y las piezas espanolas puestas
        // como las pondria un jugador: soporte de mensula en el cuadrado (pasa a su version junto a poste) con
        // otro encima (vertical), y soporte de linea electrica en el H
        String[] formas = {"iberian_flat_lattice_mast", "iberian_flat_lattice_mast_diagonal", "iberian_lattice_mast", "iberian_h_beam_mast"};
        for (int i = 0; i < formas.length; i++) {
            Block poste = ModPostes.POSTES.get(formas[i]).get();
            for (int dy = 0; dy < 4; dy++) {
                level.setBlock(new BlockPos(POSTES_X + i * 3, SUELO + dy, 0), LineaPrueba.poste(poste, Direction.SOUTH, 0), Block.UPDATE_ALL);
            }
        }
        BlockPos cuadrado = new BlockPos(POSTES_X + 6, SUELO + 2, 0);
        colocar(player, ModPostes.POSTES.get("spanish_cantilever_bracket").get(), cuadrado, Direction.SOUTH);
        colocar(player, ModPostes.POSTES.get("spanish_cantilever_bracket").get(), cuadrado.south(), Direction.UP);
        colocar(player, ModPostes.POSTES.get("spanish_power_line_bracket").get(), new BlockPos(POSTES_X + 9, SUELO + 3, 0), Direction.SOUTH);
        LOGGER.info("[autotest] piezas: {} / {} / {}", level.getBlockState(cuadrado.south()), level.getBlockState(cuadrado.south().above()),
                level.getBlockState(new BlockPos(POSTES_X + 9, SUELO + 3, 1)));
        // Fila E: tunel con catenaria rigida y transicion desde una mensula
        int tramos = LineaPrueba.montarTunel(player, new BlockPos(TUNEL_X, SUELO, 0), 5);
        // Fila E2: las otras variantes del soporte de tunel (grande, de pared en hormigon y en muro)
        int tramos2 = LineaPrueba.montarTunelVariantes(player, new BlockPos(TUNEL2_X, SUELO, 0));
        LOGGER.info("[autotest] tunel con variantes: {} tramos rigidos", tramos2);
        // el pantografo espanol al lado del de PNW
        Block pantografoPnw = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "pantograph"));
        if (pantografoPnw != null) {
            level.setBlock(new BlockPos(PANTOGRAFOS_X, SUELO, 30), pantografoPnw.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(new BlockPos(PANTOGRAFOS_X + 3, SUELO, 30), ModBlocks.PANTOGRAFO.get().defaultBlockState(), Block.UPDATE_ALL);
        // linea con postes espanoles, mensulas en zigzag y cable de PNW (vista general)
        int cablesEspanoles = LineaPrueba.tenderCables(player, LineaPrueba.montar(level, new BlockPos(0, SUELO, LINEA_ESPANOLA_Z), 4,
                ModPostes.POSTES.get("iberian_flat_lattice_mast").get()));
        LOGGER.info("[autotest] linea espanola: {} vanos", cablesEspanoles);
        // Fila F: las 16 direcciones de la mensula, con la de PNW debajo para comparar
        int diagonales = LineaPrueba.montarDiagonales(player, new BlockPos(DIAGONAL_X, SUELO, 40));
        // Fila G: tirantes diagonales (cable de soporte de PNW de la horizontal al poste)
        int tirantes = LineaPrueba.montarTirantes(player, new BlockPos(TIRANTE_X, SUELO, 0));
        LOGGER.info("[autotest] tirantes diagonales: {}", tirantes);
        Escaparate.montar(player, new BlockPos(ESCAPARATE_X, SUELO, 0));
        LOGGER.info("[autotest] escena montada ({} vanos con cable, {} tramos rigidos, {} vanos en diagonales)", cables, tramos, diagonales);
    }

    /** Camaras: miran al norte, asi se ve la mensula de perfil (poste a la izquierda, via a la derecha). */
    private static void prepararVistas() {
        VISTAS.clear();
        ModoZigzag[] modos = ModoZigzag.values();
        TipoAislador[] tipos = TipoAislador.values();
        double y = SUELO + ALTURA_MENSULA + 0.4;
        VISTAS.add(new Vista("modos", 15, y, 17, 15, y, 0));
        for (int i = 0; i < modos.length; i++) {
            double x = i * 12 + 3.2;
            VISTAS.add(new Vista("modo_" + modos[i].getSerializedName(), x, y, 4.6, x, y, 0));
        }
        for (int i = 0; i < tipos.length; i++) {
            for (int j = 0; j < modos.length; j++) {
                double x = 60 + (i * 3 + j) * 12 + 2.6;
                VISTAS.add(new Vista("variante_" + tipos[i].getSerializedName() + "_" + modos[j].getSerializedName(),
                        x, y + 0.2, 3.6, x, y + 0.2, 0));
            }
        }
        VISTAS.add(new Vista("perspectiva", 34, y + 3, 9, 12, y - 1, 0));
        // la linea con cables: a lo largo de la via y de lado
        VISTAS.add(new Vista("linea", LINEA_X + 3.5, y - 1, 8, LINEA_X + 3.5, y - 1, -36));
        VISTAS.add(new Vista("linea_lado", LINEA_X + 16, y + 1, -18, LINEA_X + 2, y - 1, -18));
        VISTAS.add(new Vista("tunel", TUNEL_X + 1.2, SUELO + 4.6, -2.5, TUNEL_X + 0.5, SUELO + 5.4, -16));
        VISTAS.add(new Vista("tunel_cerca", TUNEL_X + 2.3, SUELO + 4.3, -4.5, TUNEL_X + 0.5, SUELO + 5.4, -6.5));
        VISTAS.add(new Vista("tunel_curva", TUNEL_X + 1.5, SUELO + 4.6, -10, TUNEL_X + 5, SUELO + 5.4, -22));
        VISTAS.add(new Vista("tunel_altura", TUNEL_X + 8.8, SUELO + 4.0, -22, TUNEL_X + 6.5, SUELO + 5.0, -24.5));
        // la transicion de catenaria normal a rigida, de lado y de cerca en el primer soporte
        VISTAS.add(new Vista("transicion", TUNEL_X + 9, SUELO + 5.5, 6, TUNEL_X - 1, SUELO + 5.0, 4));
        VISTAS.add(new Vista("transicion_cerca", TUNEL_X + 2.5, SUELO + 4.6, 1.8, TUNEL_X + 0.3, SUELO + 5.3, -0.8));
        // las variantes del soporte de tunel: a lo largo y de cerca los de pared
        VISTAS.add(new Vista("tunel2", TUNEL2_X + 1.2, SUELO + 4.6, 1.5, TUNEL2_X, SUELO + 5.4, -14));
        // los de pared mirando a lo largo de la via, desde un poco antes: el brazo sale de lado
        VISTAS.add(new Vista("tunel2_pared_corto", TUNEL2_X + 0.6, SUELO + 5.0, -3.6, TUNEL2_X - 0.6, SUELO + 5.3, -6.5));
        VISTAS.add(new Vista("tunel2_pared_largo", TUNEL2_X + 0.6, SUELO + 5.0, -9.6, TUNEL2_X - 1.1, SUELO + 5.3, -12.5));
        // tamano general: de lado bajo el techo, y cada soporte de frente (mirando a lo largo de la via)
        VISTAS.add(new Vista("tunel2_escala", TUNEL2_X + 5, SUELO + 3.2, -40, TUNEL2_X - 0.5, SUELO + 5.4, -40));
        VISTAS.add(new Vista("tunel2_escala_150", TUNEL2_X + 1.3, SUELO + 4.1, -28.8, TUNEL2_X + 0.5, SUELO + 5.5, -31.5));
        VISTAS.add(new Vista("tunel2_escala_125_grande", TUNEL2_X + 1.3, SUELO + 4.1, -34.8, TUNEL2_X + 0.5, SUELO + 5.5, -37.5));
        VISTAS.add(new Vista("tunel2_escala_pared_150", TUNEL2_X + 1.4, SUELO + 5.0, -39.8, TUNEL2_X - 0.6, SUELO + 5.3, -43.5));
        VISTAS.add(new Vista("tunel2_escala_100", TUNEL2_X + 1.3, SUELO + 4.1, -46.8, TUNEL2_X + 0.5, SUELO + 5.5, -49.5));
        VISTAS.add(new Vista("tunel2_grande", TUNEL2_X + 1.5, SUELO + 4.8, 1.2, TUNEL2_X, SUELO + 5.6, -1));
        // las 16 direcciones: cada fila (una cara) vista desde justo encima, nuestra mensula encima de la de PNW
        String[] caras = {"norte", "este", "sur", "oeste"};
        for (int f = 0; f < caras.length; f++) {
            double z = 40 + f * 16;
            VISTAS.add(new Vista("diagonal_" + caras[f], DIAGONAL_X + 15.5, SUELO + 24, z + 0.5, DIAGONAL_X + 15.5, SUELO, z + 0.51));
        }
        VISTAS.add(new Vista("linea_espanola", 9, SUELO + 4.5, LINEA_ESPANOLA_Z + 7, 2, SUELO + 5.5, LINEA_ESPANOLA_Z - 22));
        VISTAS.add(new Vista("linea_espanola_cerca", 5.5, SUELO + 6.2, LINEA_ESPANOLA_Z + 3.5, 1, SUELO + 6.4, LINEA_ESPANOLA_Z - 2));
        VISTAS.add(new Vista(VENTANA_MENSULA, 15, y, 17, 15, y, 0));
        VISTAS.add(new Vista("pantografos", PANTOGRAFOS_X + 2, SUELO + 2.2, 35, PANTOGRAFOS_X + 2, SUELO + 0.6, 30.5));
        VISTAS.add(new Vista("postes_espanoles", POSTES_X + 4.5, SUELO + 3, 10, POSTES_X + 4.5, SUELO + 1.5, 1.5));
        VISTAS.add(new Vista("piezas_espanolas", POSTES_X + 8.5, SUELO + 3.2, 4.5, POSTES_X + 7.5, SUELO + 2.5, 0.5));
        VISTAS.add(new Vista(AVISO, POSTES_X + 4.5, SUELO + 3, 10, POSTES_X + 4.5, SUELO + 1.5, 1.5));
        // clic derecho con la mano vacia en el segundo soporte del tunel: tiene que abrir su ventana
        VISTAS.add(new Vista(VENTANA_TUNEL, TUNEL_X + 2.3, SUELO + 4.3, -4.5, TUNEL_X + 0.5, SUELO + 5.4, -6.5));
        // el soporte en la mano: clic derecho al aire, escoger, y ponerlo con lo escogido
        VISTAS.add(new Vista(OBJETO_TUNEL, TUNEL2_X + 0.5, SUELO + 4.5, -10.5, TUNEL2_X - 2, SUELO + 3.5, -13));
        // tirantes diagonales: de lado (el primero y el tercero, bajado), de cerca en la barra y en el poste, y la linea
        VISTAS.add(new Vista("tirante", TIRANTE_X + 2.5, SUELO + 7.5, 7, TIRANTE_X + 2.5, SUELO + 7.5, 0));
        VISTAS.add(new Vista("tirante_bajado", TIRANTE_X + 2.5, SUELO + 7.5, -17, TIRANTE_X + 2.5, SUELO + 7.5, -24));
        VISTAS.add(new Vista("tirante_barra", TIRANTE_X + 4.6, SUELO + 7.6, 2.0, TIRANTE_X + 3.3, SUELO + 6.6, 0.5));
        VISTAS.add(new Vista("tirante_poste", TIRANTE_X + 2.6, SUELO + 9.6, 2.2, TIRANTE_X + 0.5, SUELO + 9.4, 0.5));
        VISTAS.add(new Vista("tirante_linea", TIRANTE_X + 7, SUELO + 8, 8, TIRANTE_X + 1, SUELO + 7, -14));
        // la fila de las anchuras (3,5 a 6,5 y la ensanchada), 20 bloques al sur, de lado y de cerca la ensanchada
        VISTAS.add(new Vista("tirante_anchuras", TIRANTE_X + 22, SUELO + 9, 52, TIRANTE_X + 22, SUELO + 9, 20));
        VISTAS.add(new Vista("tirante_ensanchada", TIRANTE_X + 38, SUELO + 8.5, 33, TIRANTE_X + 38, SUELO + 8.5, 20));
        VISTAS.add(new Vista("tirante_alturas", TIRANTE_X + 89, SUELO + 9, 50, TIRANTE_X + 89, SUELO + 9, 20));
        // de cerca donde el hilo entra en la horizontal (anchura 3,5) y en el poste (a 2 bloques)
        VISTAS.add(new Vista("tirante_metido", TIRANTE_X + 3.3, SUELO + 7.0, 21.7, TIRANTE_X + 4.0, SUELO + 6.5, 20.5));
        VISTAS.add(new Vista("tirante_metido_poste", TIRANTE_X + 2.0, SUELO + 8.6, 21.9, TIRANTE_X + 0.7, SUELO + 8.5, 20.5));
        VISTAS.add(new Vista("tirante_pnw", TIRANTE_X + 2.5, SUELO + 7.5, -5, TIRANTE_X + 2.5, SUELO + 7.5, -12));
        vistasDocumentacion();
        // la ultima con la interfaz visible y la mensula en la mano (modelo del objeto)
        VISTAS.add(new Vista(OBJETO, 15, y, 17, 15, y, 0));
        if (filtro != null && !filtro.isEmpty()) {
            List<String> prefijos = java.util.Arrays.stream(filtro.split(",")).map(String::trim).filter(p -> !p.isEmpty()).toList();
            VISTAS.removeIf(v -> prefijos.stream().noneMatch(v.nombre()::startsWith));
        }
    }

    /** Poste de celosia de PNW, mensula en su cara este y una via de Create debajo del sustentador. */
    private static void poner(ServerLevel level, BlockPos base, TipoAislador tipo, ModoZigzag modo, boolean tirante, int alcance) {
        Block poste = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
        for (int dy = 0; dy < ALTO_POSTE; dy++) {
            level.setBlock(base.above(dy), LineaPrueba.poste(poste, Direction.EAST, 1), Block.UPDATE_ALL);
        }
        BlockState mensula = ModBlocks.MENSULA.get().defaultBlockState()
                .setValue(AbstractRotatableBlock.FACING, Direction.EAST)
                .setValue(AbstractRotatableBlock.ROTATION, 1);
        BlockPos pos = base.above(ALTURA_MENSULA).east();
        level.setBlock(pos, mensula, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
            be.setAjustes(Ajustes.DEFECTO.conTipo(tipo).conModo(modo).conTirante(tirante).conAnchura(alcance + 0.5f)
                    .conAltura(Ajustes.alturaAutomatica(alcance + 0.5f, modo)));
        }

        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        if (via != null) {
            for (int dz = -8; dz <= 8; dz++) {
                level.setBlock(new BlockPos(base.getX() + 1 + alcance, SUELO, base.getZ() + dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Camaras de la documentacion (prefijo doc_), sobre el escaparate y las filas de los tirantes. */
    private static void vistasDocumentacion() {
        double s = SUELO;
        double ex = ESCAPARATE_X;
        // portada: la linea entera desde un lado de la via, y de cerca la primera mensula con su tirante
        VISTAS.add(new Vista("doc_portada", ex + 9.5, s + 3.4, 15, ex + 2.5, s + 6.2, -22, 50));
        VISTAS.add(new Vista("doc_portada_cerca", ex + 6.8, s + 7.6, 3.8, ex + 3.0, s + 6.8, 0.5, 50));
        // todas las anchuras de lado
        double az = Escaparate.ANCHURAS_Z;
        VISTAS.add(new Vista("doc_anchuras", ex + 22, s + 4.8, az + 36, ex + 22, s + 4.8, az, 40));
        // los postes, de frente
        double pz = Escaparate.POSTES_Z;
        VISTAS.add(new Vista("doc_postes", ex + 13.5, s + 3.2, pz + 21, ex + 13.5, s + 2.4, pz, 40));
        // los pantografos en su anden
        double px = ex + Escaparate.PANTOGRAFOS_X;
        double ppz = Escaparate.PANTOGRAFOS_Z;
        VISTAS.add(new Vista("doc_pantografos", px + 2, s + 3.4, ppz + 4.6, px + 2, s + 1.5, ppz + 0.3, 50));
        // vitrinas de soportes: desde la via, mirando a lo largo de los perfiles
        // a la altura de las pinzas, justo bajo la losa, mirando a lo largo de los perfiles
        double vz = Escaparate.VITRINA_Z;
        String[] vitrinas = {"doc_vitrina_techo", "doc_vitrina_grande", "doc_vitrina_escala"};
        for (int i = 0; i < vitrinas.length; i++) {
            double cx = ex + Escaparate.VITRINA_X + Escaparate.VITRINAS[i] + 0.5;
            VISTAS.add(new Vista(vitrinas[i], cx + 1.5, s + 4.8, vz + 5.0, cx, s + 5.6, vz - 1.5, 60));
        }
        // y cada soporte de techo de cerca, de frente a la altura de la pinza
        String[] grupos = {"normal", "grande", "escala"};
        for (int g = 0; g < grupos.length; g++) {
            for (int i = 0; i < 3; i++) {
                double via = ex + Escaparate.VITRINA_X + Escaparate.VITRINAS[g] - 4 + i * 4 + 0.5;
                VISTAS.add(new Vista("doc_soporte_" + grupos[g] + "_" + i, via + 1.5, s + 5.0, vz + 2.2, via, s + 5.6, vz - 0.4, 50));
            }
        }
        String[] paredes = {"doc_vitrina_pared_corto", "doc_vitrina_pared_largo"};
        for (int i = 0; i < paredes.length; i++) {
            double via = ex + Escaparate.VITRINA_X + Escaparate.VIAS_PARED[i] + 0.5;
            VISTAS.add(new Vista(paredes[i], via + 0.3, s + 5.25, vz + 2.0, via - 1.1, s + 5.45, vz + 0.1, 55));
        }
        // el tunel: la boca con la transicion, dentro a lo largo y un soporte de cerca
        double tx = ex + Escaparate.TUNEL_X + 0.5;
        // la curva del perfil, de lado y desde abajo
        double cz = Escaparate.CURVA_Z;
        VISTAS.add(new Vista("doc_curva", ex - 1.0, s + 4.4, cz + 3.5, ex + 3.0, s + 5.6, cz - 20, 55));
        VISTAS.add(new Vista("doc_tunel_boca", tx + 7, s + 3.8, 22, tx - 0.5, s + 5.0, -1, 55));
        VISTAS.add(new Vista("doc_transicion", tx + 1.3, s + 4.6, -5.5, tx - 0.6, s + 5.6, 12, 55));
        VISTAS.add(new Vista("doc_tunel_dentro", tx + 1.2, s + 4.4, -2.5, tx + 0.2, s + 5.3, -30, 55));
        VISTAS.add(new Vista("doc_tunel_soporte", tx + 1.3, s + 4.8, -5.0, tx, s + 5.9, -7.0, 50));
        // tirantes diagonales: las anchuras, las alturas y de cerca donde entra en la barra y en el poste
        double ty = s + 7.6;
        double tz = Escaparate.TIRANTES_Z;
        double taz = Escaparate.TIRANTES_ALTURAS_Z;
        VISTAS.add(new Vista("doc_tirante_anchuras", ex + 17.5, ty, tz + 30, ex + 17.5, ty, tz, 40));
        VISTAS.add(new Vista("doc_tirante_alturas", ex + 17.5, ty, taz + 30, ex + 17.5, ty, taz, 40));
        VISTAS.add(new Vista("doc_tirante_metido", ex + 8 + 3.9, s + 7.1, tz + 2.0, ex + 8 + 4.9, s + 6.5, tz + 0.5, 50));
        VISTAS.add(new Vista("doc_tirante_poste", ex + 8 + 2.3, s + 8.5, tz + 2.4, ex + 8 + 0.6, s + 9.5, tz + 0.5, 50));
    }

    /** Pone {@code bloque} como un jugador: con su objeto, clic en la cara {@code cara} de {@code contra}. */
    private static void colocar(ServerPlayer player, Block bloque, BlockPos contra, Direction cara) {
        ItemStack antes = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bloque));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(contra).relative(cara, 0.5), cara, contra, false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        player.setItemInHand(InteractionHand.MAIN_HAND, antes);
    }

    private static void mover(MinecraftServer server, Vista v) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.fov().set(v.fov()));
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        // solo estas vistas llevan la interfaz (la barra rapida)
        boolean conInterfaz = OBJETO.equals(v.nombre()) || AVISO.equals(v.nombre());
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.hideGui = !conInterfaz);
        if (AVISO.equals(v.nombre())) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        } else if (OBJETO.equals(v.nombre())) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, new ItemStack(ModBlocks.MENSULA.get()));
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.hideGui = false);
        } else if (VENTANA_MENSULA.equals(v.nombre())) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, new ItemStack(ModBlocks.MENSULA_ITEM.get()));
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
        } else if (VENTANA_TUNEL.equals(v.nombre()) || OBJETO_TUNEL.equals(v.nombre())) {
            // en espectador el clic derecho no llega a los bloques
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, OBJETO_TUNEL.equals(v.nombre()) ? new ItemStack(ModBlocks.SOPORTE_TUNEL_ITEM.get()) : ItemStack.EMPTY);
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
        } else {
            player.setGameMode(GameType.SPECTATOR);
        }
        double dx = v.mirarX() - v.x(), dy = v.mirarY() - v.y(), dz = v.mirarZ() - v.z();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // los ojos estan 1,62 por encima de los pies
        player.teleportTo(server.overworld(), v.x(), v.y() - 1.62, v.z(), yaw, pitch);
    }
}

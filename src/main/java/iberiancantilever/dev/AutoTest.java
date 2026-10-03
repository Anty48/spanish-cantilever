package iberiancantilever.dev;

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
import iberiancantilever.ModBlocks;
import iberiancantilever.ModPostes;
import iberiancantilever.IberianCantilever;
import iberiancantilever.block.MensulaBlockEntity;
import iberiancantilever.block.ModoZigzag;
import iberiancantilever.block.TipoAislador;
import iberiancantilever.geometry.Ajustes;

/**
 * Prueba automatica (gradlew runClient -Pautotest, o con el archivo run/autotest.flag):
 * crea un mundo plano, monta postes de PNW con mensulas en todas las variantes, hace capturas en
 * run/screenshots/autotest_*.png y cierra el juego.
 */
@Mod.EventBusSubscriber(modid = IberianCantilever.MOD_ID, value = Dist.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Se activa con -Pautotest en gradle o dejando un archivo run/autotest.flag (se borra al usarlo). */
    private static final boolean ENABLED = Boolean.getBoolean("iberiancantilever.autotest") || consumirFlag();
    private static final String WORLD = "mensula_test";
    /** Superficie del mundo plano por defecto (la hierba esta en y = -61). */
    private static final int SUELO = -60;
    /** Altura del bloque de la mensula sobre el suelo: hilo de contacto a ~5,4 m del carril. */
    private static final int ALTURA_MENSULA = 6;
    private static final int ALTO_POSTE = 8;
    private static final String OBJETO = "objeto";
    private static final String VENTANA_TUNEL = "ventana_tunel";
    private static final String OBJETO_TUNEL = "objeto_tunel";
    private static final String VENTANA_MENSULA = "ventana_mensula";
    /** Linea con postes ibericos (para las capturas de la documentacion): empieza en z = LINEA_IBERICA_Z. */
    private static final int LINEA_IBERICA_Z = 120;
    /** X de los postes de la linea con cables (lejos de las otras filas). */
    private static final int LINEA_X = -40;
    /** X de la fila de postes ibericos. */
    private static final int POSTES_X = -80;
    /** X del eje de la via del tunel. */
    private static final int TUNEL_X = -120;
    /** X del primer poste de la escena de las 16 direcciones. */
    private static final int DIAGONAL_X = -200;
    /** X del eje de la via del tunel con las otras variantes del soporte. */
    private static final int TUNEL2_X = -160;
    /** X del pantografo de PNW (el iberico, 3 bloques al este). */
    private static final int PANTOGRAFOS_X = 20;

    private record Vista(String nombre, double x, double y, double z, double mirarX, double mirarY, double mirarZ) {
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
        return flag.isFile() && flag.delete();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!ENABLED || started
                || !(event.getScreen() instanceof TitleScreen || event.getScreen() instanceof AccessibilityOnboardingScreen)) {
            return;
        }
        started = true;
        Minecraft mc = Minecraft.getInstance();
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
                // tamano; con el objeto, el de version: pasa a pared) y del de altura
                double cx = mc.getWindow().getGuiScaledWidth() / 2.0;
                double cy = mc.getWindow().getGuiScaledHeight() / 2.0;
                double x = wait == 40 ? (OBJETO_TUNEL.equals(v.nombre()) ? cx - 36 : cx + 22) : cx + 20;
                double y = wait == 40 ? cy - 115 + 187 : cy - 115 + 162;
                boolean ok = mc.screen.mouseClicked(x, y, 0);
                mc.screen.mouseReleased(x, y, 0);
                LOGGER.info("[autotest] clic ventana ({}, {}) -> {}", x, y, ok);
            }
            if ((VENTANA_TUNEL.equals(v.nombre()) || OBJETO_TUNEL.equals(v.nombre()) || VENTANA_MENSULA.equals(v.nombre())) && wait == 60) {
                // el raton fuera de la ventana: si se queda encima de un deslizador, su tooltip tapa la vista previa
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
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
        int cables = LineaPrueba.tenderCables(player, LineaPrueba.montar(level, new BlockPos(LINEA_X, SUELO, 0), 4));
        // Fila D: los postes ibericos en sus cuatro estados de oxidacion, normal y diagonal
        String[] estados = {"", "_exposed", "_weathered", "_oxidized"};
        for (int i = 0; i < estados.length; i++) {
            for (int j = 0; j < 2; j++) {
                String id = (j == 0 ? "iberian_flat_lattice_mast" : "iberian_flat_lattice_mast_diagonal") + estados[i];
                Block poste = ModPostes.POSTES.get(id).get();
                for (int dy = 0; dy < 4; dy++) {
                    level.setBlock(new BlockPos(POSTES_X + i * 3, SUELO + dy, j * 3), poste.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        // Fila E: tunel con catenaria rigida y transicion desde una mensula
        int tramos = LineaPrueba.montarTunel(player, new BlockPos(TUNEL_X, SUELO, 0), 5);
        // Fila E2: las otras variantes del soporte de tunel (grande, de pared en hormigon y en muro)
        int tramos2 = LineaPrueba.montarTunelVariantes(player, new BlockPos(TUNEL2_X, SUELO, 0));
        LOGGER.info("[autotest] tunel con variantes: {} tramos rigidos", tramos2);
        // el pantografo iberico al lado del de PNW
        Block pantografoPnw = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "pantograph"));
        if (pantografoPnw != null) {
            level.setBlock(new BlockPos(PANTOGRAFOS_X, SUELO, 30), pantografoPnw.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(new BlockPos(PANTOGRAFOS_X + 3, SUELO, 30), ModBlocks.PANTOGRAFO.get().defaultBlockState(), Block.UPDATE_ALL);
        // linea con postes ibericos, mensulas en zigzag y cable de PNW (vista general)
        int cablesIbericos = LineaPrueba.tenderCables(player, LineaPrueba.montar(level, new BlockPos(0, SUELO, LINEA_IBERICA_Z), 4,
                ModPostes.POSTES.get("iberian_flat_lattice_mast").get()));
        LOGGER.info("[autotest] linea iberica: {} vanos", cablesIbericos);
        // Fila F: las 16 direcciones de la mensula, con la de PNW debajo para comparar
        int diagonales = LineaPrueba.montarDiagonales(player, new BlockPos(DIAGONAL_X, SUELO, 40));
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
        VISTAS.add(new Vista("tunel2_grande", TUNEL2_X + 1.5, SUELO + 4.8, 1.2, TUNEL2_X, SUELO + 5.6, -1));
        // las 16 direcciones: cada fila (una cara) vista desde justo encima, nuestra mensula encima de la de PNW
        String[] caras = {"norte", "este", "sur", "oeste"};
        for (int f = 0; f < caras.length; f++) {
            double z = 40 + f * 16;
            VISTAS.add(new Vista("diagonal_" + caras[f], DIAGONAL_X + 15.5, SUELO + 24, z + 0.5, DIAGONAL_X + 15.5, SUELO, z + 0.51));
        }
        VISTAS.add(new Vista("linea_iberica", 9, SUELO + 4.5, LINEA_IBERICA_Z + 7, 2, SUELO + 5.5, LINEA_IBERICA_Z - 22));
        VISTAS.add(new Vista("linea_iberica_cerca", 5.5, SUELO + 6.2, LINEA_IBERICA_Z + 3.5, 1, SUELO + 6.4, LINEA_IBERICA_Z - 2));
        VISTAS.add(new Vista(VENTANA_MENSULA, 15, y, 17, 15, y, 0));
        VISTAS.add(new Vista("pantografos", PANTOGRAFOS_X + 2, SUELO + 2.2, 35, PANTOGRAFOS_X + 2, SUELO + 0.6, 30.5));
        VISTAS.add(new Vista("postes_ibericos", POSTES_X + 4.5, SUELO + 3, 10, POSTES_X + 4.5, SUELO + 1.5, 1.5));
        // clic derecho con la mano vacia en el segundo soporte del tunel: tiene que abrir su ventana
        VISTAS.add(new Vista(VENTANA_TUNEL, TUNEL_X + 2.3, SUELO + 4.3, -4.5, TUNEL_X + 0.5, SUELO + 5.4, -6.5));
        // el soporte en la mano: clic derecho al aire, escoger, y ponerlo con lo escogido
        VISTAS.add(new Vista(OBJETO_TUNEL, TUNEL2_X + 0.5, SUELO + 4.5, -10.5, TUNEL2_X - 2, SUELO + 3.5, -13));
        // la ultima con la interfaz visible y la mensula en la mano (modelo del objeto)
        VISTAS.add(new Vista(OBJETO, 15, y, 17, 15, y, 0));
    }

    /** Poste de celosia de PNW, mensula en su cara este y una via de Create debajo del sustentador. */
    private static void poner(ServerLevel level, BlockPos base, TipoAislador tipo, ModoZigzag modo, boolean tirante, int alcance) {
        Block poste = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
        for (int dy = 0; dy < ALTO_POSTE; dy++) {
            level.setBlock(base.above(dy), poste.defaultBlockState(), Block.UPDATE_ALL);
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

    private static void mover(MinecraftServer server, Vista v) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (OBJETO.equals(v.nombre())) {
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

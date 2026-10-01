package spanishcantilever.dev;

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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
import spanishcantilever.ModBlocks;
import spanishcantilever.SpanishCantilever;
import spanishcantilever.block.MensulaBlock;
import spanishcantilever.block.ModoZigzag;
import spanishcantilever.block.TipoAislador;

/**
 * Prueba automatica (gradlew runClient -Pautotest, o con el archivo run/autotest.flag):
 * crea un mundo plano, monta postes de PNW con mensulas en todas las variantes, hace capturas en
 * run/screenshots/autotest_*.png y cierra el juego.
 */
@Mod.EventBusSubscriber(modid = SpanishCantilever.MOD_ID, value = Dist.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Se activa con -Pautotest en gradle o dejando un archivo run/autotest.flag (se borra al usarlo). */
    private static final boolean ENABLED = Boolean.getBoolean("spanishcantilever.autotest") || consumirFlag();
    private static final String WORLD = "mensula_test";
    /** Superficie del mundo plano por defecto (la hierba esta en y = -61). */
    private static final int SUELO = -60;
    /** Altura del bloque de la mensula sobre el suelo: hilo de contacto a ~5,4 m del carril. */
    private static final int ALTURA_MENSULA = 6;
    private static final int ALTO_POSTE = 8;

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
        // Fila B: los tres tipos de aislador (modo medio), 60 bloques mas al este.
        TipoAislador[] tipos = TipoAislador.values();
        for (int i = 0; i < tipos.length; i++) {
            BlockPos poste = new BlockPos(60 + i * 12, SUELO, 0);
            poner(level, poste, tipos[i], ModoZigzag.MEDIO, i != 1, 2);
        }
        LOGGER.info("[autotest] escena montada");
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
        VISTAS.add(new Vista("tipos", 75, y, 17, 75, y, 0));
        for (int i = 0; i < tipos.length; i++) {
            double x = 60 + i * 12 + 3.5;
            VISTAS.add(new Vista("tipo_" + tipos[i].getSerializedName(), x, y + 0.6, 2.2, x, y + 0.6, 0));
        }
        VISTAS.add(new Vista("perspectiva", 34, y + 3, 9, 12, y - 1, 0));
    }

    /** Poste de celosia de PNW, mensula en su cara este y una via de Create debajo del sustentador. */
    private static void poner(ServerLevel level, BlockPos base, TipoAislador tipo, ModoZigzag modo, boolean tirante, int alcance) {
        Block poste = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
        for (int dy = 0; dy < ALTO_POSTE; dy++) {
            level.setBlock(base.above(dy), poste.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState mensula = ModBlocks.MENSULA.get().defaultBlockState()
                .setValue(AbstractRotatableBlock.FACING, Direction.EAST)
                .setValue(AbstractRotatableBlock.ROTATION, 1)
                .setValue(MensulaBlock.TIPO, tipo)
                .setValue(MensulaBlock.MODO, modo)
                .setValue(MensulaBlock.TIRANTE, tirante)
                .setValue(MensulaBlock.ALCANCE, alcance);
        level.setBlock(base.above(ALTURA_MENSULA).east(), mensula, Block.UPDATE_ALL);

        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        if (via != null) {
            for (int dz = -8; dz <= 8; dz++) {
                level.setBlock(new BlockPos(base.getX() + 1 + alcance, SUELO, base.getZ() + dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void mover(MinecraftServer server, Vista v) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.SPECTATOR);
        double dx = v.mirarX() - v.x(), dy = v.mirarY() - v.y(), dz = v.mirarZ() - v.z();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // los ojos estan 1,62 por encima de los pies
        player.teleportTo(server.overworld(), v.x(), v.y() - 1.62, v.z(), yaw, pitch);
    }
}

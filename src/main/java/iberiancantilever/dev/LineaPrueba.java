package iberiancantilever.dev;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import de.mrjulsen.paw.data.WireSettingsData;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import iberiancantilever.ModBlocks;
import iberiancantilever.IberianCantilever;
import iberiancantilever.block.MensulaBlockEntity;
import iberiancantilever.block.ModoZigzag;
import iberiancantilever.block.AjustesTunel;
import iberiancantilever.block.PosicionTunel;
import iberiancantilever.block.SoporteTunelBlock;
import iberiancantilever.block.TamanoTunel;
import iberiancantilever.block.VersionTunel;
import iberiancantilever.geometry.Ajustes;

/**
 * Para probar: {@code /mensula linea [postes]} monta delante del jugador una via recta con postes
 * de celosia de PNW, mensulas en zigzag (interior / exterior alternando) y les tiende el cable de
 * catenaria de PNW igual que lo haria un jugador con la bobina. {@code /mensula tunel [soportes]} monta
 * un tunel con catenaria rigida, {@code /mensula tunel2} las otras variantes del soporte de tunel (grande
 * de techo y de pared, en un poste de hormigon y en un muro) y {@code /mensula diagonal} las 16
 * direcciones de la mensula.
 */
@Mod.EventBusSubscriber(modid = IberianCantilever.MOD_ID)
public final class LineaPrueba {
    /** Separacion entre postes, en bloques. */
    private static final int VANO = 12;
    /** Separacion entre soportes de tunel, en bloques. */
    private static final int VANO_TUNEL = 6;
    private static final int ALTO_POSTE = 8;
    /** La mensula va en el bloque ALTURA_MENSULA del poste: hilo de contacto a ~5,4 m del carril. */
    private static final int ALTURA_MENSULA = 6;
    /**
     * Los soportes de tunel van un bloque por debajo de las mensulas (el techo, al nivel de ellas): asi su
     * hilo de contacto queda a la misma altura que el de la mensula por defecto.
     */
    private static final int ALTURA_TUNEL = ALTURA_MENSULA - 1;

    private LineaPrueba() {
    }

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mensula")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("linea")
                        .executes(ctx -> montar(ctx.getSource(), 5))
                        .then(Commands.argument("postes", IntegerArgumentType.integer(2, 16))
                                .executes(ctx -> montar(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "postes")))))
                .then(Commands.literal("diagonal")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int cables = montarDiagonales(player, player.blockPosition().offset(3, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Diagonales: 16 postes, " + cables + " vanos con cable"), false);
                            return cables;
                        }))
                .then(Commands.literal("tunel2")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int tramos = montarTunelVariantes(player, player.blockPosition().offset(0, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Variantes del soporte de tunel: " + tramos + " tramos"), false);
                            return tramos;
                        }))
                .then(Commands.literal("tunel")
                        .executes(ctx -> montarTunel(ctx.getSource(), 6))
                        .then(Commands.argument("soportes", IntegerArgumentType.integer(2, 16))
                                .executes(ctx -> montarTunel(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "soportes"))))));
    }

    private static int montar(CommandSourceStack src, int postes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        BlockPos suelo = player.blockPosition();
        // la via va hacia el norte (-Z), los postes a su izquierda (oeste)
        List<BlockPos> mensulas = montar(player.serverLevel(), suelo.offset(-3, 0, -3), postes);
        int cables = tenderCables(player, mensulas);
        src.sendSuccess(() -> Component.literal("Linea de prueba: " + postes + " postes, " + cables + " vanos con cable"), false);
        return cables;
    }

    private static int montarTunel(CommandSourceStack src, int soportes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        int tramos = montarTunel(player, player.blockPosition().offset(0, 0, -3), soportes);
        src.sendSuccess(() -> Component.literal("Tunel de prueba: " + soportes + " soportes, " + tramos + " tramos"), false);
        return tramos;
    }

    /**
     * Tunel de prueba hacia el norte desde {@code inicio} (a ras de suelo, en el eje de la via): techo,
     * soportes en zigzag con el perfil rigido tendido y, a la entrada, un poste con mensula unido al
     * primer soporte con cable de catenaria normal (la transicion). Devuelve cuantos tramos tendio.
     */
    public static int montarTunel(ServerPlayer player, BlockPos inicio, int soportes) {
        ServerLevel level = player.serverLevel();
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        int largo = (soportes - 1) * VANO_TUNEL;
        // techo de piedra justo encima de los soportes, desde la boca del tunel hasta el final
        for (int dz = 0; dz <= largo + 2; dz++) {
            for (int dx = -3; dx <= curva(soportes - 1) + 3; dx++) {
                level.setBlock(inicio.offset(dx, ALTURA_TUNEL + 1, -dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (via != null) {
                level.setBlock(inicio.offset(0, 0, -dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        PosicionTunel[] zigzag = {PosicionTunel.DERECHA, PosicionTunel.CENTRO, PosicionTunel.IZQUIERDA, PosicionTunel.CENTRO};
        List<BlockPos> puntos = new ArrayList<>();
        for (int i = 0; i < soportes; i++) {
            BlockPos pos = inicio.offset(curva(i), ALTURA_TUNEL, -i * VANO_TUNEL - 1);
            level.setBlock(pos, ModBlocks.SOPORTE_TUNEL.get().defaultBlockState()
                    .setValue(AbstractRotatableBlock.FACING, Direction.SOUTH)
                    .setValue(AbstractRotatableBlock.ROTATION, 1)
                    .setValue(SoporteTunelBlock.ALTURA, i == soportes - 1 ? 2 : 0)
                    .setValue(SoporteTunelBlock.POSICION, zigzag[i % zigzag.length]), Block.UPDATE_ALL);
            puntos.add(pos);
        }
        int tramos = tender(player, puntos, () -> new ItemStack(ModBlocks.PERFIL_RIGIDO.get()));
        // transicion: poste con mensula fuera del tunel, unido al primer soporte con catenaria normal
        BlockPos poste = inicio.offset(-3, 0, VANO);
        List<BlockPos> mensula = montarPostes(level, poste, 1, ModoZigzag.MEDIO);
        tender(player, List.of(mensula.get(0), puntos.get(0)), LineaPrueba::bobinaCatenaria);
        return tramos;
    }

    /**
     * Las 16 direcciones de la mensula, para compararlas con la de PNW: una fila de 4 postes por cada
     * cara (norte, este, sur, oeste hacia el sur desde {@code inicio}) con los 4 giros finos (-26,6,
     * 0, 26,6 y 45 grados, este ultimo con el poste en diagonal). En cada poste va nuestra mensula y,
     * 3 bloques mas abajo, la de PNW con el mismo giro; las de cada fila se unen con cable de catenaria
     * (las nuestras entre si y las de PNW entre si). Devuelve cuantos vanos tendio.
     */
    public static int montarDiagonales(ServerPlayer player, BlockPos inicio) {
        ServerLevel level = player.serverLevel();
        Block poste = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
        Block pnw = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "cantilever_brown"));
        Direction[] caras = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int cables = 0;
        for (int f = 0; f < caras.length; f++) {
            List<BlockPos> nuestras = new ArrayList<>();
            List<BlockPos> dePnw = new ArrayList<>();
            for (int r = 0; r <= 3; r++) {
                BlockPos base = inicio.offset(r * 10, 0, f * 16);
                for (int dy = 0; dy < ALTO_POSTE; dy++) {
                    level.setBlock(base.above(dy), poste.defaultBlockState(), Block.UPDATE_ALL);
                }
                // con 45 grados PNW busca el poste en la diagonal (getSupportBlockPos)
                Direction cara = caras[f];
                BlockPos sitio = base.relative(cara);
                if (r == 3) {
                    sitio = sitio.relative(cara.getCounterClockWise());
                }
                BlockPos mia = sitio.above(ALTURA_MENSULA);
                level.setBlock(mia, ModBlocks.MENSULA.get().defaultBlockState()
                        .setValue(AbstractRotatableBlock.FACING, cara)
                        .setValue(AbstractRotatableBlock.ROTATION, r), Block.UPDATE_ALL);
                if (level.getBlockEntity(mia) instanceof MensulaBlockEntity be) {
                    be.setAjustes(Ajustes.DEFECTO.conModo(ModoZigzag.MEDIO)
                            .conAltura(Ajustes.alturaAutomatica(Ajustes.DEFECTO.anchura(), ModoZigzag.MEDIO)));
                }
                nuestras.add(mia);
                if (pnw != null) {
                    BlockPos suya = sitio.above(ALTURA_MENSULA - 3);
                    level.setBlock(suya, pnw.defaultBlockState()
                            .setValue(AbstractRotatableBlock.FACING, cara)
                            .setValue(AbstractRotatableBlock.ROTATION, r), Block.UPDATE_ALL);
                    dePnw.add(suya);
                }
            }
            cables += tenderCables(player, nuestras) + tenderCables(player, dePnw);
        }
        return cables;
    }

    /**
     * Las variantes del soporte de tunel en una linea recta hacia el norte desde {@code inicio} (eje de
     * la via), unidas con perfil rigido: de techo grande (pinza a la izquierda), de pared corto en un
     * poste de hormigon de PNW, de pared largo en un muro de piedra, de techo grande (pinza a la derecha)
     * y de pared corto bajado del todo. Los de pared van al oeste de la via, mirando al este.
     */
    public static int montarTunelVariantes(ServerPlayer player, BlockPos inicio) {
        ServerLevel level = player.serverLevel();
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        Block hormigon = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "concrete_pillar"));
        AjustesTunel[] variantes = {
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.GRANDE, PosicionTunel.IZQUIERDA, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.GRANDE, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO),
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.GRANDE, PosicionTunel.DERECHA, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, 0)};
        int largo = (variantes.length - 1) * VANO_TUNEL;
        for (int dz = 0; dz <= largo + 2; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                level.setBlock(inicio.offset(dx, ALTURA_TUNEL + 1, -dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (via != null) {
                level.setBlock(inicio.offset(0, 0, -dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        List<BlockPos> puntos = new ArrayList<>();
        for (int i = 0; i < variantes.length; i++) {
            AjustesTunel v = variantes[i];
            BlockPos pos;
            BlockState state = v.aplicar(ModBlocks.SOPORTE_TUNEL.get().defaultBlockState());
            if (v.version() == VersionTunel.TECHO) {
                pos = inicio.offset(0, ALTURA_TUNEL, -i * VANO_TUNEL - 1);
                state = state.setValue(AbstractRotatableBlock.FACING, Direction.SOUTH).setValue(AbstractRotatableBlock.ROTATION, 1);
            } else {
                // el corto llega a la via desde el bloque de al lado; el largo, desde uno mas alla
                int dx = v.tamano() == TamanoTunel.GRANDE ? -2 : -1;
                pos = inicio.offset(dx, ALTURA_TUNEL, -i * VANO_TUNEL - 1);
                Block muro = i == 1 && hormigon != null ? hormigon : Blocks.STONE_BRICKS;
                for (int dy = 0; dy <= ALTURA_TUNEL; dy++) {
                    level.setBlock(pos.west().atY(inicio.getY() + dy), muro.defaultBlockState(), Block.UPDATE_ALL);
                }
                state = state.setValue(AbstractRotatableBlock.FACING, Direction.EAST).setValue(AbstractRotatableBlock.ROTATION, 1);
            }
            level.setBlock(pos, state, Block.UPDATE_ALL);
            puntos.add(pos);
        }
        return tender(player, puntos, () -> new ItemStack(ModBlocks.PERFIL_RIGIDO.get()));
    }

    /** Hacia donde se va apartando el soporte i del tunel: la linea hace curva al fondo (para probar las curvas). */
    private static int curva(int i) {
        return i * (i - 1) / 2;
    }

    /** Monta postes, mensulas y via; devuelve las posiciones de las mensulas en orden. */
    public static List<BlockPos> montar(ServerLevel level, BlockPos inicio, int postes) {
        return montar(level, inicio, postes, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast")));
    }

    /** Lo mismo con los postes que se digan (los ibericos, por ejemplo). */
    public static List<BlockPos> montar(ServerLevel level, BlockPos inicio, int postes, Block poste) {
        List<BlockPos> mensulas = montarPostes(level, inicio, postes, null, poste);
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        int anchoVia = Math.round(Ajustes.DEFECTO.anchura() - 0.5f);
        if (via != null) {
            for (int dz = 2; dz >= -(postes - 1) * VANO - 2; dz--) {
                level.setBlock(inicio.offset(1 + anchoVia, 0, dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return mensulas;
    }

    /** Postes con mensula hacia el este; con {@code modoFijo} null van en zigzag interior / exterior. */
    private static List<BlockPos> montarPostes(ServerLevel level, BlockPos inicio, int postes, ModoZigzag modoFijo) {
        return montarPostes(level, inicio, postes, modoFijo, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast")));
    }

    private static List<BlockPos> montarPostes(ServerLevel level, BlockPos inicio, int postes, ModoZigzag modoFijo, Block poste) {
        Ajustes ajustes = Ajustes.DEFECTO;
        int anchoVia = Math.round(ajustes.anchura() - 0.5f);
        List<BlockPos> mensulas = new ArrayList<>();
        for (int i = 0; i < postes; i++) {
            BlockPos base = inicio.offset(0, 0, -i * VANO);
            for (int dy = 0; dy < ALTO_POSTE; dy++) {
                level.setBlock(base.above(dy), poste.defaultBlockState(), Block.UPDATE_ALL);
            }
            BlockPos pos = base.above(ALTURA_MENSULA).east();
            BlockState mensula = ModBlocks.MENSULA.get().defaultBlockState()
                    .setValue(AbstractRotatableBlock.FACING, Direction.EAST)
                    .setValue(AbstractRotatableBlock.ROTATION, 1);
            level.setBlock(pos, mensula, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
                // zigzag: el hilo va de un lado al otro en postes seguidos
                ModoZigzag modo = modoFijo != null ? modoFijo : i % 2 == 0 ? ModoZigzag.INTERIOR : ModoZigzag.EXTERIOR;
                be.setAjustes(ajustes.conModo(modo).conAltura(Ajustes.alturaAutomatica(ajustes.anchura(), modo)));
            }
            mensulas.add(pos);
        }
        return mensulas;
    }

    /**
     * Une cada mensula con la siguiente con cable de catenaria de PNW. Se hace como un jugador: bobina
     * en la mano (tipo catenaria) y "clic" en una mensula y luego en la otra.
     */
    public static int tenderCables(ServerPlayer player, List<BlockPos> mensulas) {
        return tender(player, mensulas, LineaPrueba::bobinaCatenaria);
    }

    private static ItemStack bobinaCatenaria() {
        ItemStack bobina = new ItemStack(ModItems.WIRE.get());
        MultiWireItem.setNbt(bobina, new WireSettingsData(ModWireRegistry.CATENARY_WIRE_ITEM_SUBTYPE.get()));
        return bobina;
    }

    /** Une cada punto con el siguiente con lo que de {@code objeto} (bobina de PNW, perfil rigido...). */
    private static int tender(ServerPlayer player, List<BlockPos> mensulas, java.util.function.Supplier<ItemStack> objeto) {
        ItemStack antes = player.getItemInHand(InteractionHand.MAIN_HAND);
        GameType modo = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        int hechos = 0;
        try {
            for (int i = 0; i + 1 < mensulas.size(); i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, objeto.get());
                clic(player, mensulas.get(i));
                clic(player, mensulas.get(i + 1));
                hechos++;
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, antes);
            player.setGameMode(modo);
        }
        return hechos;
    }

    private static void clic(ServerPlayer player, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}

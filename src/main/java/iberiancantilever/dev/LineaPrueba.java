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
import iberiancantilever.block.PosicionTunel;
import iberiancantilever.block.SoporteTunelBlock;
import iberiancantilever.geometry.Ajustes;

/**
 * Para probar: {@code /mensula linea [postes]} monta delante del jugador una via recta con postes
 * de celosia de PNW, mensulas en zigzag (interior / exterior alternando) y les tiende el cable de
 * catenaria de PNW igual que lo haria un jugador con la bobina.
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
                level.setBlock(inicio.offset(dx, ALTURA_MENSULA + 1, -dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (via != null) {
                level.setBlock(inicio.offset(0, 0, -dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        PosicionTunel[] zigzag = {PosicionTunel.DERECHA, PosicionTunel.CENTRO, PosicionTunel.IZQUIERDA, PosicionTunel.CENTRO};
        List<BlockPos> puntos = new ArrayList<>();
        for (int i = 0; i < soportes; i++) {
            BlockPos pos = inicio.offset(curva(i), ALTURA_MENSULA, -i * VANO_TUNEL - 1);
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

    /** Hacia donde se va apartando el soporte i del tunel: la linea hace curva al fondo (para probar las curvas). */
    private static int curva(int i) {
        return i * (i - 1) / 2;
    }

    /** Monta postes, mensulas y via; devuelve las posiciones de las mensulas en orden. */
    public static List<BlockPos> montar(ServerLevel level, BlockPos inicio, int postes) {
        List<BlockPos> mensulas = montarPostes(level, inicio, postes, null);
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
        Block poste = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
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

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import iberiancantilever.ModBlocks;
import iberiancantilever.ModPostes;
import iberiancantilever.cable.RecolocarCables;
import iberiancantilever.cable.TiranteDiagonal;
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
    static final int ALTURA_MENSULA = 6;
    /**
     * Los soportes de tunel van un bloque por debajo de las mensulas (el techo, al nivel de ellas): asi su
     * hilo de contacto queda a la misma altura que el de la mensula por defecto.
     */
    static final int ALTURA_TUNEL = ALTURA_MENSULA - 1;

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
                            visionNocturna(player);
                            int cables = montarDiagonales(player, player.blockPosition().offset(3, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Diagonales: 16 postes, " + cables + " vanos con cable"), false);
                            return cables;
                        }))
                .then(Commands.literal("escaparate")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            visionNocturna(player);
                            Escaparate.montar(player, player.blockPosition().offset(3, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Escaparate montado"), false);
                            return 1;
                        }))
                .then(Commands.literal("tirante")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            visionNocturna(player);
                            int tirantes = montarTirantes(player, player.blockPosition().offset(-3, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Tirantes diagonales: " + tirantes), false);
                            return tirantes;
                        }))
                .then(Commands.literal("tunel2")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            visionNocturna(player);
                            int tramos = montarTunelVariantes(player, player.blockPosition().offset(0, 0, -3));
                            ctx.getSource().sendSuccess(() -> Component.literal("Variantes del soporte de tunel: " + tramos + " tramos"), false);
                            return tramos;
                        }))
                .then(Commands.literal("tunel")
                        .executes(ctx -> montarTunel(ctx.getSource(), 6))
                        .then(Commands.argument("soportes", IntegerArgumentType.integer(2, 16))
                                .executes(ctx -> montarTunel(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "soportes"))))));
    }

    /** En las pruebas se ve todo aunque sea de noche o dentro del tunel. */
    public static void visionNocturna(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
    }

    private static int montar(CommandSourceStack src, int postes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        visionNocturna(player);
        BlockPos suelo = player.blockPosition();
        // la via va hacia el norte (-Z), los postes a su izquierda (oeste)
        List<BlockPos> mensulas = montar(player.serverLevel(), suelo.offset(-3, 0, -3), postes);
        int cables = tenderCables(player, mensulas);
        src.sendSuccess(() -> Component.literal("Linea de prueba: " + postes + " postes, " + cables + " vanos con cable"), false);
        return cables;
    }

    private static int montarTunel(CommandSourceStack src, int soportes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        visionNocturna(player);
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
                    level.setBlock(base.above(dy), poste(poste, caras[f], r), Block.UPDATE_ALL);
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

    /** Altura de los postes de la escena de los tirantes: el tirante sube hasta su ultimo bloque. */
    private static final int ALTO_POSTE_TIRANTE = 10;

    /**
     * Tirantes diagonales: postes hacia el norte desde {@code inicio} (iberico plano, de celosia de PNW,
     * iberico plano) con mensulas de anchura 3,5 (medio, exterior, interior) unidas con catenaria y, en
     * cada una, el cable de soporte de PNW de la horizontal al ultimo bloque del poste: en la primera
     * clicando mensula y poste, en la segunda al reves y en la tercera luego se baja la mensula (YOffset)
     * para ver que el tirante la sigue. Un cuarto poste con anchura 2,5 no lo admite. Ademas, 20 bloques
     * al sur, la fila de las anchuras ({@link #montarAnchuras}). Devuelve cuantos tirantes quedaron tendidos.
     */
    public static int montarTirantes(ServerPlayer player, BlockPos inicio) {
        ServerLevel level = player.serverLevel();
        Block iberico = ModPostes.POSTES.get("iberian_flat_lattice_mast").get();
        Block pnw = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"));
        ModoZigzag[] modos = {ModoZigzag.MEDIO, ModoZigzag.EXTERIOR, ModoZigzag.INTERIOR, ModoZigzag.MEDIO};
        float[] anchuras = {3.5f, 3.5f, 3.5f, 2.5f};
        List<BlockPos> mensulas = new ArrayList<>();
        List<BlockPos> cimas = new ArrayList<>();
        for (int i = 0; i < modos.length; i++) {
            BlockPos base = inicio.offset(0, 0, -i * VANO);
            mensulas.add(posteConMensula(level, base, i == 1 ? pnw : iberico, ALTO_POSTE_TIRANTE, anchuras[i], modos[i]));
            cimas.add(base.above(ALTO_POSTE_TIRANTE - 1));
        }
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        if (via != null) {
            for (int dz = 2; dz >= -2 * VANO - 2; dz--) {
                level.setBlock(inicio.offset(4, 0, dz), via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        tenderCables(player, mensulas.subList(0, 3));
        clics(player, bobinaSoporte(), mensulas.get(0), cimas.get(0));
        clics(player, bobinaSoporte(), cimas.get(1), mensulas.get(1));
        clics(player, bobinaSoporte(), mensulas.get(2), cimas.get(2));
        clics(player, bobinaSoporte(), mensulas.get(3), cimas.get(3));
        // la tercera baja medio bloque: el tirante (y la catenaria) se rehacen hasta su nuevo sitio
        BlockPos tercera = mensulas.get(2);
        if (level.getBlockEntity(tercera) instanceof MensulaBlockEntity be) {
            be.setAjustes(be.getAjustes().conDesplazamientoY(0.5f));
            RecolocarCables.enBloque(level, tercera);
            TiranteDiagonal.recolocar(level, tercera, player);
        }
        montarAnchuras(player, inicio.offset(0, 0, 20));
        return TiranteDiagonal.contar(level);
    }

    /** Anchuras de la fila de {@link #montarAnchuras}; la ultima mensula empieza en la primera y acaba en la ultima. */
    public static final float[] ANCHURAS_TIRANTE = {3.5f, 4.5f, 5.5f, 6.5f};

    /**
     * Dos filas hacia el este. En {@code inicio}, una mensula por anchura de {@link #ANCHURAS_TIRANTE} con su
     * tirante cada vez mas alto (de 2 a 5 bloques sobre la mensula) y una mas que se pone con la anchura
     * minima, se le tiende el tirante y luego se ensancha a la maxima: su tirante tiene que acabar igual que
     * el de la de la maxima. 70 bloques al este, cuatro mensulas iguales (anchura 4,5) con el tirante a 2, 3,
     * 4 y 5 bloques (los triangulos posibles) y una quinta donde primero se pincha el poste a 6 y a 1 bloque
     * (no vale) y luego a 3.
     */
    public static void montarAnchuras(ServerPlayer player, BlockPos inicio) {
        int alto = ALTURA_MENSULA + TiranteDiagonal.ALTURA_POSTE_MAX + 2;
        int x = 0;
        for (int i = 0; i <= ANCHURAS_TIRANTE.length; i++) {
            boolean ensanchar = i == ANCHURAS_TIRANTE.length;
            float anchura = ensanchar ? ANCHURAS_TIRANTE[0] : ANCHURAS_TIRANTE[i];
            float anchuraFinal = ensanchar ? ANCHURAS_TIRANTE[ANCHURAS_TIRANTE.length - 1] : anchura;
            int sobre = ensanchar ? TiranteDiagonal.ALTURA_POSTE_MAX : TiranteDiagonal.ALTURA_POSTE_MIN + i;
            BlockPos pos = posteConTirante(player, inicio.offset(x, 0, 0), i, alto, anchura, sobre);
            if (ensanchar && player.serverLevel().getBlockEntity(pos) instanceof MensulaBlockEntity be) {
                be.setAjustes(be.getAjustes().conAnchura(anchuraFinal));
                TiranteDiagonal.recolocar(player.serverLevel(), pos, player);
            }
            x += (int) Math.ceil(anchuraFinal) + 3;
        }
        BlockPos alturas = inicio.offset(70, 0, 0);
        int triangulos = TiranteDiagonal.ALTURA_POSTE_MAX - TiranteDiagonal.ALTURA_POSTE_MIN + 1;
        for (int i = 0; i <= triangulos; i++) {
            BlockPos base = alturas.offset(i * 8, 0, 0);
            if (i < triangulos) {
                posteConTirante(player, base, i, alto, 4.5f, TiranteDiagonal.ALTURA_POSTE_MIN + i);
            } else {
                BlockPos pos = posteConMensula(player.serverLevel(), base, poste(i), alto, 4.5f, ModoZigzag.MEDIO);
                clics(player, bobinaSoporte(), pos, pos.west().above(TiranteDiagonal.ALTURA_POSTE_MAX + 1),
                        pos.west().above(TiranteDiagonal.ALTURA_POSTE_MIN - 1), pos.west().above(3));
            }
        }
    }

    /** Poste (iberico o de PNW, alternando) con mensula y su tirante al bloque del poste {@code sobre} bloques mas arriba. */
    private static BlockPos posteConTirante(ServerPlayer player, BlockPos base, int i, int alto, float anchura, int sobre) {
        ModoZigzag[] modos = {ModoZigzag.MEDIO, ModoZigzag.EXTERIOR, ModoZigzag.INTERIOR};
        BlockPos pos = posteConMensula(player.serverLevel(), base, poste(i), alto, anchura, modos[i % modos.length]);
        clics(player, bobinaSoporte(), pos, base.above(ALTURA_MENSULA + sobre));
        return pos;
    }

    private static Block poste(int i) {
        return i % 2 == 1 ? ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "lattice_mast"))
                : ModPostes.POSTES.get("iberian_flat_lattice_mast").get();
    }

    /** Poste de {@code alto} bloques en {@code base} con una mensula hacia el este a la altura de siempre. */
    static BlockPos posteConMensula(ServerLevel level, BlockPos base, Block poste, int alto, float anchura, ModoZigzag modo) {
        for (int dy = 0; dy < alto; dy++) {
            level.setBlock(base.above(dy), poste(poste, Direction.EAST, 1), Block.UPDATE_ALL);
        }
        BlockPos pos = base.above(ALTURA_MENSULA).east();
        level.setBlock(pos, ModBlocks.MENSULA.get().defaultBlockState()
                .setValue(AbstractRotatableBlock.FACING, Direction.EAST)
                .setValue(AbstractRotatableBlock.ROTATION, 1), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
            be.setAjustes(Ajustes.DEFECTO.conAnchura(anchura).conModo(modo).conAltura(Ajustes.alturaAutomatica(anchura, modo)));
        }
        return pos;
    }

    /**
     * Las variantes del soporte de tunel en una linea recta hacia el norte desde {@code inicio} (eje de
     * la via), unidas con perfil rigido: de techo grande (pinza a la izquierda), de pared corto en un
     * poste de hormigon de PNW, de pared largo en un muro de piedra, de techo grande (pinza a la derecha)
     * y de pared corto bajado del todo; luego, con tamano general: de techo al 150 %, grande al 125 %, de pared
     * al 150 % y uno normal al 100 %. Los de pared van al oeste de la via, mirando al este.
     */
    public static int montarTunelVariantes(ServerPlayer player, BlockPos inicio) {
        ServerLevel level = player.serverLevel();
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        Block hormigon = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "concrete_pillar"));
        AjustesTunel[] variantes = {
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.GRANDE, PosicionTunel.IZQUIERDA, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.GRANDE, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0),
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.GRANDE, PosicionTunel.DERECHA, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, 0, 0),
                // tamano general: el perfil sale del tamano del soporte del que parte (el pequeno se une al grande)
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 2),
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.GRANDE, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 1),
                new AjustesTunel(VersionTunel.PARED, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 2),
                new AjustesTunel(VersionTunel.TECHO, TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0)};
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

    /**
     * El poste como lo pone un jugador para colgarle una mensula: mirando hacia ella y con su mismo giro
     * (la mensula copia los dos del poste al ponerla en el). Los planos solo aceptan mensulas en el eje
     * hacia el que miran: con el estado por defecto (al norte) salian girados 90 grados.
     */
    public static BlockState poste(Block poste, Direction cara, int giro) {
        BlockState state = poste.defaultBlockState();
        if (state.hasProperty(AbstractRotatableBlock.FACING)) {
            state = state.setValue(AbstractRotatableBlock.FACING, cara);
        }
        if (state.hasProperty(AbstractRotatableBlock.ROTATION)) {
            state = state.setValue(AbstractRotatableBlock.ROTATION, giro);
        }
        return state;
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
                level.setBlock(base.above(dy), poste(poste, Direction.EAST, 1), Block.UPDATE_ALL);
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

    static ItemStack bobinaCatenaria() {
        ItemStack bobina = new ItemStack(ModItems.WIRE.get());
        MultiWireItem.setNbt(bobina, new WireSettingsData(ModWireRegistry.CATENARY_WIRE_ITEM_SUBTYPE.get()));
        return bobina;
    }

    /** Une cada punto con el siguiente con lo que de {@code objeto} (bobina de PNW, perfil rigido...). */
    static int tender(ServerPlayer player, List<BlockPos> mensulas, java.util.function.Supplier<ItemStack> objeto) {
        int hechos = 0;
        for (int i = 0; i + 1 < mensulas.size(); i++) {
            clics(player, objeto.get(), mensulas.get(i), mensulas.get(i + 1));
            hechos++;
        }
        return hechos;
    }

    /** Clics seguidos en esos bloques con {@code objeto} en la mano (en creativo, como hace falta para tender). */
    static void clics(ServerPlayer player, ItemStack objeto, BlockPos... bloques) {
        ItemStack antes = player.getItemInHand(InteractionHand.MAIN_HAND);
        GameType modo = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, objeto);
            for (BlockPos pos : bloques) {
                clic(player, pos);
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, antes);
            player.setGameMode(modo);
        }
    }

    static ItemStack bobinaSoporte() {
        ItemStack bobina = new ItemStack(ModItems.WIRE.get());
        MultiWireItem.setNbt(bobina, new WireSettingsData(ModWireRegistry.SUPPORT_WIRE_ITEM_SUBTYPE.get()));
        return bobina;
    }

    /** Como el clic derecho de un jugador: primero el evento de Forge (el tirante diagonal lo usa) y luego el objeto. */
    private static void clic(ServerPlayer player, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit).isCanceled()) {
            return;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}

package iberiancantilever.block;

import org.joml.Vector3d;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import de.mrjulsen.paw.block.abstractions.AbstractSupportedRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import iberiancantilever.ModBlocks;
import iberiancantilever.client.ClienteMensula;
import iberiancantilever.geometry.Ajustes;
import iberiancantilever.geometry.PiezasDatos;
import iberiancantilever.item.SoporteTunelItem;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Soporte de catenaria rigida de tunel, en dos versiones ({@link VersionTunel}): colgado del techo
 * (agarra el perfil en uno de tres sitios, {@link PosicionTunel}) o empotrado en la pared con un brazo
 * horizontal, que se pone como la mensula: en la cara de un poste de PNW (los de hormigon, por ejemplo)
 * o de cualquier bloque solido. Cada version tiene dos tamanos ({@link TamanoTunel}). Todo se escoge en
 * su ventana (clic derecho con la mano vacia, o con el objeto al aire antes de ponerlo).
 * Es un conector de cables de PNW: el perfil rigido va de soporte a soporte y el cable de catenaria
 * normal tambien se le engancha (transicion): el hilo de contacto en la pinza y el sustentador baja
 * hasta justo encima de ella.
 */
public class SoporteTunelBlock extends AbstractSupportedRotatableWireConnectorBlock<SoporteTunelBlockEntity>
        implements ICatenaryWireConnector {
    public static final EnumProperty<VersionTunel> VERSION = EnumProperty.create("version", VersionTunel.class);
    public static final EnumProperty<TamanoTunel> TAMANO = EnumProperty.create("tamano", TamanoTunel.class);
    public static final EnumProperty<PosicionTunel> POSICION = EnumProperty.create("posicion", PosicionTunel.class);
    public static final int ALTURA_MAX = 4;
    /**
     * De techo: cuanto cuelga de mas, en pasos de {@link #PX_POR_ALTURA}: la varilla se alarga y la pinza
     * baja. Con 0 es corto (la pinza a {@link #PINZA_TECHO} px del suelo de su bloque): puesto bajo el
     * techo de un tunel cuya boca esta al nivel de una mensula por defecto, el hilo de contacto queda a la
     * misma altura que el de ella y la transicion sale recta.
     */
    public static final IntegerProperty ALTURA = IntegerProperty.create("altura", 0, ALTURA_MAX);
    /** Px que baja la pinza por cada paso de ALTURA (4 pasos = un bloque). */
    public static final float PX_POR_ALTURA = 4f;
    /**
     * Donde queda la pinza del de techo con altura 0 (px desde el suelo de su bloque): un bloque por
     * encima del hilo de la mensula por defecto (que queda bajo su bloque).
     */
    public static final float PINZA_TECHO = 16f - 16f * Ajustes.ALTURA_CATENARIA.defecto();
    /**
     * De pared: altura en pasos de {@link #PX_POR_ALTURA_PARED} desde abajo del todo; en el medio
     * ({@link #ALTURA_PARED_CENTRO}) la pieza queda centrada en el bloque, y nunca se sale de el.
     */
    public static final int ALTURA_PARED_MAX = 18;
    public static final int ALTURA_PARED_CENTRO = ALTURA_PARED_MAX / 2;
    public static final IntegerProperty ALTURA_PARED = IntegerProperty.create("altura_pared", 0, ALTURA_PARED_MAX);
    public static final float PX_POR_ALTURA_PARED = 0.5f;
    /** Lo que sube la pieza de pared (px) para quedar centrada en el bloque. */
    private static final float CENTRAR_PARED = 8f - (PiezasDatos.TUNEL_PARED_CORTO.MIN[1] + PiezasDatos.TUNEL_PARED_CORTO.MAX[1]) / 2f;
    /**
     * El sustentador de una catenaria normal que llega al tunel baja y se ancla justo encima de la pinza
     * (en el de techo, sobre el travesano de arriba). Tiene que quedar a mas de 4 px del hilo de contacto:
     * PNW le quita 0.25 bloques a lo que puede colgar el sustentador.
     */
    public static final float SUSTENTADOR_SOBRE_CONTACTO = 4.05f;

    /**
     * La pieza de Blockbench de un soporte y donde agarra el perfil (centro de su "cable_attach", px).
     * {@code colgada}: su cubo 0 es la varilla del techo, que se alarga con la altura.
     */
    public record PiezaTunel(String id, int elementos, float y, float z, boolean colgada) {
    }

    private static final PiezaTunel[] TECHO_NORMAL = {
            new PiezaTunel(PiezasDatos.TUNEL_IZQUIERDA.ID, PiezasDatos.TUNEL_IZQUIERDA.ELEMENTS, PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[1], 10f, true),
            new PiezaTunel(PiezasDatos.TUNEL_CENTRO.ID, PiezasDatos.TUNEL_CENTRO.ELEMENTS, PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[1], 8f, true),
            new PiezaTunel(PiezasDatos.TUNEL_DERECHA.ID, PiezasDatos.TUNEL_DERECHA.ELEMENTS, PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[1],
                    PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[2], true)};
    private static final PiezaTunel[] TECHO_GRANDE = {
            new PiezaTunel(PiezasDatos.TUNEL_GRANDE_IZQUIERDA.ID, PiezasDatos.TUNEL_GRANDE_IZQUIERDA.ELEMENTS,
                    PiezasDatos.TUNEL_GRANDE_IZQUIERDA.CABLE_ATTACH[1], PiezasDatos.TUNEL_GRANDE_IZQUIERDA.CABLE_ATTACH[2], true),
            new PiezaTunel(PiezasDatos.TUNEL_GRANDE_CENTRO.ID, PiezasDatos.TUNEL_GRANDE_CENTRO.ELEMENTS,
                    PiezasDatos.TUNEL_GRANDE_CENTRO.CABLE_ATTACH[1], PiezasDatos.TUNEL_GRANDE_CENTRO.CABLE_ATTACH[2], true),
            new PiezaTunel(PiezasDatos.TUNEL_GRANDE_DERECHA.ID, PiezasDatos.TUNEL_GRANDE_DERECHA.ELEMENTS,
                    PiezasDatos.TUNEL_GRANDE_DERECHA.CABLE_ATTACH[1], PiezasDatos.TUNEL_GRANDE_DERECHA.CABLE_ATTACH[2], true)};
    private static final PiezaTunel PARED_CORTO = new PiezaTunel(PiezasDatos.TUNEL_PARED_CORTO.ID, PiezasDatos.TUNEL_PARED_CORTO.ELEMENTS,
            PiezasDatos.TUNEL_PARED_CORTO.CABLE_ATTACH[1], PiezasDatos.TUNEL_PARED_CORTO.CABLE_ATTACH[2], false);
    private static final PiezaTunel PARED_LARGO = new PiezaTunel(PiezasDatos.TUNEL_PARED_LARGO.ID, PiezasDatos.TUNEL_PARED_LARGO.ELEMENTS,
            PiezasDatos.TUNEL_PARED_LARGO.CABLE_ATTACH_CENTER[1], PiezasDatos.TUNEL_PARED_LARGO.CABLE_ATTACH_CENTER[2], false);

    public SoporteTunelBlock(Properties properties) {
        super(properties.noCollission());
        registerDefaultState(defaultBlockState().setValue(VERSION, VersionTunel.TECHO).setValue(TAMANO, TamanoTunel.NORMAL)
                .setValue(POSICION, PosicionTunel.CENTRO).setValue(ALTURA, 0).setValue(ALTURA_PARED, ALTURA_PARED_CENTRO));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VERSION, TAMANO, POSICION, ALTURA, ALTURA_PARED);
    }

    public static PiezaTunel pieza(BlockState state) {
        boolean grande = state.getValue(TAMANO) == TamanoTunel.GRANDE;
        if (pared(state)) {
            return grande ? PARED_LARGO : PARED_CORTO;
        }
        return (grande ? TECHO_GRANDE : TECHO_NORMAL)[state.getValue(POSICION).ordinal()];
    }

    public static boolean pared(BlockState state) {
        return state.getValue(VERSION) == VersionTunel.PARED;
    }

    // ------------------------------------------------------------------ colocacion

    /**
     * Lo escogido en el objeto se aplica ya aqui (y no solo despues, con el BlockStateTag). La version la
     * decide la cara que se pincha: en un lado (pared o poste) sale el de pared, que se pone como la
     * mensula; por debajo de un bloque (el techo), el de techo, mirando hacia donde mira el jugador (16
     * direcciones), como los bloques de PNW. Pinchando encima de un bloque vale la del objeto.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        AjustesTunel ajustes = AjustesTunel.de(stack.getItem() instanceof SoporteTunelItem item ? item.estado(stack) : defaultBlockState());
        Direction pinchada = context.getClickedFace();
        if (pinchada.getAxis().isHorizontal()) {
            ajustes = ajustes.conVersion(VersionTunel.PARED);
        } else if (pinchada == Direction.DOWN) {
            ajustes = ajustes.conVersion(VersionTunel.TECHO);
        }
        if (ajustes.version() == VersionTunel.PARED) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : ajustes.aplicar(state);
        }
        Direction cara = context.getClickedFace();
        BlockState pinchado = context.getLevel().getBlockState(context.getClickedPos().relative(cara.getOpposite()));
        BlockState state = defaultBlockState();
        if (pinchado.getBlock() instanceof AbstractRotatableBlock && cara.getAxis() == Direction.Axis.Y) {
            state = state.setValue(FACING, pinchado.getValue(FACING)).setValue(ROTATION, pinchado.getValue(ROTATION));
        } else {
            int rot = Mth.floor((180f + context.getRotation()) * 16f / 360f + 0.5f) & 15;
            state = state.setValue(FACING, Direction.from2DDataValue((rot + 2) / 4)).setValue(ROTATION, 3 - (rot + 2) % 4);
        }
        return ajustes.aplicar(state);
    }

    /** El de techo necesita el bloque de encima con la cara de abajo solida; el de pared, su poste o pared. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (pared(state)) {
            return super.canSurvive(state, level, pos);
        }
        BlockPos arriba = pos.above();
        return level.getBlockState(arriba).isFaceSturdy(level, arriba, Direction.DOWN);
    }

    @Override
    protected TagKey<Block> getSupportBlockTag() {
        return ModBlockTags.CANTILEVER_CONNECTABLE;
    }

    /** Con la mano vacia se abre la ventana del soporte. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirSoporteTunel(pos, state));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float b = bajada(state);
        if (!pared(state)) {
            return state.getValue(TAMANO) == TamanoTunel.GRANDE ? Block.box(0, 2 - b, 0, 16, 16, 16) : Block.box(3, 2 - b, 3, 13, 16, 13);
        }
        // el brazo, de la pared (detras) hacia delante; el largo se sale del bloque
        double z0 = pieza(state) == PARED_LARGO ? PiezasDatos.TUNEL_PARED_LARGO.MIN[2] : PiezasDatos.TUNEL_PARED_CORTO.MIN[2];
        double y0 = 2 - b, y1 = 9 - b;
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(6, y0, 0, 10, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 6, 16, y1, 10);
            case EAST -> Block.box(0, y0, 6, 16 - z0, y1, 10);
            default -> Block.box(6, y0, z0, 10, y1, 16);
        };
    }

    /**
     * El de techo: la pieza de Blockbench esta de lado respecto a la direccion en la que mira quien la
     * pone; girada 90 grados los travesanos quedan cruzados sobre la via. El de pared ya esta bien: su
     * placa (z = 16) contra el poste, que PNW pone detras (+Z sin girar).
     */
    @Override
    public float getYRotation(BlockState state) {
        return super.getYRotation(state) + (pared(state) ? 0f : 90f);
    }

    /** El de pared gira alrededor del centro del poste, como la mensula; el de techo, del suyo. */
    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return pared(state) ? new Vec2(0f, 1f) : Vec2.ZERO;
    }

    @Override
    public Vec2 getOffset(BlockState state) {
        return pared(state) ? super.getOffset(state) : Vec2.ZERO;
    }

    /**
     * Cuanto se baja la pieza de Blockbench (px; negativo, sube). El de techo sube hasta dejar la pinza
     * en {@link #PINZA_TECHO} (la varilla se acorta) y baja lo que diga su altura (la varilla se alarga);
     * el de pared se centra en el bloque y sube o baja con la suya.
     */
    public static float bajada(BlockState state) {
        if (pared(state)) {
            return -CENTRAR_PARED - (state.getValue(ALTURA_PARED) - ALTURA_PARED_CENTRO) * PX_POR_ALTURA_PARED;
        }
        return pieza(state).y() - PINZA_TECHO + state.getValue(ALTURA) * PX_POR_ALTURA;
    }

    /** El de pared se acerca (px, hacia +Z de la pieza) hasta la cara de un poste fino; 0 en una pared. */
    public float hueco(BlockGetter level, BlockPos pos, BlockState state) {
        return pared(state) ? MensulaBlock.hueco(level.getBlockState(getSupportBlockPos(level, pos, state))) : 0f;
    }

    // ------------------------------------------------------------------ geometria

    /** Punto de la pieza de Blockbench (px, sin girar) en el sistema de enganche de PNW (sin girar). */
    private static Vec3 local(double x, double y, double z) {
        return new Vec3(x / 16.0 - 0.5, y / 16.0, z / 16.0 - 0.5);
    }

    /**
     * Lo mismo que hace PNW con los puntos de enganche (transformWireAttachPoint), para un punto
     * cualquiera de la pieza: asi el modelo y los cables cuadran siempre. Devuelve coordenadas del
     * bloque (0..1): el pivote girado de PNW ya lleva el medio bloque hasta el centro.
     */
    public Vec3 aBloque(BlockState state, double x, double y, double z) {
        Vec2 pivote = getRotationPivotPoint(state);
        Vec2 pivoteGirado = rotatedPivotPoint(state);
        Vec2 desplazamiento = getOffset(state);
        return VecHelper.rotate(local(x, y, z).subtract(pivote.x, 0, pivote.y), getYRotation(state), Direction.Axis.Y)
                .add(pivoteGirado.x, 0, pivoteGirado.y)
                .add(desplazamiento.x, 0, desplazamiento.y);
    }

    // ------------------------------------------------------------------ cables

    @Override
    protected Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        PiezaTunel p = pieza(state);
        return local(8, p.y() - bajada(state), p.z() + hueco(level, pos, state));
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        PiezaTunel p = pieza(state);
        return local(8, p.y() + SUSTENTADOR_SOBRE_CONTACTO - bajada(state), p.z() + hueco(level, pos, state));
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        BlockState state = level.getBlockState(pos);
        Vec3 contacto = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::defaultWireAttachPoint);
        Vec3 sustentador = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::tensionWireAttachPoint);
        return new CantileverConnectorDataProvider(new Vector3d(contacto.x, contacto.y, contacto.z),
                new Vector3d(sustentador.x, sustentador.y, sustentador.z));
    }

    @Override
    public Class<SoporteTunelBlockEntity> getBlockEntityClass() {
        return SoporteTunelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SoporteTunelBlockEntity> getBlockEntityType() {
        return ModBlocks.SOPORTE_TUNEL_BE.get();
    }
}

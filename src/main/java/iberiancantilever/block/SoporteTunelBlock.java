package iberiancantilever.block;

import org.joml.Vector3d;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import iberiancantilever.ModBlocks;
import iberiancantilever.client.ClienteMensula;
import iberiancantilever.geometry.PiezasDatos;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Soporte de catenaria rigida de tunel: cuelga del techo (16 direcciones, como los bloques de PNW) y
 * agarra el perfil rigido en uno de tres sitios ({@link PosicionTunel}) a la altura que se quiera; las
 * dos cosas se escogen en su ventana (clic derecho con la mano vacia).
 * Es un conector de cables de PNW: el perfil rigido va de soporte a soporte y el cable de catenaria
 * normal tambien se le engancha (transicion): el hilo de contacto en la pinza y el sustentador en el
 * techo.
 */
public class SoporteTunelBlock extends AbstractRotatableWireConnectorBlock<SoporteTunelBlockEntity>
        implements ICatenaryWireConnector {
    public static final EnumProperty<PosicionTunel> POSICION = EnumProperty.create("posicion", PosicionTunel.class);
    /** Cuanto cuelga de mas, en pasos de {@link #PX_POR_ALTURA}: la varilla del techo se alarga y la pinza baja. */
    public static final IntegerProperty ALTURA = IntegerProperty.create("altura", 0, 4);
    /** Px que baja la pinza por cada paso de ALTURA (4 pasos = un bloque). */
    public static final float PX_POR_ALTURA = 4f;
    /** Altura (px) del centro de la pinza donde se engancha el perfil. */
    public static final float ENGANCHE_Y = PiezasDatos.TUNEL_DERECHA.CABLE_ATTACH[1];
    /** El sustentador de una catenaria normal que llega al tunel se ancla en el techo. */
    public static final float TECHO_Y = 16f;

    public SoporteTunelBlock(Properties properties) {
        super(properties.noCollission());
        registerDefaultState(defaultBlockState().setValue(POSICION, PosicionTunel.CENTRO).setValue(ALTURA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POSICION, ALTURA);
    }

    // ------------------------------------------------------------------ colocacion

    /** Necesita techo: el bloque de encima con la cara de abajo solida. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos arriba = pos.above();
        return level.getBlockState(arriba).isFaceSturdy(level, arriba, Direction.DOWN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos posVecino) {
        return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, vecino, level, pos, posVecino);
    }

    /** Con la mano vacia se abre la ventana del soporte (posicion de la pinza y altura). */
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
        return Block.box(3, 2 - bajada(state), 3, 13, 16, 13);
    }

    /**
     * La pieza de Blockbench esta de lado respecto a la direccion en la que mira quien la pone: girada
     * 90 grados los travesanos quedan cruzados sobre la via. Vale para el modelo y para los enganches.
     */
    @Override
    public float getYRotation(BlockState state) {
        return super.getYRotation(state) + 90f;
    }

    /** Lo que baja la pinza (px) con la altura escogida. */
    public static float bajada(BlockState state) {
        return state.getValue(ALTURA) * PX_POR_ALTURA;
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
        return local(8, ENGANCHE_Y - bajada(state), state.getValue(POSICION).enganche());
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return local(8, TECHO_Y, state.getValue(POSICION).enganche());
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        BlockState state = level.getBlockState(pos);
        Vec3 contacto = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::defaultWireAttachPoint);
        Vec3 techo = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::tensionWireAttachPoint);
        return new CantileverConnectorDataProvider(new Vector3d(contacto.x, contacto.y, contacto.z), new Vector3d(techo.x, techo.y, techo.z));
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

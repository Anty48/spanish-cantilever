package spanishcantilevers.block;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.paw.block.CantileverBracketBaseBlock;
import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.PowerLineBracketBlock;
import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import de.mrjulsen.paw.block.abstractions.AbstractRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.AjustesColgante;
import spanishcantilevers.geometry.MensulaLayout;
import spanishcantilevers.network.Aviso;

/**
 * Mensula colgante: el puntal de la mensula interior ancha colgado de una viga horizontal (un brazo de poste
 * o un soporte de linea, espanoles o de PNW) o de la cara de abajo de un bloque solido. Se pone clicando
 * la cara de abajo de la viga; el bloque queda debajo, encima del hilo, y el tubo cuelga {@code anchura}
 * bloques mas alla, al otro lado de quien lo pone y a lo largo de la viga. El aislador del sustentador va
 * encima de la viga o colgado debajo (por defecto), a la altura que de su grosor.
 *
 * <p>Clic derecho con la mano vacia abre su ventana (aislador, forma del tubo, anchura y altura).
 */
public class MensulaColganteBlock extends AbstractRotatableWireConnectorBlock<MensulaColganteBlockEntity>
        implements ICatenaryWireConnector {

    /**
     * La viga de la que cuelga: sus caras de abajo y de arriba (px desde la base de este bloque) y hacia
     * donde va (mundo, horizontal; null en un bloque solido, que no tiene direccion).
     */
    public record Viga(float abajo, float arriba, @Nullable Vec3 eje) {
    }

    /** Un bloque solido: su cara de abajo es el techo de este bloque. */
    private static final Viga BLOQUE = new Viga(16f, 32f, null);

    public MensulaColganteBlock(Properties properties) {
        super(properties.noCollission());
    }

    /** La viga del bloque de encima de {@code pos}, o null si ahi no se puede colgar. */
    @Nullable
    public static Viga viga(BlockGetter level, BlockPos pos) {
        BlockPos arriba = pos.above();
        BlockState s = level.getBlockState(arriba);
        if (s.getBlock() instanceof CantileverBracketVerticalBlock) {
            return null;
        }
        if (s.getBlock() instanceof PowerLineBracketBlock) {
            // perfil en H de 3 px, abajo o arriba de su bloque
            boolean alto = s.getValue(PowerLineBracketBlock.HALF) == Half.TOP;
            return new Viga(alto ? 29f : 16f, alto ? 32f : 19f, eje(s));
        }
        if (s.getBlock() instanceof CantileverBracketBaseBlock) {
            // tubo cuadrado de 4 px en medio de su bloque
            return new Viga(22f, 26f, eje(s));
        }
        return s.isFaceSturdy(level, arriba, Direction.DOWN) ? BLOQUE : null;
    }

    /** Los brazos de PNW van a lo largo de Z de su modelo, girado como cualquier bloque girable de PNW. */
    private static Vec3 eje(BlockState viga) {
        double a = Math.toRadians(((AbstractRotatableBlock) viga.getBlock()).getYRotation(viga));
        return new Vec3(Math.sin(a), 0, Math.cos(a));
    }

    public static AjustesColgante ajustes(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MensulaColganteBlockEntity be ? be.getAjustes() : AjustesColgante.DEFECTO;
    }

    // ------------------------------------------------------------------ colocacion

    /**
     * Solo por debajo de una viga o de un bloque solido. El tubo queda al otro lado de quien lo pone: a lo
     * largo de la viga, hacia donde mira (o, bajo un bloque, hacia donde mira, en 16 direcciones).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Viga viga = context.getClickedFace() == Direction.DOWN ? viga(context.getLevel(), context.getClickedPos()) : null;
        if (viga == null) {
            if (context.getPlayer() instanceof ServerPlayer jugador) {
                Aviso.enviar(jugador, "aviso.spanishcantilevers.colgante_soporte");
            }
            return null;
        }
        Vec3 mirada = Vec3.directionFromRotation(0, context.getRotation());
        Vec3 haciaTubo = viga.eje() == null ? mirada : viga.eje().dot(mirada) >= 0 ? viga.eje() : viga.eje().scale(-1);
        return orientar(defaultBlockState(), haciaTubo);
    }

    /** De las 16 direcciones de PNW, la que deja el tubo (-X del sistema de la colgante) hacia {@code haciaTubo}. */
    private BlockState orientar(BlockState base, Vec3 haciaTubo) {
        BlockState mejor = base;
        double mejorAlineacion = -2;
        for (Direction cara : Direction.Plane.HORIZONTAL) {
            for (int giro = 0; giro <= 3; giro++) {
                BlockState prueba = base.setValue(FACING, cara).setValue(ROTATION, giro);
                Vec3 tubo = VecHelper.rotate(new Vec3(-1, 0, 0), getYRotation(prueba), Direction.Axis.Y);
                double alineacion = tubo.x * haciaTubo.x + tubo.z * haciaTubo.z;
                if (alineacion > mejorAlineacion + 1e-6) {
                    mejorAlineacion = alineacion;
                    mejor = prueba;
                }
            }
        }
        return mejor;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return viga(level, pos) != null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    /** Con la mano vacia se abre su ventana. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            AjustesColgante ajustes = ajustes(level, pos);
            Viga viga = viga(level, pos);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirColgante(pos, ajustes, viga));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ geometria

    public MensulaLayout.Resultado layout(BlockGetter level, BlockPos pos) {
        Viga viga = viga(level, pos);
        if (viga == null) {
            viga = BLOQUE;
        }
        return MensulaLayout.calcularColgante(ajustes(level, pos), viga.abajo(), viga.arriba());
    }

    /** El bloque entero: facil de clicar con la mano o con la bobina de cable. */
    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    /** Gira alrededor del centro del bloque y sin desplazamientos: el hilo queda siempre en el centro. */
    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }

    @Override
    public Vec2 getOffset(BlockState state) {
        return Vec2.ZERO;
    }

    /** Punto del sistema de la colgante (px, sin girar) en el sistema de enganche de PNW (sin girar). */
    private static Vec3 local(double x, double y, double z) {
        return new Vec3(x / 16.0 - 0.5, y / 16.0, z / 16.0 - 0.5);
    }

    /**
     * Lo mismo que hace PNW con los puntos de enganche (transformWireAttachPoint), para cualquier punto de
     * la colgante: asi el modelo y los cables cuadran siempre (como en el soporte de tunel). Devuelve
     * coordenadas del bloque (0..1).
     */
    public Vec3 aBloque(BlockState state, double x, double y, double z) {
        Vec2 pivoteGirado = rotatedPivotPoint(state);
        return VecHelper.rotate(local(x, y, z), getYRotation(state), Direction.Axis.Y).add(pivoteGirado.x, 0, pivoteGirado.y);
    }

    // ------------------------------------------------------------------ cables

    private static Vec3 local(Vector3f p) {
        return local(p.x(), p.y(), p.z());
    }

    @Override
    protected Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return local(layout(level, pos).contacto());
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return local(layout(level, pos).sustentador());
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
    public Class<MensulaColganteBlockEntity> getBlockEntityClass() {
        return MensulaColganteBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MensulaColganteBlockEntity> getBlockEntityType() {
        return ModBlocks.MENSULA_COLGANTE_BE.get();
    }
}

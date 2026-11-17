package spanishcantilevers.block;

import java.util.EnumMap;
import java.util.Map;

import org.joml.Vector3d;

import com.simibubi.create.foundation.block.IBE;

import de.mrjulsen.paw.block.FlatLatticeMastBlock;
import de.mrjulsen.paw.block.abstractions.AbstractMultipartPostBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.property.EPostPart;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import spanishcantilevers.ModPostes;
import spanishcantilevers.cable.ConectorAisladores;

/**
 * Remate con aisladores (azul GIF o verde ADIF de la alta velocidad, o gris espanol): el poste plano (o plano diagonal) de
 * PNW con una barra arriba, a lo largo del plano del poste, de la que cuelgan uno o dos aisladores. Se pone
 * y se gira como los postes planos (16 direcciones; encima de un poste sigue su linea, con el aislador
 * hacia donde mira el jugador). Los aisladores son conectores de los cables de corriente de PNW (bobina en
 * modo cable de corriente). Con dos aisladores el cable se engancha a los dos a la vez: ver
 * {@link ConectorAisladores} y {@link spanishcantilevers.cable.CableAisladores}.
 *
 * <p>Las medidas son las de los modelos de Blockbench del usuario, que miran al norte como el poste plano
 * de PNW (columnas en z 4..5.5 y 10.5..12, barra a lo largo de z).
 */
public class RemateAisladoresBlock extends FlatLatticeMastBlock implements IBE<WireConnectorBlockEntity>, IWireConnector {
    /** z (px) de los aisladores: el de -z y el de +z. */
    private static final float Z_MENOS = -5.25f;
    private static final float Z_MAS = 21.25f;
    /** El hilo se engancha con su parte de arriba en la punta de la varilla del aislador. */
    private static final float RADIO_HILO = 0.5f;

    private final boolean doble;
    /** Puntos de enganche (px, modelo mirando al norte), uno o dos. */
    private final float[][] aisladores;
    private final Map<Direction, VoxelShape> formas = new EnumMap<>(Direction.class);

    public RemateAisladoresBlock(Properties properties, IWeatheringBlock.WeatherData<FlatLatticeMastBlock> datos, boolean diagonal, boolean doble) {
        super(properties, datos);
        this.doble = doble;
        // punta de la varilla de los aisladores y barra de arriba (px), segun el modelo
        float punta = diagonal ? 7.6f : 6.1f;
        float barraAbajo = diagonal ? 15.6f : 14.1f;
        // con un aislador, el plano lo tiene en +z y el diagonal en -z
        float[] zs = doble ? new float[]{Z_MENOS, Z_MAS} : new float[]{diagonal ? Z_MENOS : Z_MAS};
        aisladores = new float[zs.length][];
        for (int i = 0; i < zs.length; i++) {
            aisladores[i] = new float[]{8f, punta - RADIO_HILO, zs[i]};
        }
        float barraDesde = doble || diagonal ? -7.5f : 4.5f;
        float barraHasta = doble || !diagonal ? 23.5f : 12.5f;
        for (Direction cara : Direction.Plane.HORIZONTAL) {
            VoxelShape forma = Shapes.or(caja(cara, 6, 0, 4, 10, 16, 12), caja(cara, 6.5f, barraAbajo, barraDesde, 9.5f, 16, barraHasta));
            for (float z : zs) {
                forma = Shapes.or(forma, caja(cara, 6.25f, punta, z - 1.75f, 9.75f, barraAbajo, z + 1.75f));
            }
            formas.put(cara, forma);
        }
    }

    /** Si tiene dos aisladores. */
    public boolean doble() {
        return doble;
    }

    /** Una caja (px, modelo mirando al norte) girada como el modelo de esa cara (el "y" del blockstate). */
    static VoxelShape caja(Direction cara, float x1, float y1, float z1, float x2, float y2, float z2) {
        float[] a = girarPx(cara, x1, z1);
        float[] b = girarPx(cara, x2, z2);
        return Block.box(Math.min(a[0], b[0]), y1, Math.min(a[1], b[1]), Math.max(a[0], b[0]), y2, Math.max(a[1], b[1]));
    }

    /** (x, z) en px girado como el blockstate gira el modelo norte para esa cara (en sentido horario). */
    private static float[] girarPx(Direction cara, float x, float z) {
        return switch (cara) {
            case EAST -> new float[]{16 - z, x};
            case SOUTH -> new float[]{16 - x, 16 - z};
            case WEST -> new float[]{z, 16 - x};
            default -> new float[]{x, z};
        };
    }

    /**
     * Encima de un poste PNW copia su cara y su giro, asi que el remate de un aislador solo quedaba hacia un
     * lado (el poste es simetrico y al ponerlo no se piensa hacia donde mira). De las dos orientaciones con
     * la misma linea de poste (la copiada y girada 180 grados) se queda la que deja el aislador hacia donde
     * mira el jugador. El doble es simetrico: da igual.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || doble) {
            return state;
        }
        Vector3d lado = enganche(state, 0).sub(0.5, 0, 0.5);
        Vec3 mirada = Vec3.directionFromRotation(0, context.getRotation());
        if (lado.x * mirada.x + lado.z * mirada.z < 0) {
            state = state.setValue(FACING, state.getValue(FACING).getOpposite());
        }
        return state;
    }

    // ------------------------------------------------------------------ continuacion del poste

    /*
     * PNW pone el remate cuadrado (PART=TOP) a un poste que no tiene encima otro de su misma clase, y el
     * remate es de otra clase: el poste de debajo se cerraba y el remate quedaba como un bloque suelto
     * encima. Aqui el poste de debajo se pasa a tramo intermedio. PNW lo vuelve a cerrar cada vez que algo
     * cambia encima o debajo de el, y ese cambio de estado nos llega como vecino de abajo, asi que se
     * corrige otra vez al tick siguiente (en el mismo momento PNW aun no ha recalculado su parte).
     */

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState anterior, boolean movido) {
        super.onPlace(state, level, pos, anterior, movido);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos posVecino) {
        if (direction == Direction.DOWN) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, vecino, level, pos, posVecino);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos abajo = pos.below();
        BlockState poste = level.getBlockState(abajo);
        if (poste.getBlock() instanceof AbstractMultipartPostBlock && poste.getValue(PART) == EPostPart.TOP) {
            level.setBlock(abajo, poste.setValue(PART, EPostPart.IN_BETWEEN), Block.UPDATE_ALL);
        }
    }

    /** La forma sin el giro fino (lo pone PNW); no tiene base de cimentacion. */
    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return formas.get(state.getValue(FACING));
    }

    // ------------------------------------------------------------------ cables

    /**
     * Donde se engancha el cable en el aislador {@code i}, relativo a la esquina del bloque (bloques): el
     * punto del modelo girado igual que lo dibuja el modelo girable de PNW (cara + giro fino, sobre el centro).
     */
    public Vector3d enganche(BlockState state, int i) {
        float[] p = aisladores[i];
        double x = p[0] / 16.0 - 0.5;
        double z = p[2] / 16.0 - 0.5;
        double angulo = Math.toRadians(getYRotation(state));
        double cos = Math.cos(angulo);
        double sen = Math.sin(angulo);
        Vec2 desplazamiento = getOffset(state);
        return new Vector3d(x * cos + z * sen + 0.5 + desplazamiento.x, p[1] / 16.0, -x * sen + z * cos + 0.5 + desplazamiento.y);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int index) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RemateAisladoresBlock)) {
            return new ConnectorDataProvider.Empty();
        }
        // con uno, un conector normal de PNW (su cable de corriente lo entiende tal cual)
        return doble ? new ConectorAisladores(enganche(state, 0), enganche(state, 1)) : new BasicConnectorDataProvider(enganche(state, 0));
    }

    @Override
    public Class<WireConnectorBlockEntity> getBlockEntityClass() {
        return WireConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WireConnectorBlockEntity> getBlockEntityType() {
        return ModPostes.REMATE_BE.get();
    }
}

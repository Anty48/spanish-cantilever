package spanishcantilevers.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.joml.Vector3d;

import com.simibubi.create.foundation.block.IBE;

import de.mrjulsen.paw.block.ConcretePillarBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import spanishcantilevers.ModPostes;

/**
 * Columna de hormigon con aislador arriba: una varilla a lo largo de la via (z del modelo, que mira al
 * norte) con un aislador a cada lado del fuste. Cada punta de la varilla es un enganche para los cables de
 * corriente de PNW, y cada cable va a UNO de los dos: al del lado por el que se clica la columna con la
 * bobina. Asi la linea sigue la via de columna en columna, un cable por cada lado.
 *
 * <p>El lado elegido viaja en los datos propios del punto de la bobina ({@link #datosClic}, los pone
 * {@link spanishcantilevers.cable.CableAisladores}) y PNW nos los devuelve al pedir el conector.
 */
public class ColumnaAisladorBlock extends ColumnaBlock implements IBE<WireConnectorBlockEntity>, IWireConnector {
    /** Clave del lado (0 = punta de -z, 1 = punta de +z) en los datos del punto de la bobina. */
    private static final String NBT_LADO = "SpanishCantileversInsulatorSide";
    /** Enganches (px, modelo mirando al norte): en el eje de la varilla, medio pixel dentro de cada punta. */
    private static final float[][] ENGANCHES = {{8f, 13.125f, -2f}, {8f, 13.125f, 18f}};

    private final Map<Direction, VoxelShape> formas = new EnumMap<>(Direction.class);

    public ColumnaAisladorBlock(Properties properties, IWeatheringBlock.WeatherData<ConcretePillarBlock> datos) {
        super(properties, datos);
        for (Direction cara : Direction.Plane.HORIZONTAL) {
            // fuste, varilla y los platos de los aisladores
            formas.put(cara, Shapes.or(FUSTE,
                    RemateAisladoresBlock.caja(cara, 7.5f, 12.75f, -2.5f, 8.5f, 13.5f, 18.5f),
                    RemateAisladoresBlock.caja(cara, 6.75f, 11.85f, -1.75f, 9.25f, 14.35f, 17.75f)));
        }
    }

    /**
     * PNW copia la cara y el giro de la columna de debajo, asi que la varilla solo podia ir en la direccion en
     * que se puso la columna. La columna es simetrica (una cruz), asi que se deja su giro fino y de las cuatro
     * caras se queda la que pone la varilla mas a lo largo de donde mira el jugador.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Vec3 mirada = Vec3.directionFromRotation(0, context.getRotation());
        BlockState mejor = state;
        double mejorAlineacion = -1;
        for (Direction cara : Direction.Plane.HORIZONTAL) {
            BlockState prueba = state.setValue(FACING, cara);
            Vector3d eje = enganche(prueba, 1).sub(enganche(prueba, 0));
            double alineacion = Math.abs(eje.x * mirada.x + eje.z * mirada.z) / Math.hypot(eje.x, eje.z);
            if (alineacion > mejorAlineacion + 1e-6) {
                mejorAlineacion = alineacion;
                mejor = prueba;
            }
        }
        return mejor;
    }

    /** Sin base: es el remate de la columna. */
    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return formas.get(state.getValue(FACING));
    }

    /**
     * Donde se engancha el cable en la punta {@code lado}, relativo a la esquina del bloque (bloques): el
     * punto del modelo girado igual que lo dibuja el modelo girable de PNW (cara + giro fino).
     */
    public Vector3d enganche(BlockState state, int lado) {
        float[] p = ENGANCHES[lado];
        double x = p[0] / 16.0 - 0.5;
        double z = p[2] / 16.0 - 0.5;
        double angulo = Math.toRadians(getYRotation(state));
        double cos = Math.cos(angulo);
        double sen = Math.sin(angulo);
        Vec2 desplazamiento = getOffset(state);
        return new Vector3d(x * cos + z * sen + 0.5 + desplazamiento.x, p[1] / 16.0, -x * sen + z * cos + 0.5 + desplazamiento.y);
    }

    /** La punta mas cerca del punto clicado (mundo): la del lado de la columna por el que se ha clicado. */
    public int ladoClicado(BlockState state, BlockPos pos, Vec3 clic) {
        double[] distancias = new double[2];
        for (int lado = 0; lado < 2; lado++) {
            Vector3d p = enganche(state, lado).add(pos.getX(), pos.getY(), pos.getZ());
            distancias[lado] = Math.pow(p.x - clic.x, 2) + Math.pow(p.z - clic.z, 2);
        }
        return distancias[1] < distancias[0] ? 1 : 0;
    }

    /**
     * Los datos que se guardan en el punto de la bobina al clicar esta columna (el lado clicado), o null si
     * en {@code pos} no hay una columna con aislador.
     */
    public static BiConsumer<CompoundTag, CompoundTag> datosClic(Level level, BlockPos pos, Vec3 clic) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ColumnaAisladorBlock columna)) {
            return null;
        }
        int lado = columna.ladoClicado(state, pos, clic);
        return (comunes, punto) -> punto.putInt(NBT_LADO, lado);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int index) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ColumnaAisladorBlock)) {
            return new ConnectorDataProvider.Empty();
        }
        int lado = customData == null ? 0 : customData.getCustomDataForPoint(index).getInt(NBT_LADO) == 1 ? 1 : 0;
        return new BasicConnectorDataProvider(enganche(state, lado));
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

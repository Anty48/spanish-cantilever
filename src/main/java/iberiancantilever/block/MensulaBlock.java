package iberiancantilever.block;

import java.util.List;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.paw.block.abstractions.AbstractSupportedRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import iberiancantilever.ModBlocks;
import iberiancantilever.client.ClienteMensula;
import iberiancantilever.geometry.Ajustes;
import iberiancantilever.geometry.MensulaLayout;

/**
 * Mensula espanola. Se coloca en la cara de un poste igual que la de PNW (16 direcciones, se
 * engancha a los mismos postes) y los cables de PNW se le conectan: el sustentador al aislador de
 * arriba y el hilo de contacto a la punta del brazo de atirantado.
 *
 * <p>Clic derecho con la mano vacia abre la ventana de ajustes (como la de PNW) para escoger aislador, zigzag,
 * alcance y tirante.
 */
public class MensulaBlock extends AbstractSupportedRotatableWireConnectorBlock<MensulaBlockEntity>
        implements ICatenaryWireConnector {

    /** Grosor del poste en px segun las etiquetas de PNW -> hueco entre nuestro bloque y su cara. */
    private static final List<Grosor> GROSORES = List.of(
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_16PX, 16),
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_12PX, 12),
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_8PX, 8),
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_6PX, 6),
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_5PX, 5),
            new Grosor(ModBlockTags.CANTILEVER_CONNECTABLE_4PX, 4));

    private record Grosor(TagKey<Block> tag, int px) {
    }

    public MensulaBlock(Properties properties) {
        super(properties.noCollission());
    }

    // ------------------------------------------------------------------ ajustes

    /**
     * Con la mano vacia se abre la misma ventana que con el objeto, pero para cambiar esta mensula ya
     * puesta. Con algo en la mano no hacemos nada: asi las herramientas de cables de PNW funcionan.
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide && level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
            Ajustes ajustes = be.getAjustes();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirBloque(pos, ajustes));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Ajustes ajustes(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MensulaBlockEntity be ? be.getAjustes() : Ajustes.DEFECTO;
    }

    // ------------------------------------------------------------------ geometria

    /** Hueco en px entre la cara de nuestro bloque y la del poste que la sujeta. */
    public float huecoPoste(BlockGetter level, BlockPos pos, BlockState state) {
        return hueco(level.getBlockState(getSupportBlockPos(level, pos, state)));
    }

    /**
     * Hueco en px entre la cara de un bloque y la del poste de PNW {@code soporte} que tiene al lado
     * (segun su grosor); 0 si no es un poste (una pared llega hasta la cara).
     */
    public static float hueco(BlockState soporte) {
        for (Grosor g : GROSORES) {
            if (soporte.is(g.tag())) {
                return (16 - g.px()) / 2f;
            }
        }
        return 0f;
    }

    public MensulaLayout.Resultado layout(BlockGetter level, BlockPos pos, BlockState state) {
        return MensulaLayout.calcular(ajustes(level, pos), huecoPoste(level, pos, state));
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        // como la mensula de PNW: gira alrededor del centro del poste
        return new Vec2(0.0f, 1.0f);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double stretch = 16.0 * (1.0 / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state)))) - 1.0);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(6, 0, 0, 10, 16, 16 + stretch);
            case WEST -> Block.box(-stretch, 0, 6, 16, 16, 10);
            case EAST -> Block.box(0, 0, 6, 16 + stretch, 16, 10);
            default -> Block.box(6, 0, -stretch, 10, 16, 16);
        };
    }

    @Override
    protected TagKey<Block> getSupportBlockTag() {
        return ModBlockTags.CANTILEVER_CONNECTABLE;
    }

    // ------------------------------------------------------------------ cables de PNW

    /** Pixeles de la mensula -> sistema en el que PNW espera los puntos de enganche. */
    private static Vec3 aEnganche(Vector3f p) {
        return new Vec3(p.z() / 16.0 - 0.5, p.y() / 16.0, 0.5 - p.x() / 16.0);
    }

    @Override
    protected Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return aEnganche(layout(level, pos, state).contacto());
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return aEnganche(layout(level, pos, state).sustentador());
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
    public Class<MensulaBlockEntity> getBlockEntityClass() {
        return MensulaBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MensulaBlockEntity> getBlockEntityType() {
        return ModBlocks.MENSULA_BE.get();
    }
}

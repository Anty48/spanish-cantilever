package spanishcantilever.block;

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
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import spanishcantilever.ModBlocks;
import spanishcantilever.geometry.MensulaLayout;

/**
 * Mensula espanola. Se coloca en la cara de un poste igual que la de PNW (16 direcciones, se
 * engancha a los mismos postes) y los cables de PNW se le conectan: el sustentador al aislador de
 * arriba y el hilo de contacto a la punta del brazo de atirantado.
 *
 * <p>Ajustes (en el propio bloque):
 * <ul>
 * <li>mano vacia: modo interior / medio / exterior</li>
 * <li>mano vacia + agachado: tipo de aislador 1 / 2 / 3</li>
 * <li>palo: alcance (distancia del poste al centro de la via)</li>
 * <li>palo + agachado: tirante si / no</li>
 * </ul>
 */
public class MensulaBlock extends AbstractSupportedRotatableWireConnectorBlock<MensulaBlockEntity>
        implements ICatenaryWireConnector {

    public static final EnumProperty<TipoAislador> TIPO = EnumProperty.create("tipo", TipoAislador.class);
    public static final EnumProperty<ModoZigzag> MODO = EnumProperty.create("modo", ModoZigzag.class);
    public static final BooleanProperty TIRANTE = BooleanProperty.create("tirante");
    public static final IntegerProperty ALCANCE = IntegerProperty.create("alcance", 1, 4);

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
        registerDefaultState(defaultBlockState()
                .setValue(TIPO, TipoAislador.TIPO1)
                .setValue(MODO, ModoZigzag.MEDIO)
                .setValue(TIRANTE, true)
                .setValue(ALCANCE, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIPO, MODO, TIRANTE, ALCANCE);
    }

    // ------------------------------------------------------------------ ajustes

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean agachado = player.isShiftKeyDown();
        BlockState nuevo;
        Component msg;
        if (stack.isEmpty() && !agachado) {
            nuevo = state.setValue(MODO, state.getValue(MODO).next());
            msg = Component.translatable("message.spanishcantilever.modo",
                    Component.translatable("modo.spanishcantilever." + nuevo.getValue(MODO).getSerializedName()));
        } else if (stack.isEmpty()) {
            nuevo = state.setValue(TIPO, state.getValue(TIPO).next());
            msg = Component.translatable("message.spanishcantilever.tipo",
                    Component.translatable("tipo.spanishcantilever." + nuevo.getValue(TIPO).getSerializedName()));
        } else if (stack.is(Items.STICK) && !agachado) {
            nuevo = state.setValue(ALCANCE, state.getValue(ALCANCE) % 4 + 1);
            msg = Component.translatable("message.spanishcantilever.alcance", nuevo.getValue(ALCANCE));
        } else if (stack.is(Items.STICK)) {
            nuevo = state.setValue(TIRANTE, !state.getValue(TIRANTE));
            msg = Component.translatable(nuevo.getValue(TIRANTE)
                    ? "message.spanishcantilever.tirante_si" : "message.spanishcantilever.tirante_no");
        } else {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, nuevo, Block.UPDATE_ALL);
            player.displayClientMessage(msg, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ geometria

    /** Hueco en px entre la cara de nuestro bloque y la del poste que la sujeta. */
    public float huecoPoste(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState soporte = level.getBlockState(getSupportBlockPos(level, pos, state));
        for (Grosor g : GROSORES) {
            if (soporte.is(g.tag())) {
                return (16 - g.px()) / 2f;
            }
        }
        return 0f;
    }

    public MensulaLayout.Resultado layout(BlockGetter level, BlockPos pos, BlockState state) {
        return layout(state, huecoPoste(level, pos, state));
    }

    public static MensulaLayout.Resultado layout(BlockState state, float hueco) {
        return MensulaLayout.calcular(state.getValue(TIPO), state.getValue(MODO), state.getValue(TIRANTE),
                state.getValue(ALCANCE), hueco);
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

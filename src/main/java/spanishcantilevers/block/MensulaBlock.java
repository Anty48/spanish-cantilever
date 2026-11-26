package spanishcantilevers.block;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.paw.block.abstractions.AbstractSupportedRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.cable.ConectorMensulaDoble;
import spanishcantilevers.cable.RecolocarCables;
import spanishcantilevers.cable.TiranteDiagonal;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.geometry.MensulaDoble;
import spanishcantilevers.geometry.MensulaLayout;
import spanishcantilevers.item.MensulaItem;
import spanishcantilevers.network.Aviso;

/**
 * Mensula espanola. Se coloca en la cara de un poste igual que la de PNW (16 direcciones, se
 * engancha a los mismos postes) y los cables de PNW se le conectan: el sustentador al aislador de
 * arriba y el hilo de contacto a la punta del brazo de atirantado.
 *
 * <p>Clic derecho con la mano vacia abre la ventana de ajustes (como la de PNW) para escoger aislador, zigzag,
 * alcance y tirante.
 *
 * <p>Clicada con otra mensula en la mano se hace doble ({@link MensulaDoble}): la nueva queda al lado de la
 * via por el que se clica. En una doble, la ventana es la de la mensula del lado clicado, y el cable de
 * catenaria va a la del lado por el que se clica (ver {@link spanishcantilevers.cable.CablesMensulaDoble}).
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

    /** Clave, en los datos del punto de la bobina, de a cual de las dos mensulas de una doble va el cable. */
    public static final String NBT_CUAL = "SpanishCantileversCantilever";
    /** Valor de {@link #NBT_CUAL} para enganchar las dos a la vez (clic en la cara de arriba). */
    public static final int AMBAS = 2;

    /**
     * Con la mano vacia se abre la misma ventana que con el objeto, pero para cambiar esta mensula ya
     * puesta (en una doble, la del lado clicado). Con otra mensula en la mano se hace doble. Con otra cosa
     * no hacemos nada: asi las herramientas de cables de PNW funcionan.
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack enMano = player.getItemInHand(hand);
        if (enMano.getItem() instanceof MensulaItem) {
            return bifurcar(state, level, pos, player, enMano, hit);
        }
        if (hand != InteractionHand.MAIN_HAND || !enMano.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide && level.getBlockEntity(pos) instanceof MensulaBlockEntity be) {
            int cual = be.doble() ? cualEn(state, be, pos, hit.getLocation()) : 0;
            Ajustes ajustes = be.getAjustes(cual);
            MensulaDoble.Pareja pareja = be.doble() ? new MensulaDoble.Pareja(be.getAjustes(1 - cual), cual, be.getLado()) : null;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirBloque(pos, ajustes, pareja));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Anade una segunda mensula, si cabe, en el lado de la via por el que se ha clicado. */
    private InteractionResult bifurcar(BlockState state, Level level, BlockPos pos, Player player, ItemStack objeto, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MensulaBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        String problema = be.doble() ? "aviso.spanishcantilevers.mensula_doble_llena"
                : !MensulaDoble.cabe(be.getAjustes()) ? "aviso.spanishcantilevers.mensula_doble_anchura" : null;
        if (problema != null) {
            if (player instanceof ServerPlayer jugador) {
                Aviso.enviar(jugador, problema);
            }
            return InteractionResult.CONSUME;
        }
        be.bifurcar(MensulaItem.configurado(objeto) ? MensulaItem.ajustes(objeto) : Ajustes.DEFECTO, ladoEn(state, pos, hit.getLocation()));
        if (!player.getAbilities().instabuild) {
            objeto.shrink(1);
        }
        level.playSound(null, pos, getSoundType(state, level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 1f, 0.8f);
        // la que ya estaba se ha movido a un lado: sus cables tambien
        RecolocarCables.enBloque(level, pos);
        return InteractionResult.CONSUME;
    }

    /** Un punto del mundo en pixeles de la mensula (lo contrario de lo que hace el modelo). */
    private static Vector3f aMensula(BlockState state, BlockPos pos, Vec3 mundo) {
        Matrix4f inversa = mensulaABloque(state).invert();
        return inversa.transformPosition(new Vector3f((float) (mundo.x - pos.getX()), (float) (mundo.y - pos.getY()),
                (float) (mundo.z - pos.getZ())));
    }

    /** +1 o -1: a que lado de la via (Z de la mensula) cae un punto del mundo. */
    public static int ladoEn(BlockState state, BlockPos pos, Vec3 mundo) {
        return aMensula(state, pos, mundo).z() >= MensulaLayout.Z ? 1 : -1;
    }

    /** A cual de las dos mensulas de una doble (0 o 1) le toca un punto clicado: la de su lado. */
    public static int cualEn(BlockState state, MensulaBlockEntity be, BlockPos pos, Vec3 mundo) {
        return ladoEn(state, pos, mundo) == be.getLado() ? 1 : 0;
    }

    /**
     * A cual de las dos mensulas de una doble le toca un clic en la cara {@code cara}: la del lado de la via
     * hacia el que mira, si es una de las dos caras grandes de los lados; null si es otra (arriba, abajo o
     * los cantos estrechos, que no dejan claro a cual se refiere).
     */
    @Nullable
    public static Integer cualPorCara(BlockState state, MensulaBlockEntity be, Direction cara) {
        Direction.Axis largo = state.getValue(FACING).getAxis();
        if (!cara.getAxis().isHorizontal() || cara.getAxis() == largo) {
            return null;
        }
        Vector3f z = mensulaABloque(state).transformDirection(new Vector3f(0, 0, 1));
        int lado = z.x() * cara.getStepX() + z.z() * cara.getStepZ() >= 0 ? 1 : -1;
        return lado == be.getLado() ? 1 : 0;
    }

    /**
     * Pixeles de la mensula -> coordenadas del bloque. Misma secuencia que CantileverModel de PNW:
     * giro fino alrededor del centro del poste, desplazamiento en la diagonal de 45 grados y giro
     * segun FACING.
     */
    public static Matrix4f mensulaABloque(BlockState state) {
        MensulaBlock block = (MensulaBlock) state.getBlock();
        Direction facing = state.getValue(FACING);
        if (facing.getAxis() == Direction.Axis.Z) {
            facing = facing.getOpposite();
        }
        float fino = block.getRelativeYRotation(state);
        // pivote de PNW: (-p.y, 0, p.x) + centro, con p = getRotationPivotPoint = (0, 1)
        float px = -1f + 0.5f, pz = 0f + 0.5f;
        Matrix4f m = new Matrix4f()
                .translate(0.5f, 0, 0.5f)
                .rotateY((float) Math.toRadians(facing.toYRot() + 90f))
                .translate(-0.5f, 0, -0.5f);
        if (state.getValue(ROTATION) >= 3) {
            m.translate(0, 0, 1);
        }
        m.translate(px, 0, pz)
                .rotateY((float) Math.toRadians(fino))
                .translate(-px, 0, -pz)
                .scale(1f / 16f);
        return m;
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
        return layout(level, pos, state, 0);
    }

    /** La primera (0) o la segunda (1) de una doble; en una sencilla, la unica. */
    public MensulaLayout.Resultado layout(BlockGetter level, BlockPos pos, BlockState state, int cual) {
        float hueco = huecoPoste(level, pos, state);
        if (level.getBlockEntity(pos) instanceof MensulaBlockEntity be && be.doble()) {
            return MensulaLayout.calcularDoble(be.getAjustes(), be.getSegunda(), be.getLado(), hueco).de(cual);
        }
        return MensulaLayout.calcular(ajustes(level, pos), hueco);
    }

    private static boolean esDoble(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MensulaBlockEntity be && be.doble();
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        // como la mensula de PNW: gira alrededor del centro del poste
        return new Vec2(0.0f, 1.0f);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double stretch = 16.0 * (1.0 / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state)))) - 1.0);
        // la doble abarca las dos mensulas
        double a = esDoble(level, pos) ? 2 : 6;
        double b = 16 - a;
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(a, 0, 0, b, 16, 16 + stretch);
            case WEST -> Block.box(-stretch, 0, a, 16, 16, b);
            case EAST -> Block.box(0, 0, a, 16 + stretch, 16, b);
            default -> Block.box(a, 0, -stretch, b, 16, 16);
        };
    }

    /** PNW guarda la forma por estado: la de la doble es otra. */
    @Override
    protected int shapeHash(BlockGetter level, BlockPos pos, BlockState state) {
        return super.shapeHash(level, pos, state) * 31 + (esDoble(level, pos) ? 1 : 0);
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

    /**
     * A cual de las dos mensulas va el punto {@code index} de un cable: 0 (si no lo dice: la que ya estaba),
     * 1 o {@link #AMBAS}.
     */
    private static int cualDe(@Nullable CustomData customData, int index) {
        if (customData == null) {
            return 0;
        }
        CompoundTag datos = customData.getCustomDataForPoint(index);
        return datos == null ? 0 : Math.max(0, Math.min(AMBAS, datos.getInt(NBT_CUAL)));
    }

    /** Como {@link #cualDe}, pero solo una (con ambas, la primera: es la que lee la catenaria de PNW). */
    private static int cual(@Nullable CustomData customData, int index) {
        return cualDe(customData, index) == 1 ? 1 : 0;
    }

    @Override
    protected Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return aEnganche(layout(level, pos, state, cual(customData, index)).contacto());
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        return aEnganche(layout(level, pos, state, cual(customData, index)).sustentador());
    }

    /** Donde se engancha el tirante diagonal, en coordenadas del mundo (ver {@link TiranteDiagonal}). */
    public Vec3 puntoTiranteDiagonal(Level level, BlockPos pos, BlockState state) {
        Vec3 enBloque = transformWireAttachPoint(level, pos, state, null, 0,
                (l, p, s, c, i) -> aEnganche(layout(l, p, s).tiranteDiagonal()));
        return Vec3.atLowerCornerOf(pos).add(enBloque);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            // si la quita un jugador ya lo ha hecho el evento de romper (y le devuelve el cable)
            TiranteDiagonal.quitar(level, pos, Optional.empty());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** La loot table da una mensula; si era doble, se devuelve tambien la segunda. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity be, ItemStack herramienta) {
        super.playerDestroy(level, player, pos, state, be, herramienta);
        if (!level.isClientSide && be instanceof MensulaBlockEntity mensula && mensula.doble()) {
            popResource(level, pos, new ItemStack(ModBlocks.MENSULA_ITEM.get()));
        }
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        BlockState state = level.getBlockState(pos);
        if (cualDe(customData, connectionPointIndex) == AMBAS && esDoble(level, pos)) {
            // enganchada por arriba: las dos a la vez (la catenaria bifurcada tiende una a cada una)
            return new ConectorMensulaDoble(
                    punto(level, pos, state, 0, true), punto(level, pos, state, 0, false),
                    punto(level, pos, state, 1, true), punto(level, pos, state, 1, false));
        }
        Vec3 contacto = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::defaultWireAttachPoint);
        Vec3 sustentador = transformWireAttachPoint(level, pos, state, customData, connectionPointIndex, this::tensionWireAttachPoint);
        return new CantileverConnectorDataProvider(new Vector3d(contacto.x, contacto.y, contacto.z),
                new Vector3d(sustentador.x, sustentador.y, sustentador.z));
    }

    /** El hilo de contacto o el sustentador de una de las dos de una doble, girado como hace PNW. */
    private Vector3d punto(Level level, BlockPos pos, BlockState state, int cual, boolean contacto) {
        Vec3 p = transformWireAttachPoint(level, pos, state, null, 0, (l, ps, s, c, i) -> {
            MensulaLayout.Resultado r = layout(l, ps, s, cual);
            return aEnganche(contacto ? r.contacto() : r.sustentador());
        });
        return new Vector3d(p.x, p.y, p.z);
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

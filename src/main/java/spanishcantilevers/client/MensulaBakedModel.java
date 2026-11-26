package spanishcantilevers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.block.MensulaBlockEntity;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.geometry.Colocacion;
import spanishcantilevers.geometry.MensulaDoble;
import spanishcantilevers.geometry.MensulaLayout;

/**
 * Modelo de la mensula: monta las piezas de Blockbench segun MensulaLayout y lo gira igual que
 * PNW gira su mensula (16 direcciones). El hueco hasta la cara del poste se lee del mundo.
 */
public class MensulaBakedModel implements BakedModel {
    private static final ModelProperty<Float> HUECO = new ModelProperty<>();
    private static final ModelProperty<Ajustes> AJUSTES = new ModelProperty<>();
    /** La segunda mensula de una doble (null si es sencilla) y hacia que lado quedo. */
    private static final ModelProperty<Ajustes> SEGUNDA = new ModelProperty<>();
    private static final ModelProperty<Integer> LADO = new ModelProperty<>();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel original;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(BlockState state, Ajustes ajustes, Ajustes segunda, int lado, float hueco) {
    }

    public MensulaBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (state.getBlock() instanceof MensulaBlock block) {
            ModelData.Builder datos = ModelData.builder()
                    .with(HUECO, block.huecoPoste(level, pos, state))
                    .with(AJUSTES, MensulaBlock.ajustes(level, pos));
            if (level.getBlockEntity(pos) instanceof MensulaBlockEntity be && be.doble()) {
                datos.with(SEGUNDA, be.getSegunda()).with(LADO, be.getLado());
            }
            return datos.build();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        // sin capa es el agrietado de cuando se pica el bloque: las catenarias no lo llevan
        if (side != null || renderType == null || state == null || !(state.getBlock() instanceof MensulaBlock)) {
            return List.of();
        }
        Float hueco = data.get(HUECO);
        Ajustes ajustes = data.get(AJUSTES);
        Integer lado = data.get(LADO);
        return cache.computeIfAbsent(new Key(state, ajustes == null ? Ajustes.DEFECTO : ajustes, data.get(SEGUNDA),
                        lado == null ? 1 : lado, hueco == null ? 2f : hueco),
                k -> hornear(piezas(k.ajustes(), k.segunda(), k.lado(), k.hueco()), MensulaBlock.mensulaABloque(k.state())));
    }

    /** Las piezas de una mensula sencilla o, si hay {@code segunda}, de las dos de una doble. */
    private static List<Colocacion> piezas(Ajustes ajustes, @Nullable Ajustes segunda, int lado, float hueco) {
        return segunda == null ? MensulaLayout.calcular(ajustes, hueco).piezas()
                : MensulaLayout.calcularDoble(ajustes, segunda, lado, hueco).piezas();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * La mensula sin girar, para la vista previa de la ventana: en bloques, con X del poste hacia la
     * via, Y hacia arriba y el plano de la mensula en Z = 0,5.
     */
    public static List<BakedQuad> vistaPrevia(Ajustes ajustes) {
        return hornear(MensulaLayout.calcular(ajustes, 0f).piezas(), new Matrix4f().scale(1f / 16f));
    }

    /** Las dos de una doble (para cambiar una de ellas en la ventana), igual que {@link #vistaPrevia}. */
    public static List<BakedQuad> vistaPrevia(Ajustes editada, MensulaDoble.Pareja pareja) {
        Ajustes[] par = pareja.con(editada);
        return hornear(piezas(par[0], par[1], pareja.lado(), 0f), new Matrix4f().scale(1f / 16f));
    }

    private static List<BakedQuad> hornear(List<Colocacion> piezas, Matrix4f mundo) {
        return hornear(piezas, mundo::transformPosition);
    }

    /** Las piezas en quads; {@code aBloque} lleva cada vertice de px de la mensula a donde se dibuja. */
    static List<BakedQuad> hornear(List<Colocacion> piezas, Consumer<Vector3f> aBloque) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Colocacion c : piezas) {
            // bloques de la pieza -> px de la pieza -> px de la mensula -> bloque final
            Pieza.get(c.pieza()).bake(c.elementos(), c.variacion(), p -> {
                c.aMensula(p.mul(16f));
                aBloque.accept(p);
            }, quads);
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return original.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return original.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return original.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}

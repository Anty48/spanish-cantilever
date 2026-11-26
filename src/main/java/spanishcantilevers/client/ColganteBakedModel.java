package spanishcantilevers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import spanishcantilevers.block.MensulaColganteBlock;
import spanishcantilevers.geometry.AjustesColgante;
import spanishcantilevers.geometry.Colocacion;
import spanishcantilevers.geometry.MensulaLayout;

/**
 * Modelo de la mensula colgante: monta las piezas de Blockbench segun MensulaLayout.calcularColgante y las
 * gira con el mismo calculo que PNW usa para los puntos de enganche ({@link MensulaColganteBlock#aBloque}).
 * La viga de encima (su grosor) se lee del mundo.
 */
public class ColganteBakedModel implements BakedModel {
    private static final ModelProperty<AjustesColgante> AJUSTES = new ModelProperty<>();
    private static final ModelProperty<MensulaColganteBlock.Viga> VIGA = new ModelProperty<>();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    /** Bajo un bloque solido, si no se sabe otra cosa. */
    private static final MensulaColganteBlock.Viga SIN_VIGA = new MensulaColganteBlock.Viga(16f, 32f, null);

    private final BakedModel original;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(BlockState state, AjustesColgante ajustes, float abajo, float arriba) {
    }

    public ColganteBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (state.getBlock() instanceof MensulaColganteBlock) {
            MensulaColganteBlock.Viga viga = MensulaColganteBlock.viga(level, pos);
            return ModelData.builder()
                    .with(AJUSTES, MensulaColganteBlock.ajustes(level, pos))
                    .with(VIGA, viga == null ? SIN_VIGA : viga)
                    .build();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        // sin capa es el agrietado de cuando se pica el bloque: las catenarias no lo llevan
        if (side != null || renderType == null || state == null || !(state.getBlock() instanceof MensulaColganteBlock block)) {
            return List.of();
        }
        AjustesColgante ajustes = data.get(AJUSTES);
        MensulaColganteBlock.Viga viga = data.get(VIGA);
        if (viga == null) {
            viga = SIN_VIGA;
        }
        return cache.computeIfAbsent(new Key(state, ajustes == null ? AjustesColgante.DEFECTO : ajustes, viga.abajo(), viga.arriba()),
                k -> MensulaBakedModel.hornear(MensulaLayout.calcularColgante(k.ajustes(), k.abajo(), k.arriba()).piezas(), p -> {
                    Vec3 b = block.aBloque(k.state(), p.x(), p.y(), p.z());
                    p.set((float) b.x, (float) b.y, (float) b.z);
                }));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * La colgante sin girar, para la vista previa de la ventana, con un trozo de su viga de referencia: en
     * bloques, con X hacia el hilo, Y hacia arriba y su plano en Z = 0,5.
     */
    public static List<BakedQuad> vistaPrevia(AjustesColgante ajustes, MensulaColganteBlock.Viga viga) {
        MensulaLayout.Resultado r = MensulaLayout.calcularColgante(ajustes, viga.abajo(), viga.arriba());
        List<Colocacion> piezas = new ArrayList<>(r.piezas());
        float x0 = MensulaLayout.CENTRO_COLGANTE - 16f * ajustes.anchura() - 10f;
        piezas.addAll(MensulaLayout.vigaReferencia(x0, MensulaLayout.CENTRO_COLGANTE + 10f, viga.abajo(), viga.arriba()));
        Matrix4f escala = new Matrix4f().scale(1f / 16f);
        return MensulaBakedModel.hornear(piezas, escala::transformPosition);
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

package spanishcantilevers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import spanishcantilevers.block.SoporteTunelBlock;
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

/**
 * Modelo del soporte de tunel: la pieza de Blockbench de su version, tamano y posicion
 * ({@link SoporteTunelBlock#pieza}), con su tamano general, girada con la misma cuenta que PNW usa para los puntos de enganche
 * ({@link SoporteTunelBlock#aBloque}), asi el perfil y el cable caen siempre en la pinza.
 */
public class SoporteTunelBakedModel implements BakedModel {
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    /** El de pared se acerca (px) a la cara de un poste fino: depende del poste, se lee del mundo. */
    private static final ModelProperty<Float> HUECO = new ModelProperty<>();

    /** Cubo de la varilla que sube al techo ("ceeling joint") en los de techo. */
    private static final int VARILLA = 0;
    /** Altura (px) a partir de la cual los vertices de la varilla se quedan en su sitio (la punta del techo). */
    private static final float MITAD_VARILLA = 12f;

    private final BakedModel original;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(BlockState state, float hueco) {
    }

    public SoporteTunelBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                           @NotNull ModelData modelData) {
        if (state.getBlock() instanceof SoporteTunelBlock bloque) {
            return ModelData.builder().with(HUECO, bloque.hueco(level, pos, state)).build();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        // sin capa es el agrietado de cuando se pica el bloque: las catenarias no lo llevan
        if (side != null || renderType == null || state == null || !(state.getBlock() instanceof SoporteTunelBlock)) {
            return List.of();
        }
        Float hueco = data.get(HUECO);
        return cache.computeIfAbsent(new Key(state, hueco == null ? 0f : hueco), k -> hornear(k.state(), k.hueco()));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * La pieza en coordenadas del bloque (0..1). Con altura, en el de techo la varilla se alarga hacia
     * abajo (su punta de arriba no se mueve) y todo lo demas baja con ella; el de pared baja entero.
     * {@code hueco}: px que el de pared se acerca a su poste.
     */
    public static List<BakedQuad> hornear(BlockState state, float hueco) {
        SoporteTunelBlock bloque = (SoporteTunelBlock) state.getBlock();
        float bajada = SoporteTunelBlock.bajada(state);
        SoporteTunelBlock.PiezaTunel datos = SoporteTunelBlock.pieza(state);
        Pieza pieza = Pieza.get(datos.id());
        List<BakedQuad> quads = new ArrayList<>();
        if (!datos.colgada()) {
            pieza.bake(null, 0, p -> colocar(bloque, state, p, false, bajada, hueco), quads);
            return quads;
        }
        int[] resto = IntStream.range(0, datos.elementos()).filter(i -> i != VARILLA).toArray();
        pieza.bake(new int[]{VARILLA}, 0, p -> colocar(bloque, state, p, p.y * 16 >= MITAD_VARILLA, bajada, hueco), quads);
        pieza.bake(resto, 0, p -> colocar(bloque, state, p, false, bajada, hueco), quads);
        return quads;
    }

    /**
     * Un vertice de la pieza a su sitio: con el tamano general ({@link SoporteTunelBlock#escalar}) y bajado;
     * {@code enTecho}: la punta de arriba de la varilla, que no baja ni crece hacia arriba (se queda en el
     * techo), solo engorda.
     */
    private static void colocar(SoporteTunelBlock bloque, BlockState state, Vector3f p, boolean enTecho, float bajada, float hueco) {
        Vector3f q = SoporteTunelBlock.escalar(state, p.x * 16, p.y * 16, p.z * 16);
        float y = enTecho ? p.y * 16 : q.y - bajada;
        Vec3 v = bloque.aBloque(state, q.x, y, q.z + hueco);
        p.set((float) v.x, (float) v.y, (float) v.z);
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

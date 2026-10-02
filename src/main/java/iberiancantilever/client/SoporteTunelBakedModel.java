package iberiancantilever.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import iberiancantilever.block.SoporteTunelBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Modelo del soporte de tunel: la pieza de Blockbench de su posicion, girada con la misma cuenta que
 * PNW usa para los puntos de enganche ({@link SoporteTunelBlock#aBloque}), asi el perfil y el cable
 * caen siempre en la pinza.
 */
public class SoporteTunelBakedModel implements BakedModel {
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    /** Cubo de la varilla que sube al techo ("ceeling joint") y el resto de la pieza. */
    private static final int VARILLA = 0;
    private static final int[] RESTO = {1, 2, 3, 4, 5, 6, 7};
    /** Altura (px) a partir de la cual los vertices de la varilla se quedan en su sitio (la punta del techo). */
    private static final float MITAD_VARILLA = 12f;

    private final BakedModel original;
    private final Map<BlockState, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public SoporteTunelBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null || state == null || !(state.getBlock() instanceof SoporteTunelBlock)) {
            return List.of();
        }
        return cache.computeIfAbsent(state, SoporteTunelBakedModel::hornear);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * La pieza de su posicion en coordenadas del bloque (0..1). Con altura, la varilla del techo se
     * alarga hacia abajo (su punta de arriba no se mueve) y todo lo demas baja con ella.
     */
    public static List<BakedQuad> hornear(BlockState state) {
        SoporteTunelBlock bloque = (SoporteTunelBlock) state.getBlock();
        float bajada = SoporteTunelBlock.bajada(state);
        Pieza pieza = Pieza.get(state.getValue(SoporteTunelBlock.POSICION).pieza());
        List<BakedQuad> quads = new ArrayList<>();
        pieza.bake(new int[]{VARILLA}, 0, p -> colocar(bloque, state, p, p.y * 16 >= MITAD_VARILLA ? p.y * 16 : p.y * 16 - bajada), quads);
        pieza.bake(RESTO, 0, p -> colocar(bloque, state, p, p.y * 16 - bajada), quads);
        return quads;
    }

    private static void colocar(SoporteTunelBlock bloque, BlockState state, Vector3f p, float yPx) {
        Vec3 v = bloque.aBloque(state, p.x * 16, yPx, p.z * 16);
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

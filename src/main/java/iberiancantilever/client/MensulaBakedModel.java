package iberiancantilever.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
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
import iberiancantilever.block.MensulaBlock;
import iberiancantilever.geometry.Ajustes;
import iberiancantilever.geometry.Colocacion;
import iberiancantilever.geometry.MensulaLayout;

/**
 * Modelo de la mensula: monta las piezas de Blockbench segun MensulaLayout y lo gira igual que
 * PNW gira su mensula (16 direcciones). El hueco hasta la cara del poste se lee del mundo.
 */
public class MensulaBakedModel implements BakedModel {
    private static final ModelProperty<Float> HUECO = new ModelProperty<>();
    private static final ModelProperty<Ajustes> AJUSTES = new ModelProperty<>();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel original;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(BlockState state, Ajustes ajustes, float hueco) {
    }

    public MensulaBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (state.getBlock() instanceof MensulaBlock block) {
            return ModelData.builder()
                    .with(HUECO, block.huecoPoste(level, pos, state))
                    .with(AJUSTES, MensulaBlock.ajustes(level, pos))
                    .build();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null || state == null || !(state.getBlock() instanceof MensulaBlock)) {
            return List.of();
        }
        Float hueco = data.get(HUECO);
        Ajustes ajustes = data.get(AJUSTES);
        return cache.computeIfAbsent(new Key(state, ajustes == null ? Ajustes.DEFECTO : ajustes, hueco == null ? 2f : hueco),
                k -> hornear(MensulaLayout.calcular(k.ajustes(), k.hueco()), mensulaABloque(k.state())));
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
        return hornear(MensulaLayout.calcular(ajustes, 0f), new Matrix4f().scale(1f / 16f));
    }

    private static List<BakedQuad> hornear(MensulaLayout.Resultado layout, Matrix4f mundo) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Colocacion c : layout.piezas()) {
            // bloques de la pieza -> px de la pieza -> px de la mensula -> bloque final
            Pieza.get(c.pieza()).bake(c.elementos(), c.variacion(), p -> {
                c.aMensula(p.mul(16f));
                mundo.transformPosition(p);
            }, quads);
        }
        return quads;
    }

    /**
     * Pixeles de la mensula -> coordenadas del bloque. Misma secuencia que CantileverModel de PNW:
     * giro fino alrededor del centro del poste, desplazamiento en la diagonal de 45 grados y giro
     * segun FACING.
     */
    static Matrix4f mensulaABloque(BlockState state) {
        MensulaBlock block = (MensulaBlock) state.getBlock();
        Direction facing = state.getValue(AbstractRotatableBlock.FACING);
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
        if (state.getValue(AbstractRotatableBlock.ROTATION) >= 3) {
            m.translate(0, 0, 1);
        }
        m.translate(px, 0, pz)
                .rotateY((float) Math.toRadians(fino))
                .translate(-px, 0, -pz)
                .scale(1f / 16f);
        return m;
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

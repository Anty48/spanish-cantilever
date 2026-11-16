package spanishcantilevers.client;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel.ModelType;
import de.mrjulsen.mcdragonlib.client.model.mesh.Face;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import spanishcantilevers.SpanishCantilevers;

/**
 * Modelo de los remates con aisladores: el girable de PNW (16 direcciones), pero con el cristal de los
 * aisladores en la capa translucida. El de PNW pasa todas las caras a la capa solida (DragonLib no lee el
 * render_type del JSON) y el cristal, blanco semitransparente, no se veia bien.
 */
public class ModeloRemate extends BasicRotatableBlockModel {
    private static final ResourceLocation CRISTAL = new ResourceLocation(SpanishCantilevers.MOD_ID, "block/insulator_glass");

    @Override
    protected Mesh getBaseMesh(ModelType type, BakedModel original, BlockState state, RandomSource random, ModelContext context) {
        BasicMesh mesh = new BasicMesh();
        Direction[] lados = new Direction[Direction.values().length + 1];
        System.arraycopy(Direction.values(), 0, lados, 0, Direction.values().length);
        for (Direction lado : lados) {
            for (BakedQuad quad : original.getQuads(state, lado, random)) {
                Face cara = new Face(quad, lado);
                if (CRISTAL.equals(quad.getSprite().contents().name())) {
                    cara.setRenderType(RenderType.translucent());
                }
                mesh.addFace(cara);
            }
        }
        return mesh;
    }
}

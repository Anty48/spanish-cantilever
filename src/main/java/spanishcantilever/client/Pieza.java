package spanishcantilever.client;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import spanishcantilever.SpanishCantilever;

/**
 * Una pieza de Blockbench (models/block/piezas/&lt;id&gt;.json) lista para hornear cubo a cubo con
 * una matriz cualquiera (giros libres, estirados...). Los indices de cubo coinciden con el orden
 * del .json, que es el que usa PiezasDatos.
 */
public final class Pieza {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final Map<String, Pieza> CACHE = new ConcurrentHashMap<>();
    private static final int STRIDE = 8; // DefaultVertexFormat.BLOCK en enteros

    private final ResourceLocation location;
    private final List<BlockElement> elements;
    private final Map<String, ResourceLocation> textures;

    private Pieza(ResourceLocation location, List<BlockElement> elements, Map<String, ResourceLocation> textures) {
        this.location = location;
        this.elements = elements;
        this.textures = textures;
    }

    public static Pieza get(String id) {
        return CACHE.computeIfAbsent(id, Pieza::load);
    }

    /** Al recargar recursos (F3+T) se vuelven a leer los .json. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static Pieza load(String id) {
        ResourceLocation file = new ResourceLocation(SpanishCantilever.MOD_ID, "models/block/piezas/" + id + ".json");
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, ResourceLocation> textures = new HashMap<>();
            if (json.has("textures")) {
                for (var e : json.getAsJsonObject("textures").entrySet()) {
                    textures.put(e.getKey(), new ResourceLocation(e.getValue().getAsString()));
                }
            }
            BlockModel model = BlockModel.fromString(json.toString());
            return new Pieza(new ResourceLocation(SpanishCantilever.MOD_ID, "block/piezas/" + id), model.getElements(), textures);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la pieza " + file, e);
        }
    }

    public TextureAtlasSprite sprite(String ref) {
        String key = ref.startsWith("#") ? ref.substring(1) : ref;
        ResourceLocation tex = textures.getOrDefault(key, textures.get("particle"));
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
    }

    /**
     * Hornea los cubos indicados (null = todos) y los transforma con {@code toBlock}, que va de
     * coordenadas de la pieza en BLOQUES (lo que da FaceBakery) a coordenadas finales del bloque.
     */
    public void bake(int[] indices, Matrix4f toBlock, List<BakedQuad> out) {
        Matrix3f normalMatrix = new Matrix3f(toBlock).invert().transpose();
        int count = indices == null ? elements.size() : indices.length;
        for (int k = 0; k < count; k++) {
            BlockElement element = elements.get(indices == null ? k : indices[k]);
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                BakedQuad quad = BAKERY.bakeQuad(element.from, element.to, face, sprite(face.texture), entry.getKey(),
                        BlockModelRotation.X0_Y0, element.rotation, element.shade, location);
                out.add(transform(quad, toBlock, normalMatrix));
            }
        }
    }

    private static BakedQuad transform(BakedQuad quad, Matrix4f m, Matrix3f normalMatrix) {
        int[] v = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            Vector3f p = new Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]));
            m.transformPosition(p);
            v[o] = Float.floatToRawIntBits(p.x);
            v[o + 1] = Float.floatToRawIntBits(p.y);
            v[o + 2] = Float.floatToRawIntBits(p.z);
        }
        Vector3f n = new Vector3f(quad.getDirection().getStepX(), quad.getDirection().getStepY(), quad.getDirection().getStepZ());
        normalMatrix.transform(n).normalize();
        int packed = ((int) (n.x * 127) & 0xFF) | (((int) (n.y * 127) & 0xFF) << 8) | (((int) (n.z * 127) & 0xFF) << 16);
        for (int i = 0; i < 4; i++) {
            v[i * STRIDE + 7] = packed;
        }
        Direction dir = Direction.getNearest(n.x, n.y, n.z);
        return new BakedQuad(v, quad.getTintIndex(), dir, quad.getSprite(), quad.isShade());
    }
}

package spanishcantilevers.client;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.joml.Vector3f;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import spanishcantilevers.SpanishCantilevers;

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
        ResourceLocation file = new ResourceLocation(SpanishCantilevers.MOD_ID, "models/block/piezas/" + id + ".json");
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, ResourceLocation> textures = new HashMap<>();
            if (json.has("textures")) {
                for (var e : json.getAsJsonObject("textures").entrySet()) {
                    textures.put(e.getKey(), new ResourceLocation(e.getValue().getAsString()));
                }
            }
            BlockModel model = BlockModel.fromString(json.toString());
            return new Pieza(new ResourceLocation(SpanishCantilevers.MOD_ID, "block/piezas/" + id), model.getElements(), textures);
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
     * Hornea los cubos indicados (null = todos) y mueve cada vertice con {@code toBlock}, que va de
     * coordenadas de la pieza en BLOQUES (lo que da FaceBakery) a coordenadas finales del bloque.
     * Puede ser cualquier deformacion que deje las caras planas (giros, estirados, ingletes...).
     */
    public void bake(int[] indices, int variacion, Consumer<Vector3f> toBlock, List<BakedQuad> out) {
        int count = indices == null ? elements.size() : indices.length;
        Random rnd = new Random(variacion);
        float desplazamientoU = rnd.nextFloat();
        float desplazamientoV = rnd.nextFloat();
        boolean volteo = rnd.nextBoolean();
        for (int k = 0; k < count; k++) {
            BlockElement element = elements.get(indices == null ? k : indices[k]);
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                if (variacion != 0 && entry.getKey().getAxis() != Direction.Axis.X) {
                    face = variar(face, desplazamientoU, desplazamientoV, volteo);
                }
                BakedQuad quad = BAKERY.bakeQuad(element.from, element.to, face, sprite(face.texture), entry.getKey(),
                        BlockModelRotation.X0_Y0, element.rotation, element.shade, location);
                BakedQuad moved = transform(quad, toBlock);
                if (moved != null) {
                    out.add(moved);
                }
            }
        }
    }

    /**
     * Desplaza los UV de una cara (y a veces la voltea) sin salirse de la textura de 16 px: los
     * tramos repetidos de la barra cogen trozos distintos de la textura.
     */
    private static BlockElementFace variar(BlockElementFace face, float desplazamientoU, float desplazamientoV, boolean volteo) {
        float[] uv = face.uv.uvs.clone();
        for (int i = 0; i < 2; i++) {
            float min = Math.min(uv[i], uv[i + 2]);
            float max = Math.max(uv[i], uv[i + 2]);
            float s = Math.round(-min + (i == 0 ? desplazamientoU : desplazamientoV) * (16f - max + min));
            uv[i] += s;
            uv[i + 2] += s;
        }
        if (volteo) {
            float t = uv[0];
            uv[0] = uv[2];
            uv[2] = t;
        }
        return new BlockElementFace(face.cullForDirection, face.tintIndex, face.texture, new BlockFaceUV(uv, face.uv.rotation));
    }

    /** null si la cara queda sin area (cubos planos de Blockbench): no se ve y no tiene normal. */
    private static BakedQuad transform(BakedQuad quad, Consumer<Vector3f> toBlock) {
        int[] v = quad.getVertices().clone();
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            p[i] = new Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]));
            toBlock.accept(p[i]);
            v[o] = Float.floatToRawIntBits(p[i].x);
            v[o + 1] = Float.floatToRawIntBits(p[i].y);
            v[o + 2] = Float.floatToRawIntBits(p[i].z);
        }
        // misma normal que FaceBakery.calculateFacing: (v2 - v1) x (v0 - v1), con la diagonal si un lado es nulo
        Vector3f n = new Vector3f(p[2]).sub(p[1]).cross(new Vector3f(p[0]).sub(p[1]));
        if (n.lengthSquared() < 1e-12f) {
            n = new Vector3f(p[2]).sub(p[0]).cross(new Vector3f(p[3]).sub(p[1]));
            if (n.lengthSquared() < 1e-12f) {
                return null;
            }
        }
        n.normalize();
        int packed = ((int) (n.x * 127) & 0xFF) | (((int) (n.y * 127) & 0xFF) << 8) | (((int) (n.z * 127) & 0xFF) << 16);
        for (int i = 0; i < 4; i++) {
            v[i * STRIDE + 7] = packed;
        }
        Direction dir = Direction.getNearest(n.x, n.y, n.z);
        return new BakedQuad(v, quad.getTintIndex(), dir, quad.getSprite(), quad.isShade());
    }
}

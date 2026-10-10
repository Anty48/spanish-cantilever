package spanishcantilevers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import spanishcantilevers.SpanishCantilevers;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.cable.CatenariaRigida;
import spanishcantilevers.cable.ModCables;
import spanishcantilevers.geometry.PiezasDatos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dibuja la catenaria rigida: la pieza "perfil_rigido" de Blockbench repetida de pinza a pinza en
 * cada tramo de {@link CatenariaRigida} (PNW solo dibuja un hilo fino que queda dentro). Si en un
 * soporte se juntan dos tramos que no van rectos, cada tramo se parte en trozos rectos un poco girados
 * entre si siguiendo una curva suave (Catmull-Rom) que pasa por las pinzas: el perfil hace la curva.
 * Donde llega una catenaria normal (transicion), el hilo de contacto cuenta como el tramo siguiente:
 * la punta del perfil se tuerce hacia el, sin codo.
 */
@Mod.EventBusSubscriber(modid = SpanishCantilevers.MOD_ID, value = Dist.CLIENT)
public final class PerfilRigidoRenderer {
    /** Hasta donde se dibuja (bloques desde la camara). */
    private static final double DISTANCIA = 160;
    /** Largo de un tramo repetido de la pieza (px) y de cada remate. */
    private static final float TRAMO = 5f;
    private static final float REMATE = 1f;
    /** Largo aproximado (bloques) de cada trozo recto de una curva. */
    private static final double TROZO_CURVA = 1.0;
    private static final int TROZOS_MAX = 32;
    /** Por debajo de este giro (grados) el tramo se dibuja recto, de una pieza. */
    private static final double GIRO_MIN = 0.5;
    /** Si en un soporte el perfil gira mas que esto, es una esquina: no se curva. */
    private static final double GIRO_MAX = 60;

    /**
     * Un tramo y, si los hay, el punto anterior y el siguiente de la linea (para curvarlo). {@code escala}: el
     * tamano general del soporte del primer clic (el nodo A), que es el que manda en todo el tramo.
     */
    private record Tramo(@Nullable Vector3d antes, Vector3d a, Vector3d b, @Nullable Vector3d despues, float escala) {
    }

    /** Un tramo rigido del grafo: sus dos nodos y donde engancha en cada uno. */
    private record Extremos(UUID nodoA, Vector3d a, UUID nodoB, Vector3d b) {
        Vector3d en(UUID nodo) {
            return nodo.equals(nodoA) ? a : b;
        }

        Vector3d otro(UUID nodo) {
            return nodo.equals(nodoA) ? b : a;
        }
    }

    private static final Map<Tramo, List<BakedQuad>> CACHE = new ConcurrentHashMap<>();

    private PerfilRigidoRenderer() {
    }

    /** Al recargar recursos (F3+T) se vuelven a montar. */
    public static void limpiar() {
        CACHE.clear();
    }

    @SubscribeEvent
    public static void alDibujar(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        WireGraphClient grafo = WireGraphManager.getClient(level, WiresApi.PAW_CATENARY_WIRES);
        if (grafo == null) {
            return;
        }
        // todos los tramos rigidos y, por nodo, los que llegan a el (tambien los hilos de contacto de las
        // catenarias normales, que no se dibujan aqui pero guian la punta del perfil en las transiciones)
        List<Extremos> tramos = new ArrayList<>();
        Map<UUID, List<Extremos>> porNodo = new HashMap<>();
        Map<UUID, Float> escalas = new HashMap<>();
        for (WireEdge edge : grafo.getEdges()) {
            boolean rigido = edge.getType() == ModCables.RIGIDA;
            if (!rigido && !(edge.getType() instanceof CatenaryWireType)) {
                continue;
            }
            WireNode na = grafo.getNode(edge.getNodeAId());
            WireNode nb = grafo.getNode(edge.getNodeBId());
            if (na == null || nb == null) {
                continue;
            }
            Extremos e = new Extremos(na.getId(), CatenariaRigida.enganche(na, edge.getWireConnectionData().connectorA()),
                    nb.getId(), CatenariaRigida.enganche(nb, edge.getWireConnectionData().connectorB()));
            if (rigido) {
                tramos.add(e);
                escalas.put(na.getId(), escala(level, na));
            }
            porNodo.computeIfAbsent(e.nodoA(), k -> new ArrayList<>()).add(e);
            porNodo.computeIfAbsent(e.nodoB(), k -> new ArrayList<>()).add(e);
        }
        if (tramos.isEmpty()) {
            return;
        }
        Vec3 camara = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
        boolean algo = false;
        for (Extremos e : tramos) {
            Vector3d a = e.a();
            Vector3d b = e.b();
            if (camara.distanceTo(new Vec3(a.x, a.y, a.z)) > DISTANCIA && camara.distanceTo(new Vec3(b.x, b.y, b.z)) > DISTANCIA) {
                continue;
            }
            Tramo t = new Tramo(vecino(porNodo, e, e.nodoA()), a, b, vecino(porNodo, e, e.nodoB()), escalas.getOrDefault(e.nodoA(), 1f));
            List<BakedQuad> quads = CACHE.computeIfAbsent(t, PerfilRigidoRenderer::montar);
            Vector3d medio = new Vector3d(a).add(b).mul(0.5);
            int luz = LevelRenderer.getLightColor(level, BlockPos.containing(medio.x, medio.y, medio.z));
            pose.pushPose();
            pose.translate(a.x - camara.x, a.y - camara.y, a.z - camara.z);
            for (BakedQuad q : quads) {
                vc.putBulkData(pose.last(), q, 1f, 1f, 1f, luz, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
            algo = true;
        }
        if (algo) {
            buffer.endBatch(Sheets.cutoutBlockSheet());
        }
    }

    /** Tamano general del soporte de tunel de un nodo (1 si no es uno de ellos o no esta cargado). */
    private static float escala(ClientLevel level, WireNode nodo) {
        if (nodo.getData() instanceof BlockConnectorNodeData datos) {
            BlockState state = level.getBlockState(datos.getPos());
            if (state.getBlock() instanceof SoporteTunelBlock) {
                return SoporteTunelBlock.escala(state);
            }
        }
        return 1f;
    }

    /**
     * El otro extremo del tramo que sigue a {@code e} por el nodo dado (el que sale mas recto, sea perfil
     * o hilo de contacto de una transicion), o null si el perfil acaba ahi o hace esquina.
     */
    @Nullable
    private static Vector3d vecino(Map<UUID, List<Extremos>> porNodo, Extremos e, UUID nodo) {
        Vector3d aqui = e.en(nodo);
        Vector3d entrada = new Vector3d(aqui).sub(e.otro(nodo));
        Vector3d mejor = null;
        double mejorGiro = GIRO_MAX;
        for (Extremos otro : porNodo.getOrDefault(nodo, List.of())) {
            if (otro == e) {
                continue;
            }
            Vector3d salida = new Vector3d(otro.otro(nodo)).sub(aqui);
            double giro = Math.toDegrees(entrada.angle(salida));
            if (giro < mejorGiro) {
                mejorGiro = giro;
                mejor = otro.otro(nodo);
            }
        }
        return mejor;
    }

    /** El perfil de a a b (en bloques, relativo a a): recto, o en trozos siguiendo la curva. */
    private static List<BakedQuad> montar(Tramo t) {
        // sin vecino, la curva sale recta hacia el otro extremo
        Vector3d antes = t.antes() != null ? t.antes() : new Vector3d(t.a()).mul(2).sub(t.b());
        Vector3d despues = t.despues() != null ? t.despues() : new Vector3d(t.b()).mul(2).sub(t.a());
        double largo = t.a().distance(t.b());
        // cuanto gira la curva de una pinza a la otra (tangente de entrada contra la de salida)
        Vector3d tanA = new Vector3d(t.b()).sub(antes);
        Vector3d tanB = new Vector3d(despues).sub(t.a());
        double giro = Math.toDegrees(tanA.angle(tanB));
        int trozos = giro < GIRO_MIN ? 1 : Math.max(2, Math.min(TROZOS_MAX, (int) Math.ceil(largo / TROZO_CURVA)));

        List<Vector3f> puntos = new ArrayList<>(trozos + 1);
        for (int i = 0; i <= trozos; i++) {
            Vector3d p = i == 0 ? t.a() : i == trozos ? t.b() : catmullRom(antes, t.a(), t.b(), despues, i / (double) trozos);
            puntos.add(new Vector3f((float) (p.x - t.a().x), (float) (p.y - t.a().y), (float) (p.z - t.a().z)));
        }
        List<BakedQuad> quads = new ArrayList<>();
        Pieza pieza = Pieza.get(PiezasDatos.PERFIL_RIGIDO.ID);
        int[] semilla = {1};
        for (int i = 0; i < trozos; i++) {
            trozo(pieza, puntos.get(i), puntos.get(i + 1), i == 0, i == trozos - 1, t.escala(), semilla, quads);
        }
        return quads;
    }

    /**
     * Un trozo recto del perfil de {@code desde} a {@code hasta} (bloques): tramos repetidos y, si toca
     * una pinza, su remate. El eje del perfil pasa por los dos puntos. Con {@code escala} la pieza entera es
     * mas grande (seccion, tramos y remates): en px de la pieza el trozo mide menos.
     */
    private static void trozo(Pieza pieza, Vector3f desde, Vector3f hasta, boolean remate1, boolean remate2,
                              float escala, int[] semilla, List<BakedQuad> quads) {
        Vector3f x = new Vector3f(hasta).sub(desde);
        float largo = x.length() * 16f / escala;
        if (largo < 1e-3f) {
            return;
        }
        x.normalize();
        Vector3f z = new Vector3f(x).cross(0, 1, 0);
        if (z.lengthSquared() < 1e-6f) {
            z.set(0, 0, 1);
        }
        z.normalize();
        Vector3f y = new Vector3f(z).cross(x).normalize();
        // px del perfil (eje X a lo largo) -> bloques, relativo a la pinza del tramo
        Matrix4f base = new Matrix4f(
                x.x, x.y, x.z, 0,
                y.x, y.y, y.z, 0,
                z.x, z.y, z.z, 0,
                0, 0, 0, 1).scaleLocal(escala / 16f).translateLocal(desde);
        float[] min = PiezasDatos.PERFIL_RIGIDO.MIN;
        float[] max = PiezasDatos.PERFIL_RIGIDO.MAX;
        Matrix4f centrado = new Matrix4f(base).translate(0, -(min[1] + max[1]) / 2f, -(min[2] + max[2]) / 2f);

        float inicio = remate1 ? REMATE : 0f;
        float medio = largo - inicio - (remate2 ? REMATE : 0f);
        if (medio <= 0.5f) {
            pieza.bake(null, 0, transformar(new Matrix4f(centrado).scale(largo / max[0], 1, 1)), quads);
            return;
        }
        if (remate1) {
            pieza.bake(PiezasDatos.PERFIL_RIGIDO.GRUPO_ATTACH_SEGMENT_END_1, 0, transformar(centrado), quads);
        }
        int n = Math.max(1, Math.round(medio / TRAMO));
        float tramo = medio / n;
        for (int i = 0; i < n; i++) {
            pieza.bake(PiezasDatos.PERFIL_RIGIDO.GRUPO_DUPLICATING_SEGMENT, semilla[0]++, transformar(new Matrix4f(centrado)
                    .translate(inicio + i * tramo, 0, 0).scale(tramo / TRAMO, 1, 1).translate(-REMATE, 0, 0)), quads);
        }
        if (remate2) {
            pieza.bake(PiezasDatos.PERFIL_RIGIDO.GRUPO_ATTACH_SEGMENT_END_2, 0,
                    transformar(new Matrix4f(centrado).translate(largo - max[0], 0, 0)), quads);
        }
    }

    /** Catmull-Rom centripeta entre p1 y p2 (u de 0 a 1): no hace bucles ni se pasa en las curvas cerradas. */
    private static Vector3d catmullRom(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, double u) {
        double t0 = 0;
        double t1 = t0 + Math.max(1e-4, Math.sqrt(p0.distance(p1)));
        double t2 = t1 + Math.max(1e-4, Math.sqrt(p1.distance(p2)));
        double t3 = t2 + Math.max(1e-4, Math.sqrt(p2.distance(p3)));
        double t = t1 + (t2 - t1) * u;
        Vector3d a1 = mezcla(p0, p1, t0, t1, t);
        Vector3d a2 = mezcla(p1, p2, t1, t2, t);
        Vector3d a3 = mezcla(p2, p3, t2, t3, t);
        Vector3d b1 = mezcla(a1, a2, t0, t2, t);
        Vector3d b2 = mezcla(a2, a3, t1, t3, t);
        return mezcla(b1, b2, t1, t2, t);
    }

    private static Vector3d mezcla(Vector3d p, Vector3d q, double tp, double tq, double t) {
        double f = (t - tp) / (tq - tp);
        return new Vector3d(p).mul(1 - f).add(new Vector3d(q).mul(f));
    }

    /** Bloques de la pieza (lo que da FaceBakery) -> px de la pieza -> bloques relativos a la pinza. */
    private static java.util.function.Consumer<Vector3f> transformar(Matrix4f m) {
        return p -> m.transformPosition(p.mul(16f));
    }
}

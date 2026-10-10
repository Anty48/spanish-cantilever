package spanishcantilevers.dev;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.ModPostes;
import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.block.ModoZigzag;
import spanishcantilevers.block.PosicionTunel;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.block.TamanoTunel;
import spanishcantilevers.block.VersionTunel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Escenas de escaparate para las capturas de la documentacion (y para ensenar el mod): cada cosa con
 * todas sus variantes a la vista, bien colocada y con su via. Todo se monta a partir de un origen; las
 * escenas van separadas por los desplazamientos de abajo, que la prueba automatica usa para sus camaras.
 * Se monta con {@code /mensula escaparate} delante del jugador o desde {@link AutoTest}.
 */
public final class Escaparate {
    /** Linea de portada: postes espanoles, mensulas anchas en zigzag con tirante diagonal y catenaria. */
    public static final int PORTADA_X = 0, PORTADA_Z = 0;
    /** Fila de mensulas de todas las anchuras (1,5 a 6,5) en postes espanoles cuadrados. */
    public static final int ANCHURAS_X = 0, ANCHURAS_Z = 50;
    /** Los postes espanoles: cuatro formas, sin oxidar (oxidados ya son los de PNW). */
    public static final int POSTES_X = 0, POSTES_Z = 100;
    /** Pantografo de PNW y el espanol, en un anden. */
    public static final int PANTOGRAFOS_X = 60, PANTOGRAFOS_Z = 100;
    /** Vitrinas de los soportes de tunel bajo una losa: techo normal, techo grande, tamanos y de pared. */
    public static final int VITRINA_X = 0, VITRINA_Z = 150;
    /** X (desde VITRINA_X) del centro de cada vitrina: techo normal, techo grande, tamano general y pared. */
    public static final int[] VITRINAS = {4, 20, 36, 53};
    /** Ejes de via de las vitrinas de pared (desde VITRINA_X): el corto y el largo, cada uno con su pared al oeste. */
    public static final int[] VIAS_PARED = {50, 60};
    /**
     * Tirantes diagonales: una fila con las anchuras 3,5 a 6,5 (tirante 3 bloques por encima) y, 50 bloques
     * al sur, otra de anchura 4,5 con el tirante a 2, 3, 4 y 5 bloques.
     */
    public static final int TIRANTES_X = 0, TIRANTES_Z = 200, TIRANTES_ALTURAS_Z = 250;
    /** X (desde TIRANTES_X) de los postes de cada fila de tirantes. */
    public static final int[] TIRANTES_ANCHURAS = {0, 8, 17, 27};
    public static final int[] TIRANTES_ALTURAS = {0, 9, 18, 27};
    /** Vitrina de la curva: soportes de techo siguiendo un arco, con el perfil rigido curvandose por ellos. */
    public static final int CURVA_X = 0, CURVA_Z = 350;
    /** Tunel con boca, paredes y boveda, catenaria rigida y la transicion desde la catenaria normal. */
    public static final int TUNEL_X = 100, TUNEL_Z = 0;
    /** Largo del tunel (bloques) y separacion entre sus soportes. */
    private static final int LARGO_TUNEL = 44;
    private static final int VANO = 12;
    private static final int VANO_TUNEL = 6;

    private Escaparate() {
    }

    /** Monta todas las escenas a partir de {@code origen} (a ras de suelo). */
    public static void montar(ServerPlayer player, BlockPos origen) {
        portada(player, origen.offset(PORTADA_X, 0, PORTADA_Z));
        anchuras(player, origen.offset(ANCHURAS_X, 0, ANCHURAS_Z));
        postes(player.serverLevel(), origen.offset(POSTES_X, 0, POSTES_Z));
        pantografos(player.serverLevel(), origen.offset(PANTOGRAFOS_X, 0, PANTOGRAFOS_Z));
        vitrinas(player, origen.offset(VITRINA_X, 0, VITRINA_Z));
        tunel(player, origen.offset(TUNEL_X, 0, TUNEL_Z));
        tirantes(player, origen.offset(TIRANTES_X, 0, TIRANTES_Z), origen.offset(TIRANTES_X, 0, TIRANTES_ALTURAS_Z));
        curva(player, origen.offset(CURVA_X, 0, CURVA_Z));
    }

    /**
     * Seis soportes de techo bajo una losa de hormigon blanco, cada 5 bloques hacia el norte y cada vez mas
     * hacia el este (un arco), con el perfil rigido tendido: se curva suavemente al pasar por cada pinza.
     */
    private static void curva(ServerPlayer player, BlockPos o) {
        ServerLevel level = player.serverLevel();
        int y = LineaPrueba.ALTURA_TUNEL;
        int[] desvio = {0, 0, 1, 2, 4, 6};
        for (int dx = -4; dx <= 11; dx++) {
            for (int dz = -28; dz <= 3; dz++) {
                level.setBlock(o.offset(dx, y + 2, dz), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
            }
            for (int dy = 0; dy <= y + 1; dy++) {
                level.setBlock(o.offset(dx, dy, -28), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        List<BlockPos> soportes = new ArrayList<>();
        for (int i = 0; i < desvio.length; i++) {
            BlockPos pos = o.offset(desvio[i], y + 1, -i * 5);
            level.setBlock(pos, techo(TamanoTunel.NORMAL, PosicionTunel.CENTRO, 0).aplicar(ModBlocks.SOPORTE_TUNEL.get().defaultBlockState())
                    .setValue(AbstractRotatableBlock.FACING, Direction.SOUTH)
                    .setValue(AbstractRotatableBlock.ROTATION, 1), Block.UPDATE_ALL);
            soportes.add(pos);
        }
        LineaPrueba.tender(player, soportes, () -> new ItemStack(ModBlocks.PERFIL_RIGIDO.get()));
    }

    /**
     * Las dos filas de tirantes diagonales en postes espanoles (plano y cuadrado alternando), cada mensula
     * con su trozo de via: las anchuras con el tirante a 3 bloques y las alturas con anchura 4,5.
     */
    private static void tirantes(ServerPlayer player, BlockPos anchuras, BlockPos alturas) {
        ServerLevel level = player.serverLevel();
        int alto = LineaPrueba.ALTURA_MENSULA + 6;
        ModoZigzag[] modos = {ModoZigzag.MEDIO, ModoZigzag.EXTERIOR, ModoZigzag.MEDIO, ModoZigzag.EXTERIOR};
        for (int i = 0; i < 4; i++) {
            tirante(player, anchuras.offset(TIRANTES_ANCHURAS[i], 0, 0), alto, 3.5f + i, modos[i], 3);
            tirante(player, alturas.offset(TIRANTES_ALTURAS[i], 0, 0), alto, 4.5f, modos[i], 2 + i);
        }
    }

    private static void tirante(ServerPlayer player, BlockPos base, int alto, float anchura, ModoZigzag modo, int sobre) {
        ServerLevel level = player.serverLevel();
        Block poste = ModPostes.POSTES.get(base.getX() % 2 == 0 ? "iberian_flat_lattice_mast" : "iberian_lattice_mast").get();
        BlockPos pos = LineaPrueba.posteConMensula(level, base, poste, alto, anchura, modo);
        LineaPrueba.clics(player, LineaPrueba.bobinaSoporte(), pos, base.above(LineaPrueba.ALTURA_MENSULA + sobre));
        int via = 1 + (int) Math.floor(anchura);
        via(level, base.offset(via, 0, 3), base.offset(via, 0, -3));
    }

    // ------------------------------------------------------------------ escenas

    /**
     * Seis postes espanoles planos hacia el norte, mensulas de anchura 3,5 en zigzag interior / exterior
     * con su cable de soporte, la catenaria de PNW tendida y en cada una un tirante diagonal al ultimo
     * bloque del poste (3 bloques por encima).
     */
    private static void portada(ServerPlayer player, BlockPos o) {
        ServerLevel level = player.serverLevel();
        Block poste = ModPostes.POSTES.get("iberian_flat_lattice_mast").get();
        int alto = LineaPrueba.ALTURA_MENSULA + 4;
        List<BlockPos> mensulas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ModoZigzag modo = i % 2 == 0 ? ModoZigzag.INTERIOR : ModoZigzag.EXTERIOR;
            mensulas.add(LineaPrueba.posteConMensula(level, o.offset(0, 0, -i * VANO), poste, alto, 3.5f, modo));
        }
        via(level, o.offset(4, 0, 14), o.offset(4, 0, -5 * VANO - 14));
        LineaPrueba.tenderCables(player, mensulas);
        for (int i = 0; i < mensulas.size(); i++) {
            LineaPrueba.clics(player, LineaPrueba.bobinaSoporte(), mensulas.get(i), o.offset(0, alto - 1, -i * VANO));
        }
    }

    /** Una mensula (medio) de cada anchura en postes espanoles cuadrados, cada una con su trozo de via. */
    private static void anchuras(ServerPlayer player, BlockPos o) {
        ServerLevel level = player.serverLevel();
        Block poste = ModPostes.POSTES.get("iberian_lattice_mast").get();
        int x = 0;
        for (float anchura = 1.5f; anchura <= 6.5f; anchura += 1f) {
            LineaPrueba.posteConMensula(level, o.offset(x, 0, 0), poste, LineaPrueba.ALTURA_MENSULA + 2, anchura, ModoZigzag.MEDIO);
            int via = x + 1 + (int) Math.floor(anchura);
            via(level, o.offset(via, 0, 3), o.offset(via, 0, -3));
            x += (int) Math.ceil(anchura) + 3;
        }
    }

    /** Los postes espanoles (plano, plano diagonal, cuadrado y H), en fila; oxidados son ya los de PNW. */
    private static void postes(ServerLevel level, BlockPos o) {
        String[] formas = {"iberian_flat_lattice_mast", "iberian_flat_lattice_mast_diagonal", "iberian_lattice_mast", "iberian_h_beam_mast"};
        String[] estados = {""};
        for (int f = 0; f < formas.length; f++) {
            for (int e = 0; e < estados.length; e++) {
                Block poste = ModPostes.POSTES.get(formas[f] + estados[e]).get();
                for (int dy = 0; dy < 5; dy++) {
                    level.setBlock(o.offset(f * 10 + e * 2, dy, 0), LineaPrueba.poste(poste, Direction.EAST, 1), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** El pantografo de PNW y, a su derecha, el espanol, sobre un anden de hormigon. */
    private static void pantografos(ServerLevel level, BlockPos o) {
        for (int dx = -2; dx <= 5; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(o.offset(dx, 0, dz), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        Block pnw = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pantographsandwires", "pantograph"));
        if (pnw != null) {
            level.setBlock(o.offset(0, 1, 0), pnw.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(o.offset(3, 1, 0), ModBlocks.PANTOGRAFO.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Vitrinas de los soportes de tunel bajo una losa de hormigon blanco (el metal oscuro destaca): cada soporte tiene detras su
     * gemelo 6 bloques al norte con el perfil rigido tendido entre los dos y una via debajo. De izquierda a
     * derecha: de techo normal (pinza izquierda, centro, derecha), de techo grande (igual), de techo normal
     * al 100, 125 y 150 % de tamano general, y de pared corto y largo (con su pared al oeste).
     */
    private static void vitrinas(ServerPlayer player, BlockPos o) {
        ServerLevel level = player.serverLevel();
        int y = LineaPrueba.ALTURA_TUNEL;
        // la losa un bloque mas alta que en la prueba (los de techo cuelgan un bloque mas, como en el tunel) y
        // un muro al fondo, para que detras de los perfiles no se vea el horizonte
        for (int dx = -4; dx <= VIAS_PARED[1] + 4; dx++) {
            for (int dz = -10; dz <= 9; dz++) {
                level.setBlock(o.offset(dx, y + 2, dz), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
            }
            for (int dy = 0; dy <= y + 1; dy++) {
                level.setBlock(o.offset(dx, dy, -10), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        PosicionTunel[] pinzas = PosicionTunel.values();
        for (int i = 0; i < 3; i++) {
            vitrina(player, o.offset(VITRINAS[0] - 4 + i * 4, 0, 0), techo(TamanoTunel.NORMAL, pinzas[i], 0), false);
            vitrina(player, o.offset(VITRINAS[1] - 4 + i * 4, 0, 0), techo(TamanoTunel.GRANDE, pinzas[i], 0), false);
            vitrina(player, o.offset(VITRINAS[2] - 4 + i * 4, 0, 0), techo(TamanoTunel.NORMAL, PosicionTunel.CENTRO, i), false);
        }
        // de pared: el soporte al lado de su pared; el corto llega a la via desde el bloque de al lado, el largo desde uno mas alla
        vitrina(player, o.offset(VIAS_PARED[0], 0, 0), pared(TamanoTunel.NORMAL), true);
        vitrina(player, o.offset(VIAS_PARED[1], 0, 0), pared(TamanoTunel.GRANDE), true);
    }

    private static AjustesTunel techo(TamanoTunel tamano, PosicionTunel pinza, int escala) {
        return new AjustesTunel(VersionTunel.TECHO, tamano, pinza, SoporteTunelBlock.ALTURA_MAX, SoporteTunelBlock.ALTURA_PARED_CENTRO, escala);
    }

    private static AjustesTunel pared(TamanoTunel tamano) {
        return new AjustesTunel(VersionTunel.PARED, tamano, PosicionTunel.CENTRO, 0, SoporteTunelBlock.ALTURA_PARED_CENTRO, 0);
    }

    /**
     * Un soporte con su gemelo al norte y el perfil entre ellos; {@code via} es el eje de la via. Los de
     * pared van al oeste de la via (el corto a 1 bloque, el largo a 2) con su pared detras.
     */
    private static void vitrina(ServerPlayer player, BlockPos via, AjustesTunel ajustes, boolean dePared) {
        ServerLevel level = player.serverLevel();
        int y = LineaPrueba.ALTURA_TUNEL;
        int dx = dePared ? (ajustes.tamano() == TamanoTunel.GRANDE ? -2 : -1) : 0;
        BlockState state = ajustes.aplicar(ModBlocks.SOPORTE_TUNEL.get().defaultBlockState())
                .setValue(AbstractRotatableBlock.FACING, dePared ? Direction.EAST : Direction.SOUTH)
                .setValue(AbstractRotatableBlock.ROTATION, 1);
        if (dePared) {
            for (int dz = -9; dz <= 9; dz++) {
                for (int dy = 0; dy <= y + 1; dy++) {
                    level.setBlock(via.offset(dx - 1, dy, dz), Blocks.WHITE_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        // los de techo, un bloque mas arriba (bajo la losa) y colgando un bloque mas: la pinza queda igual
        int alto = dePared ? y : y + 1;
        List<BlockPos> soportes = List.of(via.offset(dx, alto, 0), via.offset(dx, alto, -VANO_TUNEL));
        for (BlockPos pos : soportes) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        LineaPrueba.tender(player, soportes, () -> new ItemStack(ModBlocks.PERFIL_RIGIDO.get()));
        via(level, via.offset(0, 0, 9), via.offset(0, 0, -9));
    }

    /**
     * Tunel recto hacia el norte (eje de la via en {@code o}): boca de ladrillo en z = +1, paredes, techo y
     * una capa de piedra por encima, soportes de techo en zigzag cada 6 bloques con el perfil rigido y,
     * fuera, dos postes espanoles con mensula y catenaria normal que entra en el tunel (la transicion).
     */
    private static void tunel(ServerPlayer player, BlockPos o) {
        ServerLevel level = player.serverLevel();
        // un bloque mas alto que el de la prueba: los soportes cuelgan un bloque mas (altura 4) y el hilo de
        // contacto queda donde el de la mensula
        int techo = LineaPrueba.ALTURA_TUNEL + 2;
        BlockState ladrillo = Blocks.STONE_BRICKS.defaultBlockState();
        for (int dz = 1; dz >= -LARGO_TUNEL; dz--) {
            for (int dy = 0; dy < techo; dy++) {
                level.setBlock(o.offset(-4, dy, dz), ladrillo, Block.UPDATE_ALL);
                level.setBlock(o.offset(4, dy, dz), ladrillo, Block.UPDATE_ALL);
            }
            for (int dx = -4; dx <= 4; dx++) {
                level.setBlock(o.offset(dx, techo, dz), ladrillo, Block.UPDATE_ALL);
            }
            for (int dx = -6; dx <= 6; dx++) {
                level.setBlock(o.offset(dx, techo + 1, dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            // el monte por encima
            for (int capa = 0; capa < 3; capa++) {
                for (int dx = -9 + 3 * capa; dx <= 9 - 3 * capa; dx++) {
                    level.setBlock(o.offset(dx, techo + 2 + capa, dz), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    if (capa > 0) {
                        level.setBlock(o.offset(dx, techo + 1 + capa, dz), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        // la boca: un marco de ladrillo un poco mas ancho y alto que el tunel
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = 0; dy <= techo + 2; dy++) {
                if (Math.abs(dx) > 3 || dy >= techo) {
                    level.setBlock(o.offset(dx, dy, 2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        PosicionTunel[] zigzag = {PosicionTunel.DERECHA, PosicionTunel.CENTRO, PosicionTunel.IZQUIERDA, PosicionTunel.CENTRO};
        List<BlockPos> soportes = new ArrayList<>();
        for (int i = 0; -1 - i * VANO_TUNEL > -LARGO_TUNEL; i++) {
            BlockPos pos = o.offset(0, techo - 1, -1 - i * VANO_TUNEL);
            level.setBlock(pos, ModBlocks.SOPORTE_TUNEL.get().defaultBlockState()
                    .setValue(AbstractRotatableBlock.FACING, Direction.SOUTH)
                    .setValue(AbstractRotatableBlock.ROTATION, 1)
                    .setValue(SoporteTunelBlock.ALTURA, SoporteTunelBlock.ALTURA_MAX)
                    .setValue(SoporteTunelBlock.POSICION, zigzag[i % zigzag.length]), Block.UPDATE_ALL);
            soportes.add(pos);
        }
        LineaPrueba.tender(player, soportes, () -> new ItemStack(ModBlocks.PERFIL_RIGIDO.get()));
        via(level, o.offset(0, 0, 2 * VANO + 6), o.offset(0, 0, -LARGO_TUNEL - 2));
        Block poste = ModPostes.POSTES.get("iberian_flat_lattice_mast").get();
        BlockPos lejos = LineaPrueba.posteConMensula(level, o.offset(-3, 0, 2 * VANO), poste, LineaPrueba.ALTURA_MENSULA + 2, 2.5f, ModoZigzag.INTERIOR);
        BlockPos cerca = LineaPrueba.posteConMensula(level, o.offset(-3, 0, VANO), poste, LineaPrueba.ALTURA_MENSULA + 2, 2.5f, ModoZigzag.MEDIO);
        LineaPrueba.tender(player, List.of(lejos, cerca, soportes.get(0)), LineaPrueba::bobinaCatenaria);
    }

    /** Via de Create de {@code desde} a {@code hasta} (misma x) sobre una cama de grava de 3 bloques. */
    private static void via(ServerLevel level, BlockPos desde, BlockPos hasta) {
        Block via = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "track"));
        int z0 = Math.min(desde.getZ(), hasta.getZ());
        int z1 = Math.max(desde.getZ(), hasta.getZ());
        for (int z = z0; z <= z1; z++) {
            BlockPos p = new BlockPos(desde.getX(), desde.getY(), z);
            for (int dx = -1; dx <= 1; dx++) {
                level.setBlock(p.offset(dx, -1, 0), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (via != null) {
                level.setBlock(p, via.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}

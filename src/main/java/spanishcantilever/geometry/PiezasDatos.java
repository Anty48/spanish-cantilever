package spanishcantilever.geometry;

// GENERADO por tools/instalar_piezas.py a partir de las piezas de Blockbench. No editar a mano.
// Coordenadas en pixeles de Blockbench, en el sistema de cada pieza.
public final class PiezasDatos {
    private PiezasDatos() {}

    /** Main_steel_bar.json -> models/block/piezas/barra.json */
    public static final class BARRA {
        public static final String ID = "barra";
        public static final int ELEMENTS = 9;
        public static final float[] MIN = {0f, 0f, 0f};
        public static final float[] MAX = {7f, 2f, 2f};
        /** grupo "duplicating_segment" */
        public static final int[] GRUPO_DUPLICATING_SEGMENT = {0, 1, 2};
        /** grupo "attach_segment_end_1" */
        public static final int[] GRUPO_ATTACH_SEGMENT_END_1 = {3, 4, 5};
        /** grupo "attach_segment_end_2" */
        public static final int[] GRUPO_ATTACH_SEGMENT_END_2 = {6, 7, 8};
    }

    /** stee_bar_holes.json -> models/block/piezas/perforada_diagonal.json */
    public static final class PERFORADA_DIAGONAL {
        public static final String ID = "perforada_diagonal";
        public static final int ELEMENTS = 6;
        public static final float[] MIN = {0f, 0f, 0.5f};
        public static final float[] MAX = {7f, 1.5f, 1.5f};
        /** centro del cubo "attaching_center_segment" (elemento 5) */
        public static final float[] ATTACHING_CENTER_SEGMENT = {0.5f, 0.75f, 1.0f};
        /** grupo "duplicating_segment" */
        public static final int[] GRUPO_DUPLICATING_SEGMENT = {0, 1, 2, 3, 4, 5};
    }

    /** stee_bar_holes_vertical.json -> models/block/piezas/perforada_vertical.json */
    public static final class PERFORADA_VERTICAL {
        public static final String ID = "perforada_vertical";
        public static final int ELEMENTS = 7;
        public static final float[] MIN = {0f, -7f, 0.5f};
        public static final float[] MAX = {1.5f, 0f, 1.5f};
        /** centro del cubo "top_joint" (elemento 5) */
        public static final float[] TOP_JOINT = {0.75f, -0.5f, 1.0f};
        /** grupo "duplicating_segment" */
        public static final int[] GRUPO_DUPLICATING_SEGMENT = {0, 1, 2, 3, 4, 5, 6};
    }

    /** cable_holder_short.json -> models/block/piezas/brazo_corto.json */
    public static final class BRAZO_CORTO {
        public static final String ID = "brazo_corto";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {0f, 0f, 0f};
        public static final float[] MAX = {16f, 4f, 1f};
        /** centro del cubo "cable_joint" (elemento 0) */
        public static final float[] CABLE_JOINT = {15.0f, 0.5f, 0.5f};
        /** centro del cubo "wall_joint" (elemento 7) */
        public static final float[] WALL_JOINT = {0.5f, 1.0f, 0.5f};
        /** grupo "brazo_sujetador" */
        public static final int[] GRUPO_BRAZO_SUJETADOR = {1, 2, 3, 4, 5, 6};
    }

    /** cable_holder_long.json -> models/block/piezas/brazo_largo.json */
    public static final class BRAZO_LARGO {
        public static final String ID = "brazo_largo";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {0f, 0f, 0f};
        public static final float[] MAX = {22f, 4f, 1f};
        /** centro del cubo "cable_joint" (elemento 0) */
        public static final float[] CABLE_JOINT = {21.0f, 0.5f, 0.5f};
        /** centro del cubo "wall_joint" (elemento 7) */
        public static final float[] WALL_JOINT = {0.5f, 1.0f, 0.5f};
        /** grupo "brazo_sujetador" */
        public static final int[] GRUPO_BRAZO_SUJETADOR = {1, 2, 3, 4, 5, 6};
    }

    /** sustainig_metal_cable.json -> models/block/piezas/tirante.json */
    public static final class TIRANTE {
        public static final String ID = "tirante";
        public static final int ELEMENTS = 7;
        public static final float[] MIN = {1f, 0f, 0f};
        public static final float[] MAX = {5.5f, 4f, 4f};
        /** centro del cubo "joint_wall_point" (elemento 0) */
        public static final float[] JOINT_WALL_POINT = {1.25f, 2.0f, 2.0f};
        /** centro del cubo "cable_pieza_clonable_p=4" (elemento 2) */
        public static final float[] CABLE_PIEZA_CLONABLE_P_4 = {3.5f, 2.125f, 2.125f};
        /** centro del cubo "screw" (elemento 3) */
        public static final float[] SCREW = {1.625f, 3.5f, 3.5f};
        /** grupo "clonable" */
        public static final int[] GRUPO_CLONABLE = {2};
    }

    /** horizontal_insulator.json -> models/block/piezas/aislador_horizontal.json */
    public static final class AISLADOR_HORIZONTAL {
        public static final String ID = "aislador_horizontal";
        public static final int ELEMENTS = 10;
        public static final float[] MIN = {-1f, 0f, -0.4f};
        public static final float[] MAX = {4f, 4f, 1.6f};
        /** centro del cubo "joint_point" (elemento 0) */
        public static final float[] JOINT_POINT = {1.5f, 0.25f, 0.6f};
        /** centro del cubo "cable_joint" (elemento 8) */
        public static final float[] CABLE_JOINT = {1.5f, 3.0f, 0.6f};
    }

    /** vertical_insulator.json -> models/block/piezas/aislador_vertical.json */
    public static final class AISLADOR_VERTICAL {
        public static final String ID = "aislador_vertical";
        public static final int ELEMENTS = 7;
        public static final float[] MIN = {0f, 0f, 0f};
        public static final float[] MAX = {2f, 4f, 2f};
        /** centro del cubo "joint_point" (elemento 0) */
        public static final float[] JOINT_POINT = {1.0f, 0.25f, 1.0f};
        /** centro del cubo "cable_joint" (elemento 3) */
        public static final float[] CABLE_JOINT = {1.0f, 3.25f, 1.0f};
    }
}

package spanishcantilevers.geometry;

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

    /** cable_holder_convex.json -> models/block/piezas/brazo_convexo.json */
    public static final class BRAZO_CONVEXO {
        public static final String ID = "brazo_convexo";
        public static final int ELEMENTS = 10;
        public static final float[] MIN = {0f, 0f, -0.75f};
        public static final float[] MAX = {16f, 4.1305f, 1.75f};
        /** centro del cubo "cable_joint" (elemento 0) */
        public static final float[] CABLE_JOINT = {15.25f, 0.5f, 0.5f};
        /** centro del cubo "wall_joint" (elemento 9) */
        public static final float[] WALL_JOINT = {0.5f, 1.0f, 0.5f};
        /** grupo "brazo_sujetador" */
        public static final int[] GRUPO_BRAZO_SUJETADOR = {1, 2, 3, 4, 5, 6, 7, 8};
    }

    /** cable_holder_all.json -> models/block/piezas/sujetador.json */
    public static final class SUJETADOR {
        public static final String ID = "sujetador";
        public static final int ELEMENTS = 10;
        public static final float[] MIN = {0f, 0f, -0.75f};
        public static final float[] MAX = {23.25f, 2.75f, 1.75f};
        /** centro del cubo "cable_joint" (elemento 0) */
        public static final float[] CABLE_JOINT = {22.625f, 0.5f, 0.5f};
        /** centro del cubo "wall_joint" (elemento 9) */
        public static final float[] WALL_JOINT = {0.5f, 1.0f, 0.5f};
        /** grupo "brazo_sujetador" */
        public static final int[] GRUPO_BRAZO_SUJETADOR = {1, 2, 3, 4, 5, 6, 7, 8};
    }

    /** cable_holder_all_short.json -> models/block/piezas/sujetador_corto.json */
    public static final class SUJETADOR_CORTO {
        public static final String ID = "sujetador_corto";
        public static final int ELEMENTS = 9;
        public static final float[] MIN = {0f, 0f, -0.75f};
        public static final float[] MAX = {16.25f, 2.75f, 1.75f};
        /** centro del cubo "cable_joint" (elemento 0) */
        public static final float[] CABLE_JOINT = {15.625f, 0.5f, 0.5f};
        /** centro del cubo "wall_joint" (elemento 8) */
        public static final float[] WALL_JOINT = {0.5f, 1.0f, 0.5f};
        /** grupo "brazo_sujetador" */
        public static final int[] GRUPO_BRAZO_SUJETADOR = {1, 2, 3, 4, 5, 6, 7};
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

    /** Tunnel spanish canteliver left.json -> models/block/piezas/tunel_izquierda.json */
    public static final class TUNEL_IZQUIERDA {
        public static final String ID = "tunel_izquierda";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {7f, 2f, 3f};
        public static final float[] MAX = {9f, 18f, 13f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
    }

    /** Tunnel spanish canteliver center.json -> models/block/piezas/tunel_centro.json */
    public static final class TUNEL_CENTRO {
        public static final String ID = "tunel_centro";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {7f, 2f, 3f};
        public static final float[] MAX = {9f, 18f, 13f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
    }

    /** Tunnel spanish canteliver right.json -> models/block/piezas/tunel_derecha.json */
    public static final class TUNEL_DERECHA {
        public static final String ID = "tunel_derecha";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {7f, 2f, 3f};
        public static final float[] MAX = {9f, 18f, 13f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
        /** centro del cubo "cable_attach" (elemento 5) */
        public static final float[] CABLE_ATTACH = {8.0f, 3.25f, 6.0f};
    }

    /** Tunnel spanish canteliver large left.json -> models/block/piezas/tunel_grande_izquierda.json */
    public static final class TUNEL_GRANDE_IZQUIERDA {
        public static final String ID = "tunel_grande_izquierda";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {6.5f, 2f, 0f};
        public static final float[] MAX = {9.5f, 18f, 16f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
        /** centro del cubo "cable_attach" (elemento 3) */
        public static final float[] CABLE_ATTACH = {8.0f, 3.25f, 12.0f};
        /** centro del cubo "right_cable_attach" (elemento 4) */
        public static final float[] RIGHT_CABLE_ATTACH = {8.0f, 2.5f, 10.5f};
        /** centro del cubo "left_cable_attach" (elemento 5) */
        public static final float[] LEFT_CABLE_ATTACH = {8.0f, 2.5f, 13.5f};
        /** grupo "cable_joints" */
        public static final int[] GRUPO_CABLE_JOINTS = {3, 4, 5};
    }

    /** Tunnel spanish canteliver large center.json -> models/block/piezas/tunel_grande_centro.json */
    public static final class TUNEL_GRANDE_CENTRO {
        public static final String ID = "tunel_grande_centro";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {6.5f, 2f, 0f};
        public static final float[] MAX = {9.5f, 18f, 16f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
        /** centro del cubo "cable_attach" (elemento 3) */
        public static final float[] CABLE_ATTACH = {8.0f, 3.25f, 8.0f};
        /** centro del cubo "right_cable_attach" (elemento 4) */
        public static final float[] RIGHT_CABLE_ATTACH = {8.0f, 2.5f, 6.5f};
        /** centro del cubo "left_cable_attach" (elemento 5) */
        public static final float[] LEFT_CABLE_ATTACH = {8.0f, 2.5f, 9.5f};
        /** grupo "cable_joints" */
        public static final int[] GRUPO_CABLE_JOINTS = {3, 4, 5};
    }

    /** Tunnel spanish canteliver large right.bbmodel -> models/block/piezas/tunel_grande_derecha.json */
    public static final class TUNEL_GRANDE_DERECHA {
        public static final String ID = "tunel_grande_derecha";
        public static final int ELEMENTS = 8;
        public static final float[] MIN = {6.5f, 2f, 0f};
        public static final float[] MAX = {9.5f, 18f, 16f};
        /** centro del cubo "ceeling joint" (elemento 0) */
        public static final float[] CEELING_JOINT = {8.0f, 12.5f, 8.0f};
        /** centro del cubo "left_cable_attach" (elemento 5) */
        public static final float[] LEFT_CABLE_ATTACH = {8.0f, 2.5f, 5.5f};
        /** centro del cubo "right_cable_attach" (elemento 6) */
        public static final float[] RIGHT_CABLE_ATTACH = {8.0f, 2.5f, 2.5f};
        /** centro del cubo "cable_attach" (elemento 7) */
        public static final float[] CABLE_ATTACH = {8.0f, 3.25f, 4.0f};
        /** grupo "group" */
        public static final int[] GRUPO_GROUP = {7, 6, 5};
    }

    /** alternative_tunnel_canteliver_short.json -> models/block/piezas/tunel_pared_corto.json */
    public static final class TUNEL_PARED_CORTO {
        public static final String ID = "tunel_pared_corto";
        public static final int ELEMENTS = 16;
        public static final float[] MIN = {6.4f, 2f, -2f};
        public static final float[] MAX = {9.6f, 9f, 16.1f};
        /** centro del cubo "cable_attach" (elemento 0) */
        public static final float[] CABLE_ATTACH = {8.0f, 4.25f, 0.0f};
        /** centro del cubo "wall_joint" (elemento 11) */
        public static final float[] WALL_JOINT = {8.0f, 5.5f, 15.85f};
        /** grupo "cable_joints" */
        public static final int[] GRUPO_CABLE_JOINTS = {0, 1, 2};
    }

    /** alternative_tunnel_canteliver_long.json -> models/block/piezas/tunel_pared_largo.json */
    public static final class TUNEL_PARED_LARGO {
        public static final String ID = "tunel_pared_largo";
        public static final int ELEMENTS = 16;
        public static final float[] MIN = {6.4f, 2f, -9f};
        public static final float[] MAX = {9.6f, 9f, 16.1f};
        /** centro del cubo "cable_attach_center" (elemento 0) */
        public static final float[] CABLE_ATTACH_CENTER = {8.0f, 4.25f, -7.0f};
        /** centro del cubo "cable_attach" (elemento 1) */
        public static final float[] CABLE_ATTACH = {8.0f, 4.0f, -8.25f};
        /** centro del cubo "wall_joint" (elemento 11) */
        public static final float[] WALL_JOINT = {8.0f, 5.5f, 15.85f};
        /** grupo "cable_joints" */
        public static final int[] GRUPO_CABLE_JOINTS = {0, 1, 2};
    }

    /** steel catenary tunnel fixed cable segment.json -> models/block/piezas/perfil_rigido.json */
    public static final class PERFIL_RIGIDO {
        public static final String ID = "perfil_rigido";
        public static final int ELEMENTS = 9;
        public static final float[] MIN = {0f, 0f, 0f};
        public static final float[] MAX = {7f, 2.5f, 2f};
        /** grupo "duplicating_segment" */
        public static final int[] GRUPO_DUPLICATING_SEGMENT = {0, 1, 2};
        /** grupo "attach_segment_end_1" */
        public static final int[] GRUPO_ATTACH_SEGMENT_END_1 = {3, 4, 5};
        /** grupo "attach_segment_end_2" */
        public static final int[] GRUPO_ATTACH_SEGMENT_END_2 = {6, 7, 8};
    }
}

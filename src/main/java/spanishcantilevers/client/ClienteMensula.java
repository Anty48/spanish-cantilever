package spanishcantilevers.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;
import spanishcantilevers.geometry.Ajustes;
import spanishcantilevers.item.MensulaItem;
import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.item.SoporteTunelItem;
import spanishcantilevers.network.ConfigurarMensula;
import spanishcantilevers.network.ConfigurarSoporteTunel;
import spanishcantilevers.network.ModRed;

/** Lo que solo existe en el cliente y llaman el bloque y el objeto (separado para el servidor dedicado). */
public final class ClienteMensula {
    private ClienteMensula() {
    }

    /** Donde se guarda la configuracion por defecto de este jugador (boton "set default config"). */
    private static final Path PREDETERMINADO = FMLPaths.CONFIGDIR.get().resolve("spanishcantilevers-predeterminado.nbt");

    /** La configuracion por defecto guardada por el jugador, o {@link Ajustes#DEFECTO} si no hay. */
    public static Ajustes predeterminado() {
        try {
            CompoundTag nbt = Files.exists(PREDETERMINADO) ? NbtIo.read(PREDETERMINADO.toFile()) : null;
            return nbt == null ? Ajustes.DEFECTO : Ajustes.leer(nbt);
        } catch (IOException e) {
            return Ajustes.DEFECTO;
        }
    }

    public static boolean guardarPredeterminado(Ajustes ajustes) {
        try {
            NbtIo.write(ajustes.validados().escribir(new CompoundTag()), PREDETERMINADO.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Ventana para el objeto en la mano: al cerrarla los ajustes se guardan en el objeto. Si el objeto
     * nunca se configuro ({@code ajustes} null), propone la configuracion por defecto del jugador.
     */
    public static void abrirObjeto(@Nullable Ajustes ajustes) {
        abrir(ajustes != null ? ajustes : predeterminado(), nuevos -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                // como PNW: se cambia ya en el cliente y se pide al servidor que haga lo mismo
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof MensulaItem) {
                        MensulaItem.setAjustes(stack, nuevos);
                        break;
                    }
                }
            }
            ModRed.CANAL.sendToServer(new ConfigurarMensula(null, nuevos));
        });
    }

    /** Ventana para una mensula ya puesta. */
    public static void abrirBloque(BlockPos pos, Ajustes ajustes) {
        abrir(ajustes, nuevos -> ModRed.CANAL.sendToServer(new ConfigurarMensula(pos, nuevos)));
    }

    /** Donde se guarda la configuracion por defecto del soporte de tunel de este jugador. */
    private static final Path PREDETERMINADO_TUNEL = FMLPaths.CONFIGDIR.get().resolve("spanishcantilevers-predeterminado-tunel.nbt");

    /** La configuracion por defecto del soporte de tunel guardada por el jugador, o la del bloque si no hay. */
    public static AjustesTunel predeterminadoTunel() {
        try {
            CompoundTag nbt = Files.exists(PREDETERMINADO_TUNEL) ? NbtIo.read(PREDETERMINADO_TUNEL.toFile()) : null;
            return nbt == null ? AjustesTunel.DEFECTO : AjustesTunel.leer(nbt);
        } catch (IOException e) {
            return AjustesTunel.DEFECTO;
        }
    }

    public static boolean guardarPredeterminadoTunel(AjustesTunel ajustes) {
        try {
            NbtIo.write(ajustes.escribir(new CompoundTag()), PREDETERMINADO_TUNEL.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Ventana de un soporte de tunel ya puesto: version, tamano, posicion de la pinza y altura. */
    public static void abrirSoporteTunel(BlockPos pos, BlockState state) {
        abrirVentana(root -> new VentanaSoporteTunel(root, AjustesTunel.de(state),
                ajustes -> ModRed.CANAL.sendToServer(new ConfigurarSoporteTunel(pos, ajustes))));
    }

    /**
     * Ventana para el soporte de tunel en la mano: lo escogido se guarda en el objeto y se aplica al
     * ponerlo. Si el objeto nunca se configuro ({@code state} null), propone la configuracion por defecto.
     */
    public static void abrirSoporteTunelObjeto(@Nullable BlockState state) {
        AjustesTunel inicial = state != null ? AjustesTunel.de(state) : predeterminadoTunel();
        abrirVentana(root -> new VentanaSoporteTunel(root, inicial, ajustes -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                for (InteractionHand mano : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(mano);
                    if (stack.getItem() instanceof SoporteTunelItem) {
                        SoporteTunelItem.setAjustes(stack, ajustes);
                        break;
                    }
                }
            }
            ModRed.CANAL.sendToServer(new ConfigurarSoporteTunel(null, ajustes));
        }));
    }

    private static void abrir(Ajustes ajustes, java.util.function.Consumer<Ajustes> alCerrar) {
        abrirVentana(root -> new VentanaMensula(root, ajustes, alCerrar));
    }

    /** Igual que ClientWrapper.showCantileverSettingsScreen de PNW. */
    private static void abrirVentana(java.util.function.Function<DLWindowManager, DLWindow> ventana) {
        DLWindow.openWindow(root -> {
            root.addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
                if (e.keyCode() == 256 || Minecraft.getInstance().options.keyInventory.matches(e.keyCode(), e.scanCode())) {
                    ((DLWindowManager) root).close();
                    return true;
                }
                return false;
            });
            return ventana.apply((DLWindowManager) root);
        });
    }
}

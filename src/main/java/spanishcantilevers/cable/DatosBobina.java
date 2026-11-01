package spanishcantilevers.cable;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.paw.components.WireConnectionDataComponent;
import de.mrjulsen.paw.registry.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Puntos que una bobina de P&W lleva apuntados (el primer clic de un tramo). En 1.21 viven en el
 * componente {@code WIRE_CONNECTION_DATA} del objeto; en 1.20.1 estaban en su NBT.
 */
public final class DatosBobina {
    private DatosBobina() {
    }

    private static WireConnectionDataComponent datos(ItemStack stack) {
        return ModDataComponents.getComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent::empty);
    }

    /** Copia de los puntos apuntados (para leer, o para cambiarlos y guardarlos con {@link #guardar}). */
    public static List<CompoundTag> puntos(ItemStack stack) {
        List<CompoundTag> copia = new ArrayList<>();
        for (CompoundTag punto : datos(stack).customPointData()) {
            copia.add(punto.copy());
        }
        return copia;
    }

    /** El ultimo punto apuntado, o null si no hay ninguno. */
    public static CompoundTag ultimo(ItemStack stack) {
        List<CompoundTag> puntos = datos(stack).customPointData();
        return puntos.isEmpty() ? null : puntos.get(puntos.size() - 1).copy();
    }

    /** Guarda en la bobina los puntos (normalmente los de {@link #puntos} ya cambiados). */
    public static void guardar(ItemStack stack, List<CompoundTag> puntos) {
        ModDataComponents.setComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA,
                new WireConnectionDataComponent(new ArrayList<>(puntos), datos(stack).customData()));
    }
}

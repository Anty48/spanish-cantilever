package spanishcantilevers.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import spanishcantilevers.ModBlocks;
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.Ajustes;

/**
 * Como el objeto de la mensula de PNW: clic derecho al aire abre la ventana de ajustes y lo que se
 * escoge se guarda en el propio objeto (componente {@code block_entity_data}, el antiguo "BlockEntityTag"),
 * asi cada mensula que se coloca sale ya configurada.
 */
public class MensulaItem extends BlockItem {
    public MensulaItem(MensulaBlock block, Properties properties) {
        super(block, properties);
    }

    public static Ajustes ajustes(ItemStack stack) {
        CustomData datos = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return datos == null ? Ajustes.DEFECTO : Ajustes.leer(datos.copyTag());
    }

    /** Si el objeto ya se configuro alguna vez (si no, se coloca con {@link Ajustes#DEFECTO}). */
    public static boolean configurado(ItemStack stack) {
        return stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }

    public static void setAjustes(ItemStack stack, Ajustes ajustes) {
        BlockItem.setBlockEntityData(stack, ModBlocks.MENSULA_BE.get(), ajustes.validados().escribir(new CompoundTag()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Ajustes ajustes = configurado(stack) ? ajustes(stack) : null;
            ClienteMensula.abrirObjeto(ajustes);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

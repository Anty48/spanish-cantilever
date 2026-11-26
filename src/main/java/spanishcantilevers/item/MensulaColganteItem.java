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
import spanishcantilevers.block.MensulaColganteBlock;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.AjustesColgante;

/**
 * Como el de la mensula: clic derecho al aire abre la ventana y lo escogido se guarda en el objeto
 * (componente {@code block_entity_data}), asi cada mensula colgante sale ya configurada.
 */
public class MensulaColganteItem extends BlockItem {
    public MensulaColganteItem(MensulaColganteBlock block, Properties properties) {
        super(block, properties);
    }

    public static AjustesColgante ajustes(ItemStack stack) {
        CustomData datos = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return datos == null ? AjustesColgante.DEFECTO : AjustesColgante.leer(datos.copyTag());
    }

    /** Si el objeto ya se configuro alguna vez (si no, se coloca con {@link Ajustes#DEFECTO}). */
    public static boolean configurado(ItemStack stack) {
        return stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }

    public static void setAjustes(ItemStack stack, AjustesColgante ajustes) {
        BlockItem.setBlockEntityData(stack, ModBlocks.MENSULA_COLGANTE_BE.get(), ajustes.validados().escribir(new CompoundTag()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            AjustesColgante ajustes = configurado(stack) ? ajustes(stack) : null;
            ClienteMensula.abrirColganteObjeto(ajustes);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

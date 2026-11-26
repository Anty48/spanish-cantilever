package spanishcantilevers.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import spanishcantilevers.block.MensulaColganteBlock;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.AjustesColgante;

/**
 * Como el de la mensula: clic derecho al aire abre la ventana y lo escogido se guarda en el objeto
 * ("BlockEntityTag"), asi cada mensula colgante sale ya configurada.
 */
public class MensulaColganteItem extends BlockItem {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    public MensulaColganteItem(MensulaColganteBlock block, Properties properties) {
        super(block, properties);
    }

    public static AjustesColgante ajustes(ItemStack stack) {
        CompoundTag nbt = stack.getTagElement(BLOCK_ENTITY_TAG);
        return nbt == null ? AjustesColgante.DEFECTO : AjustesColgante.leer(nbt);
    }

    /** Si el objeto ya se configuro alguna vez (si no, se coloca con {@link AjustesColgante#DEFECTO}). */
    public static boolean configurado(ItemStack stack) {
        return stack.getTagElement(BLOCK_ENTITY_TAG) != null;
    }

    public static void setAjustes(ItemStack stack, AjustesColgante ajustes) {
        ajustes.validados().escribir(stack.getOrCreateTagElement(BLOCK_ENTITY_TAG));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            AjustesColgante ajustes = configurado(stack) ? ajustes(stack) : null;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirColganteObjeto(ajustes));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

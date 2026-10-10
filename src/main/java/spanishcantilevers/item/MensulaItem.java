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
import spanishcantilevers.block.MensulaBlock;
import spanishcantilevers.client.ClienteMensula;
import spanishcantilevers.geometry.Ajustes;

/**
 * Como el objeto de la mensula de PNW: clic derecho al aire abre la ventana de ajustes y lo que se
 * escoge se guarda en el propio objeto ("BlockEntityTag"), asi cada mensula que se coloca sale ya
 * configurada.
 */
public class MensulaItem extends BlockItem {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    public MensulaItem(MensulaBlock block, Properties properties) {
        super(block, properties);
    }

    public static Ajustes ajustes(ItemStack stack) {
        CompoundTag nbt = stack.getTagElement(BLOCK_ENTITY_TAG);
        return nbt == null ? Ajustes.DEFECTO : Ajustes.leer(nbt);
    }

    /** Si el objeto ya se configuro alguna vez (si no, se coloca con {@link Ajustes#DEFECTO}). */
    public static boolean configurado(ItemStack stack) {
        return stack.getTagElement(BLOCK_ENTITY_TAG) != null;
    }

    public static void setAjustes(ItemStack stack, Ajustes ajustes) {
        ajustes.validados().escribir(stack.getOrCreateTagElement(BLOCK_ENTITY_TAG));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Ajustes ajustes = configurado(stack) ? ajustes(stack) : null;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirObjeto(ajustes));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

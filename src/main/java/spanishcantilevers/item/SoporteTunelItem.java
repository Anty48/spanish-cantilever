package spanishcantilevers.item;

import java.util.List;


import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.client.ClienteMensula;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Objeto del soporte de tunel: como la mensula, clic derecho al aire abre su ventana y lo escogido
 * (version, tamano, posicion de la pinza, altura y tamano general) se guarda en el objeto y se aplica al ponerlo. Se
 * guarda en el "BlockStateTag" de vanilla, que BlockItem ya aplica al colocar el bloque.
 */
public class SoporteTunelItem extends BlockItem {
    private static final String NBT_ESTADO = "BlockStateTag";
    private static final List<Property<?>> AJUSTES = List.of(SoporteTunelBlock.VERSION, SoporteTunelBlock.TAMANO,
            SoporteTunelBlock.POSICION, SoporteTunelBlock.ALTURA, SoporteTunelBlock.ALTURA_PARED, SoporteTunelBlock.ESCALA);

    public SoporteTunelItem(SoporteTunelBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // sin configurar, la ventana propone la configuracion por defecto del jugador
            BlockState estado = stack.getTagElement(NBT_ESTADO) == null ? null : estado(stack);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirSoporteTunelObjeto(estado));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** El bloque tal como quedara al ponerlo (solo cuentan los ajustes de la ventana). */
    public BlockState estado(ItemStack stack) {
        BlockState state = getBlock().defaultBlockState();
        CompoundTag tag = stack.getTagElement(NBT_ESTADO);
        if (tag == null) {
            return state;
        }
        for (Property<?> p : AJUSTES) {
            state = leer(state, p, tag.getString(p.getName()));
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState leer(BlockState state, Property<T> p, String valor) {
        return p.getValue(valor).map(v -> state.setValue(p, v)).orElse(state);
    }

    public static void setAjustes(ItemStack stack, AjustesTunel ajustes) {
        CompoundTag tag = stack.getOrCreateTagElement(NBT_ESTADO);
        tag.putString(SoporteTunelBlock.VERSION.getName(), ajustes.version().getSerializedName());
        tag.putString(SoporteTunelBlock.TAMANO.getName(), ajustes.tamano().getSerializedName());
        tag.putString(SoporteTunelBlock.POSICION.getName(), ajustes.posicion().getSerializedName());
        tag.putString(SoporteTunelBlock.ALTURA.getName(), String.valueOf(ajustes.altura()));
        tag.putString(SoporteTunelBlock.ALTURA_PARED.getName(), String.valueOf(ajustes.alturaPared()));
        tag.putString(SoporteTunelBlock.ESCALA.getName(), String.valueOf(ajustes.escala()));
    }

}

package iberiancantilever.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import iberiancantilever.block.PosicionTunel;
import iberiancantilever.block.SoporteTunelBlock;
import iberiancantilever.client.ClienteMensula;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Objeto del soporte de tunel: como la mensula, clic derecho al aire abre su ventana y lo escogido
 * (posicion de la pinza y altura) se guarda en el objeto y se aplica al ponerlo. Se guarda en el
 * "BlockStateTag" de vanilla, que BlockItem ya aplica al colocar el bloque.
 */
public class SoporteTunelItem extends BlockItem {
    private static final String NBT_ESTADO = "BlockStateTag";

    public SoporteTunelItem(SoporteTunelBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteMensula.abrirSoporteTunelObjeto(estado(stack)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** El bloque tal como quedara al ponerlo (solo cuentan la posicion y la altura). */
    public BlockState estado(ItemStack stack) {
        BlockState state = getBlock().defaultBlockState();
        CompoundTag tag = stack.getTagElement(NBT_ESTADO);
        if (tag == null) {
            return state;
        }
        for (PosicionTunel p : PosicionTunel.values()) {
            if (p.getSerializedName().equals(tag.getString(SoporteTunelBlock.POSICION.getName()))) {
                state = state.setValue(SoporteTunelBlock.POSICION, p);
            }
        }
        try {
            int altura = Integer.parseInt(tag.getString(SoporteTunelBlock.ALTURA.getName()));
            if (SoporteTunelBlock.ALTURA.getPossibleValues().contains(altura)) {
                state = state.setValue(SoporteTunelBlock.ALTURA, altura);
            }
        } catch (NumberFormatException ignored) {
            // sin altura guardada: la de por defecto
        }
        return state;
    }

    public static void setAjustes(ItemStack stack, PosicionTunel posicion, int altura) {
        CompoundTag tag = stack.getOrCreateTagElement(NBT_ESTADO);
        tag.putString(SoporteTunelBlock.POSICION.getName(), posicion.getSerializedName());
        tag.putString(SoporteTunelBlock.ALTURA.getName(), String.valueOf(altura));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        BlockState state = estado(stack);
        tooltip.add(Component.translatable("item.iberiancantilever.soporte_tunel.ajustes",
                Component.translatable("enum.iberiancantilever.zigzag." + switch (state.getValue(SoporteTunelBlock.POSICION)) {
                    case IZQUIERDA -> "interior";
                    case CENTRO -> "medio";
                    case DERECHA -> "exterior";
                }),
                state.getValue(SoporteTunelBlock.ALTURA) * SoporteTunelBlock.PX_POR_ALTURA / 16f));
    }
}

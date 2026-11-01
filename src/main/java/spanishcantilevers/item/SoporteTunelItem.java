package spanishcantilevers.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import spanishcantilevers.block.AjustesTunel;
import spanishcantilevers.block.SoporteTunelBlock;
import spanishcantilevers.client.ClienteMensula;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;

/**
 * Objeto del soporte de tunel: como la mensula, clic derecho al aire abre su ventana y lo escogido
 * (version, tamano, posicion de la pinza, altura y tamano general) se guarda en el objeto y se aplica al ponerlo. Se
 * guarda en el componente {@code block_state} de vanilla (el antiguo "BlockStateTag"), que BlockItem ya aplica
 * al colocar el bloque.
 */
public class SoporteTunelItem extends BlockItem {
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
            BlockState estado = stack.has(DataComponents.BLOCK_STATE) ? estado(stack) : null;
            ClienteMensula.abrirSoporteTunelObjeto(estado);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** El bloque tal como quedara al ponerlo (solo cuentan los ajustes de la ventana). */
    public BlockState estado(ItemStack stack) {
        BlockState state = getBlock().defaultBlockState();
        BlockItemStateProperties guardado = stack.get(DataComponents.BLOCK_STATE);
        if (guardado == null) {
            return state;
        }
        for (Property<?> p : AJUSTES) {
            String valor = guardado.properties().get(p.getName());
            if (valor != null) {
                state = leer(state, p, valor);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState leer(BlockState state, Property<T> p, String valor) {
        return p.getValue(valor).map(v -> state.setValue(p, v)).orElse(state);
    }

    public static void setAjustes(ItemStack stack, AjustesTunel ajustes) {
        BlockItemStateProperties antes = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Map<String, String> tag = new HashMap<>(antes.properties());
        tag.put(SoporteTunelBlock.VERSION.getName(), ajustes.version().getSerializedName());
        tag.put(SoporteTunelBlock.TAMANO.getName(), ajustes.tamano().getSerializedName());
        tag.put(SoporteTunelBlock.POSICION.getName(), ajustes.posicion().getSerializedName());
        tag.put(SoporteTunelBlock.ALTURA.getName(), String.valueOf(ajustes.altura()));
        tag.put(SoporteTunelBlock.ALTURA_PARED.getName(), String.valueOf(ajustes.alturaPared()));
        tag.put(SoporteTunelBlock.ESCALA.getName(), String.valueOf(ajustes.escala()));
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(tag));
    }

}

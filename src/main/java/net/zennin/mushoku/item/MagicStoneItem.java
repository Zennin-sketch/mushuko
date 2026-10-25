package net.zennin.mushoku.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Piedra magica. Si fue imbuida en un circulo, guarda un hechizo y brilla. */
public class MagicStoneItem extends Item {
    public MagicStoneItem(Properties props) { super(props); }

    @Override
    public boolean isFoil(ItemStack stack) { return SpellData.get(stack) != null || super.isFoil(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        SpellData.tooltip(stack, lines);
    }
}

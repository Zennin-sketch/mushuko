package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Objeto simple con una linea de ayuda. */
public class TooltipItem extends Item {
    private final String key;

    public TooltipItem(Properties props, String key) { super(props); this.key = key; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}

package net.zennin.mushoku.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Baculo sin stats de combate (Baculo Dragon, Baculo de Areos, Aqua Heartia). */
public class StaffItem extends Item {
    private final float discount;

    public StaffItem(float discount, Properties props) {
        super(props);
        this.discount = discount;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return StaffLogic.use(level, player, hand, discount);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return SpellData.get(stack) != null; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        StaffLogic.tooltip(stack, lines, discount);
    }
}

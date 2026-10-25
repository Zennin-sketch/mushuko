package net.zennin.mushoku.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Varita y Baculo Migurd: conservan sus stats de arma originales y ademas aceptan piedra imbuida. */
public class StaffPickaxeItem extends PickaxeItem {
    private final float discount;

    public StaffPickaxeItem(Tier tier, float discount, Properties props) {
        super(tier, props);
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

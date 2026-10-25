package net.zennin.mushoku.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.magic.PlayerClasses;

/** Orbe de cambio de clase: se consume y vuelve a abrir el menu de eleccion de clase. */
public class ClassChangeItem extends Item {
    public ClassChangeItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            PlayerClasses.reset(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

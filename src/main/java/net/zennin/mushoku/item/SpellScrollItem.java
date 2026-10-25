package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.magic.SpellEffects;

import java.util.List;

/** Pergamino: en blanco se llena en un circulo magico; con hechizo, clic derecho lo lanza una vez (sin gastar mana). */
public class SpellScrollItem extends Item {
    public SpellScrollItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        Spell spell = SpellData.get(stack);
        if (spell == null) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.scroll_blank").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }
        if (SpellEffects.cast(spell, sp)) {
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            sp.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return SpellData.get(stack) != null; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (SpellData.get(stack) == null)
            lines.add(Component.translatable("tooltip.remushoku.scroll_blank").withStyle(ChatFormatting.GRAY));
        else SpellData.tooltip(stack, lines);
    }
}

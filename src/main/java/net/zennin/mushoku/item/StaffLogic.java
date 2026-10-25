package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.magic.SpellEffects;
import net.zennin.mushoku.network.ModNetworking;

import java.util.List;

/**
 * Baculos y varitas con piedra imbuida.
 *  - Agachado + clic derecho con una piedra imbuida en la otra mano: la inserta.
 *  - Agachado + clic derecho sin piedra imbuida: expulsa la piedra.
 *  - Clic derecho: lanza el hechizo de la piedra pagando su mana (con el descuento del baculo).
 */
public final class StaffLogic {
    private StaffLogic() {}

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, float discount) {
        ItemStack staff = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(staff);
        Spell spell = SpellData.get(staff);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        if (player.isShiftKeyDown()) {
            Spell stoneSpell = other.getItem() instanceof MagicStoneItem ? SpellData.get(other) : null;
            if (stoneSpell != null) {
                if (spell != null) give(sp, SpellData.with(new ItemStack(SpellData.stoneFor(spell.level.tier)), spell));
                SpellData.with(staff, stoneSpell);
                other.shrink(1);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.4f);
                sp.displayClientMessage(Component.translatable("msg.remushoku.staff_inserted",
                        Component.translatable(stoneSpell.translationKey())).withStyle(ChatFormatting.AQUA), true);
            } else if (spell != null) {
                SpellData.clear(staff);
                give(sp, SpellData.with(new ItemStack(SpellData.stoneFor(spell.level.tier)), spell));
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
                sp.displayClientMessage(Component.translatable("msg.remushoku.staff_ejected").withStyle(ChatFormatting.GRAY), true);
            } else {
                sp.displayClientMessage(Component.translatable("msg.remushoku.staff_empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.consume(staff);
        }

        if (spell == null) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.staff_empty").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(staff);
        }
        ManaData mana = ModAttachments.get(sp);
        double cost = spell.level.cost * (1 - discount);
        if (mana.mana() < cost && !sp.getAbilities().instabuild) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(staff);
        }
        if (SpellEffects.cast(spell, sp)) {
            ManaData after = mana;
            if (!sp.getAbilities().instabuild) after = after.withMana(after.mana() - cost);
            after = after.withMaxMana(after.maxMana() + spell.level.maxManaGain);
            ModAttachments.set(sp, after);
            ModNetworking.syncMana(sp);
            sp.getCooldowns().addCooldown(staff.getItem(), 20);
        }
        return InteractionResultHolder.consume(staff);
    }

    public static void tooltip(ItemStack stack, List<Component> lines, float discount) {
        if (SpellData.get(stack) == null)
            lines.add(Component.translatable("tooltip.remushoku.staff_empty").withStyle(ChatFormatting.GRAY));
        else SpellData.tooltip(stack, lines);
        if (discount > 0)
            lines.add(Component.translatable("tooltip.remushoku.staff_discount", Math.round(discount * 100)).withStyle(ChatFormatting.BLUE));
    }

    private static void give(Player p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
}

package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.sword.SwordSkill;

import java.util.List;

/** Bomba adhesiva: se lanza y explota al tocar algo. Requiere la habilidad Objetos del Dios Norte. */
public class AdhesiveBombItem extends Item {
    public AdhesiveBombItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!sp.getAbilities().instabuild && !ModAttachments.sword(sp).knows(SwordSkill.NG_ITEMS.id)) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.need_items").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        MagicProjectile bomb = new MagicProjectile(level, sp, MagicProjectile.ROCK, 4f).explosion(1.8f);
        bomb.setPos(sp.getX(), sp.getEyeY() - 0.1, sp.getZ());
        bomb.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0f, 1.3f, 1f);
        level.addFreshEntity(bomb);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1f, 0.8f);
        sp.getCooldowns().addCooldown(this, 20);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.adhesive_bomb").withStyle(ChatFormatting.GRAY));
    }
}

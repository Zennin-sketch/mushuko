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
import net.zennin.mushoku.magic.SpellKnowledge;

import java.util.List;

/**
 * Pergamino de hechizo: un pergamino grabado que ensena un hechizo que todavia no conoces. La tinta (y el nombre del
 * hechizo) tienen el color de la piedra de mana de su nivel. Clic derecho para aprenderlo (se gasta).
 */
public class SpellLessonScrollItem extends Item {
    public SpellLessonScrollItem(Properties props) { super(props); }

    public static ItemStack of(Spell s) {
        return SpellData.with(new ItemStack(net.zennin.mushoku.registry.ModItems.SPELL_LESSON_SCROLL), s);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spell s = SpellData.get(stack);
        if (s == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (SpellKnowledge.knows(sp, s)) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.lesson_known").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        SpellKnowledge.learn(sp, s);
        ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                sp.getX(), sp.getY() + 1.2, sp.getZ(), 40, 0.5, 0.6, 0.5, 0.8);
        level.playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        Spell s = SpellData.get(stack);
        if (s == null) return;
        lines.add(Component.translatable("tooltip.remushoku.lesson_teaches").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(s.translationKey()).withStyle(st -> st.withColor(SpellKnowledge.color(s.level)))));
        lines.add(Component.translatable("level.remushoku." + s.level.tier).withStyle(st -> st.withColor(SpellKnowledge.color(s.level)).withItalic(true)));
    }

    /** Color de la tinta (capa 1 del modelo) segun el nivel del hechizo. */
    public static int inkColor(ItemStack stack) {
        Spell s = SpellData.get(stack);
        return 0xFF000000 | (s == null ? 0x8A6A40 : SpellKnowledge.rgb(s.level));
    }
}

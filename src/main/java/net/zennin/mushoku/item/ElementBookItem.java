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
import net.minecraft.world.level.Level;
import net.zennin.mushoku.data.MagicData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.Element;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.registry.ModItems;

/**
 * Libro elemental: con una piedra magica en la otra mano, clic derecho para aprender ese nivel de magia del elemento.
 * Intermedia = 2, Avanzada = 3, Santo = 4, Rey = 5, Imperial = 6. La piedra se consume.
 */
public class ElementBookItem extends Item {
    private final Element element;

    public ElementBookItem(Element element, Properties props) {
        super(props);
        this.element = element;
    }

    public static int stoneLevel(ItemStack stack) {
        Item i = stack.getItem();
        if (i == ModItems.INTERMEDIATE_STONE) return 2;
        if (i == ModItems.ADVANCED_STONE) return 3;
        if (i == ModItems.SAINT_STONE) return 4;
        if (i == ModItems.KING_STONE) return 5;
        if (i == ModItems.IMPERIAL_STONE) return 6;
        if (i == ModItems.ELEMENTARY_STONE) return 1;
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(book);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stone = player.getItemInHand(other);
        int lvl = stoneLevel(stone);
        MagicData m = ModAttachments.magic(sp);
        if (lvl == 0) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.book_level",
                    Component.translatable("element.remushoku." + element.key),
                    Component.translatable("level.remushoku." + m.level(element))), true);
            return InteractionResultHolder.consume(book);
        }
        if (lvl <= m.level(element)) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.already_known").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(book);
        }
        ModAttachments.setMagic(sp, m.withLevel(element, lvl));
        if (!sp.getAbilities().instabuild) stone.shrink(1);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        sp.displayClientMessage(Component.translatable("msg.remushoku.learned",
                Component.translatable("level.remushoku." + lvl),
                Component.translatable("element.remushoku." + element.key)).withStyle(ChatFormatting.AQUA), false);
        ModNetworking.syncMagic(sp);
        return InteractionResultHolder.consume(book);
    }
}

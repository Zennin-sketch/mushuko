package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Carcaj: guarda flechas (hasta 64, como una bolsa) y solo flechas. Clic derecho en el inventario para meter o sacar.
 * Si tienes un arco en la mano y te quedas sin flechas sueltas, el carcaj te va pasando flechas solo.
 * Hay que ponerlo en su ranura del inventario (al lado del escudo): ahi se ve en la espalda y funciona.
 */
public class QuiverItem extends BundleItem {
    public QuiverItem(Properties props) {
        super(props);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack quiver, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        if (c == null) return false;
        ItemStack other = slot.getItem();
        BundleContents.Mutable m = new BundleContents.Mutable(c);
        if (other.isEmpty()) {
            ItemStack removed = m.removeOne();
            if (removed != null) {
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                ItemStack rest = slot.safeInsert(removed);
                m.tryInsert(rest);
            }
        } else if (other.is(ItemTags.ARROWS)) {
            if (m.tryTransfer(slot, player) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        } else {
            return false;
        }
        quiver.set(DataComponents.BUNDLE_CONTENTS, m.toImmutable());
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack quiver, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        if (c == null) return false;
        BundleContents.Mutable m = new BundleContents.Mutable(c);
        if (other.isEmpty()) {
            ItemStack removed = m.removeOne();
            if (removed != null) {
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1f);
                access.set(removed);
            }
        } else if (other.is(ItemTags.ARROWS)) {
            if (m.tryInsert(other) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1f);
        } else {
            return false;
        }
        quiver.set(DataComponents.BUNDLE_CONTENTS, m.toImmutable());
        return true;
    }

    /** No se vacia al usarlo en la mano (a diferencia de la bolsa). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.quiver").withStyle(ChatFormatting.GRAY));
    }

    /** El carcaj equipado en su ranura del inventario (o vacio): solo ese se ve y pasa flechas. */
    public static ItemStack find(Player p) {
        ItemStack s = net.zennin.mushoku.data.ModAttachments.quiver(p);
        return s.getItem() instanceof QuiverItem ? s : ItemStack.EMPTY;
    }

    /** Si no hay flechas sueltas, pasa hasta 16 del carcaj al inventario. */
    public static void feed(ServerPlayer p) {
        if (p.getAbilities().instabuild) return;
        Inventory inv = p.getInventory();
        if (inv.offhand.get(0).is(ItemTags.ARROWS)) return;
        for (ItemStack s : inv.items) if (s.is(ItemTags.ARROWS)) return;
        ItemStack quiver = find(p);
        if (quiver.isEmpty()) return;
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        if (c == null || c.isEmpty()) return;
        BundleContents.Mutable m = new BundleContents.Mutable(c);
        ItemStack out = m.removeOne();
        if (out == null) return;
        ItemStack give = out.split(Math.min(16, out.getCount()));
        if (!out.isEmpty()) m.tryInsert(out);
        quiver.set(DataComponents.BUNDLE_CONTENTS, m.toImmutable());
        if (!inv.add(give)) p.drop(give, false);
    }
}

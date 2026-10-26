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
    /** Flechas que entran en un carcaj (en pilas de 64). */
    public static final int CAPACITY = 999;

    public QuiverItem(Properties props) {
        super(props);
    }

    // ---------------------------------------------------------------- contenido (pilas de 64, hasta 999 en total)
    public static List<ItemStack> contents(ItemStack quiver) {
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        List<ItemStack> out = new java.util.ArrayList<>();
        if (c != null) for (ItemStack s : c.items()) out.add(s.copy());
        return out;
    }

    public static int count(ItemStack quiver) {
        int n = 0;
        for (ItemStack s : contents(quiver)) n += s.getCount();
        return n;
    }

    private static void store(ItemStack quiver, List<ItemStack> list) {
        list.removeIf(ItemStack::isEmpty);
        quiver.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(list));
    }

    /** Mete lo que entre de 'arrows' (lo saca de esa pila). Devuelve cuantas metio. */
    public static int insert(ItemStack quiver, ItemStack arrows) {
        if (!arrows.is(ItemTags.ARROWS) || arrows.isEmpty()) return 0;
        List<ItemStack> list = contents(quiver);
        int room = CAPACITY - count(quiver), moved = 0;
        for (ItemStack s : list) {
            if (room <= 0 || arrows.isEmpty()) break;
            if (ItemStack.isSameItemSameComponents(s, arrows) && s.getCount() < 64) {
                int k = Math.min(Math.min(64 - s.getCount(), arrows.getCount()), room);
                s.grow(k); arrows.shrink(k); room -= k; moved += k;
            }
        }
        while (room > 0 && !arrows.isEmpty()) {
            int k = Math.min(Math.min(64, arrows.getCount()), room);
            list.add(arrows.split(k)); room -= k; moved += k;
        }
        if (moved > 0) store(quiver, list);
        return moved;
    }

    /** Saca la ultima pila que entro (como la bolsa). */
    public static ItemStack removeOne(ItemStack quiver) {
        List<ItemStack> list = contents(quiver);
        if (list.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = list.remove(list.size() - 1);
        store(quiver, list);
        return out;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack quiver, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            ItemStack removed = removeOne(quiver);
            if (!removed.isEmpty()) {
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                ItemStack rest = slot.safeInsert(removed);
                if (!rest.isEmpty()) insert(quiver, rest);
            }
        } else if (other.is(ItemTags.ARROWS)) {
            ItemStack taken = slot.safeTake(other.getCount(), other.getCount(), player);
            int n = insert(quiver, taken);
            if (!taken.isEmpty()) slot.safeInsert(taken);
            if (n > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack quiver, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            ItemStack removed = removeOne(quiver);
            if (!removed.isEmpty()) {
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1f);
                access.set(removed);
            }
        } else if (other.is(ItemTags.ARROWS)) {
            if (insert(quiver, other) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1f);
        } else {
            return false;
        }
        return true;
    }

    /** No se vacia al usarlo en la mano (a diferencia de la bolsa). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return count(stack) > 0; }

    @Override
    public int getBarWidth(ItemStack stack) { return Math.min(13, 1 + Math.round(12f * count(stack) / CAPACITY)); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.quiver_count", count(stack), CAPACITY).withStyle(ChatFormatting.GOLD));
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
        List<ItemStack> list = contents(quiver);
        if (list.isEmpty()) return;
        ItemStack first = list.get(0);
        ItemStack give = first.split(Math.min(16, first.getCount()));
        store(quiver, list);
        if (!inv.add(give)) p.drop(give, false);
    }
}

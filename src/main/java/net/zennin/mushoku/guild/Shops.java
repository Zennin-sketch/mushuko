package net.zennin.mushoku.guild;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;
import java.util.function.Supplier;

/** Tiendas del original con sus precios (en cobre chico). sell = lo que te pagan si lo vendes (0 = no se compra). */
public final class Shops {
    public record Offer(Supplier<Item> item, int count, int buy, int sell, ResourceKey<Enchantment> ench, int enchLevel) {
        static Offer of(Supplier<Item> item, int count, int buy, int sell) { return new Offer(item, count, buy, sell, null, 0); }
    }

    public static final String GENERAL = "general_store", BLACKSMITH = "blacksmith", MAGIC = "magic_shop";

    public static final List<Offer> GENERAL_OFFERS = List.of(
            Offer.of(() -> Items.BREAD, 3, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.CARROT, 10, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.POTATO, 10, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.COOKED_BEEF, 1, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.GOLDEN_CARROT, 1, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.OAK_PLANKS, 32, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.COBBLESTONE, 32, Coins.SMALL, Coins.SMALL));

    public static final List<Offer> BLACKSMITH_OFFERS = List.of(
            Offer.of(() -> Items.IRON_SWORD, 1, 2 * Coins.SMALL, Coins.SMALL),
            new Offer(() -> Items.IRON_SWORD, 1, Coins.LARGE, 0, Enchantments.UNBREAKING, 3),
            Offer.of(() -> Items.DIAMOND_SWORD, 1, 2 * Coins.SILVER, Coins.SILVER),
            new Offer(() -> Items.DIAMOND_SWORD, 1, 3 * Coins.SILVER, 0, Enchantments.SHARPNESS, 3),
            Offer.of(() -> Items.COAL, 6, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.RAW_IRON, 3, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> Items.DIAMOND, 1, 6 * Coins.LARGE, 6 * Coins.LARGE),
            Offer.of(() -> ModItems.HIRAMUNE, 1, 4 * Coins.GOLD, Coins.GOLD));

    public static final List<Offer> MAGIC_OFFERS = List.of(
            Offer.of(() -> ModItems.ELEMENTARY_STONE, 1, Coins.SMALL, Coins.SMALL),
            Offer.of(() -> ModItems.INTERMEDIATE_STONE, 1, 5 * Coins.SMALL, Coins.SMALL),
            Offer.of(() -> ModItems.ADVANCED_STONE, 1, 5 * Coins.LARGE, Coins.LARGE),
            Offer.of(() -> ModItems.SAINT_STONE, 1, 3 * Coins.SILVER, Coins.SILVER),
            Offer.of(() -> ModItems.EARTH_BOOK, 1, Coins.LARGE, Coins.LARGE),
            Offer.of(() -> ModItems.FIRE_BOOK, 1, Coins.LARGE, Coins.LARGE),
            Offer.of(() -> ModItems.WATER_BOOK, 1, Coins.LARGE, Coins.LARGE),
            Offer.of(() -> ModItems.WIND_BOOK, 1, Coins.LARGE, Coins.LARGE),
            Offer.of(() -> ModItems.SILENT_TOME, 1, 10 * Coins.GOLD, 10 * Coins.GOLD),
            Offer.of(() -> ModItems.CLASS_CHANGE_ITEM, 1, Coins.SILVER, Coins.SILVER));

    private Shops() {}

    public static List<Offer> offers(String shop) {
        return switch (shop) {
            case BLACKSMITH -> BLACKSMITH_OFFERS;
            case MAGIC -> MAGIC_OFFERS;
            default -> GENERAL_OFFERS;
        };
    }

    public static ItemStack stack(Offer o, ServerPlayer p) {
        ItemStack s = new ItemStack(o.item().get(), o.count());
        if (o.ench() != null) {
            Holder<Enchantment> h = p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(o.ench());
            s.enchant(h, o.enchLevel());
        }
        return s;
    }

    public static void trade(ServerPlayer p, String shop, int index, boolean buy) {
        List<Offer> list = offers(shop);
        if (index < 0 || index >= list.size()) return;
        Offer o = list.get(index);
        if (buy) {
            if (!Coins.pay(p, o.buy())) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.shop_no_money")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            ItemStack s = stack(o, p);
            if (!p.getInventory().add(s)) p.drop(s, false);
            p.playNotifySound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.2f);
        } else {
            if (o.sell() <= 0 || o.ench() != null) return;
            Item item = o.item().get();
            int eligible = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (sellable(s, item)) eligible += s.getCount();
            }
            if (eligible < o.count()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.shop_no_items")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            int left = o.count();
            for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!sellable(s, item)) continue;
                int k = Math.min(left, s.getCount());
                s.shrink(k);
                left -= k;
            }
            Coins.give(p, o.sell());
            p.playNotifySound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1f);
        }
    }

    /** No se venden objetos encantados o con datos (piedras imbuidas, baculos con piedra...). */
    private static boolean sellable(ItemStack s, Item item) {
        return s.is(item) && !s.isEnchanted() && !s.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && s.getDamageValue() == 0;
    }
}

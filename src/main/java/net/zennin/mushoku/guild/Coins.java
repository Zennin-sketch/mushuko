package net.zennin.mushoku.guild;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.registry.ModItems;

/**
 * Monedas del original: 9 cobre chico = 1 cobre grande, 9 grande = 1 plata, 9 plata = 1 oro.
 * Los precios se cuentan en cobre chico y se paga con cualquier combinacion; el cambio se devuelve.
 */
public final class Coins {
    public static final int SMALL = 1, LARGE = 9, SILVER = 81, GOLD = 729;

    private Coins() {}

    public static int value(Item item) {
        if (item == ModItems.SMALL_COPPER) return SMALL;
        if (item == ModItems.LARGE_COPPER) return LARGE;
        if (item == ModItems.SILVER_COIN) return SILVER;
        if (item == ModItems.GOLD_COIN) return GOLD;
        return 0;
    }

    public static int total(Player p) {
        int sum = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            sum += value(s.getItem()) * s.getCount();
        }
        return sum;
    }

    /** Cobra el precio; devuelve false si no alcanza. */
    public static boolean pay(Player p, int price) {
        if (p.getAbilities().instabuild) return true;
        if (total(p) < price) return false;
        Inventory inv = p.getInventory();
        int paid = 0;
        // Primero las monedas mas chicas, para no romper monedas grandes si no hace falta
        for (int v : new int[]{SMALL, LARGE, SILVER, GOLD}) {
            for (int i = 0; i < inv.getContainerSize() && paid < price; i++) {
                ItemStack s = inv.getItem(i);
                if (value(s.getItem()) != v) continue;
                while (!s.isEmpty() && paid < price) {
                    s.shrink(1);
                    paid += v;
                }
            }
        }
        give(p, paid - price);
        return true;
    }

    /** Da una cantidad en monedas, usando las mas grandes posibles. */
    public static void give(Player p, int amount) {
        int[][] coins = {{GOLD, 0}, {SILVER, 1}, {LARGE, 2}, {SMALL, 3}};
        Item[] items = {ModItems.GOLD_COIN, ModItems.SILVER_COIN, ModItems.LARGE_COPPER, ModItems.SMALL_COPPER};
        for (int[] c : coins) {
            int n = amount / c[0];
            amount -= n * c[0];
            while (n > 0) {
                int k = Math.min(64, n);
                ItemStack st = new ItemStack(items[c[1]], k);
                if (!p.getInventory().add(st)) p.drop(st, false);
                n -= k;
            }
        }
    }

    /** "2 Plata" / "5 Cobre chico"... para mostrar precios. */
    public static String key(int amount) {
        if (amount % GOLD == 0) return "gold";
        if (amount % SILVER == 0) return "silver";
        if (amount % LARGE == 0) return "large";
        return "small";
    }

    public static int count(int amount) {
        return switch (key(amount)) { case "gold" -> amount / GOLD; case "silver" -> amount / SILVER; case "large" -> amount / LARGE; default -> amount; };
    }
}

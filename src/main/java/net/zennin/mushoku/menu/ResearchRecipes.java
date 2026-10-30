package net.zennin.mushoku.menu;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.item.SpellData;

import java.util.List;

/**
 * Investigaciones de la Mesa de Investigacion (piedras de mana). Los ingredientes van en cualquier orden en los tres
 * huecos; ademas cuesta mana del que investiga.
 * <ul>
 *   <li>Piedra Elemental: 2 fragmentos de amatista + 1 lapislazuli (20 de mana).</li>
 *   <li>Subir de rango: 3 piedras del rango anterior + un catalizador (oro, diamante, esmeralda, fragmento de eco,
 *       chatarra de netherita). Rinde mas que juntar 4 en el circulo, pero cuesta mana.</li>
 * </ul>
 */
public final class ResearchRecipes {
    public record Need(Item item, int count) {}
    public record Recipe(List<Need> needs, int stoneTier, int stones, int mana, int resultTier,
                         java.util.function.Supplier<ItemStack> other) {
        Recipe(List<Need> needs, int stoneTier, int stones, int mana, int resultTier) { this(needs, stoneTier, stones, mana, resultTier, null); }
    }

    public static final List<Recipe> ALL = List.of(
            new Recipe(List.of(new Need(Items.AMETHYST_SHARD, 2), new Need(Items.LAPIS_LAZULI, 1)), 0, 0, 20, 1),
            new Recipe(List.of(new Need(Items.GOLD_INGOT, 1)), 1, 3, 60, 2),
            new Recipe(List.of(new Need(Items.DIAMOND, 1)), 2, 3, 150, 3),
            new Recipe(List.of(new Need(Items.EMERALD, 1)), 3, 3, 350, 4),
            new Recipe(List.of(new Need(Items.ECHO_SHARD, 1)), 4, 3, 650, 5),
            new Recipe(List.of(new Need(Items.NETHERITE_SCRAP, 1)), 5, 3, 900, 6),
            // pergaminos en blanco: 3 papeles y un frasco de tinta
            new Recipe(List.of(new Need(Items.PAPER, 3), new Need(net.zennin.mushoku.registry.ModItems.INK_VIAL, 1)), 0, 0, 10, 0,
                    () -> new ItemStack(net.zennin.mushoku.registry.ModItems.SCROLL, 3)));

    private ResearchRecipes() {}

    /** La investigacion que arman los tres huecos (o null). Tiene que estar exacta, sin cosas de mas. */
    public static Recipe match(Container in) {
        for (Recipe r : ALL) if (matches(r, in)) return r;
        return null;
    }

    private static boolean matches(Recipe r, Container in) {
        int[] have = new int[r.needs().size()];
        int stones = 0;
        for (int i = 0; i < in.getContainerSize(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            int tier = SpellData.get(s) == null ? ElementBookItem.stoneLevel(s) : -1;
            if (r.stones() > 0 && tier == r.stoneTier()) { stones += s.getCount(); continue; }
            boolean used = false;
            for (int k = 0; k < have.length; k++) if (s.is(r.needs().get(k).item())) { have[k] += s.getCount(); used = true; }
            if (!used) return false;
        }
        if (stones < r.stones()) return false;
        for (int k = 0; k < have.length; k++) if (have[k] < r.needs().get(k).count()) return false;
        return true;
    }

    /** Saca de los huecos lo que gasta la investigacion. */
    public static void consume(Recipe r, Container in) {
        int stones = r.stones();
        int[] left = r.needs().stream().mapToInt(Need::count).toArray();
        for (int i = 0; i < in.getContainerSize(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (stones > 0 && SpellData.get(s) == null && ElementBookItem.stoneLevel(s) == r.stoneTier()) {
                int n = Math.min(stones, s.getCount());
                s.shrink(n);
                stones -= n;
                continue;
            }
            for (int k = 0; k < left.length; k++) {
                if (left[k] > 0 && s.is(r.needs().get(k).item())) {
                    int n = Math.min(left[k], s.getCount());
                    s.shrink(n);
                    left[k] -= n;
                }
            }
        }
        in.setChanged();
    }

    public static ItemStack result(Recipe r) { return r.other() != null ? r.other().get() : new ItemStack(SpellData.stoneFor(r.resultTier())); }

    /** Grabar: pluma con tinta + pergamino en blanco (y nada mas) en los huecos. */
    public static boolean engraving(Container in) {
        boolean quill = false, scroll = false;
        for (int i = 0; i < in.getContainerSize(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(net.zennin.mushoku.registry.ModItems.INKAND_QUILL)) quill = true;
            else if (s.is(net.zennin.mushoku.registry.ModItems.SCROLL) && SpellData.get(s) == null) scroll = true;
            else if (!isStone(s)) return false; // una piedra de mana se puede ofrecer para investigar
        }
        return quill && scroll;
    }

    private static boolean isStone(ItemStack s) {
        for (int t = 1; t <= 6; t++) if (s.is(SpellData.stoneFor(t))) return true;
        return false;
    }

    /** Mana para grabar un hechizo en un pergamino: una vez y media lo que cuesta lanzarlo. */
    public static int engraveCost(net.zennin.mushoku.magic.Spell s) { return (int) Math.round(s.level.cost * 1.5); }
}

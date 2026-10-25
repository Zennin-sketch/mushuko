package net.zennin.mushoku.companion;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.zennin.mushoku.registry.ModItems;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Personajes con los que se puede interactuar (estilo Minecraft Comes Alive).
 * Stats de combate del mod/addon original; gustos y personalidad segun la novela.
 * romance = false para los personajes con apariencia infantil: solo amistad.
 */
public enum CompanionKind {
    ERIS("eris_greyrat", "eris_greyrat", true, 256, 20, 16, 32, Combat.MELEE, () -> Items.IRON_SWORD, true,
            s -> s.getItem() instanceof SwordItem || s.is(Items.COOKED_BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.COOKED_MUTTON),
            s -> s.is(Items.BOOK) || s.is(Items.WRITABLE_BOOK) || s.is(Items.ROTTEN_FLESH),
            Map.of("joke", 0.8f, "story", 0.7f, "compliment", 1.2f, "flirt", 0.8f, "chat", 1.0f)),
    SYLPHIETTE("sylphiette_greyrat", "sylphiette_greyrat", true, 150, 10, 4, 32, Combat.WIND, () -> ModItems.WAND, true,
            s -> s.is(ItemTags.FLOWERS) || s.is(Items.APPLE) || s.is(Items.BREAD) || s.is(Items.CAKE),
            s -> s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE),
            Map.of("joke", 1.1f, "compliment", 1.2f, "flirt", 1.1f, "chat", 1.2f, "story", 1.0f)),
    SYLPHIETTE_YOUNG("sylphiette_joven", "sylphiette_joven", true, 40, 2, 4, 32, Combat.MELEE, () -> Items.AIR, false,
            s -> s.is(ItemTags.FLOWERS) || s.is(Items.COOKIE) || s.is(Items.SWEET_BERRIES) || s.is(Items.APPLE),
            s -> s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE),
            Map.of("joke", 1.2f, "compliment", 1.1f, "chat", 1.0f, "story", 1.2f)),
    ROXY("roxy_migurdia", "roxy_migurdia", true, 512, 20, 10, 48, Combat.WATER, () -> ModItems.MIGURD_STAFF, true,
            s -> s.is(Items.BOOK) || s.is(Items.ENCHANTED_BOOK) || s.getItem() == ModItems.SPELL_BOOK || s.getItem() == ModItems.FIRE_BOOK
                    || s.getItem() == ModItems.WATER_BOOK || s.getItem() == ModItems.EARTH_BOOK || s.getItem() == ModItems.WIND_BOOK
                    || isStone(s.getItem()),
            s -> s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE) || s.is(Items.POISONOUS_POTATO),
            Map.of("story", 1.3f, "chat", 1.1f, "joke", 0.8f, "flirt", 0.7f, "compliment", 1.0f)),
    RUDEUS("rudeus_greyrat", "rudeus_greyrat", false, 512, 30, 10, 48, Combat.EARTH, () -> ModItems.AQUA_HEARTIA, true,
            s -> s.is(Items.BOOK) || isStone(s.getItem()) || s.getItem() == ModItems.AQUA_HEARTIA || s.is(Items.CLAY_BALL),
            s -> s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE),
            Map.of("joke", 1.3f, "chat", 1.1f, "story", 1.1f, "flirt", 1.0f, "compliment", 1.0f)),
    GHISLAINE("ghislaine", "ghislaine", false, 256, 20, 16, 32, Combat.GHISLAINE, () -> Items.IRON_SWORD, true,
            s -> s.is(Items.COOKED_BEEF) || s.is(Items.BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.PORKCHOP) || s.getItem() instanceof SwordItem,
            s -> s.is(Items.BOOK) || s.is(Items.WRITABLE_BOOK) || s.is(Items.PAPER),
            Map.of("story", 0.7f, "joke", 0.8f, "compliment", 1.1f, "flirt", 0.8f, "chat", 0.9f)),
    RUIJERD("ruijerd", "ruijerd", false, 512, 25, 20, 32, Combat.RUIJERD, () -> Items.AIR, true,
            s -> s.is(Items.BREAD) || s.is(Items.GOLDEN_APPLE) || s.is(Items.COOKED_BEEF) || s.is(Items.TOTEM_OF_UNDYING),
            s -> s.is(Items.ROTTEN_FLESH) || s.is(Items.WITHER_ROSE),
            Map.of("joke", 0.6f, "story", 1.2f, "compliment", 0.9f, "flirt", 0.6f, "chat", 1.0f)),
    KISHIRIKA("kishirika", "kishirika", false, 1024, 40, 40, 32, Combat.KISHIRIKA, () -> Items.AIR, false,
            s -> s.is(Items.CAKE) || s.is(Items.COOKIE) || s.is(Items.PUMPKIN_PIE) || s.is(Items.GOLDEN_APPLE) || s.is(Items.HONEY_BOTTLE)
                    || s.has(net.minecraft.core.component.DataComponents.FOOD) || s.getItem() == ModItems.DEMON_EYE,
            s -> false,
            Map.of("joke", 1.3f, "compliment", 1.4f, "story", 1.1f, "chat", 1.0f));

    public enum Combat { MELEE, WIND, WATER, EARTH, GHISLAINE, RUIJERD, KISHIRIKA }

    public final String id, texture;
    public final boolean slim, romance;
    public final double health, armor, damage, follow;
    public final Combat combat;
    public final Supplier<Item> held;
    public final Predicate<ItemStack> likes, dislikes;
    /** Multiplicador de exito por tipo de charla segun la personalidad. */
    public final Map<String, Float> affinity;

    CompanionKind(String id, String texture, boolean slim, double health, double armor, double damage, double follow, Combat combat,
                  Supplier<Item> held, boolean romance, Predicate<ItemStack> likes, Predicate<ItemStack> dislikes, Map<String, Float> affinity) {
        this.id = id;
        this.texture = texture;
        this.slim = slim;
        this.health = health;
        this.armor = armor;
        this.damage = damage;
        this.follow = follow;
        this.combat = combat;
        this.held = held;
        this.romance = romance;
        this.likes = likes;
        this.dislikes = dislikes;
        this.affinity = affinity;
    }

    public boolean ranged() { return combat == Combat.WIND || combat == Combat.WATER || combat == Combat.EARTH; }

    private static boolean isStone(Item i) {
        return i == ModItems.ELEMENTARY_STONE || i == ModItems.INTERMEDIATE_STONE || i == ModItems.ADVANCED_STONE
                || i == ModItems.SAINT_STONE || i == ModItems.KING_STONE || i == ModItems.IMPERIAL_STONE;
    }
}

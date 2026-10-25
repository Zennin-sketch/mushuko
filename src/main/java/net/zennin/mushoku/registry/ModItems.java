package net.zennin.mushoku.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.item.ClassChangeItem;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.magic.Element;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    /** Todos los items del mod, en el orden en que aparecen en la pestaña creativa. */
    public static final List<Item> ALL = new ArrayList<>();

    // --- Piedras mágicas ---
    public static final Item ELEMENTARY_STONE = simple("elementary_stone");
    public static final Item INTERMEDIATE_STONE = simple("intermediate_stone");
    public static final Item ADVANCED_STONE = simple("advanced_stone");
    public static final Item SAINT_STONE = simple("saint_stone");
    public static final Item KING_STONE = simple("king_stone");
    public static final Item IMPERIAL_STONE = simple("imperial_stone");

    // --- Libros y magia (funcionalidad en fases siguientes) ---
    public static final Item FIRE_BOOK = register("fire_book", new ElementBookItem(Element.FIRE, new Item.Properties().stacksTo(1)));
    public static final Item EARTH_BOOK = register("earth_book", new ElementBookItem(Element.EARTH, new Item.Properties().stacksTo(1)));
    public static final Item WIND_BOOK = register("wind_book", new ElementBookItem(Element.WIND, new Item.Properties().stacksTo(1)));
    public static final Item WATER_BOOK = register("water_book", new ElementBookItem(Element.WATER, new Item.Properties().stacksTo(1)));
    public static final Item SPELL_BOOK = single("spell_book");
    public static final Item GUIDE_BOOK = single("guide_book");
    public static final Item SILENT_TOME = single("silent_tome");
    public static final Item SCROLL = simple("scroll");
    public static final Item CHALK = single("chalk");
    public static final Item INKAND_QUILL = single("inkand_quill");
    public static final Item INK_VIAL = simple("ink_vial");
    public static final Item CLASS_CHANGE_ITEM = register("class_change_item", new ClassChangeItem(new Item.Properties().stacksTo(16)));
    public static final Item SUMMON_SCROLL_KALMAN = single("summon_scroll_kalman");
    public static final Item DEMON_EYE = single("demon_eye");

    // --- Monedas ---
    public static final Item SMALL_COPPER = simple("small_copper");
    public static final Item LARGE_COPPER = simple("large_copper");
    public static final Item SILVER_COIN = simple("silver_coin");
    public static final Item GOLD_COIN = simple("gold_coin");

    // --- Llaves y objetos varios ---
    public static final Item MOSSY_KEY = simple("mossy_key");
    public static final Item ICE_KEY = simple("ice_key");
    public static final Item SPICE_BAG = simple("spice_bag");
    public static final Item ROCK = simple("rock");
    public static final Item ADHESIVE_BOMB = simple("adhesive_bomb");
    public static final Item SWAP_TOTEM = single("swap_totem");
    public static final Item SWAP_CLOAK = single("swap_cloak");
    public static final Item METAL_CLAWS = single("metal_claws");
    public static final Item AQUA_HEARTIA = single("aqua_heartia");
    public static final Item DRAGON_STAFF = single("dragon_staff");
    public static final Item STAFF_OF_AREOS = single("staff_of_areos");
    public static final Item ULTIMO_SUSPIRO = single("ultimo_suspiro");

    // --- Armas (mismas stats que el original: dano base + bonus del material, velocidad) ---
    public static final Item ALMANFI_SWORD = sword("almanfi_sword", ModTiers.ALMANFI, 3, -1.0f);
    public static final Item HIRAMUNE = sword("hiramune", ModTiers.HIRAMUNE, 3, -2.0f);
    public static final Item ICE_BLADE = sword("ice_blade", ModTiers.ICE_BLADE, 3, -3.0f);
    public static final Item KAJAKUT = sword("kajakut", ModTiers.KAJAKUT, 3, -2.2f);
    public static final Item KATANA = sword("katana", ModTiers.KATANA, 3, -2.0f);
    public static final Item MIGURD_SWORD = sword("migurd_sword", ModTiers.MIGURD_SWORD, 3, -2.5f);
    public static final Item PAULS_SWORD = sword("pauls_sword", ModTiers.PAULS_SWORD, 3, -2.2f);
    public static final Item PHOENIX_DRAGON_SWORD = sword("phoenix_dragon_sword", ModTiers.PHOENIX_DRAGON, 3, -2.0f);
    public static final Item SCYTHE = sword("scythe", ModTiers.SCYTHE, 3, -2.7f);
    public static final Item METAL_ROD = axe("metal_rod", ModTiers.METAL_ROD, 1f, -2.6f);
    public static final Item WAR_AXE = axe("war_axe", ModTiers.WAR_AXE, 1f, -3.2f);
    public static final Item MIGURD_STAFF = pickaxe("migurd_staff", ModTiers.MIGURD_STAFF, 1f, -3.0f);
    public static final Item WAND = pickaxe("wand", ModTiers.WAND, 1f, -3.0f);

    private ModItems() {}

    private static <T extends Item> T register(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(name), item);
        ALL.add(item);
        return item;
    }

    private static Item simple(String name) { return register(name, new Item(new Item.Properties())); }

    private static Item single(String name) { return register(name, new Item(new Item.Properties().stacksTo(1))); }

    private static Item sword(String name, Tier tier, int damage, float speed) {
        return register(name, new SwordItem(tier, new Item.Properties().attributes(SwordItem.createAttributes(tier, damage, speed))));
    }

    private static Item axe(String name, Tier tier, float damage, float speed) {
        return register(name, new AxeItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, damage, speed))));
    }

    private static Item pickaxe(String name, Tier tier, float damage, float speed) {
        return register(name, new PickaxeItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, damage, speed))));
    }

    public static void init() {
        MushokuMod.LOG.info("Items registrados: {}", ALL.size());
    }
}

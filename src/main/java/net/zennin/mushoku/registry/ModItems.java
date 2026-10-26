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
import net.zennin.mushoku.item.AdhesiveBombItem;
import net.zennin.mushoku.item.ChalkItem;
import net.zennin.mushoku.item.ExtraItems;
import net.zennin.mushoku.item.Mk2ArmorItem;
import net.zennin.mushoku.item.ClassChangeItem;
import net.zennin.mushoku.item.MagicStoneItem;
import net.zennin.mushoku.item.SpellScrollItem;
import net.zennin.mushoku.item.StaffItem;
import net.zennin.mushoku.item.SummonScrollItem;
import net.zennin.mushoku.item.StaffPickaxeItem;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.magic.Element;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    /** Todos los items del mod, en el orden en que aparecen en la pestaña creativa. */
    public static final List<Item> ALL = new ArrayList<>();

    // --- Piedras mágicas ---
    public static final Item ELEMENTARY_STONE = stone("elementary_stone");
    public static final Item INTERMEDIATE_STONE = stone("intermediate_stone");
    public static final Item ADVANCED_STONE = stone("advanced_stone");
    public static final Item SAINT_STONE = stone("saint_stone");
    public static final Item KING_STONE = stone("king_stone");
    public static final Item IMPERIAL_STONE = stone("imperial_stone");

    // --- Libros y magia (funcionalidad en fases siguientes) ---
    public static final Item FIRE_BOOK = register("fire_book", new ElementBookItem(Element.FIRE, new Item.Properties().stacksTo(1)));
    public static final Item EARTH_BOOK = register("earth_book", new ElementBookItem(Element.EARTH, new Item.Properties().stacksTo(1)));
    public static final Item WIND_BOOK = register("wind_book", new ElementBookItem(Element.WIND, new Item.Properties().stacksTo(1)));
    public static final Item WATER_BOOK = register("water_book", new ElementBookItem(Element.WATER, new Item.Properties().stacksTo(1)));
    public static final Item SPELL_BOOK = register("spell_book", new ExtraItems.ScreenItem("spellbook", "tooltip.remushoku.spell_book", new Item.Properties().stacksTo(1)));
    public static final Item GUIDE_BOOK = register("guide_book", new ExtraItems.ScreenItem("guide", "tooltip.remushoku.guide_book", new Item.Properties().stacksTo(1)));
    public static final Item SILENT_TOME = register("silent_tome", new ExtraItems.SilentTomeItem(new Item.Properties().stacksTo(1)));
    public static final Item SCROLL = register("scroll", new SpellScrollItem(new Item.Properties().stacksTo(16)));
    public static final Item CHALK = register("chalk", new ChalkItem(new Item.Properties().durability(32)));
    public static final Item INKAND_QUILL = single("inkand_quill");
    public static final Item INK_VIAL = simple("ink_vial");
    public static final Item CLASS_CHANGE_ITEM = register("class_change_item", new ClassChangeItem(new Item.Properties().stacksTo(16)));
    public static final Item SUMMON_SCROLL_KALMAN = register("summon_scroll_kalman", new SummonScrollItem(new Item.Properties().stacksTo(1)));
    public static final Item DEMON_EYE = register("demon_eye", new ExtraItems.ScreenItem("demon_eye", "tooltip.remushoku.demon_eye", new Item.Properties().stacksTo(16)));

    // --- Monedas ---
    public static final Item SMALL_COPPER = simple("small_copper");
    public static final Item LARGE_COPPER = simple("large_copper");
    public static final Item SILVER_COIN = simple("silver_coin");
    public static final Item GOLD_COIN = simple("gold_coin");

    // --- Llaves y objetos varios ---
    public static final Item MOSSY_KEY = simple("mossy_key");
    public static final Item ICE_KEY = simple("ice_key");
    public static final Item SPICE_BAG = register("spice_bag", new ExtraItems.ThrowItem(0f, "tooltip.remushoku.spice_bag", new Item.Properties().stacksTo(16)));
    public static final Item ROCK = register("rock", new ExtraItems.ThrowItem(4f, "tooltip.remushoku.rock", new Item.Properties().stacksTo(16)));
    public static final Item ADHESIVE_BOMB = register("adhesive_bomb", new AdhesiveBombItem(new Item.Properties().stacksTo(16)));
    public static final Item SWAP_TOTEM = register("swap_totem", new ExtraItems.SwapTotemItem(new Item.Properties().stacksTo(1)));
    public static final Item SWAP_CLOAK = register("swap_cloak", new ExtraItems.SwapCloakItem(new Item.Properties().stacksTo(1)));
    public static final Item METAL_CLAWS = register("metal_claws", new ExtraItems.TipItem("tooltip.remushoku.metal_claws", new Item.Properties().stacksTo(1)));
    public static final Item AQUA_HEARTIA = staff("aqua_heartia", 0.25f);
    public static final Item DRAGON_STAFF = staff("dragon_staff", 0.20f);
    public static final Item STAFF_OF_AREOS = staff("staff_of_areos", 0.30f);
    public static final Item ULTIMO_SUSPIRO = register("ultimo_suspiro", new ExtraItems.TipItem("tooltip.remushoku.ultimo_suspiro", new Item.Properties().stacksTo(1)));

    public static final Item DODGE_SKILL_BOOK = register("dodge_skill_book", new ExtraItems.DodgeBookItem(new Item.Properties().stacksTo(1)));
    public static final Item WEDDING_RING = register("wedding_ring", new ExtraItems.TipItem("tooltip.remushoku.wedding_ring", new Item.Properties().stacksTo(1)));

    // --- Armadura Magica Mk 2 (defensa y durabilidad del original) ---
    public static final net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> MK2_MATERIAL = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL, MushokuMod.id("mk2"), new net.minecraft.world.item.ArmorMaterial(
                    java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.BOOTS, 3, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 6,
                            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 8, net.minecraft.world.item.ArmorItem.Type.HELMET, 3,
                            net.minecraft.world.item.ArmorItem.Type.BODY, 8),
                    9, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.NETHERITE_SCRAP),
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(MushokuMod.id("mk2"))), 3f, 0.1f));
    public static final Item MK2_CHESTPLATE = register("magic_armor_mk_2_chestplate", new Mk2ArmorItem(MK2_MATERIAL,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(16 * 40)));
    public static final Item MK2_LEGGINGS = register("magic_armor_mk_2_leggings", new Mk2ArmorItem(MK2_MATERIAL,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().durability(15 * 40)));
    public static final Item MK2_BOOTS = register("magic_armor_mk_2_boots", new Mk2ArmorItem(MK2_MATERIAL,
            net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().durability(13 * 40)));

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
    public static final Item MIGURD_STAFF = staffWeapon("migurd_staff", ModTiers.MIGURD_STAFF, 0.10f, 1f, -3.0f);
    public static final Item WAND = staffWeapon("wand", ModTiers.WAND, 0.05f, 1f, -3.0f);

    private ModItems() {}

    /** Carcaj del arquero: guarda hasta 64 flechas y te las pasa cuando te quedas sin flechas sueltas. */
    public static final Item QUIVER = register("quiver", new net.zennin.mushoku.item.QuiverItem(new Item.Properties().stacksTo(1)
            .component(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS, net.minecraft.world.item.component.BundleContents.EMPTY)));

    /** Creativo: todas las clases a la vez. */
    public static final Item ALL_CLASSES = register("all_classes_scroll", new net.zennin.mushoku.item.AllClassesItem(new Item.Properties().stacksTo(1)
            .rarity(net.minecraft.world.item.Rarity.EPIC)));

    private static <T extends Item> T register(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(name), item);
        ALL.add(item);
        return item;
    }

    private static Item simple(String name) { return register(name, new Item(new Item.Properties())); }

    private static Item stone(String name) { return register(name, new MagicStoneItem(new Item.Properties())); }

    private static Item staff(String name, float discount) {
        return register(name, new StaffItem(discount, new Item.Properties().stacksTo(1)));
    }

    private static Item staffWeapon(String name, Tier tier, float discount, float damage, float speed) {
        return register(name, new StaffPickaxeItem(tier, discount, new Item.Properties().attributes(DiggerItem.createAttributes(tier, damage, speed))));
    }

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

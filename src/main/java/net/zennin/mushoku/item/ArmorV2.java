package net.zennin.mushoku.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.addon.AddonItems;
import net.zennin.mushoku.registry.ModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Armaduras rediseñadas (v2). Solo estan en su propia pestaña del creativo (sin receta ni botin) para probarlas
 * al lado de las originales; cada una cuenta como su original para los efectos (set de la Mk 2, gafas, etc.).
 */
public final class ArmorV2 {
    private static final Map<Item, Item> ORIGINAL = new HashMap<>();
    public static final List<Item> ALL = new ArrayList<>();

    private static Holder<ArmorMaterial> material(String name, int defense, float toughness) {
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, MushokuMod.id(name), new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, defense, ArmorItem.Type.CHESTPLATE, defense, ArmorItem.Type.LEGGINGS, defense,
                        ArmorItem.Type.BOOTS, defense, ArmorItem.Type.BODY, defense),
                15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(MushokuMod.id(name))), toughness, 0f));
    }

    private static Item reg(String id, Item original, ArmorV2Item item) {
        Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(id), item);
        ORIGINAL.put(item, original);
        ALL.add(item);
        return item;
    }

    private static Item.Properties props(ArmorItem.Type t, int mult) {
        return new Item.Properties().stacksTo(1).durability(t.getDurability(mult));
    }

    public static final Item MK2_CHESTPLATE = reg("magic_armor_mk_2_v2_chestplate", ModItems.MK2_CHESTPLATE,
            new ArmorV2Item(ModItems.MK2_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(16 * 40), "mk2_v2", true, "tooltip.remushoku.mk2"));
    public static final Item MK2_LEGGINGS = reg("magic_armor_mk_2_v2_leggings", ModItems.MK2_LEGGINGS,
            new ArmorV2Item(ModItems.MK2_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(15 * 40), "mk2_v2", true, "tooltip.remushoku.mk2"));
    public static final Item MK2_BOOTS = reg("magic_armor_mk_2_v2_boots", ModItems.MK2_BOOTS,
            new ArmorV2Item(ModItems.MK2_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(13 * 40), "mk2_v2", true, "tooltip.remushoku.mk2"));
    public static final Item CURSE_HELMET = reg("curse_suppressing_helmet_v2", AddonItems.CURSE_SUPPRESSING_HELMET,
            new ArmorV2Item(material("curse_helmet_v2", 3, 0), ArmorItem.Type.HELMET, props(ArmorItem.Type.HELMET, 40), "curse_helmet_v2", true, "tooltip.remushoku.curse_helmet"));
    public static final Item SUNGLASSES = reg("magical_sunglasses_v2", AddonItems.MAGICAL_SUNGLASSES,
            new ArmorV2Item(material("sunglasses_v2", 1, 0), ArmorItem.Type.HELMET, props(ArmorItem.Type.HELMET, 20), "sunglasses_v2", false, "tooltip.remushoku.sunglasses"));
    public static final Item ENCHANTED_SHOES = reg("enchanted_shoes_v2", AddonItems.ENCHANTED_SHOES,
            new ArmorV2Item(material("enchanted_shoes_v2", 2, 0), ArmorItem.Type.BOOTS, props(ArmorItem.Type.BOOTS, 20), "enchanted_shoes_v2", true, "tooltip.remushoku.enchanted_shoes"));
    public static final Item FLAME_CLOAK = reg("flame_trapping_cloak_v2", AddonItems.FLAME_TRAPPING_CLOAK,
            new ArmorV2Item(material("flame_cloak_v2", 3, 0), ArmorItem.Type.CHESTPLATE, props(ArmorItem.Type.CHESTPLATE, 20), "flame_cloak_v2", true, "tooltip.remushoku.flame_cloak", true));

    // ---------- Ropa de personajes (equipable, sin efectos) ----------
    private static Item outfit(String id, String model, Holder<ArmorMaterial> mat, ArmorItem.Type type) {
        ArmorV2Item item = new ArmorV2Item(mat, type, props(type, 12), model, false, "tooltip.remushoku." + model);
        Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(id), item);
        ALL.add(item);
        return item;
    }

    private static final Holder<ArmorMaterial> CLOTH = material("outfit_cloth", 1, 0);
    private static final Holder<ArmorMaterial> LEATHER = material("outfit_leather", 2, 0);
    public static final Item ROXY_HAT = outfit("roxy_hat", "outfit_roxy", CLOTH, ArmorItem.Type.HELMET);
    public static final Item ROXY_ROBE = outfit("roxy_robe", "outfit_roxy", CLOTH, ArmorItem.Type.CHESTPLATE);
    public static final Item ROXY_STOCKINGS = outfit("roxy_stockings", "outfit_roxy", CLOTH, ArmorItem.Type.LEGGINGS);
    public static final Item ROXY_BOOTS = outfit("roxy_boots", "outfit_roxy", CLOTH, ArmorItem.Type.BOOTS);
    public static final Item GHISLAINE_EYEPATCH = outfit("ghislaine_eyepatch", "outfit_ghislaine", LEATHER, ArmorItem.Type.HELMET);
    public static final Item GHISLAINE_TOP = outfit("ghislaine_top", "outfit_ghislaine", LEATHER, ArmorItem.Type.CHESTPLATE);
    public static final Item GHISLAINE_STRAPS = outfit("ghislaine_straps", "outfit_ghislaine", LEATHER, ArmorItem.Type.LEGGINGS);
    public static final Item GHISLAINE_BOOTS = outfit("ghislaine_boots", "outfit_ghislaine", LEATHER, ArmorItem.Type.BOOTS);
    public static final Item PAUL_ARMOR = outfit("paul_armor", "outfit_paul", LEATHER, ArmorItem.Type.CHESTPLATE);
    public static final Item PAUL_PANTS = outfit("paul_pants", "outfit_paul", LEATHER, ArmorItem.Type.LEGGINGS);
    public static final Item PAUL_BOOTS = outfit("paul_boots", "outfit_paul", LEATHER, ArmorItem.Type.BOOTS);
    public static final Item ERIS_HAIRBAND = outfit("eris_hairband", "outfit_eris", CLOTH, ArmorItem.Type.HELMET);
    public static final Item ERIS_BLOUSE = outfit("eris_blouse", "outfit_eris", LEATHER, ArmorItem.Type.CHESTPLATE);
    public static final Item ERIS_SHORTS = outfit("eris_shorts", "outfit_eris", CLOTH, ArmorItem.Type.LEGGINGS);
    public static final Item ERIS_BOOTS = outfit("eris_boots", "outfit_eris", LEATHER, ArmorItem.Type.BOOTS);

    public static final CreativeModeTab TAB = FabricItemGroup.builder()
            .title(Component.translatable("item_group.remushoku.v2_tab"))
            .icon(() -> new ItemStack(MK2_CHESTPLATE))
            .displayItems((params, output) -> { for (Item i : ALL) output.accept(i); })
            .build();

    private ArmorV2() {}

    /** La pieza es 'original' o su version v2. */
    public static boolean is(ItemStack stack, Item original) {
        return stack.is(original) || ORIGINAL.get(stack.getItem()) == original;
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MushokuMod.id("v2_tab"), TAB);
    }
}

package net.zennin.mushoku.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Materiales de las armas, con los mismos valores que el mod original. */
public enum ModTiers implements Tier {
    ALMANFI(2000, 7f), HIRAMUNE(3000, 12f), ICE_BLADE(15, 0f), KAJAKUT(3000, 14f), KATANA(1000, 3f),
    METAL_ROD(400, 4f), MIGURD_STAFF(100, 2f), MIGURD_SWORD(1000, 4f), PAULS_SWORD(1500, 5f),
    PHOENIX_DRAGON(2000, 12f), SCYTHE(1400, 7f), WAND(100, 2f), WAR_AXE(1500, 13f);

    private final int uses;
    private final float damageBonus;

    ModTiers(int uses, float damageBonus) {
        this.uses = uses;
        this.damageBonus = damageBonus;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return 4f; }
    @Override public float getAttackDamageBonus() { return damageBonus; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
    @Override public int getEnchantmentValue() { return 2; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
}

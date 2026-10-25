package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;

/** Hechizo guardado dentro de un item (piedra imbuida, pergamino, baculo). */
public final class SpellData {
    private static final String KEY = "remushoku_spell";

    private SpellData() {}

    public static Spell get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String id = data.copyTag().getString(KEY);
        return id.isEmpty() ? null : Spell.byId(id);
    }

    public static ItemStack with(ItemStack stack, Spell spell) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, spell.id));
        return stack;
    }

    public static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(KEY));
    }

    public static Item stoneFor(int tier) {
        return switch (tier) {
            case 2 -> ModItems.INTERMEDIATE_STONE;
            case 3 -> ModItems.ADVANCED_STONE;
            case 4 -> ModItems.SAINT_STONE;
            case 5 -> ModItems.KING_STONE;
            case 6 -> ModItems.IMPERIAL_STONE;
            default -> ModItems.ELEMENTARY_STONE;
        };
    }

    public static void tooltip(ItemStack stack, List<Component> lines) {
        Spell s = get(stack);
        if (s == null) return;
        lines.add(Component.translatable("tooltip.remushoku.spell",
                Component.translatable(s.translationKey()),
                Component.translatable("level.remushoku." + s.level.tier)).withStyle(ChatFormatting.AQUA));
    }
}

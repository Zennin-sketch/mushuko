package net.zennin.mushoku.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.MushokuMod;

public final class ModTabs {
    public static final CreativeModeTab MAIN = FabricItemGroup.builder()
            .title(Component.translatable("item_group.remushoku.mushoku_tab"))
            .icon(() -> new ItemStack(ModItems.WAND))
            .displayItems((params, output) -> {
                for (Item item : ModItems.ALL) output.accept(item);
            })
            .build();

    private ModTabs() {}

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MushokuMod.id("mushoku_tab"), MAIN);
    }
}

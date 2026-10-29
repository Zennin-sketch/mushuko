package net.zennin.mushoku.menu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.zennin.mushoku.MushokuMod;

public final class ModMenus {
    public static final MenuType<ResearchMenu> RESEARCH = Registry.register(BuiltInRegistries.MENU, MushokuMod.id("research_table"),
            new MenuType<>(ResearchMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<AlembicMenu> ALEMBIC = Registry.register(BuiltInRegistries.MENU, MushokuMod.id("alembic"),
            new MenuType<>(AlembicMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}

    public static void init() {}
}

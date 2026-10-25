package net.zennin.mushokucombat;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushokucombat.net.CombatNet;

/** Addon de combate: estamina, roll/dash con esquiva perfecta y animaciones (los combos los pone Better Combat). */
public final class MushokuCombat implements ModInitializer {
    public static final String MOD_ID = "mushoku_combat";
    public static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CombatNet.registerTypes();
        Stamina.init();
        Dodge.init();
        Sheath.init();
    }
}

package net.zennin.mushoku;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.command.MushokuCommands;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.PlayerClasses;
import net.zennin.mushoku.magic.TempBlocks;
import net.zennin.mushoku.registry.ModEntities;
import net.zennin.mushoku.magic.ManaSystem;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.registry.ModItems;
import net.zennin.mushoku.registry.ModTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MushokuMod implements ModInitializer {
    public static final String MODID = "remushoku";
    public static final Logger LOG = LoggerFactory.getLogger("Mushoku");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.init();
        ModEntities.init();
        ModTabs.init();
        ModAttachments.init();
        ModNetworking.init();
        ManaSystem.init();
        TempBlocks.init();
        PlayerClasses.init();
        MushokuCommands.init();
        LOG.info("Mushoku Tensei (Fabric) cargado");
    }
}

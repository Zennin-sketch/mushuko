package net.zennin.mushoku.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.zennin.mushoku.registry.ModBlocks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.zennin.mushoku.network.ActionPayload;
import net.zennin.mushoku.network.MagicSyncPayload;
import net.zennin.mushoku.network.ManaSyncPayload;
import net.zennin.mushoku.network.SwordSyncPayload;
import net.zennin.mushoku.registry.ModEntities;

public class MushokuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.MAGIC_CIRCLE, ModBlocks.GLOW, ModBlocks.SHELF);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(), ModBlocks.CLIMB_BLOCK, ModBlocks.ABSOLUTE_BARRIER, ModBlocks.BLOCK_SAND_CLOUD);
        ModKeys.init();
        CastingClient.init();
        SwordClient.init();
        EntityRendererRegistry.register(ModEntities.MAGIC_PROJECTILE, NoopRenderer::new);
        AnimatedRenderer.register(ModEntities.ASSAULT_DOG, "assaultdog2", "assaultdog", 0.6f);
        AnimatedRenderer.register(ModEntities.MUCKY_RAT, "muckyrat", "muckyrattxt", 0.15f);
        AnimatedRenderer.register(ModEntities.DEADLY_WRAITH, "deadlywraith", "deadlywraithtxt", 0.5f);
        AnimatedRenderer.register(ModEntities.SNOW_DRAKE, "snowdrake", "snowdraketxt", 0.5f);
        AnimatedRenderer.register(ModEntities.GIANT_TURTLE, "greattortoise", "gianttortoisetexture", 3f);
        AnimatedRenderer.register(ModEntities.SNOW_DRAKE_MOM, "snowdrakemommy", "snowdrakemommytxt", 1.5f);
        HumanoidNpcRenderer.register(ModEntities.SG_BANDIT, "sgbandittxt");
        HumanoidNpcRenderer.register(ModEntities.NG_BANDIT, "ngbandittxt");
        HumanoidNpcRenderer.register(ModEntities.WG_BANDIT, "wgbandittxt");
        HumanoidNpcRenderer.register(ModEntities.M_BANDIT, "mbandit");
        HumanoidNpcRenderer.register(ModEntities.KALMAN, "kalmanskin3");
        HumanoidNpcRenderer.register(ModEntities.GUILD_RECEPTIONIST, "guild_receptionist");
        HumanoidNpcRenderer.register(ModEntities.BLACK_SMITH_KEEP, "blacksmithskin");
        HumanoidNpcRenderer.register(ModEntities.GENERAL_STORE_KEEP, "genstoreclerk");
        HumanoidNpcRenderer.register(ModEntities.MAGIC_SHOP_KEEPER, "magicskin");

        ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.TYPE, (payload, context) -> {
            ClientManaState.mana = payload.mana();
            ClientManaState.maxMana = payload.maxMana();
            ClientManaState.playerClass = payload.playerClass();
        });
        ClientPlayNetworking.registerGlobalReceiver(MagicSyncPayload.TYPE, (payload, context) -> {
            ClientManaState.queued = payload.queued();
            ClientManaState.noElement = payload.noElement();
            ClientManaState.water = payload.water();
            ClientManaState.fire = payload.fire();
            ClientManaState.earth = payload.earth();
            ClientManaState.wind = payload.wind();
        });
        ClientPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            if (payload.action() == ActionPayload.OPEN_CLASS_SCREEN) context.client().setScreen(new ClassPickScreen());
        });
        ClientPlayNetworking.registerGlobalReceiver(SwordSyncPayload.TYPE, (payload, context) -> ClientSwordState.set(payload.data()));
        HudRenderCallback.EVENT.register(ManaBarHud::render);
    }
}

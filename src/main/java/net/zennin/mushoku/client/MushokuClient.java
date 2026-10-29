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
import net.zennin.mushoku.network.GuildSyncPayload;
import net.zennin.mushoku.network.OpenScreenPayload;
import net.zennin.mushoku.registry.ModEntities;

public class MushokuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VisualTest.init();
        net.zennin.mushoku.client.fp.FpAnimations.init();
        // Arcos y carcaj del mod: modelos 3D lisos dibujados por codigo (client/render)
        for (net.minecraft.world.item.Item it : net.zennin.mushoku.registry.ModItems.ALL) {
            if (it instanceof net.zennin.mushoku.item.MushokuBowItem bow)
                net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(bow, ItemModels::bow);
        }
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(net.zennin.mushoku.registry.ModItems.QUIVER, ItemModels::quiver);
        BowClient.init();
        // Linea de tiza: roja apagada, azul cargada (mas clara con mas senal)
        BlockRenderLayerMap.INSTANCE.putBlock(net.zennin.mushoku.registry.ModBlocks.CHALK_LINE, RenderType.cutout());
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, level, pos, tint) ->
                net.zennin.mushoku.block.ChalkLineBlock.color(state), net.zennin.mushoku.registry.ModBlocks.CHALK_LINE);
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.QuiverSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (context.client().level == null) return;
            if (context.client().level.getEntity(payload.entityId()) instanceof net.minecraft.world.entity.player.Player pl)
                net.zennin.mushoku.data.ModAttachments.setQuiver(pl, payload.quiver());
        }));
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.MAGIC_CIRCLE, ModBlocks.GLOW, ModBlocks.SHELF);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(), ModBlocks.CLIMB_BLOCK, ModBlocks.ABSOLUTE_BARRIER, ModBlocks.BLOCK_SAND_CLOUD);
        ModKeys.init();
        CastingClient.init();
        SwordClient.init();
        EntityRendererRegistry.register(ModEntities.MAGIC_PROJECTILE, MagicProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPELL_FX, SpellFxRenderer::new);
        // --- Hitogami, dimension del vacio y rasgos de raza ---
        HumanoidNpcRenderer.register(ModEntities.HITOGAMI, "hitogami");
        EntityRendererRegistry.register(ModEntities.SPIRIT_BODY, SpiritBodyRenderer::new);
        VoidSkyEffects.register();
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.ArcherSyncPayload.TYPE, (payload, context) -> ClientArcherState.set(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.HitogamiScreenPayload.TYPE, (payload, context) ->
                context.client().setScreen(new HitogamiDialogScreen(payload.mode(), payload.text())));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.AppearancePayload.TYPE, (payload, context) ->
                ClientAppearance.set(payload.entityId(), new net.zennin.mushoku.race.Appearance(payload.kind(), payload.variant(), payload.height(), payload.color())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            ClientAppearance.clear();
            ClientReadySpell.clear();
        });
        // Hechizo listo: pose de apuntar con la mano y el hechizo flotando delante (ver SpellHandLayer)
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.ReadySpellPayload.TYPE, (payload, context) ->
                ClientReadySpell.set(payload.entityId(), payload.spell()));
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(SpellHandLayer::renderFirstPerson);
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                helper.register(new CosmeticLayer(pr));
                helper.register(new QuiverLayer(pr));
                helper.register(new SpellHandLayer(pr));
            }
        });
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(
                new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override public net.minecraft.resources.ResourceLocation getFabricId() { return net.zennin.mushoku.MushokuMod.id("spell_models"); }
                    @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) { SpellModel.clearCache(); CosmeticModel.clearCache(); ClientReadySpell.reload(); }
                });
        EntityRendererRegistry.register(ModEntities.THROWN_ITEM, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.PartySyncPayload.TYPE, (payload, context) -> ClientPartyState.set(payload));
        AnimatedRenderer.register(ModEntities.SWAP_TOTEM, "tptotem", "tptotemtexture", 0.4f);
        AnimatedRenderer.register(ModEntities.ASSAULT_DOG, "assaultdog2", "assaultdog", 0.6f);
        AnimatedRenderer.register(ModEntities.MUCKY_RAT, "muckyrat", "muckyrattxt", 0.15f);
        AnimatedRenderer.register(ModEntities.DEADLY_WRAITH, "deadlywraith", "deadlywraithtxt", 0.5f);
        AnimatedRenderer.register(ModEntities.SNOW_DRAKE, "snowdrake", "snowdraketxt", 0.5f);
        AnimatedRenderer.register(ModEntities.GIANT_TURTLE, "greattortoise", "gianttortoisetexture", 3f);
        AnimatedRenderer.register(ModEntities.SNOW_DRAKE_MOM, "snowdrakemommy", "snowdrakemommytxt", 1.5f);
        for (var k : net.zennin.mushoku.companion.CompanionKind.values())
            HumanoidNpcRenderer.register(ModEntities.COMPANIONS.get(k), k.texture, k.slim);
            NpcChestLayer.registerType(ModEntities.COMPANIONS.get(k), k.texture);
        HumanoidNpcRenderer.register(ModEntities.VILLAGER_GUARD, "villager_guard");
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.RaceSyncPayload.TYPE, (payload, context) -> ClientRaceState.set(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.CompanionStatePayload.TYPE, (payload, context) -> CompanionScreen.receive(payload.state()));
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
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.RefinePayloads.Open.TYPE, (payload, context) -> RefineScreen.onOpen(payload));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.RefinePayloads.Spin.TYPE, (payload, context) -> RefineScreen.onSpin(payload));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.RefinePayloads.Result.TYPE, (payload, context) -> RefineScreen.onResult(payload));
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(net.zennin.mushoku.registry.ModBlocks.REFINING_STONE_BE, RefiningStoneRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            if (payload.action() == ActionPayload.OPEN_CLASS_SCREEN) context.client().setScreen(new ClassPickScreen());
            if (payload.action() == ActionPayload.ESCAPE && context.client().level != null)
                net.zennin.mushoku.magic.Escape.clientUntil = context.client().level.getGameTime() + net.zennin.mushoku.magic.Escape.TICKS;
        });
        ClientPlayNetworking.registerGlobalReceiver(SwordSyncPayload.TYPE, (payload, context) -> ClientSwordState.set(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(GuildSyncPayload.TYPE, (payload, context) -> ClientGuildState.set(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(OpenScreenPayload.TYPE, (payload, context) ->
                context.client().setScreen(switch (payload.screen()) {
                    case "guild" -> new GuildScreen();
                    case "guide" -> new GuideScreen();
                    case "spellbook" -> new SpellBookScreen();
                    case "demon_eye" -> new DemonEyeScreen();
                    default -> new ShopScreen(payload.screen());
                }));
        ClientPlayNetworking.registerGlobalReceiver(net.zennin.mushoku.network.ExtraSyncPayload.TYPE, (payload, context) -> ClientExtraState.set(payload.data()));
        HudRenderCallback.EVENT.register(ManaBarHud::render);
    }
}

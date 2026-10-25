package net.zennin.mushoku.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.MagicData;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.PlayerClasses;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.sword.SwordSkills;
import net.zennin.mushoku.guild.GuildSystem;
import net.zennin.mushoku.guild.Shops;
import net.zennin.mushoku.sword.SwordSystem;

public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ManaSyncPayload.TYPE, ManaSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MagicSyncPayload.TYPE, MagicSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ActionPayload.TYPE, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SwordSyncPayload.TYPE, SwordSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SwordActionPayload.TYPE, SwordActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GuildSyncPayload.TYPE, GuildSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExtraSyncPayload.TYPE, ExtraSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CompanionStatePayload.TYPE, CompanionStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RaceSyncPayload.TYPE, RaceSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CompanionActionPayload.TYPE, CompanionActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CompanionActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> net.zennin.mushoku.companion.Interactions.handle(p, payload.entityId(), payload.action()));
        });
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GuildActionPayload.TYPE, GuildActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ComboPayload.TYPE, ComboPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ComboPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> SpellCasting.onCombo(p, payload.combo()));
        });
        ServerPlayNetworking.registerGlobalReceiver(SwordActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                switch (payload.action()) {
                    case SwordActionPayload.AFFINITY -> SwordSystem.chooseAffinity(p, payload.arg());
                    case SwordActionPayload.LEARN -> SwordSystem.learn(p, payload.arg());
                    case SwordActionPayload.BIND -> SwordSystem.bind(p, payload.slot(), payload.arg());
                    case SwordActionPayload.TOGGLE -> SwordSystem.toggle(p, payload.arg());
                    case SwordActionPayload.TOUKI_LEVEL -> SwordSystem.buyTouki(p);
                    case SwordActionPayload.TOUKI_TOGGLE -> SwordSystem.toggleTouki(p);
                    case SwordActionPayload.USE_SLOT -> {
                        if (net.zennin.mushoku.sword.SwordSystem.isSwordsman(p)) SwordSkills.useSlot(p, payload.slot());
                        else net.zennin.mushoku.extra.SilentCasting.useSlot(p, payload.slot());
                    }
                    default -> {}
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(GuildActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                switch (payload.action()) {
                    case GuildActionPayload.ACCEPT -> GuildSystem.accept(p, payload.arg());
                    case GuildActionPayload.SUBMIT -> GuildSystem.submit(p);
                    case GuildActionPayload.COMPLETE -> GuildSystem.complete(p);
                    case GuildActionPayload.ABANDON -> GuildSystem.abandon(p);
                    case GuildActionPayload.RANK_UP -> GuildSystem.rankUp(p);
                    case GuildActionPayload.BUY -> Shops.trade(p, payload.shop(), payload.arg(), true);
                    case GuildActionPayload.SELL -> Shops.trade(p, payload.shop(), payload.arg(), false);
                    case GuildActionPayload.EYE_PICK -> net.zennin.mushoku.extra.DemonEyes.pick(p, payload.arg(), payload.shop());
                    case GuildActionPayload.RACE_PICK -> net.zennin.mushoku.race.RaceSystem.choose(p, payload.shop(), false);
                    case GuildActionPayload.RACE_TOGGLE -> net.zennin.mushoku.race.RaceSystem.toggle(p, payload.shop());
                    case GuildActionPayload.RACE_SKILL -> net.zennin.mushoku.race.RaceSystem.raceSkill(p);
                    default -> {}
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                switch (payload.action()) {
                    case ActionPayload.CAST -> SpellCasting.onActivate(p);
                    case ActionPayload.CHOOSE_MAGE -> PlayerClasses.choose(p, SpellCasting.CLASS_MAGE);
                    case ActionPayload.CHOOSE_SWORDSMAN -> PlayerClasses.choose(p, SpellCasting.CLASS_SWORDSMAN);
                    case ActionPayload.EYE_ONE -> net.zennin.mushoku.extra.DemonEyes.toggle(p, 0);
                    case ActionPayload.EYE_TWO -> net.zennin.mushoku.extra.DemonEyes.toggle(p, 1);
                    default -> {}
                }
            });
        });
    }

    public static void syncMana(ServerPlayer player) {
        ManaData d = ModAttachments.get(player);
        ServerPlayNetworking.send(player, new ManaSyncPayload(d.mana(), d.maxMana(), d.playerClass()));
    }

    public static void syncMagic(ServerPlayer player) {
        MagicData m = ModAttachments.magic(player);
        ServerPlayNetworking.send(player, new MagicSyncPayload(m.queued(), m.noElement(), m.water(), m.fire(), m.earth(), m.wind()));
    }

    public static void openClassScreen(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ActionPayload(ActionPayload.OPEN_CLASS_SCREEN));
    }
}

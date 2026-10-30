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
        PayloadTypeRegistry.playS2C().register(SpellXpPayload.TYPE, SpellXpPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReadySpellPayload.TYPE, ReadySpellPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayAnimPayload.TYPE, PlayAnimPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RefinePayloads.Open.TYPE, RefinePayloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(RefinePayloads.Spin.TYPE, RefinePayloads.Spin.CODEC);
        PayloadTypeRegistry.playS2C().register(RefinePayloads.Result.TYPE, RefinePayloads.Result.CODEC);
        PayloadTypeRegistry.playC2S().register(RefinePayloads.Action.TYPE, RefinePayloads.Action.CODEC);
        PayloadTypeRegistry.playC2S().register(EngravePayload.TYPE, EngravePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResearchRollPayload.TYPE, ResearchRollPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GustGlidePayload.TYPE, GustGlidePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RefinePayloads.Action.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> net.zennin.mushoku.refine.Refining.action(p, payload.pos(), payload.action()));
        });
        PayloadTypeRegistry.playS2C().register(QuiverSyncPayload.TYPE, QuiverSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ActionPayload.TYPE, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SwordSyncPayload.TYPE, SwordSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SwordActionPayload.TYPE, SwordActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GuildSyncPayload.TYPE, GuildSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExtraSyncPayload.TYPE, ExtraSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CompanionStatePayload.TYPE, CompanionStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RaceSyncPayload.TYPE, RaceSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartySyncPayload.TYPE, PartySyncPayload.CODEC);
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
                    // multiclase sin espada en la mano: conjuro silencioso
                    case SwordActionPayload.USE_SILENT -> net.zennin.mushoku.extra.SilentCasting.useSlot(p, payload.slot());
                    default -> {}
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(EngravePayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                if (p.containerMenu instanceof net.zennin.mushoku.menu.ResearchMenu menu) {
                    if (payload.spell().equals(net.zennin.mushoku.menu.ResearchMenu.RESEARCH)) menu.research(p);
                    else menu.engrave(p, net.zennin.mushoku.magic.Spell.byId(payload.spell()));
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
                    case GuildActionPayload.PARTY -> {
                        String[] a = payload.shop().split(":", 2);
                        net.zennin.mushoku.party.Parties.action(p, a[0], a.length > 1 ? a[1] : "");
                    }
                    case GuildActionPayload.COMBINE -> SpellCasting.onCombine(p);
                    case GuildActionPayload.TRADE_BOOK -> net.zennin.mushoku.guild.GuildSystem.open(p, "trading");
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
                    case ActionPayload.CHOOSE_ARCHER -> PlayerClasses.choose(p, SpellCasting.CLASS_ARCHER);
                    case ActionPayload.EYE_ONE -> net.zennin.mushoku.extra.DemonEyes.toggle(p, 0);
                    case ActionPayload.EYE_TWO -> net.zennin.mushoku.extra.DemonEyes.toggle(p, 1);
                    case ActionPayload.BOW_RELOAD -> net.zennin.mushoku.archer.BowSystem.reload(p);
                    default -> {}
                }
            });
        });
    }

    /** Manda un mensaje al jugador si su cliente lo entiende. */
    public static void send(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) ServerPlayNetworking.send(p, payload);
    }

    public static void syncMana(ServerPlayer player) {
        ManaData d = ModAttachments.get(player);
        ServerPlayNetworking.send(player, new ManaSyncPayload(d.mana(), d.maxMana(), d.playerClass()));
    }

    public static void syncMagic(ServerPlayer player) {
        MagicData m = ModAttachments.magic(player);
        ServerPlayNetworking.send(player, new MagicSyncPayload(m.queued(), m.noElement(), m.water(), m.fire(), m.earth(), m.wind()));
        broadcastReady(player, m.queued());
    }

    private static final java.util.Map<java.util.UUID, String> LAST_READY = new java.util.concurrent.ConcurrentHashMap<>();

    /** Avisa a el y a los que lo ven que tiene un hechizo listo (para la pose y el hechizo flotando en la mano). */
    public static void broadcastReady(ServerPlayer player, String spell) {
        String prev = LAST_READY.put(player.getUUID(), spell);
        if (spell.equals(prev)) return;
        ReadySpellPayload pl = new ReadySpellPayload(player.getId(), spell);
        ServerPlayNetworking.send(player, pl);
        for (ServerPlayer other : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player))
            if (other != player) ServerPlayNetworking.send(other, pl);
    }

    /** Cuando alguien empieza a ver a este jugador, le manda su hechizo listo (si tiene). */
    public static void sendReadyTo(ServerPlayer tracked, ServerPlayer viewer) {
        String spell = ModAttachments.magic(tracked).queued();
        if (!spell.isEmpty()) ServerPlayNetworking.send(viewer, new ReadySpellPayload(tracked.getId(), spell));
    }

    public static void forgetReady(ServerPlayer player) { LAST_READY.remove(player.getUUID()); }

    public static void openClassScreen(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ActionPayload(ActionPayload.OPEN_CLASS_SCREEN));
    }

    /** Animacion del addon en este jugador, para el y para los que lo ven. */
    /** Rafaga: avisa a el y a los que lo ven que va impulsado (pose de planeo). */
    public static void gustGlide(ServerPlayer p) {
        GustGlidePayload msg = new GustGlidePayload(p.getId());
        if (ServerPlayNetworking.canSend(p, GustGlidePayload.TYPE)) ServerPlayNetworking.send(p, msg);
        for (ServerPlayer o : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p))
            if (o != p && ServerPlayNetworking.canSend(o, GustGlidePayload.TYPE)) ServerPlayNetworking.send(o, msg);
    }

    public static void playAnim(ServerPlayer p, String anim) {
        PlayAnimPayload msg = new PlayAnimPayload(p.getId(), anim);
        if (ServerPlayNetworking.canSend(p, PlayAnimPayload.TYPE)) ServerPlayNetworking.send(p, msg);
        for (ServerPlayer o : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p))
            if (o != p && ServerPlayNetworking.canSend(o, PlayAnimPayload.TYPE)) ServerPlayNetworking.send(o, msg);
    }
}

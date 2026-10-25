package net.zennin.mushoku.guild;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.GuildSyncPayload;
import net.zennin.mushoku.network.OpenScreenPayload;

/** Gremio de aventureros: aceptar trabajos, entregar, completar y subir de rango. */
public final class GuildSystem {
    private static final String COUNTED = "remushoku_counted";

    private GuildSystem() {}

    public static void sync(ServerPlayer p) {
        ServerPlayNetworking.send(p, new GuildSyncPayload(ModAttachments.guild(p)));
    }

    public static void open(ServerPlayer p, String screen) {
        sync(p);
        ServerPlayNetworking.send(p, new OpenScreenPayload(screen));
    }

    private static void msg(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    public static boolean canTake(GuildData g, Jobs.Job j) { return j.rank() <= g.rank() + 1; }

    public static void accept(ServerPlayer p, int id) {
        GuildData g = ModAttachments.guild(p);
        Jobs.Job j = Jobs.byId(id);
        if (j == null || g.job() != 0) return;
        if (!canTake(g, j)) { msg(p, Component.translatable("msg.remushoku.job_rank_low").withStyle(ChatFormatting.RED)); return; }
        ModAttachments.setGuild(p, g.withJob(id, 0));
        msg(p, Component.translatable("msg.remushoku.job_accepted", Component.translatable("job.remushoku." + j.key())).withStyle(ChatFormatting.GOLD));
        sync(p);
    }

    public static void submit(ServerPlayer p) {
        GuildData g = ModAttachments.guild(p);
        Jobs.Job j = Jobs.byId(g.job());
        if (j == null || j.type() != Jobs.Type.COLLECT) return;
        int need = j.amount() - g.progress();
        int taken = 0;
        for (int i = 0; i < p.getInventory().getContainerSize() && taken < need; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.is(j.item().get())) continue;
            int k = Math.min(need - taken, s.getCount());
            s.shrink(k);
            taken += k;
        }
        if (taken == 0) { msg(p, Component.translatable("msg.remushoku.shop_no_items").withStyle(ChatFormatting.RED)); return; }
        ModAttachments.setGuild(p, g.withJob(g.job(), g.progress() + taken));
        p.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1f);
        sync(p);
    }

    public static void complete(ServerPlayer p) {
        GuildData g = ModAttachments.guild(p);
        Jobs.Job j = Jobs.byId(g.job());
        if (j == null || g.progress() < j.amount()) return;
        ModAttachments.setGuild(p, new GuildData(g.rank(), g.xp() + j.xp(), 0, 0));
        Coins.give(p, j.reward());
        p.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 1.5f);
        p.displayClientMessage(Component.translatable("msg.remushoku.job_done", Component.translatable("job.remushoku." + j.key()), j.xp())
                .withStyle(ChatFormatting.GOLD), false);
        sync(p);
    }

    public static void abandon(ServerPlayer p) {
        GuildData g = ModAttachments.guild(p);
        if (g.job() == 0) return;
        ModAttachments.setGuild(p, g.withJob(0, 0));
        msg(p, Component.translatable("msg.remushoku.job_abandoned").withStyle(ChatFormatting.GRAY));
        sync(p);
    }

    public static void rankUp(ServerPlayer p) {
        GuildData g = ModAttachments.guild(p);
        int need = GuildData.xpToRankUp(g.rank());
        if (need < 0 || g.xp() < need) return;
        ModAttachments.setGuild(p, new GuildData(g.rank() + 1, g.xp() - need, g.job(), g.progress()));
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
        p.displayClientMessage(Component.translatable("msg.remushoku.rank_up", GuildData.rankLetter(g.rank() + 1)).withStyle(ChatFormatting.GOLD), false);
        sync(p);
    }

    private static void progress(ServerPlayer p, Jobs.Job j, GuildData g) {
        if (g.progress() >= j.amount()) return;
        int n = g.progress() + 1;
        ModAttachments.setGuild(p, g.withJob(g.job(), n));
        msg(p, Component.translatable("msg.remushoku.job_progress", Component.translatable("job.remushoku." + j.key()), n, j.amount())
                .withStyle(n >= j.amount() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        sync(p);
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer p)) return;
            GuildData g = ModAttachments.guild(p);
            Jobs.Job j = Jobs.byId(g.job());
            if (j != null && j.type() == Jobs.Type.KILL && j.kill().test(entity)) progress(p, j, g);
        });

        // Patrullas: cuenta los golems que construyas cerca tuyo
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            boolean golem = (entity instanceof IronGolem ig && ig.isPlayerCreated()) || entity instanceof SnowGolem;
            if (!golem || entity.getTags().contains(COUNTED)) return;
            entity.addTag(COUNTED);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(entity) > 100) continue;
                GuildData g = ModAttachments.guild(p);
                Jobs.Job j = Jobs.byId(g.job());
                if (j != null && j.type() == Jobs.Type.GOLEM && j.kill().test(entity)) {
                    progress(p, j, g);
                    return;
                }
            }
        });
    }
}

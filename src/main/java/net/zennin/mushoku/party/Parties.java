package net.zennin.mushoku.party;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.zennin.mushoku.network.PartySyncPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grupos del addon (hasta 6): crear, invitar, aceptar/rechazar, salir y expulsar.
 * Los miembros no se hacen daño entre si y las curaciones y la magia variada los alcanzan. Se guarda con el mundo.
 */
public final class Parties extends SavedData {
    public static final int MAX = 6;
    private static Parties INSTANCE;
    private static final Map<UUID, UUID> INVITES = new HashMap<>(); // invitado -> lider

    /** lider -> miembros (incluye al lider). */
    private final Map<UUID, List<UUID>> parties = new HashMap<>();

    private static final SavedData.Factory<Parties> FACTORY = new SavedData.Factory<>(Parties::new, Parties::load, null);

    public static Parties get(MinecraftServer server) {
        if (INSTANCE == null) INSTANCE = server.overworld().getDataStorage().computeIfAbsent(FACTORY, "remushoku_parties");
        return INSTANCE;
    }

    private static Parties load(CompoundTag tag, HolderLookup.Provider reg) {
        Parties p = new Parties();
        for (Tag t : tag.getList("Parties", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            List<UUID> members = new ArrayList<>();
            for (Tag m : c.getList("Members", Tag.TAG_INT_ARRAY)) members.add(NbtUtils.loadUUID(m));
            p.parties.put(c.getUUID("Leader"), members);
        }
        return p;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        parties.forEach((leader, members) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Leader", leader);
            ListTag m = new ListTag();
            members.forEach(id -> m.add(NbtUtils.createUUID(id)));
            c.put("Members", m);
            list.add(c);
        });
        tag.put("Parties", list);
        return tag;
    }

    private UUID leaderOf(UUID player) {
        for (var e : parties.entrySet()) if (e.getValue().contains(player)) return e.getKey();
        return null;
    }

    /** Ambos jugadores estan en el mismo grupo. */
    public static boolean same(Player a, Player b) {
        if (a == b) return true;
        if (!(a instanceof ServerPlayer sa)) return false;
        Parties ps = get(sa.server);
        UUID l = ps.leaderOf(a.getUUID());
        return l != null && l.equals(ps.leaderOf(b.getUUID()));
    }

    private static void msg(ServerPlayer p, String key, ChatFormatting c, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(c), false);
    }

    public static void action(ServerPlayer p, String action, String arg) {
        Parties ps = get(p.server);
        UUID me = p.getUUID();
        UUID leader = ps.leaderOf(me);
        switch (action) {
            case "create" -> {
                if (leader != null) return;
                ps.parties.put(me, new ArrayList<>(List.of(me)));
                msg(p, "msg.remushoku.party_created", ChatFormatting.GREEN);
            }
            case "invite" -> {
                ServerPlayer t = p.server.getPlayerList().getPlayerByName(arg);
                if (t == null || t == p || leader == null || !leader.equals(me)) return;
                if (ps.leaderOf(t.getUUID()) != null) { msg(p, "msg.remushoku.party_has_group", ChatFormatting.GRAY, t.getDisplayName()); return; }
                if (ps.parties.get(me).size() >= MAX) { msg(p, "msg.remushoku.party_full", ChatFormatting.RED); return; }
                INVITES.put(t.getUUID(), me);
                msg(p, "msg.remushoku.party_invited", ChatFormatting.GREEN, t.getDisplayName());
                msg(t, "msg.remushoku.party_invite", ChatFormatting.AQUA, p.getDisplayName());
                sync(t);
            }
            case "accept" -> {
                UUID from = INVITES.remove(me);
                if (from == null || leader != null || !ps.parties.containsKey(from)) return;
                List<UUID> members = ps.parties.get(from);
                if (members.size() >= MAX) return;
                members.add(me);
                notifyAll(p.server, from, "msg.remushoku.party_joined", p.getDisplayName());
            }
            case "decline" -> INVITES.remove(me);
            case "leave" -> {
                if (leader == null) return;
                List<UUID> members = ps.parties.get(leader);
                members.remove(me);
                msg(p, "msg.remushoku.party_left", ChatFormatting.GRAY);
                if (leader.equals(me) || members.isEmpty()) {
                    ps.parties.remove(leader);
                    if (!members.isEmpty()) {
                        UUID next = members.get(0);
                        ps.parties.put(next, members);
                        notifyAll(p.server, next, "msg.remushoku.party_new_leader", nameOf(p.server, next));
                    }
                } else notifyAll(p.server, leader, "msg.remushoku.party_member_left", p.getDisplayName());
                syncGroup(p.server, members);
            }
            case "kick" -> {
                ServerPlayer t = p.server.getPlayerList().getPlayerByName(arg);
                if (t == null || leader == null || !leader.equals(me) || t == p) return;
                if (ps.parties.get(me).remove(t.getUUID())) msg(t, "msg.remushoku.party_kicked", ChatFormatting.RED);
                sync(t);
            }
            default -> { return; }
        }
        ps.setDirty();
        UUID l = ps.leaderOf(me);
        syncGroup(p.server, l == null ? List.of(me) : ps.parties.get(l));
        sync(p);
    }

    private static Component nameOf(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        return p == null ? Component.literal("?") : p.getDisplayName();
    }

    private static void notifyAll(MinecraftServer s, UUID leader, String key, Object arg) {
        for (UUID id : get(s).parties.getOrDefault(leader, List.of())) {
            ServerPlayer m = s.getPlayerList().getPlayer(id);
            if (m != null) msg(m, key, ChatFormatting.AQUA, arg);
        }
    }

    private static void syncGroup(MinecraftServer s, List<UUID> ids) {
        for (UUID id : new ArrayList<>(ids)) {
            ServerPlayer m = s.getPlayerList().getPlayer(id);
            if (m != null) sync(m);
        }
    }

    public static void sync(ServerPlayer p) {
        Parties ps = get(p.server);
        UUID leader = ps.leaderOf(p.getUUID());
        List<String> names = new ArrayList<>();
        if (leader != null) for (UUID id : ps.parties.get(leader)) {
            ServerPlayer m = p.server.getPlayerList().getPlayer(id);
            names.add(m != null ? m.getGameProfile().getName() : "(" + id.toString().substring(0, 8) + ")");
        }
        UUID inv = INVITES.get(p.getUUID());
        ServerPlayer invFrom = inv == null ? null : p.server.getPlayerList().getPlayer(inv);
        String leaderName = leader == null ? "" : (p.server.getPlayerList().getPlayer(leader) != null
                ? p.server.getPlayerList().getPlayer(leader).getGameProfile().getName() : "?");
        ServerPlayNetworking.send(p, new PartySyncPayload(leaderName, names, invFrom == null ? "" : invFrom.getGameProfile().getName()));
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> { INSTANCE = null; sync(h.getPlayer()); });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) ->
                !(victim instanceof ServerPlayer v && source.getEntity() instanceof ServerPlayer a && a != v && same(a, v)));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> INSTANCE = null);
    }

    /** Cura a los miembros del grupo a 30 bloques (curaciones de grupo del addon). */
    public static void healMembers(ServerPlayer p, float amount) {
        for (Player o : p.serverLevel().players()) {
            if (o != p && same(p, o) && o.distanceToSqr(p) <= 900) {
                o.heal(amount);
                p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, o.getX(), o.getY() + 2, o.getZ(), 3, 0.3, 0.2, 0.3, 0);
            }
        }
    }
}

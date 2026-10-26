package net.zennin.mushoku.hitogami;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.PlayerClasses;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.network.AppearancePayload;
import net.zennin.mushoku.network.HitogamiActionPayload;
import net.zennin.mushoku.network.HitogamiScreenPayload;
import net.zennin.mushoku.race.Appearance;
import net.zennin.mushoku.race.Race;
import net.zennin.mushoku.race.RaceSystem;
import net.zennin.mushoku.registry.ModEntities;
import net.zennin.mushoku.sword.SwordSystem;
import net.zennin.mushoku.world.VoidDimension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hitogami y la dimension del vacio:
 *  - Al entrar por primera vez (sin raza) apareces frente a Hitogami. Al hablarle te presenta el mundo y abre la creacion
 *    de personaje (raza, rasgo, clase). Al terminar te manda al Overworld.
 *  - Cada vez que duermes hay 1 en 10 de sonar con el: te da las coordenadas de un lugar interesante y con "Despertar"
 *    vuelves al lado de tu cama.
 */
public final class HitogamiSystem {
    public static final int MODE_INTRO = 0, MODE_ORACLE = 1;
    public static final float DREAM_CHANCE = 0.10f;
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Map<UUID, String> PROPHECY = new HashMap<>();

    private HitogamiSystem() {}

    public static HitogamiData data(ServerPlayer p) { return ModAttachments.hitogami(p); }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(HitogamiScreenPayload.TYPE, HitogamiScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AppearancePayload.TYPE, AppearancePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HitogamiActionPayload.TYPE, HitogamiActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HitogamiActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                if (payload.action() == HitogamiActionPayload.CREATE) create(p, payload);
                else if (payload.action() == HitogamiActionPayload.WAKE) wake(p);
            });
        });
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Oracle());

        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> {
            PENDING.put(h.getPlayer().getUUID(), 30);
            sendAppearance(h.getPlayer(), h.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> PROPHECY.remove(h.getPlayer().getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> sendAppearance(newP, newP));
        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> {
            if (tracked instanceof ServerPlayer other) sendAppearance(other, player);
        });
        // En el vacio no se recibe dano ni se ponen bloques
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amt) -> !(e instanceof ServerPlayer && VoidDimension.is(e.level())));
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                VoidDimension.is(level) && !player.isCreative() ? InteractionResult.FAIL : InteractionResult.PASS);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!PENDING.isEmpty()) {
                PENDING.replaceAll((id, t) -> t - 1);
                PENDING.entrySet().removeIf(e -> {
                    if (e.getValue() > 0) return false;
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) onJoinReady(p);
                    return true;
                });
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                // A los 2 segundos de acostarse se tira el dado del sueno
                if (p.isSleeping() && p.getSleepTimer() == 40 && !VoidDimension.is(p.level()) && data(p).created()
                        && p.getRandom().nextFloat() < DREAM_CHANCE) dream(p);
                // Por si alguien se cae del piso
                if (VoidDimension.is(p.level()) && p.getY() < -20) p.teleportTo(VoidDimension.ARRIVE_X, VoidDimension.ARRIVE_Y, VoidDimension.ARRIVE_Z);
            }
        });
    }

    private static void onJoinReady(ServerPlayer p) {
        HitogamiData d = data(p);
        if (d.created()) {
            if (VoidDimension.is(p.level())) ensureHitogami((ServerLevel) p.level());
            return;
        }
        // Mundos viejos: si ya tenia raza, se da por creado el personaje
        if (!RaceSystem.data(p).race().isEmpty()) {
            ModAttachments.setHitogami(p, d.withCreated());
            return;
        }
        beginIntro(p);
    }

    /** Primera vez: al vacio, frente a Hitogami. */
    public static void beginIntro(ServerPlayer p) {
        ServerLevel v = VoidDimension.level(p.server);
        if (v == null) return;
        if (!VoidDimension.is(p.level()) || p.distanceToSqr(VoidDimension.ARRIVE_X, VoidDimension.ARRIVE_Y, VoidDimension.ARRIVE_Z) > 400)
            p.teleportTo(v, VoidDimension.ARRIVE_X, VoidDimension.ARRIVE_Y, VoidDimension.ARRIVE_Z, VoidDimension.ARRIVE_YAW, 0);
        ensureHitogami(v);
        p.displayClientMessage(Component.translatable("msg.remushoku.hitogami.hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    /** Pone a Hitogami en el centro si no esta (no se guarda en el mundo, asi nunca hay dos). */
    public static void ensureHitogami(ServerLevel v) {
        v.getChunk(0, 0);
        if (!v.getEntities(ModEntities.HITOGAMI, new AABB(-16, -8, -16, 16, 16, 16), e -> true).isEmpty()) return;
        HitogamiEntity h = ModEntities.HITOGAMI.create(v);
        if (h == null) return;
        h.moveTo(0.5, 1, 0.5, 0, 0);
        h.setYHeadRot(0);
        h.setYBodyRot(0);
        v.addFreshEntity(h);
    }

    /** Clic en Hitogami. */
    public static void talk(ServerPlayer p) {
        HitogamiData d = data(p);
        if (!d.created()) {
            ServerPlayNetworking.send(p, new HitogamiScreenPayload(MODE_INTRO, ""));
            return;
        }
        String text = PROPHECY.get(p.getUUID());
        if (text == null) {
            text = makeProphecy(p, d);
            PROPHECY.put(p.getUUID(), text);
        }
        ServerPlayNetworking.send(p, new HitogamiScreenPayload(MODE_ORACLE, text));
    }

    private static String makeProphecy(ServerPlayer p, HitogamiData d) {
        ResourceKey<Level> dim = d.dreaming() && !d.retDim().isEmpty()
                ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.retDim())) : Level.OVERWORLD;
        BlockPos origin = d.dreaming() ? BlockPos.containing(d.rx(), d.ry(), d.rz()) : p.server.overworld().getSharedSpawnPos();
        String t = Oracle.prophecy(p, dim, origin);
        return t != null ? t : "Mmm... hoy el mundo está nublado, no veo nada interesante cerca de ti. Vuelve otra noche.";
    }

    /** 1 en 10 al dormir: al vacio por un rato. */
    public static void dream(ServerPlayer p) {
        ServerLevel from = p.serverLevel();
        BlockPos bed = p.getSleepingPos().orElse(p.blockPosition());
        BlockState st = from.getBlockState(bed);
        Direction dir = st.getBlock() instanceof BedBlock ? st.getValue(BedBlock.FACING) : p.getDirection();
        Vec3 ret = BedBlock.findStandUpPosition(EntityType.PLAYER, from, bed, dir, p.getYRot()).orElse(Vec3.atBottomCenterOf(bed.above()));
        p.stopSleepInBed(true, true);
        ModAttachments.setHitogami(p, data(p).dream(from.dimension().location().toString(), ret.x, ret.y, ret.z, p.getYRot()));
        PROPHECY.remove(p.getUUID());
        ServerLevel v = VoidDimension.level(p.server);
        if (v == null) return;
        p.teleportTo(v, VoidDimension.ARRIVE_X, VoidDimension.ARRIVE_Y, VoidDimension.ARRIVE_Z, VoidDimension.ARRIVE_YAW, 0);
        ensureHitogami(v);
        v.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
        p.displayClientMessage(Component.translatable("msg.remushoku.hitogami.dream").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
        // Se busca el lugar ahora (puede tardar un instante) asi al hablarle la respuesta es inmediata
        PROPHECY.put(p.getUUID(), makeProphecy(p, data(p)));
    }

    /** Boton "Despertar": vuelve al lado de la cama. */
    public static void wake(ServerPlayer p) {
        if (!VoidDimension.is(p.level())) return;
        HitogamiData d = data(p);
        if (!d.created()) return;
        MinecraftServer server = p.server;
        ServerLevel dest = null;
        if (d.dreaming() && !d.retDim().isEmpty())
            dest = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.retDim())));
        if (dest != null) p.teleportTo(dest, d.rx(), d.ry(), d.rz(), d.ryaw(), 0);
        else toWorldSpawn(p);
        ModAttachments.setHitogami(p, d.awake());
        String text = PROPHECY.remove(p.getUUID());
        if (text != null) {
            // La profecia queda en el chat para no olvidar las coordenadas
            p.sendSystemMessage(Component.literal("Hitogami: ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                    .append(Component.literal(text.replace("\n\n", " ")).withStyle(ChatFormatting.GRAY)));
        }
        // Si estas solo, al despertar ya es de dia (como si hubieras dormido toda la noche)
        ServerLevel ow = server.overworld();
        if (server.getPlayerCount() == 1 && d.dreaming()) {
            long t = ow.getDayTime();
            if (t % 24000 > 12000) ow.setDayTime(t + 24000 - t % 24000);
            if (ow.isRaining()) ow.setWeatherParameters(0, 0, false, false);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static void toWorldSpawn(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
        p.teleportTo(ow, sp.getX() + 0.5, y, sp.getZ() + 0.5, ow.getSharedSpawnAngle(), 0);
    }

    /** Boton "Confirmar" de la creacion de personaje. */
    public static void create(ServerPlayer p, HitogamiActionPayload a) {
        HitogamiData d = data(p);
        if (d.created()) return;
        Race r = Race.byKey(a.race());
        if (r == null) return;
        int cls = a.cls() == SpellCasting.CLASS_SWORDSMAN ? SpellCasting.CLASS_SWORDSMAN : SpellCasting.CLASS_MAGE;
        if (RaceSystem.data(p).race().isEmpty()) RaceSystem.choose(p, r.key, false);
        setAppearance(p, Appearance.forRace(r, a.variant(), a.height(), a.color()));
        PlayerClasses.choose(p, cls);
        if (cls == SpellCasting.CLASS_SWORDSMAN && !a.affinity().isEmpty()) SwordSystem.chooseAffinity(p, a.affinity());
        ModAttachments.setHitogami(p, d.withCreated());
        toWorldSpawn(p);
        p.displayClientMessage(Component.translatable("msg.remushoku.hitogami.reborn").withStyle(ChatFormatting.GOLD), false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.6f);
    }

    public static void setAppearance(ServerPlayer p, Appearance a) {
        ModAttachments.setAppearance(p, a);
        sendAppearance(p, p);
        for (ServerPlayer other : PlayerLookup.tracking(p)) sendAppearance(p, other);
    }

    private static void sendAppearance(ServerPlayer of, ServerPlayer to) {
        Appearance a = ModAttachments.appearance(of);
        ServerPlayNetworking.send(to, new AppearancePayload(of.getId(), a.kind(), a.variant(), a.height(), a.color()));
    }
}

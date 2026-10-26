package net.zennin.mushoku.extra;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.data.ExtraData;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ExtraSyncPayload;
import net.zennin.mushoku.registry.ModBlocks;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;
import java.util.Optional;

/**
 * Los 5 ojos demoniacos del original (dos ranuras, teclas G y H):
 *  Clarividencia: modo espectador hasta apagarlo; al apagarlo volves a donde estabas.
 *  Absorcion: gasta tu mana, pero le roba mana a quien miras.
 *  Identificacion: muestra el nombre del bloque o criatura que miras (y su vida).
 *  Vacio Absoluto: crea una esfera irrompible que te encierra a vos y a los que esten cerca hasta apagarlo.
 *  Poder Magico: hace brillar a cualquier criatura que mires, incluso a traves de paredes.
 */
public final class DemonEyes {
    public static final List<String> EYES = List.of("clairvoyance", "absorption", "identification", "absolute_void", "magic_power");
    private static final int VOID_RADIUS = 7;

    private DemonEyes() {}

    public static void sync(ServerPlayer p) {
        ServerPlayNetworking.send(p, new ExtraSyncPayload(ModAttachments.extra(p)));
    }

    private static void msg(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    /** Pone un ojo en una ranura (consume un Ojo Demoniaco de la mano). */
    public static void pick(ServerPlayer p, int slot, String eye) {
        if (slot < 0 || slot > 1 || !EYES.contains(eye)) return;
        ExtraData d = ModAttachments.extra(p);
        if (d.on(slot)) toggle(p, slot); // apaga el ojo anterior antes de cambiarlo
        d = ModAttachments.extra(p);
        boolean free = p.getAbilities().instabuild;
        if (!free) {
            var hand = p.getMainHandItem().is(ModItems.DEMON_EYE) ? p.getMainHandItem() : p.getOffhandItem();
            if (!hand.is(ModItems.DEMON_EYE)) return;
            hand.shrink(1);
        }
        ModAttachments.setExtra(p, d.withEye(slot, eye));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1f, 0.6f);
        p.displayClientMessage(Component.translatable("msg.remushoku.eye_set", slot + 1, Component.translatable("eye.remushoku." + eye))
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        sync(p);
    }

    /** Teclas G / H: enciende o apaga el ojo de esa ranura. */
    public static void toggle(ServerPlayer p, int slot) {
        ExtraData d = ModAttachments.extra(p);
        String eye = d.eye(slot);
        if (eye.isEmpty()) { msg(p, Component.translatable("msg.remushoku.eye_empty", slot + 1).withStyle(ChatFormatting.GRAY)); return; }
        boolean on = !d.on(slot);
        switch (eye) {
            case "clairvoyance" -> {
                if (on) {
                    d = d.withClair(Optional.of(new ExtraData.Clair(p.getX(), p.getY(), p.getZ(),
                            p.level().dimension().location().toString(), p.gameMode.getGameModeForPlayer().getId())));
                    spawnBody(p);
                    p.setGameMode(GameType.SPECTATOR);
                } else {
                    d = returnFromClair(p, d);
                }
            }
            case "absolute_void" -> {
                if (on) {
                    ManaData m = ModAttachments.get(p);
                    if (m.mana() < 50 && !p.getAbilities().instabuild) { msg(p, Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED)); return; }
                    if (!p.getAbilities().instabuild) ModAttachments.set(p, m.withMana(m.mana() - 50));
                    BlockPos c = p.blockPosition().above();
                    sphere(p.serverLevel(), c, true);
                    d = d.withVoid(Optional.of(c));
                    p.level().playSound(null, c, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.6f);
                } else {
                    d.voidCenter().ifPresent(c -> sphere(p.serverLevel(), c, false));
                    d = d.withVoid(Optional.empty());
                    p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5f, 0.6f);
                }
            }
            default -> p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_COW_BELL.value(), SoundSource.PLAYERS, 1f, on ? 0.8f : 0.6f);
        }
        ModAttachments.setExtra(p, d.withOn(slot, on));
        msg(p, Component.translatable(on ? "msg.remushoku.eye_on" : "msg.remushoku.eye_off", Component.translatable("eye.remushoku." + eye))
                .withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        sync(p);
    }

    /** Cuerpo de cada jugador en Clarividencia (dueno -> entidad). */
    private static final java.util.Map<java.util.UUID, net.zennin.mushoku.entity.SpiritBodyEntity> BODIES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Deja el cuerpo quieto donde estabas, con tu equipo puesto. */
    private static void spawnBody(ServerPlayer p) {
        removeBody(p);
        var body = new net.zennin.mushoku.entity.SpiritBodyEntity(net.zennin.mushoku.registry.ModEntities.SPIRIT_BODY, p.level());
        body.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        body.setYHeadRot(p.getYHeadRot());
        body.yBodyRot = p.yBodyRot;
        body.setOwner(p.getUUID());
        body.setCustomName(p.getName());
        body.setCustomNameVisible(false);
        for (net.minecraft.world.entity.EquipmentSlot s : net.minecraft.world.entity.EquipmentSlot.values())
            body.setItemSlot(s, p.getItemBySlot(s).copy());
        for (net.minecraft.world.entity.EquipmentSlot s : net.minecraft.world.entity.EquipmentSlot.values()) body.setDropChance(s, 0f);
        p.level().addFreshEntity(body);
        BODIES.put(p.getUUID(), body);
        // los que ya te estaban persiguiendo siguen yendo a por tu cuerpo
        for (net.minecraft.world.entity.Mob m : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(48),
                m -> m.getTarget() == p))
            m.setTarget(body);
    }

    private static void removeBody(ServerPlayer p) {
        var b = BODIES.remove(p.getUUID());
        if (b != null && !b.isRemoved()) b.discard();
    }

    /** El cuerpo sigue teniendo dueno en Clarividencia. */
    public static boolean hasBody(net.zennin.mushoku.entity.SpiritBodyEntity b) {
        return b.owner().map(id -> BODIES.get(id) == b).orElse(false);
    }

    /** Golpearon tu cuerpo: volves a el (sale la Clarividencia) y recibis el golpe. */
    public static void bodyHit(ServerPlayer p, net.minecraft.world.damagesource.DamageSource src, float amount) {
        ExtraData d = ModAttachments.extra(p);
        for (int slot = 0; slot < 2; slot++) {
            if ("clairvoyance".equals(d.eye(slot)) && d.on(slot)) {
                toggle(p, slot);
                break;
            }
        }
        removeBody(p);
        p.invulnerableTime = 0;
        p.hurt(src, amount);
    }

    private static ExtraData returnFromClair(ServerPlayer p, ExtraData d) {
        removeBody(p);
        if (d.clair().isEmpty()) return d;
        ExtraData.Clair c = d.clair().get();
        ServerLevel target = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.dim())));
        if (target == null) target = p.serverLevel();
        p.teleportTo(target, c.x(), c.y(), c.z(), p.getYRot(), p.getXRot());
        GameType mode = GameType.byId(c.mode());
        p.setGameMode(mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode);
        return d.withClair(Optional.empty());
    }

    /** Esfera hueca de barrera absoluta (solo ocupa el aire; al quitarla solo borra la barrera). */
    private static void sphere(ServerLevel level, BlockPos c, boolean place) {
        int r = VOID_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            double dist = Math.sqrt(p.distSqr(c));
            if (dist > r || dist < r - 1) continue;
            if (place) {
                if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, ModBlocks.ABSOLUTE_BARRIER.defaultBlockState(), Block.UPDATE_ALL);
            } else if (level.getBlockState(p).is(ModBlocks.ABSOLUTE_BARRIER)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Entity looked(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult hr = ProjectileUtil.getEntityHitResult(p, eye, eye.add(look.scale(range)), box,
                e -> e instanceof LivingEntity && e != p && e.isAlive() && !e.isSpectator(), range * range);
        return hr == null ? null : hr.getEntity();
    }

    public static void init() {
        // Los mobs hostiles tambien van por el cuerpo que queda quieto en Clarividencia (como si fueras vos)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e, world) -> {
            if (e instanceof net.minecraft.world.entity.Mob mob && e instanceof net.minecraft.world.entity.monster.Enemy)
                ((net.zennin.mushoku.mixin.MobAccessor) mob).mushoku$targetSelector().addGoal(2,
                        new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob, net.zennin.mushoku.entity.SpiritBodyEntity.class, true));
        });
        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> sync(h.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> onDeathOrLeave(h.getPlayer()));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (e instanceof ServerPlayer sp) onDeathOrLeave(sp);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            if (t % 10 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ExtraData d = ModAttachments.extra(p);
                if (!d.on1() && !d.on2()) continue;
                if (d.active("magic_power")) {
                    Entity e = looked(p, 80);
                    if (e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                }
                if (d.active("absorption") && t % 20 == 0) absorb(p);
            }
        });
    }

    private static void absorb(ServerPlayer p) {
        ManaData m = ModAttachments.get(p);
        double cost = Math.max(1, m.maxMana() * 0.01);
        if (m.mana() < cost) {
            ExtraData d = ModAttachments.extra(p);
            int slot = d.eye1().equals("absorption") ? 0 : 1;
            ModAttachments.setExtra(p, d.withOn(slot, false));
            msg(p, Component.translatable("msg.remushoku.eye_no_mana").withStyle(ChatFormatting.RED));
            sync(p);
            return;
        }
        double gain = 0;
        Entity e = looked(p, 30);
        if (e instanceof ServerPlayer other) {
            ManaData om = ModAttachments.get(other);
            gain = Math.min(30, om.mana());
            ModAttachments.set(other, om.withMana(om.mana() - gain));
        } else if (e instanceof LivingEntity le) {
            gain = 5;
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        }
        ModAttachments.set(p, m.withMana(m.mana() - cost + gain));
        if (e != null && gain > 0) {
            ServerLevel sl = p.serverLevel();
            Vec3 from = e.position().add(0, e.getBbHeight() / 2, 0), to = p.position().add(0, 1, 0);
            for (int i = 0; i <= 10; i++) {
                Vec3 pt = from.lerp(to, i / 10.0);
                sl.sendParticles(ParticleTypes.SQUID_INK, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    /** Si muere con la Clarividencia o el Vacio activos, se limpian. */
    public static void onDeathOrLeave(ServerPlayer p) {
        ExtraData d = returnFromClair(p, ModAttachments.extra(p));
        d.voidCenter().ifPresent(c -> sphere(p.serverLevel(), c, false));
        ModAttachments.setExtra(p, d.withVoid(Optional.empty()).withClair(Optional.empty()).withOn(0, false).withOn(1, false));
    }

    /** "Un jugador puede nacer con un ojo": 10% al elegir clase por primera vez. */
    public static void maybeBornWithEye(ServerPlayer p) {
        ExtraData d = ModAttachments.extra(p);
        if (!d.eye1().isEmpty() || p.getRandom().nextFloat() >= 0.10f) return;
        String eye = EYES.get(p.getRandom().nextInt(EYES.size()));
        ModAttachments.setExtra(p, d.withEye(0, eye));
        p.displayClientMessage(Component.translatable("msg.remushoku.born_eye", Component.translatable("eye.remushoku." + eye))
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        sync(p);
    }
}

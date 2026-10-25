package net.zennin.mushoku.sword;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.data.SwordData;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.magic.Tasks;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Habilidades activas del espadachin (teclas Z / X / C). */
public final class SwordSkills {
    private static final Map<UUID, Map<SwordSkill, Long>> COOLDOWNS = new HashMap<>();
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.3f, 0.6f, 1f), 1.2f);

    private SwordSkills() {}

    /** Espadas siempre; con Maestria de Arsenal encendida, tambien hachas, picos y demas herramientas. */
    public static boolean isWeapon(ItemStack s, SwordData d) {
        if (s.isEmpty()) return false;
        if (s.getItem() instanceof SwordItem || s.is(ItemTags.SWORDS)) return true;
        return d.knows(SwordSkill.ARSENAL_MASTERY.id) && d.isOn(SwordSkill.ARSENAL_MASTERY.id) && s.getItem() instanceof TieredItem;
    }

    /** Daño base del arma (1 + bonus de daño del item). */
    public static double itemDamage(ItemStack s) {
        double d = 1;
        ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value() && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                d += e.modifier().amount();
        }
        return d;
    }

    /** Hace que el mob suelte lo que tiene en la mano. */
    public static void disarm(LivingEntity e) {
        if (!(e instanceof Mob mob)) return;
        ItemStack held = mob.getMainHandItem();
        if (held.isEmpty()) return;
        ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 1, mob.getZ(), held.copy());
        drop.setPickUpDelay(20);
        mob.level().addFreshEntity(drop);
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public static void useSlot(ServerPlayer p, int slot) {
        if (!SwordSystem.isSwordsman(p)) return;
        SwordData d = ModAttachments.sword(p);
        SwordSkill s = SwordSkill.byId(d.slot(slot));
        if (s == null || !d.knows(s.id)) {
            msg(p, Component.translatable("msg.remushoku.slot_empty", slot + 1).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!isWeapon(p.getMainHandItem(), d)) {
            msg(p, Component.translatable("msg.remushoku.need_sword").withStyle(ChatFormatting.RED));
            return;
        }
        long now = p.server.getTickCount();
        Map<SwordSkill, Long> cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        long ready = cds.getOrDefault(s, 0L);
        if (ready > now) {
            msg(p, Component.translatable("msg.remushoku.cooldown", Component.translatable(s.translationKey()),
                    (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY));
            return;
        }
        ManaData m = ModAttachments.get(p);
        if (m.mana() < s.mana && !p.getAbilities().instabuild) {
            msg(p, Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED));
            return;
        }
        if (!run(p, s, d)) return;
        if (!p.getAbilities().instabuild) ModAttachments.set(p, m.withMana(m.mana() - s.mana));
        cds.put(s, now + s.cooldown);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private static boolean run(ServerPlayer p, SwordSkill s, SwordData d) {
        ServerLevel level = p.serverLevel();
        float atk = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (s) {
            case ARM_CHOP -> {
                LivingEntity t = target(p, 4.5);
                if (t == null) return noTarget(p);
                hit(p, t, atk * 1.5f + 4);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
                disarm(t);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1, t.getZ(), 3, 0.3, 0.3, 0.3, 0);
                sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f);
            }
            case QUICK_STRIKES -> {
                if (target(p, 4.5) == null) return noTarget(p);
                Tasks.repeat(0, 3, 15, tick -> {
                    LivingEntity t = target(p, 4.5);
                    if (t == null || !p.isAlive()) return false;
                    hit(p, t, atk * 0.6f);
                    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1, t.getZ(), 1, 0.2, 0.2, 0.2, 0);
                    sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f + p.getRandom().nextFloat() * 0.4f);
                    return true;
                });
            }
            case LONGSWORD_SILENCE -> dash(p, 8, atk + 8, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP);
            case LONGSWORD_LIGHT -> dash(p, 16, atk * 1.5f + 16, ParticleTypes.END_ROD, SoundEvents.TRIDENT_RIPTIDE_3.value());
            case STEADFAST -> {
                List<MobEffectInstance> bad = p.getActiveEffects().stream()
                        .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL).toList();
                for (MobEffectInstance e : bad) p.removeEffect(e.getEffect());
                p.clearFire();
                p.setTicksFrozen(0);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 25, 0.5, 0.7, 0.5, 0);
                sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            }
            case TRUE_OBSERVATION -> {
                for (LivingEntity e : around(p, 32)) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                sound(p, SoundEvents.BEACON_POWER_SELECT, 1.6f);
            }
            case FLOW -> {
                SwordSystem.FLOW.put(p.getUUID(), p.server.getTickCount() + 200L);
                level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.6, 0.5, 0.1);
                sound(p, SoundEvents.PLAYER_SPLASH, 1.3f);
                msg(p, Component.translatable("msg.remushoku.flow_ready").withStyle(ChatFormatting.AQUA));
            }
            case AURA_REPULSION -> {
                sound(p, SoundEvents.BEACON_ACTIVATE, 1.5f);
                Tasks.repeat(0, 2, 200, tick -> {
                    if (!p.isAlive()) return false;
                    for (LivingEntity e : around(p, 5)) {
                        Vec3 dir = e.position().subtract(p.position()).multiply(1, 0, 1);
                        if (dir.lengthSqr() < 0.01) dir = new Vec3(1, 0, 0);
                        dir = dir.normalize().scale(0.6);
                        e.setDeltaMovement(dir.x, Math.max(e.getDeltaMovement().y, 0.1), dir.z);
                        e.hurtMarked = true;
                    }
                    if (tick % 6 == 0) ring(level, p.position(), 5, BLUE, 40);
                    return true;
                });
            }
            case AURA_MOTION -> {
                for (LivingEntity e : around(p, 8)) {
                    hit(p, e, 6);
                    Vec3 dir = e.position().subtract(p.position()).multiply(1, 0, 1);
                    if (dir.lengthSqr() < 0.01) dir = new Vec3(1, 0, 0);
                    dir = dir.normalize().scale(2.5);
                    e.setDeltaMovement(dir.x, 0.6, dir.z);
                    e.hurtMarked = true;
                }
                for (int r = 2; r <= 8; r += 2) ring(level, p.position().add(0, 0.3, 0), r, ParticleTypes.CLOUD, 12 * r);
                sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f);
            }
            case DEPRIVATION_KINGDOM -> {
                Vec3 center = p.position();
                Map<UUID, Integer> last = new HashMap<>();
                sound(p, SoundEvents.BEACON_ACTIVATE, 0.8f);
                Tasks.repeat(0, 5, 300, tick -> {
                    if (!p.isAlive()) return false;
                    if (tick % 10 == 0) ring(level, center.add(0, 0.2, 0), 4, BLUE, 60);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(4, 3, 4),
                            e -> e != p && e.isAlive() && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == p)))) {
                        if (e.distanceToSqr(center) > 16) continue;
                        if (tick - last.getOrDefault(e.getUUID(), -100) < 20) continue;
                        last.put(e.getUUID(), tick);
                        hit(p, e, (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.6f);
                    }
                    return true;
                });
            }
            case SWORD_THROW -> {
                MagicProjectile proj = new MagicProjectile(level, p, MagicProjectile.WIND, (float) (itemDamage(p.getMainHandItem()) * 2));
                proj.knockback(0.6f);
                proj.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                proj.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.2f, 0f);
                level.addFreshEntity(proj);
                sound(p, SoundEvents.TRIDENT_THROW.value(), 1.2f);
            }
            case TOUKI_MANIPULATION -> {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1 + d.touki() / 3));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
                level.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
                sound(p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.85f);
            }
            case TOUKI_AMPLIFICATION -> {
                if (!d.toukiOn()) {
                    msg(p, Component.translatable("msg.remushoku.need_touki").withStyle(ChatFormatting.RED));
                    return false;
                }
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
                sound(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.6f);
            }
            default -> { return false; }
        }
        return true;
    }

    // ---------- Ayudas ----------

    private static void dash(ServerPlayer p, double dist, float dmg, ParticleOptions particle, SoundEvent snd) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1e-4) return;
        dir = dir.normalize();
        Vec3 start = p.position();
        Vec3 from = start.add(0, 0.5, 0);
        BlockHitResult clip = level.clip(new ClipContext(from, from.add(dir.scale(dist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double travel = clip.getType() == HitResult.Type.MISS ? dist : Math.max(0, clip.getLocation().distanceTo(from) - 0.7);
        Vec3 end = start.add(dir.scale(travel));
        Set<LivingEntity> hitSet = new HashSet<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.8), e -> e != p && e.isAlive())) {
            Vec3 c = e.position();
            double tt = Math.max(0, Math.min(travel, c.subtract(start).dot(dir)));
            if (c.distanceTo(start.add(dir.scale(tt))) <= 1.8) hitSet.add(e);
        }
        for (double s = 0; s <= travel; s += 0.8) {
            Vec3 pt = start.add(dir.scale(s));
            level.sendParticles(particle, pt.x, pt.y + 1, pt.z, 2, 0.2, 0.3, 0.2, 0);
        }
        p.teleportTo(end.x, end.y, end.z);
        p.fallDistance = 0;
        for (LivingEntity e : hitSet) hit(p, e, dmg);
        level.playSound(null, end.x, end.y, end.z, snd, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static void hit(ServerPlayer p, LivingEntity t, float dmg) {
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), dmg);
    }

    private static LivingEntity target(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult hr = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity && e != p && e.isAlive() && !e.isSpectator(), range * range);
        return hr == null ? null : (LivingEntity) hr.getEntity();
    }

    private static List<LivingEntity> around(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && e.distanceToSqr(p) <= r * r);
    }

    private static void ring(ServerLevel level, Vec3 c, double r, ParticleOptions particle, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(particle, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static boolean noTarget(ServerPlayer p) {
        msg(p, Component.translatable("msg.remushoku.no_target").withStyle(ChatFormatting.GRAY));
        return false;
    }

    private static void sound(ServerPlayer p, SoundEvent s, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 1f, pitch);
    }

    private static void msg(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }
}

package net.zennin.mushoku.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.party.Parties;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;

/** Hechizos del addon: magia variada y los 8 hechizos combinados (lluvias, nieblas, lodazal, hoja de piedra, viento gelido). */
public final class AddonSpells {
    private AddonSpells() {}

    public static boolean cast(Spell spell, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        switch (spell) {
            // ---------- Magia variada: a ti y a tu grupo en 3 bloques ----------
            case STRENGTH -> buff(p, MobEffects.DAMAGE_BOOST, 600, 0);
            case JUMP_BOOST -> buff(p, MobEffects.JUMP, 1200, 1);
            case NIGHT_VISION -> buff(p, MobEffects.NIGHT_VISION, 2400, 0);
            case WATER_BREATHING -> buff(p, MobEffects.WATER_BREATHING, 2400, 0);
            case FIRE_RESISTANCE -> buff(p, MobEffects.FIRE_RESISTANCE, 2400, 0);
            case INVISIBILITY -> buff(p, MobEffects.INVISIBILITY, 1200, 0);
            case CLEANSE -> {
                for (LivingEntity e : allies(p, 3)) {
                    List<Holder<MobEffect>> bad = e.getActiveEffects().stream().map(MobEffectInstance::getEffect)
                            .filter(h -> h.value().getCategory() == MobEffectCategory.HARMFUL).toList();
                    bad.forEach(e::removeEffect);
                    e.clearFire();
                }
                fx(level, p.position(), ParticleTypes.END_ROD, 30, 1.5);
                sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f);
            }
            case REPAIR -> {
                ItemStack s = p.getMainHandItem();
                if (!s.isDamageableItem() || s.getDamageValue() == 0) {
                    p.displayClientMessage(Component.translatable("msg.remushoku.repair_nothing").withStyle(ChatFormatting.GRAY), true);
                    return false;
                }
                s.setDamageValue(0);
                fx(level, p.position(), ParticleTypes.WAX_ON, 25, 0.8);
                sound(p, SoundEvents.ANVIL_PLACE, 1.4f);
            }
            // ---------- Lluvias: caen proyectiles del cielo sobre el punto que miras ----------
            case FIRE_RAIN -> rain(p, MagicProjectile.FIRE, 5f, SoundEvents.BLAZE_SHOOT);
            case FREEZING_RAIN -> rain(p, MagicProjectile.ICE, 7f, SoundEvents.GLASS_BREAK);
            case SPIKE_RAIN -> rain(p, MagicProjectile.ROCK, 7f, SoundEvents.POINTED_DRIPSTONE_FALL);
            // ---------- Nieblas: zona que dura varios segundos ----------
            case DEEP_MIST -> mist(p, 5, 120, ParticleTypes.CLOUD, e -> {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            });
            case POISON_MIST -> mist(p, 5, 240, ParticleTypes.ITEM_SLIME, e -> {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            });
            case FROST_WIND -> mist(p, 7, 240, ParticleTypes.SNOWFLAKE, e -> {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 60, e.getTicksFrozen() + 30));
                e.hurt(p.damageSources().freeze(), 2f);
            });
            case QUAGMIRE -> {
                // Lodazal: un cilindro de lodo de radio 7 ADELANTE del que conjura (el queda en el borde, afuera). En cada
                // columna se busca el suelo desde la altura de los pies hasta 4 bloques abajo (aire, luz o pasto se saltean).
                Vec3 fwd = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
                if (fwd.lengthSqr() < 1e-4) fwd = Vec3.directionFromRotation(0, p.getYRot());
                fwd = fwd.normalize();
                BlockPos feet = p.blockPosition();
                // lanzado por un circulo magico: centrado en el circulo (como antes, a los pies)
                boolean circle = p instanceof net.fabricmc.fabric.api.entity.FakePlayer;
                double cx = circle ? p.getX() : p.getX() + fwd.x * 8, cz = circle ? p.getZ() : p.getZ() + fwd.z * 8;
                int made = 0;
                for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                    int bx = (int) Math.floor(cx) + dx, bz = (int) Math.floor(cz) + dz;
                    double ox = bx + 0.5 - cx, oz = bz + 0.5 - cz;
                    if (ox * ox + oz * oz > 49) continue;
                    if (bx == feet.getX() && bz == feet.getZ()) continue; // nunca bajo los pies del que conjura (ni bajo el circulo)
                    for (int dy = 0; dy <= 4; dy++) {
                        BlockPos bp = new BlockPos(bx, feet.getY() - dy, bz);
                        var st = level.getBlockState(bp);
                        if (st.getCollisionShape(level, bp).isEmpty() && st.getFluidState().isEmpty()) continue; // aire, luz, pasto: mas abajo
                        if (dy == 0 && !level.getBlockState(bp.above()).getCollisionShape(level, bp.above()).isEmpty()) break; // pared: no se cubre
                        if (st.getFluidState().isEmpty() && TempBlocks.replace(level, bp, net.zennin.mushoku.registry.ModBlocks.QUAGMIRE_MUD.defaultBlockState(), 400)) made++;
                        break;
                    }
                }
                fx(level, new Vec3(cx, p.getY(), cz), ParticleTypes.FALLING_DRIPSTONE_WATER, 60, 4);
                sound(p, SoundEvents.MUD_BRICKS_BREAK, 0.6f);
                if (made == 0) return false;
            }
            case STONE_BLADE -> {
                ItemStack blade = new ItemStack(net.zennin.mushoku.addon.AddonItems.STONE_BLADE);
                if (!p.getInventory().add(blade)) p.drop(blade, false);
                fx(level, p.position(), net.minecraft.core.particles.ParticleTypes.CRIT, 20, 0.6);
                sound(p, SoundEvents.ANVIL_PLACE, 0.8f);
            }
            default -> { return false; }
        }
        return true;
    }

    // ---------- Ayudas ----------

    /** Tu, los de tu grupo y los personajes que te acompañan dentro del radio. */
    static List<LivingEntity> allies(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e == p || (e instanceof Player o && Parties.same(p, o)) || e instanceof net.zennin.mushoku.companion.CompanionEntity);
    }

    private static void buff(ServerPlayer p, Holder<MobEffect> effect, int ticks, int amp) {
        for (LivingEntity e : allies(p, 3)) e.addEffect(new MobEffectInstance(effect, ticks, amp));
        fx(p.serverLevel(), p.position(), ParticleTypes.ENCHANT, 30, 1.2);
        sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 1.3f);
    }

    static Vec3 target(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? eye.add(p.getLookAngle().scale(range)) : hit.getLocation();
    }

    private static void rain(ServerPlayer p, int kind, float dmg, SoundEvent snd) {
        Vec3 c = target(p, 30);
        ServerLevel level = p.serverLevel();
        sound(p, snd, 0.8f);
        Tasks.repeat(0, 4, 100, tick -> {
            if (!p.isAlive()) return false;
            for (int i = 0; i < 2; i++) {
                double x = c.x + (level.random.nextDouble() - 0.5) * 15, z = c.z + (level.random.nextDouble() - 0.5) * 15;
                // radio de impacto amplio: con la puntería al azar de la lluvia, pegar justo era casi imposible
                MagicProjectile proj = new MagicProjectile(level, p, kind, dmg).area(1.6f).size(1.4f);
                proj.setPos(x, c.y + 15, z);
                proj.shoot(0, -1, 0, 1.2f, 2f);
                level.addFreshEntity(proj);
            }
            return true;
        });
    }

    private interface Hit { void apply(LivingEntity e); }

    private static void mist(ServerPlayer p, double r, int ticks, ParticleOptions particle, Hit hit) {
        Vec3 c = target(p, 25);
        ServerLevel level = p.serverLevel();
        sound(p, SoundEvents.SPLASH_POTION_BREAK, 0.7f);
        Tasks.repeat(0, 10, ticks, tick -> {
            fx(level, c, particle, (int) (r * r * 2), r * 0.7);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2, r),
                    e -> e.isAlive() && e != p && !(e instanceof Player o && Parties.same(p, o)) && !(e instanceof net.zennin.mushoku.companion.CompanionEntity)))
                hit.apply(e);
            return true;
        });
    }

    private static void fx(ServerLevel level, Vec3 pos, ParticleOptions p, int n, double spread) {
        level.sendParticles(p, pos.x, pos.y + 1, pos.z, n, spread, spread * 0.6, spread, 0.02);
    }

    private static void sound(ServerPlayer p, SoundEvent s, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 1f, pitch);
    }
}

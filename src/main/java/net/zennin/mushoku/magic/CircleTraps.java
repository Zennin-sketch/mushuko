package net.zennin.mushoku.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Efecto de un circulo magico armado cuando alguien lo pisa. Depende del elemento del hechizo
 * inscrito y escala con su nivel (1 Principiante ... 6 Imperial).
 */
public final class CircleTraps {
    private CircleTraps() {}

    /** Hechizos de apoyo: tambien se activan con el dueño (sirven de "plataforma de curacion"). */
    public static boolean isFriendly(Spell s) {
        return s.element == Element.NONE && s != Spell.DISTURB_MAGIC;
    }

    public static void trigger(ServerLevel level, BlockPos pos, Spell spell, Player owner, LivingEntity target) {
        int t = spell.level.tier;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.1, z = pos.getZ() + 0.5;
        DamageSource magic = owner != null ? level.damageSources().indirectMagic(owner, owner) : level.damageSources().magic();
        burst(level, ParticleTypes.ENCHANT, x, y + 0.5, z, 60, 0.6);
        switch (spell.element) {
            case WATER -> {
                target.hurt(magic, 3 + 2 * t);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + 20 * t, Math.min(t, 4)));
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 40 * t);
                burst(level, ParticleTypes.SNOWFLAKE, x, y + 1, z, 30 + 10 * t, 0.8);
                sound(level, x, y, z, SoundEvents.GLASS_BREAK, 1.2f);
            }
            case FIRE -> {
                target.igniteForSeconds(3 + t);
                target.hurt(level.damageSources().onFire(), 2 + 2 * t);
                if (t >= 2) level.explode(owner, x, y, z, Math.min(1f + 0.5f * t, 4f), Level.ExplosionInteraction.NONE);
                burst(level, ParticleTypes.FLAME, x, y + 0.5, z, 40 + 10 * t, 0.7);
                sound(level, x, y, z, SoundEvents.FIRECHARGE_USE, 0.9f);
            }
            case EARTH -> {
                target.hurt(magic, 4 + 2 * t);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + 20 * t, 6));
                target.setDeltaMovement(target.getDeltaMovement().x * 0.2, 0.4 + 0.1 * t, target.getDeltaMovement().z * 0.2);
                target.hurtMarked = true;
                burst(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), x, y + 0.5, z, 50 + 10 * t, 0.6);
                sound(level, x, y, z, SoundEvents.STONE_BREAK, 0.7f);
            }
            case WIND -> {
                target.hurt(magic, 2 + t);
                double dx = target.getX() - x, dz = target.getZ() - z;
                double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                target.setDeltaMovement(dx / len * 0.4, 0.8 + 0.3 * t, dz / len * 0.4);
                target.hurtMarked = true;
                burst(level, ParticleTypes.CLOUD, x, y + 0.5, z, 40 + 10 * t, 0.5);
                sound(level, x, y, z, SoundEvents.PHANTOM_FLAP, 0.6f);
            }
            case NONE -> {
                if (spell == Spell.DISTURB_MAGIC) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                    burst(level, ParticleTypes.WITCH, x, y + 1, z, 40, 0.6);
                } else if (spell == Spell.MAGIC_SHIELD) {
                    target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, Math.min(t, 4)));
                    burst(level, ParticleTypes.END_ROD, x, y + 1, z, 30, 0.6);
                } else {
                    target.heal(4 + 3 * t);
                    target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100 + 20 * t, t >= 4 ? 1 : 0));
                    burst(level, ParticleTypes.HEART, x, y + 1, z, 10 + 2 * t, 0.6);
                }
                sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            }
        }
    }

    private static void burst(ServerLevel level, ParticleOptions p, double x, double y, double z, int n, double spread) {
        level.sendParticles(p, x, y, z, n, spread, spread * 0.6, spread, 0.05);
    }

    private static void sound(ServerLevel level, double x, double y, double z, SoundEvent s, float pitch) {
        level.playSound(null, x, y, z, s, SoundSource.BLOCKS, 1.2f, pitch);
    }
}

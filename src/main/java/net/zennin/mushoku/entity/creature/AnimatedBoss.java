package net.zennin.mushoku.entity.creature;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;

/**
 * Jefes de mazmorra con barra de jefe.
 *  Tortuga Gigante (mazmorra musgosa): cada 5 segundos golpea el suelo y empuja todo a su alrededor.
 *  Draca de Nieve Madre (mazmorra de hielo): cada 4 segundos lanza un aliento de hielo en abanico.
 */
public class AnimatedBoss extends AnimatedMonster {
    public enum Kind { TURTLE, DRAKE_MOM }

    private final Kind kind;
    private final ServerBossEvent bossBar;
    private int timer;

    public AnimatedBoss(EntityType<? extends Monster> type, Level level, Anims anims, Kind kind) {
        super(type, level, anims);
        this.kind = kind;
        this.xpReward = 300;
        this.bossBar = new ServerBossEvent(type.getDescription(),
                kind == Kind.TURTLE ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        setPersistenceRequired();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        ServerLevel sl = (ServerLevel) level();
        if (kind == Kind.TURTLE && ++timer >= 100) {
            timer = 0;
            triggerAnim("movement", "attack");
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8), e -> e != this && e.isAlive())) {
                e.hurt(damageSources().mobAttack(this), 8f);
                Vec3 d = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.8);
                e.setDeltaMovement(d.x, 0.7, d.z);
                e.hurtMarked = true;
            }
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                    getX(), getY() + 0.2, getZ(), 150, 5, 0.3, 5, 0.1);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
        } else if (kind == Kind.DRAKE_MOM && ++timer >= 80) {
            timer = 0;
            triggerAnim("movement", "attack");
            for (int i = -2; i <= 2; i++) {
                MagicProjectile p = new MagicProjectile(level(), this, MagicProjectile.ICE, 8f);
                p.setPos(getX(), getEyeY() - 0.5, getZ());
                Vec3 d = target.getEyePosition().subtract(p.position());
                double a = Math.toRadians(i * 10);
                p.shoot(d.x * Math.cos(a) - d.z * Math.sin(a), d.y, d.x * Math.sin(a) + d.z * Math.cos(a), 1.5f, 0f);
                level().addFreshEntity(p);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            playSound(SoundEvents.PLAYER_HURT_FREEZE, 2f, 0.6f);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) { return false; }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }
}

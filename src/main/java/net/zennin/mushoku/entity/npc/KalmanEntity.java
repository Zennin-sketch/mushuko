package net.zennin.mushoku.entity.npc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.registry.ModItems;

/**
 * El Dios Norte Kalman III (jefe). Vida 1024, daño 19, empuña la Kajakut.
 * Cada 25 ticks usa una tecnica: embestida, salto con explosion al caer o lluvia de espadas lanzadas.
 * Al bajar su vida sube su Manipulacion de Touki: 75% velocidad, 50% fuerza, 25% resistencia (como el original).
 */
public class KalmanEntity extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.remushoku.kalman_3"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int timer;
    private int toukiStage;

    public KalmanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KAJAKUT));
        setDropChance(EquipmentSlot.MAINHAND, 1f);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1024)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 19)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        bossBar.setProgress(getHealth() / getMaxHealth());
        ServerLevel sl = (ServerLevel) level();
        float frac = getHealth() / getMaxHealth();
        int stage = frac < 0.25f ? 3 : frac < 0.5f ? 2 : frac < 0.75f ? 1 : 0;
        if (stage > toukiStage) {
            toukiStage = stage;
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 60, 0.6, 1, 0.6, 0.05);
            playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 2f, 0.5f);
        }
        if (tickCount % 20 == 0) {
            if (toukiStage >= 1) addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            if (toukiStage >= 2) addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1));
            if (toukiStage >= 3) addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || ++timer < 25) return;
        timer = 0;
        int att = getRandom().nextInt(6);
        if (att <= 1) {
            // Embestida: aparece al lado del objetivo con un corte
            Vec3 dir = target.position().subtract(position()).normalize();
            Vec3 dest = target.position().subtract(dir.scale(1.5));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 5, 0.5, 0.5, 0.5, 0);
            teleportTo(dest.x, dest.y, dest.z);
            target.hurt(damageSources().mobAttack(this), 12f);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        } else if (att == 2) {
            // Salto y golpe al caer
            setDeltaMovement(0, 1.2, 0);
            hurtMarked = true;
            playSound(SoundEvents.EVOKER_CAST_SPELL, 2f, 0.6f);
            Tasks.later(20, () -> {
                if (!isAlive() || getTarget() == null) return;
                LivingEntity t = getTarget();
                teleportTo(t.getX(), t.getY(), t.getZ());
                level().explode(this, getX(), getY(), getZ(), 3f, Level.ExplosionInteraction.NONE);
            });
        } else if (att == 3 && distanceTo(target) > 4) {
            // Espadas lanzadas en abanico
            for (int i = -1; i <= 1; i++) {
                MagicProjectile p = new MagicProjectile(level(), this, MagicProjectile.WIND, 10f);
                p.setPos(getX(), getEyeY() - 0.1, getZ());
                Vec3 d = target.getEyePosition().subtract(p.position());
                double ang = Math.toRadians(i * 12);
                double x = d.x * Math.cos(ang) - d.z * Math.sin(ang), z = d.x * Math.sin(ang) + d.z * Math.cos(ang);
                p.shoot(x, d.y, z, 1.8f, 0f);
                level().addFreshEntity(p);
            }
            playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.5f, 0.8f);
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

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        toukiStage = tag.getInt("ToukiStage");
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ToukiStage", toukiStage);
    }
}

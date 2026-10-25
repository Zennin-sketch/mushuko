package net.zennin.mushoku.entity.npc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;

/**
 * Bandidos de los tres estilos de espada (mismas stats que el original: vida 40/40/60, daño 3, velocidad 0.3).
 * Cada cierto tiempo usan una tecnica de su estilo:
 *  Dios Espada: embestida contra el objetivo o Touki (fuerza y velocidad).
 *  Dios Agua: Desviar / Flujo (bloquea golpes un rato).
 *  Dios Norte: lanza su espada desde lejos o usa Touki de cerca.
 */
public class BanditEntity extends Monster {
    public enum Style { SWORD_GOD, WATER_GOD, NORTH_GOD }

    private final Style style;
    private int timer;
    private int guardTicks;

    public BanditEntity(EntityType<? extends Monster> type, Level level, Style style) {
        super(type, level);
        this.style = style;
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder attributes(double health) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (style) {
            case SWORD_GOD -> random.nextFloat() < 0.3f ? Items.IRON_SWORD : Items.STONE_SWORD;
            case WATER_GOD -> Items.IRON_SWORD;
            case NORTH_GOD -> random.nextBoolean() ? Items.IRON_AXE : Items.STONE_SWORD;
        }));
        if (style == Style.NORTH_GOD && random.nextFloat() < 0.4f) setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STONE_SWORD));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
                                                                     net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return d;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (guardTicks > 0) guardTicks--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        int interval = switch (style) { case SWORD_GOD -> 100; case WATER_GOD -> 60; case NORTH_GOD -> 40; };
        if (++timer < interval) return;
        timer = 0;
        ServerLevel sl = (ServerLevel) level();
        boolean roll = getRandom().nextBoolean();
        switch (style) {
            case SWORD_GOD -> {
                if (roll && distanceTo(target) < 10) {
                    Vec3 dir = target.position().subtract(position()).normalize();
                    setDeltaMovement(dir.x * 1.6, 0.3, dir.z * 1.6);
                    hurtMarked = true;
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 3, 0.3, 0.3, 0.3, 0);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f);
                } else touki(sl);
            }
            case WATER_GOD -> {
                guardTicks = 50;
                sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.1);
                playSound(SoundEvents.PLAYER_SPLASH, 1f, 1.3f);
            }
            case NORTH_GOD -> {
                if (distanceTo(target) > 5) {
                    MagicProjectile p = new MagicProjectile(level(), this, MagicProjectile.WIND, 5f);
                    p.setPos(getX(), getEyeY() - 0.1, getZ());
                    Vec3 d = target.getEyePosition().subtract(p.position());
                    p.shoot(d.x, d.y, d.z, 1.6f, 2f);
                    level().addFreshEntity(p);
                    playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f);
                } else touki(sl);
            }
        }
    }

    private void touki(ServerLevel sl) {
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 15, 0.4, 0.6, 0.4, 0.02);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (style == Style.WATER_GOD && guardTicks > 0 && source.getEntity() instanceof LivingEntity attacker && !level().isClientSide) {
            attacker.knockback(0.8, getX() - attacker.getX(), getZ() - attacker.getZ());
            ((ServerLevel) level()).sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.2, getZ(), 15, 0.4, 0.4, 0.4, 0.3);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1f, 1.3f);
            guardTicks = 0;
            return false;
        }
        return super.hurt(source, amount);
    }
}

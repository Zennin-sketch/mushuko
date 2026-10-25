package net.zennin.mushoku.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.registry.ModEntities;

/** Proyectil generico de los hechizos: vuela recto, deja particulas segun su tipo y aplica el efecto al impactar. */
public class MagicProjectile extends ThrowableProjectile {
    public static final int WATER = 0, ICE = 1, FIRE = 2, ROCK = 3;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MagicProjectile.class, EntityDataSerializers.INT);
    private float damage = 4f;

    public MagicProjectile(EntityType<? extends MagicProjectile> type, Level level) {
        super(type, level);
    }

    public MagicProjectile(Level level, LivingEntity owner, int kind, float damage) {
        super(ModEntities.MAGIC_PROJECTILE, owner, level);
        this.entityData.set(KIND, kind);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, WATER);
    }

    public int kind() { return this.entityData.get(KIND); }

    @Override
    protected double getDefaultGravity() { return 0.0; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ParticleOptions p = particle();
            Vec3 pos = position();
            for (int i = 0; i < 3; i++) {
                level().addParticle(p, pos.x + (random.nextDouble() - 0.5) * 0.3, pos.y + (random.nextDouble() - 0.5) * 0.3,
                        pos.z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
        } else if (tickCount > 100) {
            discard();
        }
    }

    private ParticleOptions particle() {
        return switch (kind()) {
            case ICE -> ParticleTypes.SNOWFLAKE;
            case FIRE -> ParticleTypes.FLAME;
            case ROCK -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            default -> ParticleTypes.SPLASH;
        };
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity target = hit.getEntity();
        target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
        if (target instanceof LivingEntity living) {
            switch (kind()) {
                case ICE -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                case FIRE -> living.igniteForSeconds(5);
                case ROCK -> living.knockback(0.8, getX() - living.getX(), getZ() - living.getZ());
                default -> living.clearFire();
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KIND, tag.getInt("Kind"));
        this.damage = tag.getFloat("Damage");
    }
}

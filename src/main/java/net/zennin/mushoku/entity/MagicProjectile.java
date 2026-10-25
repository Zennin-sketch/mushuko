package net.zennin.mushoku.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Proyectil generico de los hechizos: vuela recto, deja particulas segun su tipo y aplica el efecto al impactar.
 * Opcionales: tamano (mas particulas), empuje extra, levantar al objetivo y explosion al impactar.
 */
public class MagicProjectile extends ThrowableProjectile {
    public static final int WATER = 0, ICE = 1, FIRE = 2, ROCK = 3, WIND = 4;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MagicProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MagicProjectile.class, EntityDataSerializers.FLOAT);

    private float damage = 4f;
    private float knockback = 0f;
    private float explosion = 0f;
    private boolean lift = false;

    public MagicProjectile(EntityType<? extends MagicProjectile> type, Level level) {
        super(type, level);
    }

    public MagicProjectile(Level level, LivingEntity owner, int kind, float damage) {
        super(ModEntities.MAGIC_PROJECTILE, owner, level);
        this.entityData.set(KIND, kind);
        // Ultimo Suspiro / Aqua Heartia en la mano amplifican la magia
        this.damage = owner instanceof net.minecraft.world.entity.player.Player p ? damage * net.zennin.mushoku.extra.ExtraTicks.magicAmp(p) : damage;
    }

    public MagicProjectile size(float s) { this.entityData.set(SIZE, s); return this; }
    public MagicProjectile knockback(float k) { this.knockback = k; return this; }
    public MagicProjectile explosion(float power) { this.explosion = power; return this; }
    public MagicProjectile lift() { this.lift = true; return this; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, WATER);
        builder.define(SIZE, 1f);
    }

    public int kind() { return this.entityData.get(KIND); }

    public float sizeScale() { return this.entityData.get(SIZE); }

    @Override
    protected double getDefaultGravity() { return 0.0; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            float s = sizeScale();
            int count = Math.round(3 * s * s);
            double spread = 0.3 * s;
            Vec3 pos = position();
            for (int i = 0; i < count; i++) {
                level().addParticle(i % 3 == 0 ? secondary() : particle(),
                        pos.x + (random.nextDouble() - 0.5) * spread, pos.y + (random.nextDouble() - 0.5) * spread,
                        pos.z + (random.nextDouble() - 0.5) * spread, 0, 0, 0);
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
            case WIND -> ParticleTypes.CLOUD;
            default -> ParticleTypes.SPLASH;
        };
    }

    private ParticleOptions secondary() {
        return switch (kind()) {
            case ICE -> ParticleTypes.ITEM_SNOWBALL;
            case FIRE -> ParticleTypes.SMOKE;
            case ROCK -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
            case WIND -> ParticleTypes.SWEEP_ATTACK;
            default -> ParticleTypes.FALLING_WATER;
        };
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner();
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
                case WIND -> living.knockback(0.5, getX() - living.getX(), getZ() - living.getZ());
                default -> living.clearFire();
            }
            if (knockback > 0) {
                Vec3 dir = getDeltaMovement().normalize();
                living.push(dir.x * knockback, 0.2 * knockback, dir.z * knockback);
                living.hurtMarked = true;
            }
            if (lift) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 2));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(particle(), getX(), getY(), getZ(), Math.round(10 * sizeScale()), 0.3 * sizeScale(), 0.3 * sizeScale(), 0.3 * sizeScale(), 0.05);
            }
            if (explosion > 0) {
                level().explode(this, getX(), getY(), getZ(), explosion, Level.ExplosionInteraction.MOB);
            }
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
        tag.putFloat("Size", sizeScale());
        tag.putFloat("Damage", damage);
        tag.putFloat("Knockback", knockback);
        tag.putFloat("Explosion", explosion);
        tag.putBoolean("Lift", lift);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KIND, tag.getInt("Kind"));
        if (tag.contains("Size")) this.entityData.set(SIZE, tag.getFloat("Size"));
        this.damage = tag.getFloat("Damage");
        this.knockback = tag.getFloat("Knockback");
        this.explosion = tag.getFloat("Explosion");
        this.lift = tag.getBoolean("Lift");
    }
}

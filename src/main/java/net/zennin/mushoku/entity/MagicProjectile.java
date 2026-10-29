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
    /** Variante visual: 0 normal, taladro de viento, flecha de agua, tajo de fuego, invisible (golpes extra de un abanico). */
    public static final int VARIANT_DRILL = 1, VARIANT_ARROW = 2, VARIANT_SLICE = 3, VARIANT_HIDDEN = 4;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MagicProjectile.class, EntityDataSerializers.INT);

    private float damage = 4f;
    private float knockback = 0f;
    private float explosion = 0f;
    private boolean lift = false;
    /** Estallido de fuego al impactar (como la carga de un ghast): no rompe bloques, prende fuego al azar alrededor. */
    private float incendiary = 0f;
    /** Radio de impacto (lluvias): pega a todo lo que pasa cerca y, al caer, a todo lo que esta alrededor. */
    private float area = 0f;
    /** Cuantos objetivos atraviesa antes de frenar (Corte de Llama). */
    private int pierce = 0;
    private final java.util.Set<Integer> pierced = new java.util.HashSet<>();

    public MagicProjectile(EntityType<? extends MagicProjectile> type, Level level) {
        super(type, level);
    }

    public MagicProjectile(Level level, LivingEntity owner, int kind, float damage) {
        super(ModEntities.MAGIC_PROJECTILE, owner, level);
        this.entityData.set(KIND, kind);
        // Ultimo Suspiro / Aqua Heartia en la mano amplifican la magia
        this.damage = owner instanceof net.minecraft.world.entity.player.Player p
                ? damage * net.zennin.mushoku.extra.ExtraTicks.magicAmp(p) * net.zennin.mushoku.race.RaceSystem.affinity(p, kind)
                  * net.zennin.mushoku.magic.SpellMastery.mult(p) : damage;
    }

    public MagicProjectile size(float s) { this.entityData.set(SIZE, s); return this; }
    public MagicProjectile knockback(float k) { this.knockback = k; return this; }
    public MagicProjectile explosion(float power) { this.explosion = power; return this; }
    public MagicProjectile incendiary(float power) { this.incendiary = power; return this; }
    public MagicProjectile area(float radius) { this.area = radius; return this; }
    public MagicProjectile pierce(int n) { this.pierce = n; return this; }
    public MagicProjectile lift() { this.lift = true; this.entityData.set(VARIANT, VARIANT_DRILL); return this; }
    public MagicProjectile variant(int v) { this.entityData.set(VARIANT, v); return this; }
    public int variant() { return this.entityData.get(VARIANT); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, WATER);
        builder.define(SIZE, 1f);
        builder.define(VARIANT, 0);
    }

    public int kind() { return this.entityData.get(KIND); }

    public float sizeScale() { return this.entityData.get(SIZE); }

    @Override
    protected double getDefaultGravity() { return 0.0; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (variant() == VARIANT_HIDDEN) return;
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
        } else if (area > 0 && isAlive()) {
            // espoleta de proximidad: si pasa cerca de alguien, estalla ahi
            Entity owner = getOwner();
            if (!level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(area, area * 0.6, area),
                    e -> e.isAlive() && e != owner && !e.isSpectator()).isEmpty()) {
                areaImpact();
                discard();
            }
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
        return super.canHitEntity(target) && target != getOwner() && !pierced.contains(target.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (area > 0) return; // las lluvias pegan en area (onHit)
        strike(hit.getEntity());
    }

    /** Dano y efecto del elemento sobre un objetivo. */
    private void strike(Entity target) {
        target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
        if (target instanceof LivingEntity living) {
            switch (kind()) {
                case ICE -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                case FIRE -> living.igniteForSeconds(5);
                case ROCK -> living.knockback(0.8, getX() - living.getX(), getZ() - living.getZ());
                case WIND -> living.knockback(0.5, getX() - living.getX(), getZ() - living.getZ());
                default -> living.clearFire();
            }
            // la magia de agua (agua y hielo) apaga al que se esta quemando
            if (kind() == ICE && living.isOnFire()) {
                living.clearFire();
                level().playSound(null, living.getX(), living.getY(), living.getZ(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
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

    /** Sonido al caer las lluvias (fuego, puas, hielo). */
    private void impactSound() {
        var r = level().random;
        float pitch = 0.85f + r.nextFloat() * 0.3f;
        switch (kind()) {
            case FIRE -> {
                level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.55f, pitch);
                level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_BURN, net.minecraft.sounds.SoundSource.PLAYERS, 0.35f, pitch);
            }
            case ROCK -> {
                level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, pitch);
                level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, pitch * 0.8f);
            }
            case ICE -> level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, pitch * 1.3f);
            default -> level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, pitch);
        }
    }

    /** Impacto en area: le pega a todos los que esten en el radio (menos al que lo lanzo). */
    private void areaImpact() {
        Entity owner = getOwner();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(area * 1.3, area, area * 1.3),
                e -> e.isAlive() && e != owner && !e.isSpectator())) {
            strike(e);
        }
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(particle(), getX(), getY(), getZ(), Math.round(14 * sizeScale()), 0.5 * area, 0.3, 0.5 * area, 0.05);
    }

    @Override
    protected void onHit(HitResult result) {
        // atraviesa: le pega y sigue de largo (hasta 'pierce' objetivos; el siguiente lo frena)
        if (pierce > 0 && result instanceof EntityHitResult eh && !level().isClientSide) {
            pierced.add(eh.getEntity().getId());
            strike(eh.getEntity());
            pierce--;
            return;
        }
        super.onHit(result);
        if (!level().isClientSide && area > 0) impactSound();
        if (!level().isClientSide && area > 0 && isAlive()) areaImpact();
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(particle(), getX(), getY(), getZ(), Math.round(10 * sizeScale()), 0.3 * sizeScale(), 0.3 * sizeScale(), 0.3 * sizeScale(), 0.05);
            }
            if (incendiary > 0 && explosion <= 0) {
                Entity owner = getOwner();
                // dana a todos menos al que la lanzo; los bloques quedan, pero el fuego aparece al azar como con un ghast
                net.minecraft.world.level.ExplosionDamageCalculator calc = new net.minecraft.world.level.ExplosionDamageCalculator() {
                    @Override
                    public boolean shouldDamageEntity(net.minecraft.world.level.Explosion e, Entity entity) {
                        return entity != owner && super.shouldDamageEntity(e, entity);
                    }
                };
                level().explode(this, damageSources().explosion(this, owner), calc, getX(), getY(), getZ(), incendiary, true,
                        Level.ExplosionInteraction.NONE);
            }
            if (explosion > 0) {
                if (level() instanceof ServerLevel sl && kind() == FIRE)
                    SpellFxEntity.spawn(sl, "fire_burst", position(), 0, explosion * 1.3f, 20, null);
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
        tag.putFloat("Incendiary", incendiary);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KIND, tag.getInt("Kind"));
        if (tag.contains("Size")) this.entityData.set(SIZE, tag.getFloat("Size"));
        this.damage = tag.getFloat("Damage");
        this.knockback = tag.getFloat("Knockback");
        this.incendiary = tag.getFloat("Incendiary");
        this.explosion = tag.getFloat("Explosion");
        this.lift = tag.getBoolean("Lift");
    }
}

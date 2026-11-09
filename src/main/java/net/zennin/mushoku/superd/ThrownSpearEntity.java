package net.zennin.mushoku.superd;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lanza arrojada por un Superd (habilidad Lanzamiento). Es una copia de la lanza de la mano: vuela casi recta, atraviesa
 * al primero que toca (dano alto) y despues vuelve sola a su duenio y desaparece. No se puede recoger.
 */
public class ThrownSpearEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.BOOLEAN);
    private float damage = 8;
    private int life;

    public ThrownSpearEntity(EntityType<? extends ThrownSpearEntity> type, Level level) { super(type, level); }

    public ThrownSpearEntity(Level level, LivingEntity owner, ItemStack stack, float damage) {
        super(net.zennin.mushoku.registry.ModEntities.THROWN_SPEAR, owner, level);
        entityData.set(ITEM, stack);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ITEM, ItemStack.EMPTY);
        b.define(RETURNING, false);
    }

    public ItemStack getItem() { return entityData.get(ITEM); }

    public boolean returning() { return entityData.get(RETURNING); }

    @Override
    protected double getDefaultGravity() { return 0.015; }

    @Override
    public void tick() {
        if (returning()) {
            noPhysics = true;
            Entity owner = getOwner();
            if (owner == null || !owner.isAlive() || owner.level() != level()) { if (!level().isClientSide) discard(); return; }
            Vec3 to = owner.getEyePosition().subtract(position());
            if (to.length() < 1.5) {
                if (!level().isClientSide) {
                    level().playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1f, 1f);
                    discard();
                }
                return;
            }
            setDeltaMovement(getDeltaMovement().scale(0.6).add(to.normalize().scale(0.9)));
            setPos(position().add(getDeltaMovement()));
            updateRotation();
            if (level().isClientSide) level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        super.tick();
        if (++life > 30 && !level().isClientSide) entityData.set(RETURNING, true);
        if (level().isClientSide) level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity e = hit.getEntity();
        if (e == getOwner() || level().isClientSide) return;
        Entity owner = getOwner();
        e.hurt(damageSources().thrown(this, owner == null ? this : owner), damage);
        if (e instanceof LivingEntity le) {
            Vec3 v = getDeltaMovement().multiply(1, 0, 1).normalize();
            le.knockback(0.8, -v.x, -v.z);
        }
        ((ServerLevel) level()).sendParticles(ParticleTypes.CRIT, e.getX(), e.getY(0.6), e.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
        level().playSound(null, e.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1f, 1f);
        entityData.set(RETURNING, true);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) return;
        level().playSound(null, hit.getBlockPos(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1f, 1f);
        entityData.set(RETURNING, true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }

    /** No se guarda con el mundo (es una copia temporal). */
    @Override
    public boolean shouldBeSaved() { return false; }
}

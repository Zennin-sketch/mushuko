package net.zennin.mushoku.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.zennin.mushoku.registry.ModEntities;
import net.zennin.mushoku.registry.ModItems;
import org.joml.Vector3f;

/** Roca y Bolsa de Especias lanzadas (objetos del Dios Norte). Se ven como el item mismo volando. */
public class ThrownMushokuItem extends ThrowableItemProjectile {
    private float damage = 4f;

    public ThrownMushokuItem(EntityType<? extends ThrownMushokuItem> type, Level level) { super(type, level); }

    public ThrownMushokuItem(Level level, LivingEntity owner, float damage) {
        super(ModEntities.THROWN_ITEM, owner, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() { return ModItems.ROCK; }

    private boolean isSpice() { return getItem().is(ModItems.SPICE_BAG); }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!isSpice()) hit.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        if (isSpice()) {
            // Nube de especias: ciega y frena 8 segundos en 2.5 bloques (como el original)
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5), e -> e != getOwner())) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
            }
            int[][] colors = {{0x55, 0xAA, 0x33}, {0xEE, 0x66, 0x22}, {0xDD, 0xBB, 0x66}, {0x44, 0x88, 0xDD}};
            for (int[] c : colors)
                sl.sendParticles(new DustParticleOptions(new Vector3f(c[0] / 255f, c[1] / 255f, c[2] / 255f), 1.5f), getX(), getY() + 0.5, getZ(), 25, 1.2, 0.8, 1.2, 0);
            playSound(SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.8f);
        } else {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            playSound(SoundEvents.STONE_HIT, 1f, 1f);
        }
        discard();
    }
}

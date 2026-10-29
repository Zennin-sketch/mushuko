package net.zennin.mushoku.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.registry.ModEntities;
import org.joml.Vector3f;

/**
 * Mina del Campo Minado. Sale de la mano, sube en curva hasta arriba del punto marcado, cae a su lugar y queda
 * armada en el suelo: estalla si alguien la pisa o cuando se le acaba la mecha. El recorrido es el mismo en el
 * servidor y en el cliente (sale de los datos sincronizados), asi se ve suave.
 */
public class MineEntity extends Entity {
    private static final EntityDataAccessor<Vector3f> FROM = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> APEX = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> LAND = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> UP = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DOWN = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(MineEntity.class, EntityDataSerializers.INT);

    private LivingEntity owner;
    private float power = 2f;

    public MineEntity(EntityType<? extends MineEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static MineEntity launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 apex, Vec3 land, int up, int down, int fuse, float power) {
        MineEntity m = new MineEntity(ModEntities.MINE, level);
        m.entityData.set(FROM, from.toVector3f());
        m.entityData.set(APEX, apex.toVector3f());
        m.entityData.set(LAND, land.toVector3f());
        m.entityData.set(UP, up);
        m.entityData.set(DOWN, down);
        m.entityData.set(FUSE, fuse);
        m.owner = owner;
        m.power = power;
        m.setPos(from);
        level.addFreshEntity(m);
        return m;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(FROM, new Vector3f());
        b.define(APEX, new Vector3f());
        b.define(LAND, new Vector3f());
        b.define(UP, 18);
        b.define(DOWN, 12);
        b.define(FUSE, 60);
    }

    /** Tick desde el que la mina esta en el suelo. */
    public int landTick() { return entityData.get(UP) + entityData.get(DOWN); }

    public boolean armed() { return tickCount >= landTick(); }

    /** Donde esta en el tick t (curva de subida y caida acelerada). */
    public Vec3 at(float t) {
        Vec3 from = new Vec3(entityData.get(FROM)), apex = new Vec3(entityData.get(APEX)), land = new Vec3(entityData.get(LAND));
        int up = entityData.get(UP), down = entityData.get(DOWN);
        if (t < up) {
            double u = Math.max(0, t) / up, k = 1 - u;
            Vec3 ctrl = from.add(apex).scale(0.5).add(0, 10, 0);
            return from.scale(k * k).add(ctrl.scale(2 * k * u)).add(apex.scale(u * u));
        }
        double u = Math.min(1, (t - up) / down);
        return apex.add(land.subtract(apex).scale(u * u));
    }

    @Override
    public void tick() {
        xo = getX(); yo = getY(); zo = getZ();
        super.tick();
        Vec3 p = at(tickCount);
        setPos(p.x, p.y, p.z);
        if (level().isClientSide) {
            if (!armed() && tickCount % 2 == 0)
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (tickCount == entityData.get(UP))
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.PLAYERS, 0.4f, 1.6f);
        if (tickCount == landTick())
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 0.8f);
        if (!armed()) return;
        boolean stepped = !sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.1, 0.8, 1.1),
                e -> e.isAlive() && e != owner && !e.isSpectator()).isEmpty();
        if (stepped || tickCount >= landTick() + entityData.get(FUSE)) {
            sl.explode(owner != null ? owner : this, getX(), getY(), getZ(), power, Level.ExplosionInteraction.MOB);
            discard();
        }
        if (tickCount > 600) discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() { return false; }
}

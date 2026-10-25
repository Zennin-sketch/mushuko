package net.zennin.mushoku.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.registry.ModEntities;

/**
 * Efecto visual de un hechizo (nube de tormenta, pinchos, cristales, explosion, escudos...). No choca ni se guarda:
 * solo muestra el modelo voxel 'model' durante 'life' ticks. Si 'follow' es un id de entidad, la acompana.
 * La logica del hechizo (danio, rayos, etc.) la sigue haciendo el codigo del hechizo.
 */
public class SpellFxEntity extends Entity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(SpellFxEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SpellFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpellFxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FOLLOW = SynchedEntityData.defineId(SpellFxEntity.class, EntityDataSerializers.INT);

    // Tiempos del Cumulonimbus (iguales a tools/spells/spell_fx.py)
    public static final int CUMULO_FORM = 50, CUMULO_RAIN_END = 120, CUMULO_LIFE = 150;
    public static final int STORM_FORM = 14, STORM_LIFE = 45;

    public SpellFxEntity(EntityType<? extends SpellFxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Pone un efecto en 'pos' mirando a 'yaw'. scale = tamano del modelo (1 = pixeles de Minecraft). */
    public static SpellFxEntity spawn(ServerLevel level, String model, Vec3 pos, float yaw, float scale, int life, Entity follow) {
        SpellFxEntity fx = new SpellFxEntity(ModEntities.SPELL_FX, level);
        fx.entityData.set(MODEL, model);
        fx.entityData.set(LIFE, life);
        fx.entityData.set(SCALE, scale);
        fx.entityData.set(FOLLOW, follow != null ? follow.getId() : -1);
        fx.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        level.addFreshEntity(fx);
        return fx;
    }

    public String model() { return entityData.get(MODEL); }
    public int life() { return entityData.get(LIFE); }
    public float scale() { return entityData.get(SCALE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODEL, "fire_burst");
        builder.define(LIFE, 20);
        builder.define(SCALE, 1f);
        builder.define(FOLLOW, -1);
    }

    @Override
    public void tick() {
        super.tick();
        int follow = entityData.get(FOLLOW);
        if (follow >= 0) {
            Entity e = level().getEntity(follow);
            if (e != null && e.isAlive()) setPos(e.getX(), e.getY(), e.getZ());
            else if (!level().isClientSide) { discard(); return; }
        }
        if (level().isClientSide) {
            if ("cumulonimbus".equals(model()) && tickCount >= CUMULO_FORM && tickCount <= CUMULO_RAIN_END) rainSplashes();
        } else if (tickCount > life()) {
            discard();
        }
    }

    /** Salpicaduras en el suelo bajo la nube mientras llueve. */
    private void rainSplashes() {
        double radius = 24 * 0.85 * scale() / 16.0;
        for (int i = 0; i < 6; i++) {
            double x = getX() + (random.nextDouble() * 2 - 1) * radius;
            double z = getZ() + (random.nextDouble() * 2 - 1) * radius;
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (y > getY()) continue;
            level().addParticle(ParticleTypes.SPLASH, x, y + 0.05, z, 0, 0, 0);
            if (i % 3 == 0) level().addParticle(ParticleTypes.RAIN, x, y + 0.1, z, 0, 0, 0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 160 * 160;
    }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}

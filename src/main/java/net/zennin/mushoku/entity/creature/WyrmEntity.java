package net.zennin.mushoku.entity.creature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Wyrm rojo: dragon adulto basado en como funcionan los dragones de Ice and Fire.
 * <ul>
 *   <li>Duerme de noche si no tiene a quien atacar; se despierta si lo lastiman o al amanecer.</li>
 *   <li>Ataca jugadores y, a veces, animales cercanos (caza). Inmune al fuego.</li>
 *   <li>En el suelo: mordisco (si la presa es chica la sacude: doble dano y se cura la mitad), latigazo de cola (empuja
 *       a los de atras), golpe de alas (tres golpes que empujan) y aliento de fuego en chorro.</li>
 *   <li>Despega, vuela en circulos sobre el objetivo y desde el aire: embestida en picada, chorro de fuego al pasar y
 *       bolas de fuego explosivas quieto en el aire. Despues aterriza.</li>
 *   <li>Sin objetivo camina y de vez en cuando sale a volar un rato.</li>
 * </ul>
 */
public class WyrmEntity extends AnimatedMonster {
    /** 0 = en el suelo, 1 = volando, 2 = quieto en el aire. */
    private static final EntityDataAccessor<Integer> FLIGHT = SynchedEntityData.defineId(WyrmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(WyrmEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(WyrmEntity.class, EntityDataSerializers.BOOLEAN);

    /** Tamano del modelo (6 veces el wyrm original): la boca queda lejos del centro. */
    public static final float SCALE = 6f;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk"),
            SLEEP = RawAnimation.begin().thenLoop("sleep"), FLY = RawAnimation.begin().thenLoop("fly"),
            GLIDE = RawAnimation.begin().thenLoop("glide"), HOVER = RawAnimation.begin().thenLoop("hover");

    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int action, actionTime, cooldown, flyTime, breathTime, roarCooldown;
    private Vec3 flyGoal;
    private float circle;
    private int wingHits;
    /** Accion en curso. */
    private static final int NONE = 0, BITE = 1, TAIL = 2, WING = 3, BREATH = 4, DIVE = 5, HOVER_BLAST = 6, ROAR = 7, TAKEOFF = 8;

    public WyrmEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Anims("idle", "walk", "bite"));
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder wyrmAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500).add(Attributes.ATTACK_DAMAGE, 18).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ARMOR, 12)
                .add(Attributes.FLYING_SPEED, 0.9).add(Attributes.STEP_HEIGHT, 3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FLIGHT, 0);
        b.define(SLEEPING, false);
        b.define(BREATHING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !isAsleep() && !isFlying() && super.canUse(); }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return !isAsleep() && super.canUse(); }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !isAsleep()));
        // caza: a veces va por un animal grande de la zona
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 200, true, false,
                e -> !isAsleep() && e.getBbWidth() > 0.8f));
    }

    public boolean isFlying() { return entityData.get(FLIGHT) > 0; }
    public boolean isHovering() { return entityData.get(FLIGHT) == 2; }
    public boolean isAsleep() { return entityData.get(SLEEPING); }
    public boolean isBreathing() { return entityData.get(BREATHING); }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        boolean hit = super.hurt(src, amount);
        if (hit && isAsleep()) wake();
        return hit;
    }

    private void wake() {
        entityData.set(SLEEPING, false);
        if (!level().isClientSide) roar();
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isBreathing()) breathParticles(true);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) { setTarget(null); target = null; }
        if (cooldown > 0) cooldown--;
        if (roarCooldown > 0) roarCooldown--;

        // dormir de noche, en el suelo y sin objetivo (como en Ice and Fire)
        boolean night = !sl.isDay();
        if (!isAsleep() && night && target == null && !isFlying() && onGround() && tickCount % 40 == 0 && random.nextInt(4) == 0) {
            entityData.set(SLEEPING, true);
            getNavigation().stop();
        } else if (isAsleep() && (!night || target != null)) {
            wake();
        }
        if (isAsleep()) {
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            getNavigation().stop();
            if (tickCount % 60 == 0) heal(1);
            return;
        }

        if (action != NONE) tickAction(sl, target);
        else if (isFlying()) tickFlight(sl, target);
        else tickGround(sl, target);

        if (isFlying()) {
            setNoGravity(true);
            fallDistance = 0;
            if (tickCount % 24 == 0 && !isHovering())
                sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.6f + random.nextFloat() * 0.2f);
        } else {
            setNoGravity(false);
        }
    }

    private void tickGround(ServerLevel sl, LivingEntity target) {
        if (target == null) {
            // de vez en cuando sale a volar un rato
            if (random.nextInt(1600) == 0) takeOff(300 + random.nextInt(400));
            return;
        }
        if (roarCooldown <= 0 && distanceTo(target) < 48 && random.nextInt(3) == 0) { roar(); return; }
        getLookControl().setLookAt(target, 30, 30);
        double d = horizontalDistance(target);
        double reach = 11;
        if (cooldown <= 0) {
            if (d < reach && isBehind(target)) { start(TAIL, 18); return; }
            if (d < reach * 0.8 && random.nextInt(3) == 0) { start(WING, 24); wingHits = 0; return; }
            if (d < reach) { start(BITE, 16); return; }
            if (d < 30 && random.nextInt(25) == 0) { start(BREATH, 50); return; }
            if (d > 30 || random.nextInt(260) == 0) { takeOff(240 + random.nextInt(240)); return; }
        }
        if (d > reach * 0.7) {
            if (!getNavigation().moveTo(target, 1.1)) getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.1);
        } else {
            getNavigation().stop();
            turnTo(target.position(), 12);
        }
    }

    private void tickFlight(ServerLevel sl, LivingEntity target) {
        if (--flyTime <= 0 || isInWater()) { land(); return; }
        double ground = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        if (target != null) {
            // da vueltas alrededor del objetivo y ataca desde el aire
            circle += 0.025f;
            double r = 26;
            flyGoal = new Vec3(target.getX() + Mth.cos(circle) * r, Math.max(target.getY() + 16, ground + 12), target.getZ() + Mth.sin(circle) * r);
            if (cooldown <= 0) {
                int roll = random.nextInt(100);
                if (roll < 3) { start(DIVE, 60); return; }
                if (roll < 5) { start(HOVER_BLAST, 70); entityData.set(FLIGHT, 2); return; }
                if (roll < 7 && distanceTo(target) < 34) { start(BREATH, 40); return; }
            }
        } else if (flyGoal == null || position().distanceToSqr(flyGoal) < 64 || random.nextInt(200) == 0) {
            // vuelo libre: elige puntos lejanos y altos
            double a = random.nextDouble() * Math.PI * 2, r = 30 + random.nextDouble() * 40;
            flyGoal = new Vec3(getX() + Math.cos(a) * r, 0, getZ() + Math.sin(a) * r);
            double g = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) flyGoal.x, (int) flyGoal.z);
            flyGoal = new Vec3(flyGoal.x, g + 18 + random.nextInt(14), flyGoal.z);
        }
        steer(flyGoal, 0.9);
    }

    /** Vuela hacia un punto: acelera de a poco y gira el cuerpo segun hacia donde va. */
    private void steer(Vec3 goal, double speed) {
        if (goal == null) return;
        Vec3 to = goal.subtract(position());
        double len = to.length();
        if (len < 0.5) { setDeltaMovement(getDeltaMovement().scale(0.8)); return; }
        Vec3 want = to.scale(Math.min(speed, len * 0.1) / len);
        double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        if (getY() < ground + 4) want = want.add(0, 0.25, 0);
        setDeltaMovement(getDeltaMovement().scale(0.9).add(want.scale(0.1)));
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistanceSqr() > 0.004) turnTo(position().add(v), 6);
    }

    private void turnTo(Vec3 p, float max) {
        float yaw = (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * Mth.RAD_TO_DEG) - 90f;
        setYRot(Mth.approachDegrees(getYRot(), yaw, max));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void takeOff(int time) {
        entityData.set(FLIGHT, 1);
        flyTime = time;
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().add(0, 0.9, 0));
        triggerAnim("movement", "takeoff");
        level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4f, 0.5f);
        action = TAKEOFF;
        actionTime = 20;
    }

    private void land() {
        // baja hasta tocar el suelo
        double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        entityData.set(FLIGHT, 1);
        if (onGround() || getY() - ground < 1.5) {
            entityData.set(FLIGHT, 0);
            flyTime = 0;
            cooldown = 30;
            return;
        }
        setDeltaMovement(getDeltaMovement().scale(0.85).add(0, -0.12, 0));
    }

    private void roar() {
        roarCooldown = 600;
        start(ROAR, 32);
        level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5f, 0.7f);
    }

    private void start(int a, int time) {
        action = a;
        actionTime = time;
        String anim = switch (a) {
            case BITE -> "bite"; case TAIL -> "tail"; case WING -> "wing"; case BREATH -> isFlying() ? null : "breath";
            case ROAR -> isFlying() ? null : "roar"; default -> null;
        };
        if (anim != null) triggerAnim("movement", anim);
        if (a == BREATH) {
            breathTime = 0;
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 1.3f);
        }
    }

    private void tickAction(ServerLevel sl, LivingEntity target) {
        int t = actionTime--;
        if (target != null) getLookControl().setLookAt(target, 30, 30);
        switch (action) {
            case BITE -> {
                if (target != null) turnTo(target.position(), 15);
                if (t == 9 && target != null && horizontalDistance(target) < 13) {
                    float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
                    boolean small = target.getBbWidth() < 1.2f && target.getBbHeight() < 2.1f && random.nextInt(3) == 0;
                    if (small) dmg *= 2; // sacude a la presa: doble dano y se cura la mitad
                    if (target.hurt(damageSources().mobAttack(this), dmg) && small) heal(dmg * 0.5f);
                    sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 2f, 0.5f);
                }
            }
            case TAIL -> {
                if (t == 9) {
                    for (LivingEntity e : around(14)) {
                        if (!isBehind(e)) continue;
                        e.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8f);
                        Vec3 push = e.position().subtract(position()).normalize().scale(3.2);
                        e.push(push.x, 0.8, push.z);
                        if (e instanceof ServerPlayer sp) sp.hurtMarked = true;
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3f, 0.4f);
                }
            }
            case WING -> {
                // tres golpes rapidos que empujan
                if (t == 18 || t == 12 || t == 6) {
                    for (LivingEntity e : around(13)) {
                        e.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35f);
                        Vec3 push = e.position().subtract(position()).normalize().scale(1.6);
                        e.push(push.x, 0.4, push.z);
                        if (e instanceof ServerPlayer sp) sp.hurtMarked = true;
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4f, 0.8f);
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 6, 0.5, 6, 0.1);
                }
            }
            case BREATH -> {
                if (target != null) turnTo(target.position(), 4);
                if (isFlying()) steer(flyGoal, 0.7);
                breathTime++;
                boolean on = breathTime > (isFlying() ? 4 : 12);
                entityData.set(BREATHING, on);
                if (on) breathe(sl, target);
            }
            case DIVE -> {
                // embestida en picada (dano = etapa x 3 en Ice and Fire; aca 15)
                if (target == null) { t = 0; break; }
                Vec3 to = target.position().add(0, 1, 0).subtract(position());
                setDeltaMovement(getDeltaMovement().scale(0.7).add(to.normalize().scale(0.6)));
                turnTo(target.position(), 15);
                if (getBoundingBox().inflate(2).intersects(target.getBoundingBox())) {
                    target.hurt(damageSources().mobAttack(this), 15);
                    Vec3 push = to.normalize().scale(2.5);
                    target.push(push.x, 1.0, push.z);
                    if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
                    sl.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.6f);
                    setDeltaMovement(new Vec3(getDeltaMovement().x, 1.0, getDeltaMovement().z));
                    t = 0;
                }
            }
            case HOVER_BLAST -> {
                // quieto en el aire, lanza bolas de fuego que explotan
                setDeltaMovement(getDeltaMovement().scale(0.7));
                if (target != null) {
                    turnTo(target.position(), 10);
                    if (t % 18 == 0) {
                        Vec3 mouth = mouth();
                        Vec3 dir = target.position().add(0, 1, 0).subtract(mouth).normalize();
                        boolean grief = sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
                        LargeFireball ball = new LargeFireball(sl, this, dir, grief ? 2 : 1);
                        ball.setPos(mouth.x, mouth.y, mouth.z);
                        sl.addFreshEntity(ball);
                        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3f, 0.5f);
                    }
                }
            }
            case TAKEOFF -> setDeltaMovement(getDeltaMovement().scale(0.8).add(0, 0.18, 0));
            default -> {}
        }
        if (t <= 0) {
            if (action == HOVER_BLAST) entityData.set(FLIGHT, 1);
            entityData.set(BREATHING, false);
            action = NONE;
            cooldown = 20 + random.nextInt(30);
        }
    }

    // ------------------------------------------------------------------ fuego

    /** Boca del dragon (a escala 6 queda muy por delante y arriba del centro). */
    private Vec3 mouth() {
        Vec3 fwd = Vec3.directionFromRotation(0, yBodyRot);
        double up = isFlying() ? 1.5 : (isBreathing() ? 9 : 12);
        double ahead = isFlying() ? 13 : 12;
        return position().add(fwd.scale(ahead)).add(0, up, 0);
    }

    private Vec3 breathDir(LivingEntity target) {
        Vec3 m = mouth();
        if (target != null) return target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(m).normalize();
        return Vec3.directionFromRotation(30, yBodyRot);
    }

    private void breathe(ServerLevel sl, LivingEntity target) {
        Vec3 m = mouth(), dir = breathDir(target);
        for (int i = 0; i < 6; i++) {
            Vec3 v = dir.scale(1.4 + random.nextDouble() * 0.6).add(random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
            sl.sendParticles(ParticleTypes.FLAME, m.x, m.y, m.z, 0, v.x, v.y, v.z, 1);
        }
        if (breathTime % 4 == 0) sl.playSound(null, BlockPos.containing(m), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2f, 0.6f);
        // dano en el cono del chorro
        double len = 26;
        AABB box = new AABB(m, m.add(dir.scale(len))).inflate(3);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m);
            double along = to.dot(dir);
            if (along < 0 || along > len) continue;
            double off = to.subtract(dir.scale(along)).length();
            if (off > 1.5 + along * 0.12) continue;
            if (breathTime % 4 == 0) e.hurt(damageSources().mobAttack(this), 5);
            e.igniteForSeconds(6);
        }
        // prende fuego donde pega el chorro
        if (breathTime % 6 == 0 && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            var hit = sl.clip(new net.minecraft.world.level.ClipContext(m, m.add(dir.scale(len)), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos p = hit.getBlockPos().relative(hit.getDirection());
                for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1)))
                    if (random.nextInt(3) == 0 && sl.isEmptyBlock(q) && BaseFireBlock.canBePlacedAt(sl, q, hit.getDirection()))
                        sl.setBlockAndUpdate(q, BaseFireBlock.getState(sl, q));
            }
        }
    }

    private void breathParticles(boolean client) {
        Vec3 m = mouth(), dir = Vec3.directionFromRotation(isFlying() ? 30 : 20, yBodyRot);
        for (int i = 0; i < 3; i++)
            level().addParticle(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, dir.x * 0.3, dir.y * 0.3 + 0.05, dir.z * 0.3);
    }

    // ------------------------------------------------------------------ utilidades

    private java.util.List<LivingEntity> around(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r - getBbWidth() / 2, 3, r - getBbWidth() / 2),
                e -> e != this && e.isAlive() && !(e instanceof WyrmEntity));
    }

    private double horizontalDistance(Entity e) {
        return Math.sqrt(e.distanceToSqr(getX(), e.getY(), getZ())) - getBbWidth() / 2;
    }

    /** El objetivo esta detras (para el latigazo de cola). */
    private boolean isBehind(Entity e) {
        Vec3 fwd = Vec3.directionFromRotation(0, yBodyRot);
        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1).normalize();
        return fwd.dot(to) < -0.3;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (action == NONE && cooldown <= 0) start(BITE, 16);
        return false;
    }

    // ------------------------------------------------------------------ jefe, guardado

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        bar.setVisible(getTarget() != null || getHealth() < getMaxHealth());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Flight", entityData.get(FLIGHT));
        tag.putInt("FlyTime", flyTime);
        tag.putBoolean("Asleep", isAsleep());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FLIGHT, tag.getInt("Flight"));
        flyTime = tag.getInt("FlyTime");
        entityData.set(SLEEPING, tag.getBoolean("Asleep"));
    }

    // ------------------------------------------------------------------ animaciones

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WyrmEntity> c = new AnimationController<>(this, "movement", 6, state -> {
            if (isAsleep()) return state.setAndContinue(SLEEP);
            if (isFlying()) {
                if (isHovering()) return state.setAndContinue(HOVER);
                return state.setAndContinue(getDeltaMovement().y < -0.08 ? GLIDE : FLY);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (String a : new String[]{"bite", "tail", "wing", "breath", "roar", "takeoff"})
            c.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        controllers.add(c);
    }
}

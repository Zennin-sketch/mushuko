package net.zennin.mushoku.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.registry.ModItems;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Personaje con vinculos (estilo Minecraft Comes Alive): corazones por jugador, animo, ordenes
 * (seguir / quedarse / libre / casa), casamiento, y pelea a tu lado cuando te sigue.
 */
public class CompanionEntity extends PathfinderMob implements RangedAttackMob {
    public enum Mode { FREE, FOLLOW, STAY }

    public final CompanionKind kind;
    private final Map<UUID, Relation> relations = new HashMap<>();
    private Mode mode = Mode.FREE;
    private UUID leader, spouse;
    private BlockPos home;
    private int mood;
    private int abilityTimer;
    private boolean secondPhase;

    public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level, CompanionKind kind) {
        super(type, level);
        this.kind = kind;
        var held = kind.held.get();
        if (held != net.minecraft.world.item.Items.AIR) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(held));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setPersistenceRequired();
    }

    private static CompanionKind kindOf(EntityType<?> type) {
        for (Map.Entry<CompanionKind, EntityType<CompanionEntity>> e : net.zennin.mushoku.registry.ModEntities.COMPANIONS.entrySet())
            if (e.getValue() == type) return e.getKey();
        return CompanionKind.ERIS;
    }

    public static AttributeSupplier.Builder attributes(CompanionKind k) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.health).add(Attributes.ARMOR, k.armor).add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, k.follow).add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    // ---------- Datos ----------

    public Relation relation(UUID player) { return relations.computeIfAbsent(player, k -> new Relation()); }

    public Mode mode() { return mode; }

    public void setMode(Mode m, Player p) {
        mode = m;
        leader = m == Mode.FOLLOW ? p.getUUID() : null;
        if (m == Mode.STAY) getNavigation().stop();
    }

    public UUID spouse() { return spouse; }

    public void setSpouse(UUID id) { spouse = id; }

    public BlockPos home() { return home; }

    public void setHome(BlockPos pos) { home = pos; }

    public int mood() { return mood; }

    public void addMood(int n) { mood = Math.max(-15, Math.min(15, mood + n)); }

    public Player leaderPlayer() { return leader == null ? null : level().getPlayerByUUID(leader); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag rel = new CompoundTag();
        relations.forEach((id, r) -> rel.put(id.toString(), r.save()));
        tag.put("Relations", rel);
        tag.putString("Mode", mode.name());
        if (leader != null) tag.putUUID("Leader", leader);
        if (spouse != null) tag.putUUID("Spouse", spouse);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("Mood", mood);
        tag.putBoolean("SecondPhase", secondPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        relations.clear();
        CompoundTag rel = tag.getCompound("Relations");
        for (String k : rel.getAllKeys()) {
            try { relations.put(UUID.fromString(k), Relation.load(rel.getCompound(k))); } catch (IllegalArgumentException ignored) {}
        }
        try { mode = Mode.valueOf(tag.getString("Mode")); } catch (IllegalArgumentException e) { mode = Mode.FREE; }
        leader = tag.hasUUID("Leader") ? tag.getUUID("Leader") : null;
        spouse = tag.hasUUID("Spouse") ? tag.getUUID("Spouse") : null;
        Optional<BlockPos> h = NbtUtils.readBlockPos(tag, "Home");
        home = h.orElse(null);
        mood = tag.getInt("Mood");
        secondPhase = tag.getBoolean("SecondPhase");
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Ojo: registerGoals corre dentro del constructor de Mob, antes de que se asigne 'kind'; se saca del tipo de entidad.
        if (kindOf(getType()).ranged()) goalSelector.addGoal(2, new RangedAttackGoal(this, 1.1, 40, 20f));
        else goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new FollowLeaderGoal());
        goalSelector.addGoal(4, new ReturnHomeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override public boolean canUse() { return mode == Mode.FREE && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendLeaderGoal());
        targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return super.canUse() && !isFriend(getLastHurtByMob()); }
        });
    }

    /** Un amigo (corazones >= 25 o casado) no se vuelve enemigo si te pega por accidente. */
    boolean isFriend(LivingEntity e) {
        if (!(e instanceof Player p)) return false;
        return p.getUUID().equals(spouse) || p.getUUID().equals(leader) || relation(p.getUUID()).hearts >= 25;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide && source.getEntity() instanceof ServerPlayer p && isFriend(p)) {
            relation(p.getUUID()).addHearts(-5);
            addMood(-2);
            Interactions.say(this, p, "hurt");
            setTarget(null);
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (kind == CompanionKind.KISHIRIKA && source.getEntity() instanceof ServerPlayer sp) net.zennin.mushoku.race.RaceSystem.grantLaplace(sp);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) Interactions.open(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (tickCount % 1200 == 0 && mood != 0) mood += mood > 0 ? -1 : 1;
        if (tickCount % 20 == 0) relations.values().forEach(r -> r.decay(level().getGameTime()));
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        abilityTimer++;
        ability((ServerLevel) level(), target);
    }

    /** Tecnicas de cada personaje (basadas en el addon original). */
    private void ability(ServerLevel sl, LivingEntity t) {
        switch (kind.combat) {
            case GHISLAINE, RUIJERD -> {
                int every = kind.combat == CompanionKind.Combat.GHISLAINE ? 360 : 240;
                if (abilityTimer % every == 0) behind(sl, t);
                if (kind.combat == CompanionKind.Combat.RUIJERD && abilityTimer % 200 == 100) {
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1));
                }
            }
            case KISHIRIKA -> { if (abilityTimer % 120 == 0) behind(sl, t); }
            case MELEE -> {
                if (kind == CompanionKind.ERIS && abilityTimer % 100 == 0 && distanceTo(t) < 10) {
                    Vec3 d = t.position().subtract(position()).normalize();
                    setDeltaMovement(d.x * 1.5, 0.3, d.z * 1.5);
                    hurtMarked = true;
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
            }
            case EARTH -> {
                if (abilityTimer % 180 == 0) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.SAND.defaultBlockState()), t.getX(), t.getY() + 1, t.getZ(), 60, 1.2, 0.8, 1.2, 0);
                }
                // Segunda fase de Rudeus: se pone la armadura Mk 2 al quedar con poca vida
                if (kind == CompanionKind.RUDEUS && !secondPhase && getHealth() < getMaxHealth() * 0.5f) {
                    secondPhase = true;
                    setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.MK2_CHESTPLATE));
                    setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.MK2_LEGGINGS));
                    setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MK2_BOOTS));
                    for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) setDropChance(s, 0f);
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
                    playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.5f, 0.8f);
                }
            }
            default -> {}
        }
    }

    private void behind(ServerLevel sl, LivingEntity t) {
        Vec3 back = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.1);
        teleportTo(back.x, t.getY(), back.z);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        int kindP = switch (kind.combat) { case WATER -> MagicProjectile.WATER; case EARTH -> MagicProjectile.ROCK; default -> MagicProjectile.WIND; };
        float dmg = switch (kind.combat) { case WATER -> 14f; case EARTH -> 16f; default -> 6f; };
        MagicProjectile p = new MagicProjectile(level(), this, kindP, dmg).knockback(0.8f);
        if (kind.combat != CompanionKind.Combat.WIND) p.size(1.5f);
        p.setPos(getX(), getEyeY() - 0.1, getZ());
        Vec3 d = target.getEyePosition().subtract(p.position());
        p.shoot(d.x, d.y, d.z, 1.6f, 1f);
        level().addFreshEntity(p);
        playSound(kind.combat == CompanionKind.Combat.WIND ? SoundEvents.BREEZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL, 1f, 1.2f);
        swing(InteractionHand.MAIN_HAND);
    }

    // ---------- Metas ----------

    private class FollowLeaderGoal extends Goal {
        FollowLeaderGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            Player p = leaderPlayer();
            return mode == Mode.FOLLOW && p != null && !p.isSpectator() && distanceToSqr(p) > 25 && getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = leaderPlayer();
            return mode == Mode.FOLLOW && p != null && distanceToSqr(p) > 9 && getTarget() == null;
        }

        @Override
        public void tick() {
            Player p = leaderPlayer();
            if (p == null) return;
            getLookControl().setLookAt(p, 10f, getMaxHeadXRot());
            if (distanceToSqr(p) > 24 * 24 && p.onGround()) teleportTo(p.getX(), p.getY(), p.getZ());
            else getNavigation().moveTo(p, 1.15);
        }

        @Override
        public void stop() { getNavigation().stop(); }
    }

    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() { return mode == Mode.FREE && home != null && home.distSqr(blockPosition()) > 32 * 32 && getTarget() == null; }

        @Override
        public void tick() { getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0); }
    }

    /** Defiende y ayuda a quien sigue: ataca a lo que te ataca y a lo que atacas. */
    private class DefendLeaderGoal extends TargetGoal {
        private LivingEntity pick;

        DefendLeaderGoal() {
            super(CompanionEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player p = leaderPlayer();
            if (mode != Mode.FOLLOW || p == null) return false;
            LivingEntity a = p.getLastHurtByMob();
            if (a == null || p.tickCount - p.getLastHurtByMobTimestamp() > 100) a = p.getLastHurtMob();
            if (a == null || a == CompanionEntity.this || a instanceof Player || a instanceof CompanionEntity || !a.isAlive()) return false;
            pick = a;
            return canAttack(a, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(pick);
            super.start();
        }
    }

    public Component title() { return getDisplayName(); }
}

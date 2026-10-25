package net.zennin.mushoku.entity.npc;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.registry.ModItems;

/** Bandido mago: lleva varita y lanza magia desde lejos (vida 20, como el original). */
public class MageBanditEntity extends Monster implements RangedAttackMob {
    public MageBanditEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.25, 40, 60, 20f));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.WAND));
        setDropChance(EquipmentSlot.MAINHAND, 0.05f);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return d;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        int[] kinds = {MagicProjectile.WATER, MagicProjectile.FIRE, MagicProjectile.ROCK, MagicProjectile.WIND, MagicProjectile.ICE};
        MagicProjectile p = new MagicProjectile(level(), this, kinds[getRandom().nextInt(kinds.length)], 4f);
        p.setPos(getX(), getEyeY() - 0.1, getZ());
        Vec3 d = target.getEyePosition().subtract(p.position());
        p.shoot(d.x, d.y, d.z, 1.4f, 3f);
        level().addFreshEntity(p);
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1f, 1.3f);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
}

package net.zennin.mushoku.entity.npc;

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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Bandidos comunes de los caminos (hombres y mujeres): unos pelean cuerpo a cuerpo con espada o hacha, otros
 * disparan con arco y se alejan si te acercas.
 */
public class OutlawEntity extends Monster implements RangedAttackMob {
    private final boolean archer;
    private RangedBowAttackGoal<OutlawEntity> bowGoal;
    private MeleeAttackGoal meleeGoal;

    public OutlawEntity(EntityType<? extends Monster> type, Level level, boolean archer) {
        super(type, level);
        this.archer = archer;
        this.xpReward = 8;
        reassessWeaponGoal();
    }

    public boolean archer() { return archer; }

    public static AttributeSupplier.Builder attributes(boolean archer) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, archer ? 26 : 34)
                .add(Attributes.MOVEMENT_SPEED, archer ? 0.28 : 0.3)
                .add(Attributes.ATTACK_DAMAGE, archer ? 2 : 4)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, archer ? 24 : 18);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    /** Arco en la mano = dispara; si no, cuerpo a cuerpo. */
    private void reassessWeaponGoal() {
        if (level() == null || level().isClientSide) return;
        if (bowGoal == null) bowGoal = new RangedBowAttackGoal<>(this, 1.0, 22, 15f);
        if (meleeGoal == null) meleeGoal = new MeleeAttackGoal(this, 1.2, false);
        goalSelector.removeGoal(bowGoal);
        goalSelector.removeGoal(meleeGoal);
        if (getMainHandItem().getItem() instanceof BowItem) goalSelector.addGoal(3, bowGoal);
        else goalSelector.addGoal(3, meleeGoal);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!level().isClientSide) reassessWeaponGoal();
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (archer) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        else setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextFloat() < 0.35f ? Items.IRON_AXE
                : random.nextFloat() < 0.5f ? Items.IRON_SWORD : Items.STONE_SWORD));
        if (random.nextFloat() < 0.25f) setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        reassessWeaponGoal();
        return d;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        reassessWeaponGoal();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack bow = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        ItemStack ammo = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, power, bow);
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = target.getY(0.3333) - arrow.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, 10 - level().getDifficulty().getId() * 3);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, 1f, 1f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }

    @Override
    public boolean canFireProjectileWeapon(net.minecraft.world.item.ProjectileWeaponItem weapon) { return weapon == Items.BOW; }
}

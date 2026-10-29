package net.zennin.mushoku.entity.npc;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Hombres y mujeres bestia (tribus del Gran Bosque): oso, perro y gato. Andan tranquilos, cazan monstruos y solo
 * pelean con el que los ataca. Los osos son fuertes y lentos, los perros parejos y los gatos rapidos.
 */
public class BeastfolkEntity extends PathfinderMob {
    public enum Tribe { BEAR, DOG, CAT }

    private final Tribe tribe;

    public BeastfolkEntity(EntityType<? extends PathfinderMob> type, Level level, Tribe tribe) {
        super(type, level);
        this.tribe = tribe;
        this.xpReward = 5;
    }

    public Tribe tribe() { return tribe; }

    public static AttributeSupplier.Builder attributes(Tribe t) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, switch (t) { case BEAR -> 50; case DOG -> 36; case CAT -> 28; })
                .add(Attributes.MOVEMENT_SPEED, switch (t) { case BEAR -> 0.26; case DOG -> 0.31; case CAT -> 0.34; })
                .add(Attributes.ATTACK_DAMAGE, switch (t) { case BEAR -> 6; case DOG -> 4; case CAT -> 3; })
                .add(Attributes.KNOCKBACK_RESISTANCE, t == Tribe.BEAR ? 0.5 : 0.0)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, tribe == Tribe.CAT ? 1.35 : 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // cazan monstruos (no a los creepers: les tienen miedo)
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof net.minecraft.world.entity.monster.Creeper)));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (tribe) {
            case BEAR -> random.nextBoolean() ? Items.IRON_AXE : Items.STONE_AXE;
            case DOG -> random.nextFloat() < 0.6f ? Items.IRON_SWORD : Items.STONE_SWORD;
            case CAT -> random.nextFloat() < 0.5f ? Items.IRON_SWORD : Items.AIR;
        }));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return d;
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
}

package net.zennin.mushoku.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
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

/** Guardia de aldea del addon: patrulla y ataca a los monstruos cercanos (vida 40, armadura 10, daño 4). */
public class VillagerGuardEntity extends PathfinderMob {
    public VillagerGuardEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 6, true, false, e -> true));
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
}

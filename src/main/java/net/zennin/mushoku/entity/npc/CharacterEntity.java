package net.zennin.mushoku.entity.npc;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.registry.ModItems;

/**
 * Personajes extra (iguales a la version de Forge): caminan, miran y solo atacan si los atacas.
 *  Eris Greyrat: espadachina con las stats de Ghislaine (vida 256, armadura 20, daño 16).
 *  Sylphiette Greyrat: con varita, ataca a distancia con cortes de viento (vida 150).
 *  Sylphiette joven: se defiende a golpes (vida 40).
 */
public class CharacterEntity extends PathfinderMob implements RangedAttackMob {
    private final boolean caster;

    public CharacterEntity(EntityType<? extends PathfinderMob> type, Level level, boolean caster) {
        super(type, level);
        this.caster = caster;
        if (caster) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.WAND));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    public static AttributeSupplier.Builder stats(double health, double armor, double damage) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health).add(Attributes.ARMOR, armor).add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (caster) goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 30, 20f));
        else goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        MagicProjectile p = new MagicProjectile(level(), this, MagicProjectile.WIND, 6f).knockback(0.8f);
        p.setPos(getX(), getEyeY() - 0.1, getZ());
        Vec3 d = target.getEyePosition().subtract(p.position());
        p.shoot(d.x, d.y, d.z, 1.6f, 1f);
        level().addFreshEntity(p);
        playSound(SoundEvents.BREEZE_SHOOT, 1f, 1.2f);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
}

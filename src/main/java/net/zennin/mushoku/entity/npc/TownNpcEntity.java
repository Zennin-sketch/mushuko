package net.zennin.mushoku.entity.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * NPCs de las ciudades (herrero, tendero, vendedor de magia, recepcionista del gremio).
 * Se quedan en su lugar mirando a los jugadores, como en el original (vida 1000). Las tiendas llegan en la parte 5D.
 */
public class TownNpcEntity extends PathfinderMob {
    private final String role;

    public TownNpcEntity(EntityType<? extends PathfinderMob> type, Level level, String role) {
        super(type, level);
        this.role = role;
    }

    public String role() { return role; }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f, 1f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.translatable("msg.remushoku.npc_" + role).withStyle(ChatFormatting.YELLOW), false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}

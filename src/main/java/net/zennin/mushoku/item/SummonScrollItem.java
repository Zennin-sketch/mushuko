package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.npc.KalmanEntity;
import net.zennin.mushoku.registry.ModEntities;

/** Pergamino de invocacion: aparece el Dios Norte Kalman III delante tuyo. */
public class SummonScrollItem extends Item {
    public SummonScrollItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 5, 0, look.z * 5);
        KalmanEntity k = ModEntities.KALMAN.create(sl);
        if (k == null) return InteractionResultHolder.fail(stack);
        k.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180, 0);
        k.finalizeSpawn(sl, sl.getCurrentDifficultyAt(k.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        sl.addFreshEntity(k);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.2f);
        player.displayClientMessage(Component.translatable("msg.remushoku.kalman_summoned").withStyle(ChatFormatting.DARK_PURPLE), false);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}

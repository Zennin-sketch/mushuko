package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;

import java.util.List;

/** Pocion de mana (sale del Alambique de Cobre): al tomarla devuelve mana y deja la botella. */
public class ManaPotionItem extends Item {
    private final int amount;

    public ManaPotionItem(int amount, Properties props) {
        super(props);
        this.amount = amount;
    }

    /** Se toma de un click (sin mantener): trago rapido, devuelve mana y deja la botella. 5 s de espera entre pociones. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.35f + level.random.nextFloat() * 0.1f);
        if (player instanceof ServerPlayer p) {
            ManaData m = ModAttachments.get(p);
            ModAttachments.set(p, m.withMana(m.mana() + amount));
            ModNetworking.syncMana(p);
            // trago y el brillo del mana entrando, un instante despues
            p.server.execute(new net.minecraft.server.TickTask(p.server.getTickCount() + 5, () -> {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.HONEY_DRINK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 0.8f);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.4f);
            }));
            ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.5, 0.4, 0.6);
        }
        for (Item potion : new Item[]{net.zennin.mushoku.registry.ModItems.MANA_POTION_SMALL, net.zennin.mushoku.registry.ModItems.MANA_POTION,
                net.zennin.mushoku.registry.ModItems.MANA_POTION_LARGE})
            player.getCooldowns().addCooldown(potion, 100);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) return InteractionResultHolder.sidedSuccess(bottle, level.isClientSide);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.mana_potion", amount).withStyle(ChatFormatting.BLUE));
    }
}

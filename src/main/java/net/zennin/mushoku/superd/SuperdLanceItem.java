package net.zennin.mushoku.superd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Lanzas de la tribu Superd. Cualquiera puede usarlas como arma, pero solo un Superd desata con ellas sus habilidades
 * (embestida, giro, lanzamiento). La Lanza Maldita de Laplace es la que Laplace les dio en la guerra: pega mas fuerte,
 * pero a veces "susurra" y te enloquece un momento (fuerza y mareo), como la locura que convirtio a los Superd en
 * demonios temidos.
 */
public class SuperdLanceItem extends SwordItem {
    private final String lore;
    private final boolean cursed;

    public SuperdLanceItem(Tier tier, int damage, float speed, String lore, boolean cursed) {
        super(tier, new Properties().attributes(SwordItem.createAttributes(tier, damage, speed)).rarity(cursed
                ? net.minecraft.world.item.Rarity.EPIC : net.minecraft.world.item.Rarity.UNCOMMON));
        this.lore = lore;
        this.cursed = cursed;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (cursed && attacker instanceof Player p && !p.level().isClientSide && p.getRandom().nextFloat() < 0.18f) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 70, 0));
            p.displayClientMessage(Component.translatable("msg.remushoku.laplace_whisper").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) {
        l.add(Component.translatable("tooltip.remushoku." + lore).withStyle(cursed ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GREEN));
        l.add(Component.translatable("tooltip.remushoku.superd_lance_skills").withStyle(ChatFormatting.GRAY));
    }
}

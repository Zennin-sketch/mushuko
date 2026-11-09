package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Espadas y lanzas nuevas con modelo 3D (tools/blocks/weapons2.py), cada una con un efecto pasivo de su lore:
 * <ul>
 *   <li>sanctuary_blade (Estilo Dios de la Espada): a veces te da Velocidad, "el que ataca primero gana".</li>
 *   <li>north_god_greatsword (Estilo Dios del Norte): a veces aplasta y deja lento al enemigo.</li>
 *   <li>water_god_blade (Estilo Dios del Agua): a veces te da Resistencia, la defensa que espera el contraataque.</li>
 *   <li>millis_holy_spear (Caballeros Sagrados de Millis): pega mas a los no-muertos y a veces te cura.</li>
 *   <li>red_wyrm_spear (colmillo de Wyrm Rojo): prende fuego al enemigo.</li>
 *   <li>great_forest_spear (cazadores del Gran Bosque): a veces envenena.</li>
 * </ul>
 */
public class LoreWeaponItem extends SwordItem {
    private final String key;

    public LoreWeaponItem(Tier tier, int damage, float speed, String key, Rarity rarity) {
        super(tier, new Properties().attributes(SwordItem.createAttributes(tier, damage, speed)).rarity(rarity));
        this.key = key;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level().isClientSide) return r;
        float roll = attacker.getRandom().nextFloat();
        switch (key) {
            case "sanctuary_blade" -> { if (roll < 0.25f) attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0)); }
            case "north_god_greatsword" -> {
                if (roll < 0.25f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    target.knockback(0.6, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                }
            }
            case "water_god_blade" -> { if (roll < 0.3f) attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0)); }
            case "millis_holy_spear" -> {
                if (target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD))
                    target.hurt(attacker.damageSources().magic(), 4f);
                if (roll < 0.2f) attacker.heal(2f);
            }
            case "red_wyrm_spear" -> target.igniteForSeconds(4);
            case "great_forest_spear" -> { if (roll < 0.25f) target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0)); }
            default -> {}
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku." + key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.remushoku." + key + ".passive").withStyle(ChatFormatting.GOLD));
    }
}

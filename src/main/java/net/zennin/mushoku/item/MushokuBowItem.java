package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Arco del mod: modelo 3D propio (ver tools/bows/bows.py) con velocidad de tensado y dano propios.
 * speed > 1 tensa mas rapido (llega antes a la fuerza maxima); damage multiplica el dano base de la flecha.
 */
public class MushokuBowItem extends BowItem {
    public final float speed, damage;

    public MushokuBowItem(Properties props, float speed, float damage) {
        super(props);
        this.speed = speed;
        this.damage = damage;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int full = getUseDuration(stack, entity);
        int used = Math.round((full - timeLeft) * speed);
        super.releaseUsing(stack, level, entity, full - used);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
        Projectile p = super.createProjectile(level, shooter, weapon, ammo, crit);
        if (p instanceof AbstractArrow a) a.setBaseDamage(a.getBaseDamage() * damage);
        return p;
    }

    /** Cuanto esta tensado (0..1) para el modelo, ya con la velocidad del arco. */
    public float pull(ItemStack stack, LivingEntity entity) {
        if (entity == null || entity.getUseItem() != stack) return 0;
        return Math.min(1f, (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) * speed / 20f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.remushoku.bow_speed", String.format("%.0f%%", speed * 100)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.remushoku.bow_damage", String.format("×%.2f", damage)).withStyle(ChatFormatting.GRAY));
    }
}

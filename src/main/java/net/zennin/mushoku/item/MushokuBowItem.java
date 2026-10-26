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
 * Arco del mod: modelo 3D liso dibujado por codigo (client/render/BowRender), agarre tipo Unturned (mano izquierda),
 * se carga con R y se tensa con el clic. Velocidad de tensado y dano propios.
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
        boolean shoots = getPowerForTime(used) >= 0.1f && entity instanceof net.minecraft.world.entity.player.Player pl
                && (pl.getAbilities().instabuild || !pl.getProjectile(stack).isEmpty());
        super.releaseUsing(stack, level, entity, full - used);
        // sale la flecha: hay que volver a cargar con R
        if (shoots && !level.isClientSide) net.zennin.mushoku.archer.BowSystem.setLoaded(stack, false);
    }

    /** Solo se tensa con la flecha cargada (R). */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!net.zennin.mushoku.archer.BowSystem.isLoaded(stack)
                || (player instanceof net.minecraft.server.level.ServerPlayer sp && net.zennin.mushoku.archer.BowSystem.busy(sp))) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.remushoku.bow_reload_hint",
                    net.minecraft.network.chat.Component.keybind("key.remushoku.bow_reload")).withStyle(ChatFormatting.GRAY), true);
            return net.minecraft.world.InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    /** Cargar o descargar la flecha no hace la animacion de "cambiar de objeto" en primera persona. */
    @Override
    public boolean allowComponentsUpdateAnimation(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                                                  ItemStack oldStack, ItemStack newStack) {
        return false;
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

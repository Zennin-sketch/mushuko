package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.data.ExtraData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.entity.SwapTotemEntity;
import net.zennin.mushoku.entity.ThrownMushokuItem;
import net.zennin.mushoku.extra.DemonEyes;
import net.zennin.mushoku.guild.GuildSystem;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.registry.ModEntities;
import net.zennin.mushoku.registry.ModItems;
import net.zennin.mushoku.sword.SwordSkill;

import java.util.List;

/** Objetos especiales de la Fase 6. */
public final class ExtraItems {
    private ExtraItems() {}

    /** Los objetos de combate del Dios Norte necesitan la habilidad "Objetos" (como en el original). */
    static boolean needsItems(Player p) {
        if (p.getAbilities().instabuild || ModAttachments.sword(p).knows(SwordSkill.NG_ITEMS.id)) return false;
        p.displayClientMessage(Component.translatable("msg.remushoku.need_items").withStyle(ChatFormatting.RED), true);
        return true;
    }

    static void tip(List<Component> lines, String key) { lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY)); }

    /** Abre un menu del cliente (libro guia, libro de hechizos, ojos). */
    public static class ScreenItem extends Item {
        private final String screen, tooltip;

        public ScreenItem(String screen, String tooltip, Properties p) { super(p); this.screen = screen; this.tooltip = tooltip; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                DemonEyes.sync(sp);
                GuildSystem.open(sp, screen);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, tooltip); }
    }

    /** Roca y Bolsa de Especias: se lanzan. */
    public static class ThrowItem extends Item {
        private final float damage;
        private final String tooltip;

        public ThrowItem(float damage, String tooltip, Properties p) { super(p); this.damage = damage; this.tooltip = tooltip; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (needsItems(player)) return InteractionResultHolder.fail(stack);
            // La roca pega mas fuerte con Touki (como el original: +1.5 por nivel)
            float dmg = damage + (stack.is(ModItems.ROCK) ? ModAttachments.sword(player).touki() * 1.5f : 0);
            ThrownMushokuItem t = new ThrownMushokuItem(level, player, dmg);
            t.setItem(stack.copyWithCount(1));
            t.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.5f, 1f);
            level.addFreshEntity(t);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
            player.getCooldowns().addCooldown(this, 10);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, tooltip); }
    }

    /** Totem de Intercambio: clic derecho sobre un bloque lo coloca (uno por jugador). */
    public static class SwapTotemItem extends Item {
        public SwapTotemItem(Properties p) { super(p); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Player player = ctx.getPlayer();
            Level level = ctx.getLevel();
            if (player == null || level.isClientSide) return InteractionResult.SUCCESS;
            if (needsItems(player)) return InteractionResult.FAIL;
            SwapTotemEntity totem = ModEntities.SWAP_TOTEM.create(level);
            if (totem == null) return InteractionResult.FAIL;
            // Quita el totem anterior de este jugador
            level.getEntitiesOfClass(SwapTotemEntity.class, new AABB(player.blockPosition()).inflate(256),
                    e -> player.getUUID().equals(e.owner())).forEach(e -> e.discard());
            Vec3 pos = Vec3.atBottomCenterOf(ctx.getClickedPos().relative(ctx.getClickedFace()));
            totem.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180, 0);
            totem.setOwner(player.getUUID());
            totem.finalizeSpawn((net.minecraft.server.level.ServerLevel) level, level.getCurrentDifficultyAt(totem.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(totem);
            level.playSound(null, totem.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
            if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, "tooltip.remushoku.swap_totem"); }
    }

    /** Capa de Intercambio: cambia tu lugar con el de tu totem (hasta 24 bloques). */
    public static class SwapCloakItem extends Item {
        public SwapCloakItem(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (needsItems(player)) return InteractionResultHolder.fail(stack);
            SwapTotemEntity totem = level.getEntitiesOfClass(SwapTotemEntity.class, player.getBoundingBox().inflate(24),
                    e -> player.getUUID().equals(e.owner())).stream().findFirst().orElse(null);
            if (totem == null) {
                player.displayClientMessage(Component.translatable("msg.remushoku.no_totem").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            Vec3 me = player.position(), it = totem.position();
            player.teleportTo(it.x, it.y, it.z);
            totem.teleportTo(me.x, me.y, me.z);
            player.fallDistance = 0;
            level.playSound(null, it.x, it.y, it.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
            level.playSound(null, me.x, me.y, me.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, "tooltip.remushoku.swap_cloak"); }
    }

    /** Tomo de Conjuro Silencioso: activa o desactiva el conjuro silencioso del mago. */
    public static class SilentTomeItem extends Item {
        public SilentTomeItem(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
            if (!SpellCasting.canCast(sp)) {
                sp.displayClientMessage(Component.translatable("msg.remushoku.silent_mage_only").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            ExtraData d = ModAttachments.extra(sp);
            boolean on = !d.silent();
            ModAttachments.setExtra(sp, d.withSilent(on));
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), on ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.LILY_PAD_PLACE, SoundSource.PLAYERS, 1f, 1f);
            sp.displayClientMessage(Component.translatable(on ? "msg.remushoku.silent_on" : "msg.remushoku.silent_off")
                    .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY), false);
            DemonEyes.sync(sp);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public boolean isFoil(ItemStack stack) { return false; }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, "tooltip.remushoku.silent_tome"); }
    }

    /** Objeto que solo agrega una descripcion (garras de escalada, amplificadores de magia). */
    public static class TipItem extends Item {
        private final String tooltip;

        public TipItem(String tooltip, Properties p) { super(p); this.tooltip = tooltip; }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) { tip(lines, tooltip); }
    }
}

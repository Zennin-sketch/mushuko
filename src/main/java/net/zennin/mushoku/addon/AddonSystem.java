package net.zennin.mushoku.addon;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.entity.npc.VillagerGuardEntity;
import net.zennin.mushoku.registry.ModEntities;

/**
 * Maldicion Superd y efectos de las armaduras del addon.
 *  Matar con la Lanza Superd te maldice: los aldeanos no comercian contigo y los guardias te persiguen en las aldeas.
 *  El Casco Supresor anula la maldicion mientras lo llevas; la Cura la quita.
 *  Zapatos Encantados: velocidad. Capa Atrapallamas: inmune al fuego. Lentes Magicos: inmune a ceguera y oscuridad.
 */
public final class AddonSystem {
    private AddonSystem() {}

    public static boolean cursed(Player p) {
        return p.hasEffect(AddonItems.SUPERD_CURSE) && !p.getItemBySlot(EquipmentSlot.HEAD).is(AddonItems.CURSE_SUPPRESSING_HELMET);
    }

    public static void init() {
        AddonItems.init();
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (source.getEntity() instanceof ServerPlayer p && p.getMainHandItem().is(AddonItems.SUPERD_SPEAR) && !p.hasEffect(AddonItems.SUPERD_CURSE)) {
                p.addEffect(new MobEffectInstance(AddonItems.SUPERD_CURSE, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
                p.displayClientMessage(Component.translatable("msg.remushoku.cursed").withStyle(ChatFormatting.DARK_RED), false);
            }
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof AbstractVillager && cursed(player)) {
                if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.remushoku.cursed_trade").withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            if (t % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getItemBySlot(EquipmentSlot.FEET).is(AddonItems.ENCHANTED_SHOES))
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
                if (p.getItemBySlot(EquipmentSlot.CHEST).is(AddonItems.FLAME_TRAPPING_CLOAK))
                    p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false, true));
                if (p.getItemBySlot(EquipmentSlot.HEAD).is(AddonItems.MAGICAL_SUNGLASSES)) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.DARKNESS);
                }
                if (t % 600 == 0 && cursed(p)) curseTick(p);
            }
        });
    }

    /** Cada 30 s: malestar y, si hay aldeanos cerca, aparecen guardias a buscarte (como el addon). */
    private static void curseTick(ServerPlayer p) {
        if (p.getRandom().nextBoolean()) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
        }
        var level = p.serverLevel();
        boolean village = !level.getEntitiesOfClass(AbstractVillager.class, p.getBoundingBox().inflate(32)).isEmpty();
        int guards = level.getEntitiesOfClass(VillagerGuardEntity.class, p.getBoundingBox().inflate(32)).size();
        if (!village || guards >= 3) return;
        for (int i = 0; i < 2; i++) {
            VillagerGuardEntity g = ModEntities.VILLAGER_GUARD.create(level);
            if (g == null) return;
            double x = p.getX() + (p.getRandom().nextBoolean() ? 3 : -3), z = p.getZ() + (p.getRandom().nextBoolean() ? 3 : -3);
            g.moveTo(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z), z, 0, 0);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.EVENT, null);
            g.setTarget(p);
            level.addFreshEntity(g);
        }
        p.displayClientMessage(Component.translatable("msg.remushoku.guards_coming").withStyle(ChatFormatting.RED), true);
    }
}

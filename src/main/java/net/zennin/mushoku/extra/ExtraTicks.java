package net.zennin.mushoku.extra;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.registry.ModItems;

/** Efectos por tick: set completo Mk 2 y garras de escalada (sin daño de caida mientras trepas). */
public final class ExtraTicks {
    private ExtraTicks() {}

    public static boolean holdingClaws(Player p) {
        return p.getMainHandItem().is(ModItems.METAL_CLAWS) || p.getOffhandItem().is(ModItems.METAL_CLAWS);
    }

    /** Ultimo Suspiro (+50%) y Aqua Heartia (+20%) amplifican el daño de la magia cuando los llevas en la mano. */
    public static float magicAmp(Player p) {
        float amp = 1f;
        if (p.getMainHandItem().is(ModItems.ULTIMO_SUSPIRO) || p.getOffhandItem().is(ModItems.ULTIMO_SUSPIRO)) amp += 0.5f;
        if (p.getMainHandItem().is(ModItems.AQUA_HEARTIA) || p.getOffhandItem().is(ModItems.AQUA_HEARTIA)) amp += 0.2f;
        return amp;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean second = server.getTickCount() % 40 == 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (holdingClaws(p) && p.horizontalCollision) p.fallDistance = 0;
                if (!second) continue;
                boolean fullSet = p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MK2_CHESTPLATE)
                        && p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.MK2_LEGGINGS) && p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.MK2_BOOTS);
                MobEffectInstance boost = p.getEffect(MobEffects.HEALTH_BOOST);
                if (fullSet) {
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
                    // Vida extra sin refrescar (refrescarla haria perder los corazones extra)
                    if (boost == null) p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, false, true));
                } else if (boost != null && boost.isInfiniteDuration()) {
                    p.removeEffect(MobEffects.HEALTH_BOOST);
                }
            }
        });
    }
}

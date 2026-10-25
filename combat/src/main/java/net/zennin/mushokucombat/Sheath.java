package net.zennin.mushokucombat;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushokucombat.net.SheathPayload;
import net.zennin.mushokucombat.net.SheathStatePayload;

import java.util.function.Predicate;

/**
 * Modo combate: el arma esta guardada (en la cintura o la espalda) o en la mano.
 * Con el arma guardada no hay pose de Better Combat y el primer ataque la desenvaina.
 */
public final class Sheath {
    public static final byte SHEATHED = 0, DRAWN = 1, DRAWING = 2, SHEATHING = 3;

    public static final AttachmentType<Boolean> IS_DRAWN = AttachmentRegistry.<Boolean>builder()
            .initializer(() -> false)
            .buildAndRegister(MushokuCombat.id("drawn"));

    /** El cliente lo reemplaza para sus propios jugadores (ve el estado local al instante). */
    public static Predicate<Player> clientDrawn = p -> true;

    private Sheath() {}

    /** Armas de Mushoku que se guardan (mismo listado que tools/catalog.py), mas espadas y tridentes en general. */
    private static final java.util.Set<String> WEAPONS = java.util.Set.of("migurd_sword", "pauls_sword", "almanfi_sword", "hiramune", "ice_blade", "phoenix_dragon_sword", "stone_blade", "katana", "kajakut", "war_axe", "scythe", "superd_spear", "aqua_heartia", "dragon_staff", "staff_of_areos", "migurd_staff", "wand", "metal_rod");

    public static boolean sheathable(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem || stack.getItem() instanceof net.minecraft.world.item.TridentItem) return true;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals("remushoku") && WEAPONS.contains(key.getPath());
    }

    public static boolean isSheathed(Player p) {
        if (!sheathable(p.getMainHandItem())) return false;
        return p.level().isClientSide() ? !clientDrawn.test(p) : !p.getAttachedOrCreate(IS_DRAWN);
    }

    static void init() {
        ServerPlayNetworking.registerGlobalReceiver(SheathPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                byte s = payload.state();
                if (s < SHEATHED || s > SHEATHING) return;
                p.setAttached(IS_DRAWN, s == DRAWN || s == DRAWING);
                SheathStatePayload msg = new SheathStatePayload(p.getId(), s);
                for (ServerPlayer other : PlayerLookup.tracking(p)) {
                    if (other != p && ServerPlayNetworking.canSend(other, SheathStatePayload.TYPE)) ServerPlayNetworking.send(other, msg);
                }
            });
        });
        // Quien empieza a ver a un jugador recibe su estado (sin animacion)
        EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
            if (entity instanceof ServerPlayer target && ServerPlayNetworking.canSend(viewer, SheathStatePayload.TYPE)) {
                ServerPlayNetworking.send(viewer, new SheathStatePayload(target.getId(), target.getAttachedOrCreate(IS_DRAWN) ? DRAWN : SHEATHED));
            }
        });
    }
}

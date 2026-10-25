package net.zennin.mushoku.magic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Eleccion de clase: al entrar por primera vez (o con el orbe de cambio de clase) se abre el menu. */
public final class PlayerClasses {
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private PlayerClasses() {}

    public static void choose(ServerPlayer p, int cls) {
        ManaData d = ModAttachments.get(p);
        if (d.playerClass() != 0) return; // ya eligio
        ModAttachments.set(p, d.withClass(cls));
        p.displayClientMessage(Component.translatable(cls == SpellCasting.CLASS_MAGE ? "msg.remushoku.class_mage" : "msg.remushoku.class_swordsman")
                .withStyle(ChatFormatting.GOLD), false);
        ModNetworking.syncMana(p);
    }

    /** Vuelve la clase a "sin elegir" y abre el menu (orbe de cambio de clase). */
    public static void reset(ServerPlayer p) {
        ModAttachments.set(p, ModAttachments.get(p).withClass(0));
        ModNetworking.syncMana(p);
        ModNetworking.openClassScreen(p);
    }

    public static void init() {
        // Se espera 2 segundos despues de entrar para que el mundo termine de cargar antes de abrir el menu.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PENDING.put(handler.getPlayer().getUUID(), 40));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            PENDING.replaceAll((id, t) -> t - 1);
            PENDING.entrySet().removeIf(e -> {
                if (e.getValue() > 0) return false;
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null) {
                    ModNetworking.syncMagic(p);
                    if (ModAttachments.get(p).playerClass() == 0) ModNetworking.openClassScreen(p);
                }
                return true;
            });
        });
    }
}

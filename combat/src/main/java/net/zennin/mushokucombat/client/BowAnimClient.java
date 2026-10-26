package net.zennin.mushokucombat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.BowItem;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Animacion del arco para todos los jugadores que se ven (no hace falta red: el "esta usando un item" ya viaja solo):
 * al empezar a tensar saca la flecha del carcaj, la coloca y tensa; al soltar, la mano de la cuerda sale hacia atras.
 * Mientras tensa, cabeza y brazos apuntan a donde mira (ver CombatAnimations.aim).
 */
public final class BowAnimClient {
    private static final Map<AbstractClientPlayer, Boolean> DRAWING = new WeakHashMap<>();

    private BowAnimClient() {}

    static void tick(Minecraft mc) {
        if (mc.level == null) {
            DRAWING.clear();
            return;
        }
        for (AbstractClientPlayer p : mc.level.players()) {
            boolean drawing = p.isUsingItem() && p.getUseItem().getItem() instanceof BowItem;
            boolean was = DRAWING.getOrDefault(p, false);
            if (drawing && !was) CombatAnimations.play(p, "bow_draw", 3);
            else if (!drawing && was) CombatAnimations.play(p, "bow_release", 1);
            DRAWING.put(p, drawing);
        }
    }
}

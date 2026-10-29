package net.bettercombat.api.client;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.event.Publisher;
import net.minecraft.client.player.LocalPlayer;

/** Copia minima de la API de Better Combat (solo para compilar; no va en el .jar). */
public class BetterCombatClientEvents {
    public static final Publisher<PlayerAttackStart> ATTACK_START = new Publisher<>();

    public interface PlayerAttackStart {
        void onPlayerAttackStart(LocalPlayer player, AttackHand attackHand);
    }
}

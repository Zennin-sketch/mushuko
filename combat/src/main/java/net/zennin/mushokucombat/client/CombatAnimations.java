package net.zennin.mushokucombat.client;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushokucombat.MushokuCombat;

/**
 * Capa de animacion propia del addon (prioridad 2500: por encima de los ataques de Better Combat, que usan 2000),
 * para las esquivas y la pose de conjuro. Los JSON estan en assets/mushoku_combat/player_animations.
 */
public final class CombatAnimations {
    private static final ResourceLocation LAYER = MushokuCombat.id("main");
    public static final int PRIORITY = 2500;

    private CombatAnimations() {}

    static void init() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            stack.addAnimLayer(PRIORITY, layer);
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(LAYER, layer);
        });
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> layer(AbstractClientPlayer player) {
        try {
            IAnimation a = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
            return a instanceof ModifierLayer<?> m ? (ModifierLayer<IAnimation>) m : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Reproduce una animacion del addon; "" corta la actual con una transicion suave. */
    public static void play(AbstractClientPlayer player, String name, int fadeTicks) {
        ModifierLayer<IAnimation> layer = layer(player);
        if (layer == null) return;
        AbstractFadeModifier fade = AbstractFadeModifier.standardFadeIn(Math.max(1, fadeTicks), Ease.INOUTSINE);
        if (name == null || name.isEmpty()) {
            if (layer.getAnimation() != null) layer.replaceAnimationWithFade(fade, null);
            return;
        }
        IPlayable anim = PlayerAnimationRegistry.getAnimation(MushokuCombat.id(name));
        if (anim == null) return;
        layer.replaceAnimationWithFade(fade, anim.playAnimation());
    }
}

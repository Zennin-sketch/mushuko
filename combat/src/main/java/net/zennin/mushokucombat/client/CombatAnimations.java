package net.zennin.mushokucombat.client;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
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
            // Los conjuros apuntan a donde mira el jugador: cabeza y brazos suman el giro de la mirada
            layer.addModifierLast(new AdjustmentModifier(part -> aim(player, part)));
            stack.addAnimLayer(PRIORITY, layer);
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(LAYER, layer);
        });
    }

    /** Animacion que esta sonando en cada jugador (para saber si es un conjuro que tiene que apuntar). */
    private static final Map<AbstractClientPlayer, String> CURRENT = new WeakHashMap<>();

    private static boolean aims(String name) {
        return name != null && (name.startsWith("cast") || name.startsWith("magic_") || name.startsWith("bow_draw"));
    }

    private static Optional<AdjustmentModifier.PartModifier> aim(AbstractClientPlayer player, String part) {
        if (!aims(CURRENT.get(player))) return Optional.empty();
        float pitch = player.getXRot() * Mth.DEG_TO_RAD;
        float yaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot) * Mth.DEG_TO_RAD;
        return switch (part) {
            case "head" -> Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch, yaw, 0), Vec3f.ZERO));
            case "rightArm", "leftArm" -> Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch, yaw, 0), Vec3f.ZERO));
            default -> Optional.empty();
        };
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
        CURRENT.put(player, name);
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

package net.zennin.mushoku.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.data.ModAttachments;

/**
 * Hechizos conocidos. Al empezar no se conoce ninguno:
 * <ul>
 *   <li>Los de nivel Principiante, Intermedio y Avanzado se descubren entrenando: al formar su combinacion por primera
 *       vez (si tu nivel de magia de ese elemento alcanza) los aprendes.</li>
 *   <li>Si formas la combinacion de un hechizo que todavia no podes hacer (o uno Santo, Rey o Imperial, que solo se
 *       aprenden con pergaminos de hechizo), aparece "Hmm... casi dominas un nuevo hechizo...".</li>
 * </ul>
 */
public final class SpellKnowledge {
    /** Nivel mas alto que se puede descubrir entrenando (Avanzado). */
    public static final int TRAIN_MAX_TIER = 3;

    private SpellKnowledge() {}

    public static boolean knows(Player p, Spell s) {
        if (p.level().isClientSide) return net.zennin.mushoku.client.ClientSpellXp.knows(s.id);
        return ModAttachments.knownSpells(p).contains(s.id);
    }

    /** Lo aprende (devuelve false si ya lo conocia), con aviso y sonido. */
    public static boolean learn(ServerPlayer p, Spell s) {
        if (knows(p, s)) return false;
        java.util.List<String> l = new java.util.ArrayList<>(ModAttachments.knownSpells(p));
        l.add(s.id);
        ModAttachments.setKnownSpells(p, l);
        p.displayClientMessage(Component.translatable("msg.remushoku.spell_learned", Component.translatable(s.translationKey())
                .withStyle(color(s.level))).withStyle(ChatFormatting.GOLD), false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.1f);
        SpellMastery.sync(p);
        return true;
    }

    public static void forget(ServerPlayer p, Spell s) {
        java.util.List<String> l = new java.util.ArrayList<>(ModAttachments.knownSpells(p));
        if (l.remove(s.id)) { ModAttachments.setKnownSpells(p, l); SpellMastery.sync(p); }
    }

    /**
     * Antes de poner un hechizo en cola: si no lo conoce intenta descubrirlo entrenando. Devuelve true si lo conoce
     * (o lo acaba de aprender); si no, muestra "casi dominas..." y devuelve false.
     */
    public static boolean tryKnow(ServerPlayer p, Spell s, int elementLevel) {
        if (knows(p, s)) return true;
        boolean creative = p.getAbilities().instabuild;
        if (creative || (s.level.tier <= TRAIN_MAX_TIER && elementLevel >= s.level.tier)) {
            learn(p, s);
            return true;
        }
        p.displayClientMessage(Component.translatable("msg.remushoku.almost_spell").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 0.7f);
        return false;
    }

    /** Color de cada nivel de hechizo (el de su piedra de mana). */
    public static net.minecraft.network.chat.TextColor color(SpellLevel l) {
        return net.minecraft.network.chat.TextColor.fromRgb(rgb(l));
    }

    public static int rgb(SpellLevel l) {
        return switch (l) {
            case ELEMENTARY -> 0xEDDA10;
            case INTERMEDIATE -> 0xED2A2A;
            case ADVANCED -> 0x20E020;
            case SAINT -> 0x4A6AFF;
            case KING -> 0xE030E0;
            case IMPERIAL -> 0xE7E7E7;
        };
    }
}

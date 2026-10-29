package net.zennin.mushoku.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.MagicData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.sword.SwordSkill;

/** Logica del servidor: armar el hechizo con flechas, lanzarlo y cobrar el mana. */
public final class SpellCasting {
    public static final int CLASS_MAGE = 1, CLASS_SWORDSMAN = 2, CLASS_ARCHER = 3;
    /** Todas las clases a la vez (item de creativo para probar). */
    public static final int CLASS_ALL = 4;

    public static boolean isMageClass(int c) { return c == CLASS_MAGE || c == CLASS_ALL; }
    public static boolean isSwordsmanClass(int c) { return c == CLASS_SWORDSMAN || c == CLASS_ALL; }
    public static boolean isArcherClass(int c) { return c == CLASS_ARCHER || c == CLASS_ALL; }

    private SpellCasting() {}

    /** El jugador solto las teclas de conjuro con esta combinacion de flechas. */
    /** Mago, o espadachin con la habilidad Magia del Dios Norte (hasta nivel Intermedio). */
    public static boolean canCast(ServerPlayer p) {
        // En creativo se puede conjurar cualquier hechizo, con cualquier clase
        if (p.getAbilities().instabuild) return true;
        int cls = ModAttachments.get(p).playerClass();
        return isMageClass(cls) || (cls == CLASS_SWORDSMAN && ModAttachments.sword(p).knows(SwordSkill.NG_MAGIC.id));
    }

    /** Flechas maximas por combo (los combinados de corrido llevan dos hechizos). */
    public static final int MAX_COMBO = 16;

    public static void onCombo(ServerPlayer p, String combo) {
        if (!canCast(p)) return;
        if (combo == null || combo.length() < 3 || combo.length() > MAX_COMBO || !combo.matches("[udlr]+")) {
            msg(p, Component.translatable("msg.remushoku.no_spell").withStyle(ChatFormatting.GRAY));
            return;
        }
        MagicData m = ModAttachments.magic(p);
        boolean creative = p.getAbilities().instabuild;
        Spell spell = combo.length() <= 8 ? Spell.fromCombo(combo, m.noElement()) : null;
        if (spell == null) {
            // hechizo combinado escrito de corrido: las flechas del primero y despues las del segundo
            Spell[] split = Spell.splitCombined(combo, m.noElement());
            if (split != null) {
                for (int i = 0; i < 2; i++) {
                    if (!creative && m.level(split[i].element) < split[i].level.tier) {
                        msg(p, Component.translatable("msg.remushoku.not_learned").withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                spell = split[2];
            }
        }
        if (spell == null) {
            msg(p, Component.translatable("msg.remushoku.no_spell").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!creative && ModAttachments.get(p).playerClass() == CLASS_SWORDSMAN && spell.level.tier > 2) {
            msg(p, Component.translatable("msg.remushoku.swordsman_spell_limit").withStyle(ChatFormatting.RED));
            return;
        }
        if (!creative && m.level(spell.element) < spell.level.tier) {
            msg(p, Component.translatable("msg.remushoku.not_learned").withStyle(ChatFormatting.RED));
            return;
        }
        ModAttachments.setMagic(p, m.withQueued(spell.id));
        msg(p, Component.translatable("msg.remushoku.queued", Component.translatable(spell.translationKey())).withStyle(ChatFormatting.AQUA));
        ModNetworking.syncMagic(p);
    }

    /** Tecla de activacion: lanza el hechizo en cola, o alterna el modo sin elemento si no hay ninguno. */
    public static void onActivate(ServerPlayer p) {
        if (!canCast(p)) return;
        MagicData m = ModAttachments.magic(p);
        Spell spell = m.queued().isEmpty() ? null : Spell.byId(m.queued());
        if (spell == null) {
            boolean now = !m.noElement();
            ModAttachments.setMagic(p, m.withNoElement(now));
            msg(p, Component.translatable(now ? "msg.remushoku.no_element_on" : "msg.remushoku.no_element_off").withStyle(ChatFormatting.LIGHT_PURPLE));
            ModNetworking.syncMagic(p);
            return;
        }
        ManaData mana = ModAttachments.get(p);
        boolean creative = p.getAbilities().instabuild;
        if (!SpellMastery.canUse(p, spell)) return;
        double cost = SpellMastery.cost(p, spell);
        if (!creative && mana.mana() < cost) {
            msg(p, Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED));
            return;
        }
        if (!spell.implemented) {
            msg(p, Component.translatable("msg.remushoku.not_implemented", Component.translatable(spell.translationKey())).withStyle(ChatFormatting.GRAY));
            ModAttachments.setMagic(p, m.withQueued(""));
            ModNetworking.syncMagic(p);
            return;
        }
        if (SpellEffects.cast(spell, p)) {
            // Curaciones de grupo del addon: tambien curan a tu grupo a 30 bloques
            if (spell == Spell.HEALING) net.zennin.mushoku.party.Parties.healMembers(p, 4f);
            else if (spell == Spell.X_HEALING) net.zennin.mushoku.party.Parties.healMembers(p, 8f);
            else if (spell == Spell.SHRINE_HEALING) net.zennin.mushoku.party.Parties.healMembers(p, 16f);
            SpellMastery.gain(p, spell);
            if (!creative) {
                ManaData after = mana.withMana(mana.mana() - cost);
                after = after.withMaxMana(after.maxMana() + spell.level.maxManaGain);
                ModAttachments.set(p, after);
            }
        }
        ModAttachments.setMagic(p, m.withQueued(""));
        ModNetworking.syncMana(p);
        ModNetworking.syncMagic(p);
    }

    private static final java.util.Map<java.util.UUID, String> COMBINE_FIRST = new java.util.HashMap<>();

    /**
     * Tecla Combinar (como el addon): con un hechizo en cola, la primera vez lo guarda como primer hechizo;
     * formas el segundo y al pulsar Combinar otra vez queda en cola el hechizo combinado.
     */
    public static void onCombine(ServerPlayer p) {
        if (!canCast(p)) return;
        MagicData m = ModAttachments.magic(p);
        Spell queued = m.queued().isEmpty() ? null : Spell.byId(m.queued());
        String first = COMBINE_FIRST.get(p.getUUID());
        if (queued == null) {
            msg(p, Component.translatable("msg.remushoku.combine_need").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (first == null) {
            COMBINE_FIRST.put(p.getUUID(), queued.id);
            ModAttachments.setMagic(p, m.withQueued(""));
            ModNetworking.syncMagic(p);
            msg(p, Component.translatable("msg.remushoku.combine_first", Component.translatable(queued.translationKey())).withStyle(ChatFormatting.AQUA));
            return;
        }
        COMBINE_FIRST.remove(p.getUUID());
        Spell result = Spell.combine(Spell.byId(first), queued);
        if (result == null) {
            ModAttachments.setMagic(p, m.withQueued(""));
            ModNetworking.syncMagic(p);
            msg(p, Component.translatable("msg.remushoku.combine_fail").withStyle(ChatFormatting.RED));
            return;
        }
        if (!p.getAbilities().instabuild && ModAttachments.get(p).playerClass() == CLASS_SWORDSMAN && result.level.tier > 2) {
            msg(p, Component.translatable("msg.remushoku.swordsman_spell_limit").withStyle(ChatFormatting.RED));
            return;
        }
        ModAttachments.setMagic(p, m.withQueued(result.id));
        ModNetworking.syncMagic(p);
        msg(p, Component.translatable("msg.remushoku.combine_ok", Component.translatable(result.translationKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void msg(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }
}

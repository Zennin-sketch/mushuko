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
    public static final int CLASS_MAGE = 1, CLASS_SWORDSMAN = 2;

    private SpellCasting() {}

    /** El jugador solto las teclas de conjuro con esta combinacion de flechas. */
    /** Mago, o espadachin con la habilidad Magia del Dios Norte (hasta nivel Intermedio). */
    public static boolean canCast(ServerPlayer p) {
        int cls = ModAttachments.get(p).playerClass();
        return cls == CLASS_MAGE || (cls == CLASS_SWORDSMAN && ModAttachments.sword(p).knows(SwordSkill.NG_MAGIC.id));
    }

    public static void onCombo(ServerPlayer p, String combo) {
        if (!canCast(p)) return;
        if (combo == null || combo.length() < 3 || combo.length() > 8 || !combo.matches("[udlr]+")) {
            msg(p, Component.translatable("msg.remushoku.no_spell").withStyle(ChatFormatting.GRAY));
            return;
        }
        MagicData m = ModAttachments.magic(p);
        Spell spell = Spell.fromCombo(combo, m.noElement());
        if (spell == null) {
            msg(p, Component.translatable("msg.remushoku.no_spell").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (ModAttachments.get(p).playerClass() == CLASS_SWORDSMAN && spell.level.tier > 2) {
            msg(p, Component.translatable("msg.remushoku.swordsman_spell_limit").withStyle(ChatFormatting.RED));
            return;
        }
        if (m.level(spell.element) < spell.level.tier) {
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
        if (mana.mana() < spell.level.cost) {
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
            ManaData after = mana.withMana(mana.mana() - spell.level.cost);
            after = after.withMaxMana(after.maxMana() + spell.level.maxManaGain);
            ModAttachments.set(p, after);
        }
        ModAttachments.setMagic(p, m.withQueued(""));
        ModNetworking.syncMana(p);
        ModNetworking.syncMagic(p);
    }

    private static void msg(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }
}

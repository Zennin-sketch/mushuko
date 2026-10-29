package net.zennin.mushoku.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ExtraData;
import net.zennin.mushoku.data.MagicData;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.magic.SpellEffects;
import net.zennin.mushoku.network.ModNetworking;

/**
 * Conjuro silencioso (Tomo de Conjuro Silencioso): el mago asigna hechizos a Z / X / C y los lanza sin armar las flechas.
 * Con un hechizo en cola, Z/X/C lo asigna a esa ranura; sin hechizo en cola, lo lanza (cuesta 15 de mana extra, como el original).
 */
public final class SilentCasting {
    public static final double EXTRA_COST = 15;

    private SilentCasting() {}

    public static void useSlot(ServerPlayer p, int slot) {
        ExtraData d = ModAttachments.extra(p);
        if (!d.silent()) return;
        MagicData m = ModAttachments.magic(p);
        if (!m.queued().isEmpty()) {
            Spell s = Spell.byId(m.queued());
            if (s == null) return;
            ModAttachments.setExtra(p, d.withSilentSlot(slot, s.id));
            ModAttachments.setMagic(p, m.withQueued(""));
            ModNetworking.syncMagic(p);
            DemonEyes.sync(p);
            p.displayClientMessage(Component.translatable("msg.remushoku.silent_bound", Component.translatable(s.translationKey()), slot + 1)
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        Spell s = Spell.byId(d.silentSlot(slot));
        if (s == null) {
            p.displayClientMessage(Component.translatable("msg.remushoku.silent_empty", slot + 1).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ManaData mana = ModAttachments.get(p);
        if (!net.zennin.mushoku.magic.SpellMastery.canUse(p, s)) return;
        double cost = net.zennin.mushoku.magic.SpellMastery.cost(p, s) + EXTRA_COST;
        if (mana.mana() < cost && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (p.getCooldowns().isOnCooldown(net.zennin.mushoku.registry.ModItems.SILENT_TOME)) return;
        if (SpellEffects.cast(s, p)) {
            net.zennin.mushoku.magic.SpellMastery.gain(p, s);
            ManaData after = p.getAbilities().instabuild ? mana : mana.withMana(mana.mana() - cost);
            ModAttachments.set(p, after.withMaxMana(after.maxMana() + s.level.maxManaGain));
            p.getCooldowns().addCooldown(net.zennin.mushoku.registry.ModItems.SILENT_TOME, 10);
            ModNetworking.syncMana(p);
        }
    }
}

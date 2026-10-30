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
 * Con un hechizo en cola, Z/X/C lo asigna a esa ranura; sin hechizo en cola, lo empieza a formar en la mano (con la
 * misma animacion que al conjurarlo normal) y, una vez formado, otra pulsacion (o la tecla de activar) lo lanza. Cuesta
 * 15 de mana extra, como el original.
 */
public final class SilentCasting {
    public static final double EXTRA_COST = 15;

    /** Ticks que tarda en formarse en la mano (igual que la formacion del conjuro normal). */
    public static final int FORM_TICKS = 22;
    /** Hechizo que se esta formando por conjuro silencioso: jugador -> {ranura, tick de inicio}. */
    private static final java.util.Map<java.util.UUID, long[]> FORMING = new java.util.concurrent.ConcurrentHashMap<>();

    private SilentCasting() {}

    /** El hechizo en cola vino de una ranura silenciosa y todavia se esta formando: no se puede lanzar aun. */
    public static boolean stillForming(ServerPlayer p) {
        long[] f = FORMING.get(p.getUUID());
        return f != null && p.level().getGameTime() - f[1] < FORM_TICKS;
    }

    /** La tecla de activar lanzo el hechizo en cola: si era silencioso, se olvida. */
    public static boolean consumeSilent(ServerPlayer p) { return FORMING.remove(p.getUUID()) != null; }

    public static void useSlot(ServerPlayer p, int slot) {
        ExtraData d = ModAttachments.extra(p);
        if (!d.silent()) return;
        MagicData m = ModAttachments.magic(p);
        long[] forming = FORMING.get(p.getUUID());
        if (forming != null && !m.queued().isEmpty() && m.queued().equals(d.silentSlot((int) forming[0]))) {
            // segunda pulsacion: si ya esta formado en la mano, se lanza
            if (p.level().getGameTime() - forming[1] < FORM_TICKS) return;
            Spell s = Spell.byId(m.queued());
            FORMING.remove(p.getUUID());
            ModAttachments.setMagic(p, m.withQueued(""));
            ModNetworking.syncMagic(p);
            if (s != null) castSilent(p, s);
            return;
        }
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
        // empieza a formarse en la mano, como al conjurarlo con flechas
        FORMING.put(p.getUUID(), new long[]{slot, p.level().getGameTime()});
        ModAttachments.setMagic(p, m.withQueued(s.id));
        ModNetworking.syncMagic(p);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    /** Lanza el hechizo ya formado (con el costo extra del conjuro silencioso). */
    public static void castSilent(ServerPlayer p, Spell s) {
        ManaData mana = ModAttachments.get(p);
        double cost = net.zennin.mushoku.magic.SpellMastery.cost(p, s) + EXTRA_COST;
        if (mana.mana() < cost && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (SpellEffects.cast(s, p)) {
            net.zennin.mushoku.magic.SpellMastery.gain(p, s);
            ManaData after = p.getAbilities().instabuild ? mana : mana.withMana(mana.mana() - cost);
            ModAttachments.set(p, after.withMaxMana(after.maxMana() + s.level.maxManaGain));
            p.getCooldowns().addCooldown(net.zennin.mushoku.registry.ModItems.SILENT_TOME, 10);
            ModNetworking.syncMana(p);
        }
    }
}

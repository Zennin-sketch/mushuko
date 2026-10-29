package net.zennin.mushoku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.zennin.mushoku.magic.SpellCasting;

/**
 * Para los que tienen varias clases: que quiere hacer el jugador segun lo que tiene en la mano.
 * Arco en la mano = arquero; espada = espadachin; nada de eso = mago. Con una sola clase siempre es esa.
 */
public final class ClassContext {
    public enum Mode { MAGE, SWORDSMAN, ARCHER, NONE }

    private ClassContext() {}

    public static boolean mage() {
        int c = ClientManaState.playerClass;
        return SpellCasting.isMageClass(c) || (c == SpellCasting.CLASS_SWORDSMAN && ClientSwordState.data.knows("ng_magic"));
    }

    public static boolean holdingSword(Player p) {
        ItemStack s = p.getMainHandItem();
        return s.getItem() instanceof SwordItem || s.is(ItemTags.SWORDS);
    }

    public static Mode mode() {
        Player p = Minecraft.getInstance().player;
        int c = ClientManaState.playerClass;
        boolean sword = SpellCasting.isSwordsmanClass(c), archer = SpellCasting.isArcherClass(c), mage = mage();
        if (p == null) return Mode.NONE;
        if (archer && BowClient.holdingBow(p)) return Mode.ARCHER;
        if (sword && holdingSword(p)) return Mode.SWORDSMAN;
        if (SpellCasting.isMageClass(c)) return Mode.MAGE;
        if (sword) return Mode.SWORDSMAN;
        if (archer) return Mode.ARCHER;
        return mage ? Mode.MAGE : Mode.NONE;
    }

    /** Tiene mas de una clase (multiclase, o espadachin con Magia del Dios Norte). */
    public static boolean multi() {
        int c = ClientManaState.playerClass;
        return c == SpellCasting.CLASS_ALL || (c == SpellCasting.CLASS_SWORDSMAN && ClientSwordState.data.knows("ng_magic"));
    }
}

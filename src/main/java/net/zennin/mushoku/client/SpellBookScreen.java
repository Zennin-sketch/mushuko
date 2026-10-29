package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.magic.Element;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.magic.SpellMastery;

import java.util.Arrays;
import java.util.List;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Libro de Hechizos: todos los hechizos de cada elemento con su combinacion de flechas. */
public class SpellBookScreen extends BookScreen {
    private static Element tab = Element.WATER;
    private static final ResourceLocation UP = MushokuMod.id("textures/screens/uparrowful.png"), DOWN = MushokuMod.id("textures/screens/downarrowful.png"),
            LEFT = MushokuMod.id("textures/screens/leftarrowful.png"), RIGHT = MushokuMod.id("textures/screens/rightarrowful.png");

    public SpellBookScreen() { super(Component.translatable("item.remushoku.spell_book")); }

    private List<Spell> spells() { return Arrays.stream(Spell.values()).filter(s -> s.element == tab).toList(); }

    @Override
    protected void init() {
        layout();
        Element[] els = Element.values();
        int bw = 54, x0 = width / 2 - (els.length * (bw + 2)) / 2;
        for (int i = 0; i < els.length; i++) {
            Element e = els[i];
            addRenderableWidget(new MButton(x0 + i * (bw + 2), top + 18, bw, 13, Component.translatable("element.remushoku." + e.key),
                    b -> { tab = e; rebuildWidgets(); }, e == tab));
        }
    }

    private int learned(Element e) {
        return switch (e) {
            case WATER -> ClientManaState.water;
            case FIRE -> ClientManaState.fire;
            case EARTH -> ClientManaState.earth;
            case WIND -> ClientManaState.wind;
            case NONE -> Math.max(Math.max(ClientManaState.water, ClientManaState.fire), Math.max(ClientManaState.earth, ClientManaState.wind));
        };
    }

    private Spell hovered;

    private static int mastery(Spell s) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player == null ? 1 : SpellMastery.level(mc.player, s);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        if (hovered != null) g.renderComponentTooltip(font, details(hovered), mx, my);
    }

    /** Ficha del hechizo: nivel y progreso, mana, dano y requisitos de mana maximo. */
    private List<Component> details(Spell s) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        java.util.List<Component> out = new java.util.ArrayList<>();
        out.add(Component.translatable(s.translationKey()).withStyle(net.minecraft.ChatFormatting.GOLD));
        int uses = ClientSpellXp.uses(s.id), lvl = mastery(s);
        boolean nether = mc.level != null && mc.level.dimension() == net.minecraft.world.level.Level.NETHER;
        out.add(Component.translatable("book.remushoku.mastery", lvl, SpellMastery.MAX).withStyle(net.minecraft.ChatFormatting.AQUA));
        if (lvl < SpellMastery.MAX) {
            int need = SpellMastery.usesFor(s, lvl + 1);
            out.add(Component.translatable("book.remushoku.uses", uses, need).withStyle(net.minecraft.ChatFormatting.GRAY));
            int gate = SpellMastery.gate(s, lvl + 1);
            if (gate > 0) out.add(Component.translatable("book.remushoku.gate", gate)
                    .withStyle(ClientManaState.maxMana >= gate ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        }
        double cost = SpellMastery.cost(s, lvl, nether);
        out.add(Component.translatable("book.remushoku.cost", fmt(cost), fmt(s.level.cost)).withStyle(net.minecraft.ChatFormatting.BLUE));
        if (s.element == Element.WATER)
            out.add(Component.translatable("book.remushoku.nether_water").withStyle(nether ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.DARK_GRAY));
        float dmg = SpellMastery.baseDamage(s);
        if (dmg > 0) {
            float m = SpellMastery.damageMult(lvl);
            out.add(Component.translatable("book.remushoku.damage", fmt(dmg * m), Math.round((m - 1) * 100)).withStyle(net.minecraft.ChatFormatting.RED));
        }
        int next5 = Math.min(SpellMastery.MAX, (lvl / 5 + 1) * 5);
        if (dmg > 0 && lvl < SpellMastery.MAX)
            out.add(Component.translatable("book.remushoku.next_power", next5).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        int req = SpellMastery.useRequirement(s);
        if (req > 0) out.add(Component.translatable("book.remushoku.use_req", req)
                .withStyle(ClientManaState.maxMana >= req ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        return out;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        hovered = null;
        List<Spell> list = spells();
        int perPage = 7;
        for (int i = 0; i < list.size() && i < perPage * 2; i++) {
            Spell s = list.get(i);
            int x = i < perPage ? leftPageX() + 2 : rightPageX();
            int y = top + 38 + (i % perPage) * 21;
            boolean known = learned(s.element) >= s.level.tier;
            // dominio del hechizo: nivel a la derecha del nombre (dorado en 5, 10, 15 y 20)
            int mlv = mastery(s);
            String lv = Component.translatable("book.remushoku.lv", mlv).getString();
            String name = font.plainSubstrByWidth(Component.translatable(s.translationKey()).getString(), PAGE_W - 8 - font.width(lv));
            g.drawString(font, name, x, y, known ? INK : 0xFF8A7A62, false);
            g.drawString(font, lv, x + PAGE_W - 4 - font.width(lv), y, mlv % 5 == 0 ? 0xFFB8860B : INK_SOFT, false);
            if (mx >= x && mx < x + PAGE_W - 4 && my >= y - 1 && my < y + 19) hovered = s;
            String lvl = Component.translatable("level.remushoku." + s.level.tier).getString();
            // flechas (en los combinados: las del primero, un "+" y las del segundo), achicadas si no entran
            String arrows = s.arrows();
            Spell[] parts = s.combined() ? s.parts() : null;
            int room = PAGE_W - 8 - font.width(lvl);
            if (arrows == null && parts != null) { // combinado con un hechizo sin elemento: se escribe con la tecla Combinar
                String txt = font.plainSubstrByWidth(Component.translatable(parts[0].translationKey()).getString() + " + "
                        + Component.translatable(parts[1].translationKey()).getString(), room);
                g.drawString(font, txt, x, y + 10, INK_SOFT, false);
            } else if (arrows != null) {
                int n = arrows.length() + (parts != null ? 1 : 0);
                int step = Math.max(5, Math.min(10, room / Math.max(1, n))), size = Math.min(9, step - 1);
                int ax = x;
                for (int k = 0; k < arrows.length(); k++) {
                    if (parts != null && k == parts[0].combo.length()) {
                        g.drawString(font, "+", ax + (step - font.width("+")) / 2, y + 9 + (size - 8) / 2, INK, false);
                        ax += step;
                    }
                    char c = arrows.charAt(k);
                    ResourceLocation tex = switch (c) { case 'u' -> UP; case 'd' -> DOWN; case 'l' -> LEFT; default -> RIGHT; };
                    g.blit(tex, ax, y + 9, size, size, 0f, 0f, 16, 16, 16, 16);
                    ax += step;
                }
            }
            g.drawString(font, lvl, x + PAGE_W - 4 - font.width(lvl), y + 10, known ? INK_GREEN : INK_SOFT, false);
        }
    }
}

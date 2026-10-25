package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.magic.Element;
import net.zennin.mushoku.magic.Spell;

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

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        List<Spell> list = spells();
        int perPage = 7;
        for (int i = 0; i < list.size() && i < perPage * 2; i++) {
            Spell s = list.get(i);
            int x = i < perPage ? leftPageX() + 2 : rightPageX();
            int y = top + 38 + (i % perPage) * 21;
            boolean known = learned(s.element) >= s.level.tier;
            String name = font.plainSubstrByWidth(Component.translatable(s.translationKey()).getString(), PAGE_W - 4);
            g.drawString(font, name, x, y, known ? INK : 0xFF8A7A62, false);
            String lvl = Component.translatable("level.remushoku." + s.level.tier).getString();
            int ax = x;
            for (char c : s.combo.toCharArray()) {
                ResourceLocation tex = switch (c) { case 'u' -> UP; case 'd' -> DOWN; case 'l' -> LEFT; default -> RIGHT; };
                g.blit(tex, ax, y + 9, 9, 9, 0f, 0f, 16, 16, 16, 16);
                ax += 10;
            }
            g.drawString(font, lvl, x + PAGE_W - 4 - font.width(lvl), y + 10, known ? INK_GREEN : INK_SOFT, false);
        }
    }
}

package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Base de los libros (guia y hechizos): dos paginas de pergamino con lomo de cuero y titulo en cinta. */
public abstract class BookScreen extends Screen {
    protected static final int W = 300, H = 196, PAGE_W = 128;
    protected int left, top;

    protected BookScreen(Component title) { super(title); }

    protected void layout() {
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
    }

    protected int leftPageX() { return left + 12; }

    protected int rightPageX() { return left + W / 2 + 10; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        // Tapas de cuero
        g.fill(left - 4, top + 6, left + W + 4, top + H + 4, 0xFF3A1E0E);
        g.fill(left - 3, top + 7, left + W + 3, top + H + 3, 0xFF6B3A1E);
        g.blitSprite(PARCHMENT, left, top + 10, W / 2, H - 10);
        g.blitSprite(PARCHMENT, left + W / 2, top + 10, W / 2, H - 10);
        // Lomo
        g.fill(left + W / 2 - 2, top + 10, left + W / 2 + 2, top + H, 0xFF8A6A3E);
        g.fill(left + W / 2 - 1, top + 10, left + W / 2 + 1, top + H, 0xFF5A3A1E);
        title(g, title, width / 2, top);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

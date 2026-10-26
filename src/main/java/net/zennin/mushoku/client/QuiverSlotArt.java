package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.zennin.mushoku.archer.QuiverContainer;

/** Marco de la ranura del carcaj, igual a las ranuras vanilla, con la silueta de un carcaj cuando esta vacia. */
public final class QuiverSlotArt {
    private QuiverSlotArt() {}

    public static void draw(GuiGraphics g, int x, int y, AbstractContainerMenu menu) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        boolean empty = true;
        if (menu != null) for (Slot s : menu.slots) if (s.container instanceof QuiverContainer && s.hasItem()) empty = false;
        if (!empty) return;
        // silueta: tubo inclinado con tres plumas asomando
        int c = 0xFF6F6F6F;
        for (int i = 0; i < 9; i++) g.fill(x + 5 + i / 2, y + 6 + i, x + 9 + i / 2, y + 7 + i, c);
        g.fill(x + 4, y + 3, x + 6, y + 6, c);
        g.fill(x + 7, y + 2, x + 9, y + 6, c);
        g.fill(x + 10, y + 3, x + 12, y + 6, c);
    }
}

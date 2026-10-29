package net.zennin.mushokucombat.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.zennin.mushokucombat.Stamina;

/**
 * Barra de estamina amarilla/naranja justo arriba de la de experiencia, del mismo ancho (182 px).
 * Las dos marcas la dividen en 3: cada tramo es una esquiva. Los corazones y la comida suben 5 px para hacerle lugar.
 */
public final class StaminaHud {
    public static final int SHIFT = 5;
    private static float shown = Stamina.MAX;

    private StaminaHud() {}

    /** Se muestra cuando se ven los corazones (supervivencia y aventura). */
    public static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer() && !mc.options.hideGui;
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (GoldenGlow.selfFlash > 0 && !mc.options.hideGui) flash(g, GoldenGlow.selfFlash / (float) GoldenGlow.FLASH);
        if (!visible()) return;
        shown += (ClientStamina.value - shown) * 0.3f;
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - 29 - SHIFT;
        boolean empty = ClientStamina.emptyFlash > 0 && (ClientStamina.emptyFlash / 3) % 2 == 0;
        g.fill(x, y, x + 182, y + 5, empty ? 0xFF7A1A10 : 0xFF1B140C);
        g.fill(x + 1, y + 1, x + 181, y + 4, 0xFF3A2C18);
        int w = Math.round(180 * Math.max(0f, Math.min(1f, shown / Stamina.MAX)));
        boolean low = ClientStamina.value < Stamina.DODGE_COST;
        for (int i = 0; i < w; i += 2) {
            float f = i / 180f;
            int r = 255, gg = (int) (214 - 84 * f), b = (int) (74 - 40 * f);
            if (low) { gg = (int) (gg * 0.75f); b = (int) (b * 0.6f); }
            int c = 0xFF000000 | (r << 16) | (gg << 8) | b;
            g.fill(x + 1 + i, y + 1, x + 1 + Math.min(w, i + 2), y + 4, c);
        }
        if (w > 0) g.fill(x + 1, y + 1, x + 1 + w, y + 2, 0x55FFFFFF); // brillo arriba
        for (int k = 1; k < 3; k++) {
            int nx = x + 1 + 60 * k;
            g.fill(nx, y + 1, nx + 1, y + 4, 0xCC1B140C);
        }
    }

    /** Destello dorado en los bordes de la pantalla cuando esquivas perfecto. */
    private static void flash(GuiGraphics g, float s) {
        int a = (int) (110 * s);
        int gold = (a << 24) | 0xFFC846;
        int clear = 0x00FFC846;
        int h = g.guiHeight(), w = g.guiWidth(), band = Math.max(24, h / 6);
        g.fillGradient(0, 0, w, band, gold, clear);
        g.fillGradient(0, h - band, w, h, clear, gold);
        g.fill(0, 0, w, h, ((int) (28 * s) << 24) | 0xFFD878);
    }
}

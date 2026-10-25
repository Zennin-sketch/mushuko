package net.zennin.mushoku.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;

/** Barra de mana medieval arriba a la izquierda, con el numero adentro. */
public final class ManaBarHud {
    private static final ResourceLocation FRAME = MushokuMod.id("textures/screens/mana_bar_frame.png");
    private static final ResourceLocation BACK = MushokuMod.id("textures/screens/mana_bar_back.png");
    private static final ResourceLocation FILL = MushokuMod.id("textures/screens/mana_bar_fill.png");
    private static final int W = 124, H = 20, FX = 21, FY = 5, FW = 98, FH = 10, X = 4, Y = 4;

    private ManaBarHud() {}

    public static void render(GuiGraphics g, DeltaTracker tick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        double mana = ClientManaState.mana, max = ClientManaState.maxMana;
        double frac = max > 0 ? Math.max(0, Math.min(1, mana / max)) : 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(BACK, X + FX, Y + FY, 0, 0, FW, FH, FW, FH);
        int fw = (int) Math.round(FW * frac);
        if (fw <= 0 && mana > 0) fw = 1;
        if (fw > 0) g.blit(FILL, X + FX, Y + FY, 0, 0, fw, FH, FW, FH);
        g.blit(FRAME, X, Y, 0, 0, W, H, W, H);

        String text = (long) Math.floor(mana) + " / " + (long) Math.floor(max);
        int tw = mc.font.width(text);
        g.drawString(mc.font, text, X + FX + (FW - tw) / 2, Y + FY + 1, 0xFFFFFFFF, true);
    }
}

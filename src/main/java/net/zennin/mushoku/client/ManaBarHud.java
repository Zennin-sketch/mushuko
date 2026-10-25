package net.zennin.mushoku.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.zennin.mushoku.data.SwordData;
import net.zennin.mushoku.sword.SwordSkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.magic.Spell;

/** Barra de mana medieval arriba a la izquierda, hechizo en cola, y las flechas mientras conjuras. */
public final class ManaBarHud {
    private static final ResourceLocation FRAME = MushokuMod.id("textures/screens/mana_bar_frame.png");
    private static final ResourceLocation BACK = MushokuMod.id("textures/screens/mana_bar_back.png");
    private static final ResourceLocation FILL = MushokuMod.id("textures/screens/mana_bar_fill.png");
    private static final ResourceLocation UP = MushokuMod.id("textures/screens/uparrowful.png");
    private static final ResourceLocation DOWN = MushokuMod.id("textures/screens/downarrowful.png");
    private static final ResourceLocation LEFT = MushokuMod.id("textures/screens/leftarrowful.png");
    private static final ResourceLocation RIGHT = MushokuMod.id("textures/screens/rightarrowful.png");
    private static final int W = 124, H = 20, FX = 21, FY = 5, FW = 98, FH = 10, X = 4, Y = 4;

    private ManaBarHud() {}

    public static void render(GuiGraphics g, DeltaTracker tick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        double mana = ClientManaState.mana, max = ClientManaState.maxMana;
        double frac = max > 0 ? Math.max(0, Math.min(1, mana / max)) : 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(BACK, X + FX, Y + FY, 0f, 0f, FW, FH, FW, FH);
        int fw = (int) Math.round(FW * frac);
        if (fw <= 0 && mana > 0) fw = 1;
        if (fw > 0) g.blit(FILL, X + FX, Y + FY, 0f, 0f, fw, FH, FW, FH);
        g.blit(FRAME, X, Y, 0f, 0f, W, H, W, H);

        String text = (long) Math.floor(mana) + " / " + (long) Math.floor(max);
        int tw = mc.font.width(text);
        g.drawString(mc.font, text, X + FX + (FW - tw) / 2, Y + FY + 1, 0xFFFFFFFF, true);

        int line = Y + H + 4;
        if (ClientManaState.playerClass == 2) line = renderSwordsman(g, mc, line);
        line = renderExtra(g, mc, line);
        if (ClientManaState.playerClass != 0 && ClientRaceState.data.race().isEmpty()) {
            g.drawString(mc.font, Component.translatable("hud.remushoku.pick_race", ModKeys.ADVENTURER.getTranslatedKeyMessage()), X + 2, line, 0xFFFFD27F, true);
            line += 10;
        }
        if (!CastingClient.casting && ClientManaState.playerClass != 1 && ClientManaState.queued.isEmpty()) return;
        if (ClientManaState.noElement) {
            g.drawString(mc.font, Component.translatable("hud.remushoku.no_element"), X + 2, line, 0xFFD58CFF, true);
            line += 11;
        }
        Spell queued = ClientManaState.queued.isEmpty() ? null : Spell.byId(ClientManaState.queued);
        if (queued != null) {
            g.drawString(mc.font, Component.translatable("hud.remushoku.queued", Component.translatable(queued.translationKey())), X + 2, line, 0xFF8FD8FF, true);
        }

        if (CastingClient.casting) {
            String combo = CastingClient.combo;
            int size = 16, gap = 2;
            int total = Math.max(1, combo.length()) * (size + gap) - gap;
            int sx = (g.guiWidth() - total) / 2, sy = g.guiHeight() / 2 - 40;
            g.drawCenteredString(mc.font, Component.translatable("hud.remushoku.casting"), g.guiWidth() / 2, sy - 12, 0xFFFFFFFF);
            for (int i = 0; i < combo.length(); i++) {
                ResourceLocation tex = switch (combo.charAt(i)) {
                    case 'u' -> UP;
                    case 'd' -> DOWN;
                    case 'l' -> LEFT;
                    default -> RIGHT;
                };
                g.blit(tex, sx + i * (size + gap), sy, 0f, 0f, size, size, size, size);
            }
        }
    }

    private static int renderSwordsman(GuiGraphics g, Minecraft mc, int line) {
        SwordData d = ClientSwordState.data;
        if (d.style().isEmpty()) {
            g.drawString(mc.font, Component.translatable("hud.remushoku.pick_affinity", ModKeys.MENU.getTranslatedKeyMessage()), X + 2, line, 0xFFFFD27F, true);
            return line + 11;
        }
        KeyMapping[] keys = {ModKeys.SKILL_1, ModKeys.SKILL_2, ModKeys.SKILL_3};
        for (int i = 0; i < 3; i++) {
            SwordSkill s = SwordSkill.byId(d.slot(i));
            Component name = s == null ? Component.literal("-") : Component.translatable(s.translationKey());
            g.drawString(mc.font, Component.translatable("hud.remushoku.slot", keys[i].getTranslatedKeyMessage(), name), X + 2, line, 0xFFE0E0E0, true);
            line += 10;
        }
        if (d.touki() > 0) {
            g.drawString(mc.font, Component.translatable(d.toukiOn() ? "hud.remushoku.touki_on" : "hud.remushoku.touki_off", d.touki()),
                    X + 2, line, d.toukiOn() ? 0xFFFFB347 : 0xFF999999, true);
            line += 10;
        }
        return line + 2;
    }

    /** Ojos demoniacos, conjuro silencioso e Identificacion (nombre de lo que miras bajo la mira). */
    private static int renderExtra(GuiGraphics g, Minecraft mc, int line) {
        var d = ClientExtraState.data;
        KeyMapping[] eyeKeys = {ModKeys.EYE_1, ModKeys.EYE_2};
        for (int i = 0; i < 2; i++) {
            if (d.eye(i).isEmpty()) continue;
            boolean on = d.on(i);
            g.drawString(mc.font, Component.translatable("hud.remushoku.eye", eyeKeys[i].getTranslatedKeyMessage(),
                    Component.translatable("eye.remushoku." + d.eye(i))), X + 2, line, on ? 0xFFD58CFF : 0xFF8A7A9A, true);
            line += 10;
        }
        if (ClientManaState.playerClass == 1 && d.silent()) {
            KeyMapping[] keys = {ModKeys.SKILL_1, ModKeys.SKILL_2, ModKeys.SKILL_3};
            for (int i = 0; i < 3; i++) {
                Spell s = d.silentSlot(i).isEmpty() ? null : Spell.byId(d.silentSlot(i));
                Component name = s == null ? Component.literal("-") : Component.translatable(s.translationKey());
                g.drawString(mc.font, Component.translatable("hud.remushoku.slot", keys[i].getTranslatedKeyMessage(), name), X + 2, line, 0xFF8FD8FF, true);
                line += 10;
            }
        }
        if (d.active("identification") && mc.hitResult != null && mc.level != null) {
            Component what = null;
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult eh) {
                var e = eh.getEntity();
                what = e instanceof net.minecraft.world.entity.LivingEntity le
                        ? Component.translatable("hud.remushoku.identify_entity", e.getDisplayName(), (int) Math.ceil(le.getHealth()), (int) le.getMaxHealth())
                        : e.getDisplayName();
            } else if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh && mc.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                what = mc.level.getBlockState(bh.getBlockPos()).getBlock().getName();
            }
            if (what != null) g.drawCenteredString(mc.font, what, g.guiWidth() / 2, g.guiHeight() / 2 + 12, 0xFFD58CFF);
        }
        return line;
    }
}

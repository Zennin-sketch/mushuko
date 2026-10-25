package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.guild.Coins;
import net.zennin.mushoku.guild.GuildData;
import net.zennin.mushoku.guild.GuildSystem;
import net.zennin.mushoku.guild.Jobs;
import net.zennin.mushoku.network.GuildActionPayload;

import java.util.List;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Tablon del Gremio de Aventureros: rango, trabajo activo y los trabajos de cada rango como pergaminos clavados. */
public class GuildScreen extends Screen {
    private static int tab = -1;
    private static final int W = 320, H = 232, CARD_W = 92, CARD_H = 100;
    private int seen = -1, left, top;

    public GuildScreen() { super(Component.translatable("gui.remushoku.guild.title")); }

    private List<Jobs.Job> tabJobs() { return Jobs.ALL.stream().filter(j -> j.rank() == tab).toList(); }

    private int cardX(int i) { return left + (W - 3 * CARD_W - 2 * 8) / 2 + i * (CARD_W + 8); }

    @Override
    protected void init() {
        GuildData g = ClientGuildState.data;
        seen = ClientGuildState.version;
        if (tab < 1) tab = Math.min(6, g.rank());
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);

        int need = GuildData.xpToRankUp(g.rank());
        MButton up = new MButton(left + W - 100, top + 27, 86, 16, Component.translatable("gui.remushoku.guild.rank_up"), b -> send(GuildActionPayload.RANK_UP, 0));
        up.active = need > 0 && g.xp() >= need;
        addRenderableWidget(up);

        Jobs.Job active = Jobs.byId(g.job());
        if (active != null) {
            int by = top + 74, bx = left + W - 14 - 3 * 56 - 4;
            if (active.type() == Jobs.Type.COLLECT) {
                MButton s = new MButton(bx, by, 56, 14, Component.translatable("gui.remushoku.guild.submit"), b -> send(GuildActionPayload.SUBMIT, 0));
                s.active = g.progress() < active.amount();
                addRenderableWidget(s);
            }
            MButton c = new MButton(bx + 58, by, 56, 14, Component.translatable("gui.remushoku.guild.complete"), b -> send(GuildActionPayload.COMPLETE, 0));
            c.active = g.progress() >= active.amount();
            addRenderableWidget(c);
            addRenderableWidget(new MButton(bx + 116, by, 56, 14, Component.translatable("gui.remushoku.guild.abandon"), b -> send(GuildActionPayload.ABANDON, 0)));
        }

        // Pestañas de rango
        int tx = left + (W - 6 * 34) / 2;
        for (int r = 1; r <= 6; r++) {
            int rank = r;
            addRenderableWidget(new MButton(tx + (r - 1) * 34, top + 102, 32, 14, Component.literal(GuildData.rankLetter(r)),
                    b -> { tab = rank; rebuildWidgets(); }, r == tab));
        }
        // Pergaminos de trabajos
        List<Jobs.Job> jobs = tabJobs();
        for (int i = 0; i < jobs.size(); i++) {
            Jobs.Job j = jobs.get(i);
            MButton a = new MButton(cardX(i) + 14, top + 120 + CARD_H - 24, CARD_W - 28, 14,
                    Component.translatable("gui.remushoku.guild.accept"), b -> send(GuildActionPayload.ACCEPT, j.id()));
            a.active = g.job() == 0 && GuildSystem.canTake(g, j);
            if (!GuildSystem.canTake(g, j)) a.setTooltip(Tooltip.create(Component.translatable("msg.remushoku.job_rank_low")));
            addRenderableWidget(a);
        }
    }

    private void send(int action, int arg) { ClientPlayNetworking.send(new GuildActionPayload(action, arg, "")); }

    @Override
    public void tick() {
        super.tick();
        if (seen != ClientGuildState.version) rebuildWidgets();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        GuildData d = ClientGuildState.data;
        g.blitSprite(WOOD, left, top + 8, W, H - 8);
        title(g, title, width / 2, top);

        // Rango y XP
        g.blitSprite(PARCHMENT, left + 10, top + 22, W - 20, 26);
        ink(g, Component.translatable("gui.remushoku.guild.rank", GuildData.rankLetter(d.rank())), left + 18, top + 27, INK);
        int need = GuildData.xpToRankUp(d.rank());
        int barX = left + 18, barY = top + 38, barW = 150;
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF7A5A34);
        g.fill(barX + 1, barY + 1, barX + barW - 1, barY + 4, 0xFFD9C08A);
        if (need > 0) {
            int fw = (int) ((barW - 2) * Math.min(1.0, d.xp() / (double) need));
            g.fill(barX + 1, barY + 1, barX + 1 + fw, barY + 4, 0xFF3E8E2A);
        }
        String xp = need > 0 ? d.xp() + " / " + need : Component.translatable("gui.remushoku.guild.max_rank").getString();
        g.drawString(font, xp, barX + barW + 6, barY - 1, INK_SOFT, false);

        // Trabajo activo
        g.blitSprite(PARCHMENT, left + 10, top + 52, W - 20, 42);
        ink(g, Component.translatable("gui.remushoku.gui_job_board.label_active_job").withStyle(ChatFormatting.UNDERLINE), left + 18, top + 57, INK);
        Jobs.Job active = Jobs.byId(d.job());
        if (active == null) {
            ink(g, Component.translatable("gui.remushoku.guild.no_job"), left + 18, top + 70, INK_SOFT);
        } else {
            int textW = W - 20 - 3 * 56 - 22;
            String name = font.plainSubstrByWidth(Component.translatable("job.remushoku." + active.key()).getString(), textW);
            g.drawString(font, name, left + 18, top + 68, INK, false);
            boolean done = d.progress() >= active.amount();
            g.drawString(font, Component.translatable("gui.remushoku.guild.progress", Math.min(d.progress(), active.amount()), active.amount()),
                    left + 18, top + 79, done ? INK_GREEN : INK_SOFT, false);
        }

        // Pergaminos
        List<Jobs.Job> jobs = tabJobs();
        for (int i = 0; i < jobs.size(); i++) {
            Jobs.Job j = jobs.get(i);
            int x = cardX(i), y = top + 120;
            g.blitSprite(SCROLL, x, y, CARD_W, CARD_H);
            g.blitSprite(PIN, x + CARD_W / 2 - 2, y + 2, 5, 5);
            int ty = y + 12;
            for (FormattedCharSequence line : font.split(Component.translatable("job.remushoku." + j.key()), CARD_W - 14)) {
                if (ty > y + 32) break;
                g.drawString(font, line, x + CARD_W / 2 - font.width(line) / 2, ty, INK, false);
                ty += 9;
            }
            ty = Math.max(ty + 2, y + 42);
            for (FormattedCharSequence line : font.split(Component.translatable("job.remushoku." + j.key() + ".desc", j.amount()), CARD_W - 14)) {
                if (ty > y + 58) break;
                g.drawString(font, line, x + 7, ty, INK_SOFT, false);
                ty += 9;
            }
            int ry = y + CARD_H - 34;
            g.drawString(font, "+" + j.xp() + " XP", x + 7, ry, INK_GREEN, false);
            price(g, j.reward(), x + CARD_W - 9 - priceWidth(j.reward()), ry, INK);
        }
    }

    private int priceWidth(int amount) { return font.width(String.valueOf(Coins.count(amount))) + 11; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        List<Jobs.Job> jobs = tabJobs();
        for (int i = 0; i < jobs.size(); i++) {
            int x = cardX(i), y = top + 120;
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H - 26) {
                Jobs.Job j = jobs.get(i);
                Component tip = Component.translatable("job.remushoku." + j.key()).withStyle(ChatFormatting.GOLD).append("\n")
                        .append(Component.translatable("job.remushoku." + j.key() + ".desc", j.amount()).withStyle(ChatFormatting.WHITE)).append("\n")
                        .append(Component.translatable("gui.remushoku.guild.reward", j.xp(),
                                Component.translatable("coin.remushoku." + Coins.key(j.reward()), Coins.count(j.reward()))).withStyle(ChatFormatting.AQUA));
                g.renderTooltip(font, font.split(tip, 220), mx, my);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

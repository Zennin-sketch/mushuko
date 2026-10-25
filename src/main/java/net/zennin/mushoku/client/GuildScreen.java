package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.zennin.mushoku.guild.Coins;
import net.zennin.mushoku.guild.GuildData;
import net.zennin.mushoku.guild.GuildSystem;
import net.zennin.mushoku.guild.Jobs;
import net.zennin.mushoku.network.GuildActionPayload;

/** Gremio de aventureros: rango, trabajo activo y trabajos disponibles por rango (F a A). */
public class GuildScreen extends Screen {
    private static int tab = -1;
    private int seen = -1, left, top;
    private static final int W = 300;

    public GuildScreen() { super(Component.translatable("gui.remushoku.guild.title")); }

    @Override
    protected void init() {
        GuildData g = ClientGuildState.data;
        seen = ClientGuildState.version;
        if (tab < 1) tab = Math.min(6, g.rank());
        left = (width - W) / 2;
        top = Math.max(6, height / 2 - 110);

        int need = GuildData.xpToRankUp(g.rank());
        Button up = Button.builder(Component.translatable("gui.remushoku.guild.rank_up"), b -> send(GuildActionPayload.RANK_UP, 0))
                .bounds(left + W - 90, top + 14, 90, 18).build();
        up.active = need > 0 && g.xp() >= need;
        addRenderableWidget(up);

        // Trabajo activo
        Jobs.Job active = Jobs.byId(g.job());
        int y = top + 54;
        if (active != null) {
            int bx = left + W - 230;
            if (active.type() == Jobs.Type.COLLECT) {
                Button s = Button.builder(Component.translatable("gui.remushoku.guild.submit"), b -> send(GuildActionPayload.SUBMIT, 0))
                        .bounds(bx, y + 14, 74, 18).build();
                s.active = g.progress() < active.amount();
                addRenderableWidget(s);
            }
            Button c = Button.builder(Component.translatable("gui.remushoku.guild.complete"), b -> send(GuildActionPayload.COMPLETE, 0))
                    .bounds(bx + 78, y + 14, 74, 18).build();
            c.active = g.progress() >= active.amount();
            addRenderableWidget(c);
            addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.guild.abandon").withStyle(ChatFormatting.RED),
                    b -> send(GuildActionPayload.ABANDON, 0)).bounds(bx + 156, y + 14, 74, 18).build());
        }

        // Pestañas de rango
        int ty = top + 92;
        for (int r = 1; r <= 6; r++) {
            int rank = r;
            MutableComponent label = Component.literal(GuildData.rankLetter(r));
            if (r == tab) label = label.withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
            addRenderableWidget(Button.builder(label, b -> { tab = rank; rebuildWidgets(); }).bounds(left + (r - 1) * 50 + 2, ty, 46, 18).build());
        }
        int ry = ty + 26;
        for (Jobs.Job j : Jobs.ALL) {
            if (j.rank() != tab) continue;
            Button a = Button.builder(Component.translatable("gui.remushoku.guild.accept"), b -> send(GuildActionPayload.ACCEPT, j.id()))
                    .bounds(left + W - 70, ry, 70, 18).build();
            a.active = g.job() == 0 && GuildSystem.canTake(g, j);
            a.setTooltip(Tooltip.create(describe(j)));
            addRenderableWidget(a);
            ry += 24;
        }
    }

    private static Component describe(Jobs.Job j) {
        return Component.translatable("job.remushoku." + j.key()).withStyle(ChatFormatting.GOLD).append("\n")
                .append(Component.translatable("job.remushoku." + j.key() + ".desc", j.amount()).withStyle(ChatFormatting.WHITE)).append("\n")
                .append(Component.translatable("gui.remushoku.guild.reward", j.xp(), coins(j.reward())).withStyle(ChatFormatting.AQUA));
    }

    static Component coins(int amount) {
        return Component.translatable("coin.remushoku." + Coins.key(amount), Coins.count(amount));
    }

    private void send(int action, int arg) { ClientPlayNetworking.send(new GuildActionPayload(action, arg, "")); }

    @Override
    public void tick() {
        super.tick();
        if (seen != ClientGuildState.version) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        GuildData d = ClientGuildState.data;
        g.drawCenteredString(font, title, width / 2, top, 0xFFD27F);
        int need = GuildData.xpToRankUp(d.rank());
        g.drawString(font, Component.translatable("gui.remushoku.guild.rank", GuildData.rankLetter(d.rank())), left, top + 14, 0xFFFFFF, true);
        g.drawString(font, need > 0 ? Component.translatable("gui.remushoku.guild.xp", d.xp(), need) : Component.translatable("gui.remushoku.guild.max_rank"),
                left, top + 25, 0xAAAAAA, true);
        Jobs.Job active = Jobs.byId(d.job());
        int y = top + 54;
        g.drawString(font, Component.translatable("gui.remushoku.gui_job_board.label_active_job").withStyle(ChatFormatting.UNDERLINE), left, y - 10, 0xFFD27F, true);
        if (active == null) {
            g.drawString(font, Component.translatable("gui.remushoku.guild.no_job"), left, y + 4, 0x888888, true);
        } else {
            g.drawString(font, Component.translatable("job.remushoku." + active.key()), left, y + 2, 0xFFFFFF, true);
            g.drawString(font, Component.translatable("gui.remushoku.guild.progress", Math.min(d.progress(), active.amount()), active.amount()),
                    left, y + 14, d.progress() >= active.amount() ? 0x7CFC7C : 0xFFFF77, true);
        }
        int ry = top + 92 + 26;
        for (Jobs.Job j : Jobs.ALL) {
            if (j.rank() != tab) continue;
            g.drawString(font, Component.translatable("job.remushoku." + j.key()), left + 2, ry + 1, 0xFFFFFF, true);
            g.drawString(font, Component.translatable("gui.remushoku.guild.reward", j.xp(), coins(j.reward())), left + 2, ry + 11, 0x8FD8FF, false);
            if (mx >= left && mx < left + W - 72 && my >= ry && my < ry + 22) g.renderTooltip(font, font.split(describe(j), 240), mx, my);
            ry += 24;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

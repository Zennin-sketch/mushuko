package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.network.GuildActionPayload;
import net.zennin.mushoku.race.Race;
import net.zennin.mushoku.race.RaceData;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Menu del Aventurero (tecla N, como el menu principal del addon): Raza y Habilidades. */
public class AdventurerScreen extends Screen {
    private static final int W = 330, H = 214;
    private static String tab = "race";
    private static Race selected = Race.HUMAN;
    private int left, top, seen = -1;

    public AdventurerScreen() { super(Component.translatable("gui.remushoku.adventurer.title")); }

    private void send(int action, String arg) { ClientPlayNetworking.send(new GuildActionPayload(action, 0, arg)); }

    @Override
    protected void init() {
        seen = ClientRaceState.version + ClientPartyState.version * 1000;
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
        RaceData d = ClientRaceState.data;
        if (d.raceOrNull() != null) selected = d.raceOrNull();
        addRenderableWidget(new MButton(left + 12, top + 22, 70, 14, Component.translatable("gui.remushoku.adventurer.tab.race"), b -> { tab = "race"; rebuildWidgets(); }, tab.equals("race")));
        addRenderableWidget(new MButton(left + 84, top + 22, 70, 14, Component.translatable("gui.remushoku.adventurer.tab.skills"), b -> { tab = "skills"; rebuildWidgets(); }, tab.equals("skills")));
        addRenderableWidget(new MButton(left + 156, top + 22, 70, 14, Component.translatable("gui.remushoku.adventurer.tab.party"), b -> { tab = "party"; rebuildWidgets(); }, tab.equals("party")));
        if (tab.equals("party")) {
            initParty();
            return;
        }
        if (tab.equals("race")) {
            Race[] rs = Race.values();
            for (int i = 0; i < rs.length; i++) {
                Race r = rs[i];
                addRenderableWidget(new MButton(left + 14, top + 44 + i * 20, 92, 16, Component.translatable("race.remushoku." + r.key),
                        b -> { selected = r; rebuildWidgets(); }, r == selected));
            }
            if (d.race().isEmpty()) {
                MButton confirm = new MButton(left + W - 120, top + H - 26, 104, 16, Component.translatable("gui.remushoku.adventurer.confirm"),
                        b -> send(GuildActionPayload.RACE_PICK, selected.key));
                confirm.setTooltip(Tooltip.create(Component.translatable("gui.remushoku.adventurer.confirm_tip")));
                addRenderableWidget(confirm);
            }
        } else {
            MButton ms = new MButton(left + W - 96, top + 52, 80, 14, Component.translatable(d.manaSkinOn() ? "gui.remushoku.sword.on" : "gui.remushoku.sword.off"),
                    b -> send(GuildActionPayload.RACE_TOGGLE, "mana_skin"), d.manaSkinOn());
            addRenderableWidget(ms);
            MButton dg = new MButton(left + W - 96, top + 94, 80, 14, Component.translatable(d.dodgeOn() ? "gui.remushoku.sword.on" : "gui.remushoku.sword.off"),
                    b -> send(GuildActionPayload.RACE_TOGGLE, "dodge"), d.dodge() && d.dodgeOn());
            dg.active = d.dodge();
            addRenderableWidget(dg);
        }
    }

    private void party(String a) { send(GuildActionPayload.PARTY, a); }

    /** Pestaña Grupo: crear, invitar a jugadores cercanos, aceptar invitaciones, salir y expulsar. */
    private void initParty() {
        int x = left + 14, y = top + 44;
        boolean inParty = !ClientPartyState.leader.isEmpty();
        boolean leader = inParty && minecraft.player != null && ClientPartyState.leader.equals(minecraft.player.getGameProfile().getName());
        if (!ClientPartyState.invite.isEmpty() && !inParty) {
            addRenderableWidget(new MButton(x, y + 14, 70, 14, Component.translatable("gui.remushoku.guild.accept"), b -> party("accept")));
            addRenderableWidget(new MButton(x + 74, y + 14, 70, 14, Component.translatable("gui.remushoku.adventurer.decline"), b -> party("decline")));
            y += 36;
        }
        if (!inParty) {
            addRenderableWidget(new MButton(x, y, 110, 16, Component.translatable("gui.remushoku.adventurer.create_party"), b -> party("create")));
            return;
        }
        addRenderableWidget(new MButton(left + W - 90, top + H - 26, 76, 16, Component.translatable("gui.remushoku.adventurer.leave_party"), b -> party("leave")));
        // Miembros (el lider puede expulsar)
        int my = top + 58;
        for (String name : ClientPartyState.members) {
            if (leader && !name.equals(ClientPartyState.leader))
                addRenderableWidget(new MButton(left + 150, my - 2, 50, 12, Component.translatable("gui.remushoku.adventurer.kick"), b -> party("kick:" + name)));
            my += 14;
        }
        // Jugadores cercanos para invitar
        if (leader && minecraft.level != null) {
            int iy = top + 58;
            for (var pl : minecraft.level.players()) {
                String name = pl.getGameProfile().getName();
                if (pl == minecraft.player || ClientPartyState.members.contains(name) || pl.distanceTo(minecraft.player) > 32) continue;
                if (iy > top + H - 50) break;
                addRenderableWidget(new MButton(left + W - 70, iy - 2, 56, 12, Component.translatable("gui.remushoku.adventurer.invite"), b -> party("invite:" + name)));
                iy += 14;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (seen != ClientRaceState.version + ClientPartyState.version * 1000) rebuildWidgets();
    }

    private String sign(double v, boolean pct) {
        if (v == 0) return Component.translatable("gui.remushoku.adventurer.normal").getString();
        String s = (v > 0 ? "+" : "") + (pct ? Math.round(v * 100) + "%" : (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)));
        return s;
    }

    private String aff(double v) { return v == 1 ? "—" : (v > 1 ? "+" : "") + Math.round((v - 1) * 100) + "%"; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.blitSprite(WOOD, left, top + 8, W, H - 8);
        title(g, title, width / 2, top);
        RaceData d = ClientRaceState.data;
        if (tab.equals("party")) {
            renderParty(g);
            return;
        }
        int px = left + 112, py = top + 42, pw = W - 124, ph = H - 54;
        g.blitSprite(PARCHMENT, px, py, pw, ph);
        if (tab.equals("race")) {
            Race r = selected;
            int y = py + 8;
            ink(g, Component.translatable("race.remushoku." + r.key), px + 8, y, INK_RED);
            if (r.key.equals(d.race())) ink(g, Component.translatable("gui.remushoku.adventurer.yours"), px + pw - 8 - font.width(Component.translatable("gui.remushoku.adventurer.yours")), y, INK_GREEN);
            y += 12;
            for (var line : font.split(Component.translatable("race.remushoku." + r.key + ".desc"), pw - 16)) {
                g.drawString(font, line, px + 8, y, INK_SOFT, false);
                y += 9;
            }
            y += 4;
            String[][] rows = {
                    {"health", String.valueOf((int) r.health)}, {"speed", sign(r.speed, true)}, {"armor", sign(r.armor, false)},
                    {"damage", sign(r.damage, false)}, {"atk_speed", sign(r.attackSpeed, false)}, {"size", r.scale == 1 ? "—" : Math.round(r.scale * 100) + "%"}};
            for (String[] row : rows) {
                g.drawString(font, Component.translatable("gui.remushoku.adventurer.stat." + row[0]), px + 8, y, INK, false);
                g.drawString(font, row[1], px + 110, y, INK, false);
                y += 10;
            }
            y += 3;
            g.drawString(font, Component.translatable("gui.remushoku.adventurer.affinity"), px + 8, y, INK, false);
            y += 10;
            String affs = Component.translatable("element.remushoku.fire").getString() + " " + aff(r.fire) + "   "
                    + Component.translatable("element.remushoku.water").getString() + " " + aff(r.water);
            g.drawString(font, affs, px + 12, y, INK_SOFT, false);
            y += 9;
            affs = Component.translatable("element.remushoku.earth").getString() + " " + aff(r.earth) + "   "
                    + Component.translatable("element.remushoku.wind").getString() + " " + aff(r.wind);
            g.drawString(font, affs, px + 12, y, INK_SOFT, false);
            if (!d.race().isEmpty()) g.drawString(font, Component.translatable("gui.remushoku.adventurer.locked"), px + 8, py + ph - 12, INK_SOFT, false);
        } else {
            int y = py + 8;
            int ms = RaceData.manaSkinLevel(ClientManaState.maxMana);
            ink(g, Component.translatable("gui.remushoku.adventurer.skill.mana_skin"), px + 8, y, INK_RED);
            g.drawString(font, ms == 0 ? Component.translatable("gui.remushoku.adventurer.mana_skin_locked")
                    : Component.translatable("gui.remushoku.adventurer.mana_skin_level", ms, 5 + 5 * ms), px + 8, y + 11, INK_SOFT, false);
            y += 42;
            ink(g, Component.translatable("gui.remushoku.adventurer.skill.dodge"), px + 8, y, INK_RED);
            g.drawString(font, Component.translatable(d.dodge() ? "gui.remushoku.adventurer.dodge_have" : "gui.remushoku.adventurer.dodge_locked"),
                    px + 8, y + 11, INK_SOFT, false);
            y += 42;
            ink(g, Component.translatable("gui.remushoku.adventurer.skill.laplace"), px + 8, y, INK_RED);
            g.drawString(font, Component.translatable(d.laplace() ? "gui.remushoku.adventurer.laplace_have" : "gui.remushoku.adventurer.laplace_locked"),
                    px + 8, y + 11, d.laplace() ? INK_GREEN : INK_SOFT, false);
            y += 30;
            ink(g, Component.translatable("gui.remushoku.adventurer.skill.race"), px + 8, y, INK_RED);
            Race r = d.raceOrNull();
            g.drawString(font, Component.translatable(r == null ? "gui.remushoku.adventurer.no_race" : "race.remushoku." + r.key + ".skill",
                    ModKeys.RACE_SKILL.getTranslatedKeyMessage()), px + 8, y + 11, INK_SOFT, false);
        }
    }

    private void renderParty(GuiGraphics g) {
        g.blitSprite(PARCHMENT, left + 10, top + 40, W - 20, H - 70);
        boolean inParty = !ClientPartyState.leader.isEmpty();
        if (!inParty) {
            int y = top + 48;
            if (!ClientPartyState.invite.isEmpty()) {
                ink(g, Component.translatable("gui.remushoku.adventurer.invite_from", ClientPartyState.invite), left + 16, y, INK_RED);
                y += 36;
            }
            for (var line : font.split(Component.translatable("gui.remushoku.adventurer.party_info"), W - 40)) {
                g.drawString(font, line, left + 16, y + 22, INK_SOFT, false);
                y += 9;
            }
            return;
        }
        ink(g, Component.translatable("gui.remushoku.adventurer.members", ClientPartyState.members.size(), net.zennin.mushoku.party.Parties.MAX), left + 16, top + 46, INK_RED);
        int y = top + 58;
        for (String name : ClientPartyState.members) {
            g.drawString(font, (name.equals(ClientPartyState.leader) ? "★ " : "• ") + name, left + 18, y, INK, false);
            y += 14;
        }
        if (minecraft.player != null && ClientPartyState.leader.equals(minecraft.player.getGameProfile().getName()))
            ink(g, Component.translatable("gui.remushoku.adventurer.nearby"), left + W - 130, top + 46, INK_RED);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

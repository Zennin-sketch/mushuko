package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.network.HitogamiActionPayload;
import net.zennin.mushoku.race.Appearance;
import net.zennin.mushoku.race.Race;
import net.zennin.mushoku.registry.ModItems;
import net.zennin.mushoku.sword.SwordStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Creacion de personaje (estilo Dragon Mine Z): tu personaje en el centro, el selector abajo, la historia de la opcion a
 * la izquierda y lo que cambia a la derecha. Pasos: Raza -> Rasgo (elfo, demonio y bestia) -> Clase.
 * El equipo que se ve en la clase es solo de muestra: no se entrega.
 */
public class CharacterCreationScreen extends Screen {
    private enum Stage { RACE, TRAIT, CLASS }

    private static final Race[] RACES = {Race.HUMAN, Race.ELF, Race.DWARF, Race.MIGURD, Race.SUPERD, Race.OGRE, Race.BEAST, Race.DEMON};
    private static final String[] CLASSES = {"mage", "swordsman", "archer"};
    private static final int GOLD = 0xFFE8C86A, TEXT = 0xFFE6E2D8, DIM = 0xFF9C988E, PANEL = 0xC8101016, LINE = 0xFF6E5C34;

    private Stage stage = Stage.RACE;
    private int race = 0, variant = 0, height = 1, cls = 0, aff = 0;
    /** Color de las orejas: -1 = el de tu skin. hue/sat/bri = el mismo color en los deslizadores. */
    private int color = -1;
    private double hue = 0.08, sat = 0.35, bri = 0.85;

    // Tonos de piel para las orejas de elfo (incluye algunos de fantasia) y pelajes para las de bestia
    private static final int[] ELF_COLORS = {0xF6D7C3, 0xEEC1A0, 0xE0A882, 0xC68A5E, 0x9C6440, 0x6B4329,
            0x4A2E1E, 0xD7E3EF, 0xB9D2A8, 0xA7C7E7, 0xE6C7E8, 0xC8C0B0};
    private static final int[] FUR_COLORS = {0x1E1A18, 0x3B2A20, 0x5C3A24, 0x8A5A32, 0xC46A2A, 0xE09A4A,
            0xD9B26A, 0xEFE3C8, 0xF4F1EA, 0x9A9A9A, 0x5A5E66, 0xB9BFCC};

    public CharacterCreationScreen() {
        super(Component.translatable("gui.remushoku.cc.title"));
    }

    private Race race() { return RACES[race]; }

    private boolean hasTrait() { Race r = race(); return r == Race.ELF || r == Race.DEMON || r == Race.BEAST; }

    private Appearance appearance() { return Appearance.forRace(race(), variant, height, color); }

    private boolean hasColor() { return race() == Race.ELF || race() == Race.BEAST; }

    @Override
    protected void init() {
        int cx = width / 2;
        int sy = height - 72;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1)).bounds(cx - 95, sy, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1)).bounds(cx + 75, sy, 20, 20).build());
        if (stage == Stage.CLASS && cls >= 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { aff = (aff + 2) % 3; }).bounds(cx - 95, sy + 24, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { aff = (aff + 1) % 3; }).bounds(cx + 75, sy + 24, 20, 20).build());
        }
        if (stage != Stage.RACE)
            addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.cc.back"), b -> back()).bounds(cx - 190, height - 26, 90, 20).build());
        if (stage == Stage.TRAIT && hasColor()) addColorPicker();
        boolean last = stage == Stage.CLASS;
        Button next = Button.builder(Component.translatable(last ? "gui.remushoku.cc.confirm" : "gui.remushoku.cc.next"), b -> next())
                .bounds(cx + 100, height - 26, 90, 20).build();
        next.active = true;
        addRenderableWidget(next);
    }

    private void cycle(int dir) {
        switch (stage) {
            case RACE -> { race = (race + dir + RACES.length) % RACES.length; variant = 0; height = 1; color = -1; }
            case TRAIT -> {
                if (race() == Race.BEAST) variant = (variant + dir + 3) % 3;
                else height = (height + dir + 3) % 3;
            }
            case CLASS -> cls = (cls + dir + CLASSES.length) % CLASSES.length;
        }
        rebuildWidgets();
    }

    private void back() {
        stage = stage == Stage.CLASS && hasTrait() ? Stage.TRAIT : Stage.RACE;
        rebuildWidgets();
    }

    private void next() {
        switch (stage) {
            case RACE -> stage = hasTrait() ? Stage.TRAIT : Stage.CLASS;
            case TRAIT -> stage = Stage.CLASS;
            case CLASS -> {
                ClientPlayNetworking.send(new HitogamiActionPayload(HitogamiActionPayload.CREATE, race().key, variant, height, hasColor() ? color : -1,
                        cls == 0 ? SpellCasting.CLASS_MAGE : cls == 1 ? SpellCasting.CLASS_SWORDSMAN : SpellCasting.CLASS_ARCHER,
                        cls == 1 ? SwordStyle.values()[aff].key : cls == 2 ? net.zennin.mushoku.archer.ArcherSchool.values()[aff].key : ""));
                onClose();
                return;
            }
        }
        rebuildWidgets();
    }

    @Override
    public void removed() {
        ClientAppearance.preview = null;
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xB0141420, 0xE00A0A10);
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 8, GOLD);
        drawSteps(g, cx, 22);

        renderPlayer(g, cx, mouseX, mouseY);

        // Selector(es)
        int sy = height - 72;
        selectorBox(g, cx, sy, selectorLabel(), TEXT);
        if (stage == Stage.CLASS && cls == 1)
            selectorBox(g, cx, sy + 24, Component.translatable("style.remushoku." + SwordStyle.values()[aff].key), TEXT);
        if (stage == Stage.CLASS && cls == 2)
            selectorBox(g, cx, sy + 24, Component.translatable("school.remushoku." + net.zennin.mushoku.archer.ArcherSchool.values()[aff].key), TEXT);

        // Paneles
        int side = sideWidth();
        panel(g, 10, 40, side, height - 120, leftTitle(), leftText());
        panel(g, width - 10 - side, 40, side, height - 120, rightTitle(), rightText());
        if (stage == Stage.TRAIT && hasColor()) {
            // Muestra del color elegido (o "de tu skin")
            int x0 = width - 10 - side + 8, y0 = 40 + 26 + 2 * 16 + 3 + 17 + 47;
            if (y0 + 12 < 40 + height - 120) {
                if (color >= 0) g.fill(x0, y0, x0 + side - 16, y0 + 10, 0xFF000000 | color);
                else g.drawString(font, Component.translatable("gui.remushoku.cc.color_auto_hint"), x0, y0 + 1, DIM, false);
            }
        }
    }

    private void drawSteps(GuiGraphics g, int cx, int y) {
        String[] keys = {"gui.remushoku.cc.step_race", "gui.remushoku.cc.step_trait", "gui.remushoku.cc.step_class"};
        Stage[] st = {Stage.RACE, Stage.TRAIT, Stage.CLASS};
        List<Component> parts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (st[i] == Stage.TRAIT && !hasTrait()) continue;
            parts.add(Component.translatable(keys[i]).withStyle(st[i] == stage ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) line.append(Component.literal("  ›  ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(parts.get(i));
        }
        g.drawCenteredString(font, line, cx, y, 0xFFFFFFFF);
    }

    private void selectorBox(GuiGraphics g, int cx, int y, Component label, int color) {
        g.fill(cx - 72, y, cx + 72, y + 20, 0xE0101016);
        g.renderOutline(cx - 72, y, 144, 20, LINE);
        g.drawCenteredString(font, label, cx, y + 6, color);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, Component title, List<Component> lines) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, LINE);
        g.drawString(font, title, x + 8, y + 8, GOLD, false);
        g.fill(x + 8, y + 19, x + w - 8, y + 20, 0x806E5C34);
        int ty = y + 26;
        for (Component c : lines) {
            if (c.getString().isEmpty()) { ty += 5; continue; }
            for (FormattedCharSequence seq : font.split(c, w - 16)) {
                if (ty > y + h - 10) return;
                g.drawString(font, seq, x + 8, ty, TEXT, false);
                ty += 10;
            }
        }
    }

    /** Tu personaje en el centro, con el rasgo elegido y (en la clase) el equipo de muestra. */
    private void renderPlayer(GuiGraphics g, int cx, int mouseX, int mouseY) {
        LocalPlayer p = minecraft.player;
        if (p == null) return;
        ClientAppearance.preview = appearance();
        Inventory inv = p.getInventory();
        int sel = inv.selected;
        ItemStack main = inv.items.get(sel), off = inv.offhand.get(0);
        ItemStack[] armor = {inv.armor.get(0), inv.armor.get(1), inv.armor.get(2), inv.armor.get(3)};
        int qs = sel == 8 ? 7 : 8;  // ranura donde se pone el carcaj de muestra (se ve en la espalda)
        ItemStack quiverSlot = inv.items.get(qs);
        try {
            if (stage == Stage.CLASS) {
                switch (cls) {
                    case 0 -> {
                        inv.items.set(sel, new ItemStack(ModItems.AQUA_HEARTIA));
                        inv.offhand.set(0, ItemStack.EMPTY);
                        inv.armor.set(0, new ItemStack(ModItems.MK2_BOOTS));
                        inv.armor.set(1, new ItemStack(ModItems.MK2_LEGGINGS));
                        inv.armor.set(2, new ItemStack(ModItems.MK2_CHESTPLATE));
                        inv.armor.set(3, ItemStack.EMPTY);
                    }
                    case 1 -> {
                        inv.items.set(sel, new ItemStack(ModItems.PAULS_SWORD));
                        inv.offhand.set(0, new ItemStack(ModItems.MIGURD_SWORD));
                        for (int i = 0; i < 4; i++) inv.armor.set(i, ItemStack.EMPTY);
                    }
                    default -> {
                        inv.items.set(sel, new ItemStack(Items.BOW));
                        inv.items.set(qs, new ItemStack(ModItems.QUIVER));
                        inv.offhand.set(0, ItemStack.EMPTY);
                        for (int i = 0; i < 4; i++) inv.armor.set(i, ItemStack.EMPTY);
                    }
                }
            }
            int top = 38, bottom = height - 80;
            int size = Math.max(30, Math.min(90, (bottom - top) / 2 - 8));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx - 60, top, cx + 60, bottom, size, 0.0625f, mouseX, mouseY, p);
        } finally {
            inv.items.set(sel, main);
            inv.items.set(qs, quiverSlot);
            inv.offhand.set(0, off);
            for (int i = 0; i < 4; i++) inv.armor.set(i, armor[i]);
        }
    }

    // ------------------------------------------------------------------ textos
    private Component selectorLabel() {
        return switch (stage) {
            case RACE -> Component.translatable("race.remushoku." + race().key);
            case TRAIT -> race() == Race.BEAST
                    ? Component.translatable("gui.remushoku.cc.animal." + variant)
                    : Component.translatable("gui.remushoku.cc.height", Component.translatable("gui.remushoku.cc.height." + height));
            case CLASS -> Component.translatable("class.remushoku." + CLASSES[cls]);
        };
    }

    private Component leftTitle() {
        return switch (stage) {
            case RACE -> Component.translatable("race.remushoku." + race().key);
            case TRAIT -> Component.translatable("gui.remushoku.cc.trait_title." + race().key);
            case CLASS -> Component.translatable("class.remushoku." + CLASSES[cls]);
        };
    }

    private List<Component> leftText() {
        List<Component> out = new ArrayList<>();
        switch (stage) {
            case RACE -> split(out, "race.remushoku." + race().key + ".lore");
            case TRAIT -> split(out, "gui.remushoku.cc.trait." + race().key);
            case CLASS -> split(out, "class.remushoku." + CLASSES[cls] + ".lore");
        }
        return out;
    }

    private Component rightTitle() {
        if (stage == Stage.TRAIT && hasColor())
            return Component.translatable(race() == Race.ELF ? "gui.remushoku.cc.color_ear" : "gui.remushoku.cc.color_fur");
        return Component.translatable(stage == Stage.CLASS ? "gui.remushoku.cc.features" : "gui.remushoku.cc.stats");
    }

    private int sideWidth() { return Math.max(110, Math.min(180, width / 2 - 120)); }

    /** Muestras de color, boton "de tu skin" y deslizadores de tono, saturacion y brillo (dentro del panel derecho). */
    private void addColorPicker() {
        int side = sideWidth(), x0 = width - 10 - side + 8, y = 40 + 26;
        int[] colors = race() == Race.ELF ? ELF_COLORS : FUR_COLORS;
        int cols = 6, size = Math.min(14, (side - 16 - (cols - 1) * 2) / cols);
        for (int i = 0; i < colors.length; i++) {
            int c = colors[i];
            addRenderableWidget(new Swatch(x0 + (i % cols) * (size + 2), y + (i / cols) * (size + 2), size, c));
        }
        y += 2 * (size + 2) + 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.cc.color_auto"), b -> { color = -1; rebuildWidgets(); })
                .bounds(x0, y, side - 16, 14).build());
        y += 17;
        addRenderableWidget(new HsvSlider(x0, y, side - 16, "gui.remushoku.cc.hue", hue, v -> hue = v));
        addRenderableWidget(new HsvSlider(x0, y + 15, side - 16, "gui.remushoku.cc.sat", sat, v -> sat = v));
        addRenderableWidget(new HsvSlider(x0, y + 30, side - 16, "gui.remushoku.cc.bright", bri, v -> bri = v));
    }

    private void pickColor(int c) {
        color = c;
        float[] hsv = rgbToHsv(c);
        hue = hsv[0]; sat = hsv[1]; bri = hsv[2];
        rebuildWidgets();
    }

    private static float[] rgbToHsv(int c) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    /** Cuadrado de color que se puede elegir. */
    private class Swatch extends net.minecraft.client.gui.components.AbstractWidget {
        private final int rgb;

        Swatch(int x, int y, int size, int rgb) {
            super(x, y, size, size, Component.empty());
            this.rgb = rgb;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = color == rgb;
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | rgb);
            g.renderOutline(getX() - (selected ? 1 : 0), getY() - (selected ? 1 : 0), width + (selected ? 2 : 0), height + (selected ? 2 : 0),
                    selected ? GOLD : isHovered() ? 0xFFFFFFFF : 0xFF2A2A30);
        }

        @Override
        public void onClick(double mouseX, double mouseY) { pickColor(rgb); }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {}
    }

    /** Deslizador de tono / saturacion / brillo: al moverlo, el color pasa a ser el de los tres deslizadores. */
    private class HsvSlider extends net.minecraft.client.gui.components.AbstractSliderButton {
        private final String key;
        private final java.util.function.DoubleConsumer setter;

        HsvSlider(int x, int y, int w, String key, double value, java.util.function.DoubleConsumer setter) {
            super(x, y, w, 14, Component.empty(), value);
            this.key = key;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() { setMessage(Component.translatable(key, Math.round(value * 100) + "%")); }

        @Override
        protected void applyValue() {
            setter.accept(value);
            color = net.minecraft.util.Mth.hsvToRgb((float) hue, (float) sat, (float) bri) & 0xFFFFFF;
        }
    }

    private List<Component> rightText() {
        List<Component> out = new ArrayList<>();
        if (stage == Stage.TRAIT && hasColor()) {
            // Debajo de los deslizadores: muestra del color actual
            return out;
        }
        if (stage != Stage.CLASS) {
            raceStats(out, race());
            return out;
        }
        split(out, "class.remushoku." + CLASSES[cls] + ".features");
        if (cls == 1) {
            out.add(Component.empty());
            SwordStyle st = SwordStyle.values()[aff];
            out.add(Component.translatable("style.remushoku." + st.key).withStyle(ChatFormatting.GOLD));
            out.add(Component.translatable("style.remushoku." + st.key + ".desc").withStyle(ChatFormatting.GRAY));
        }
        if (cls == 2) {
            out.add(Component.empty());
            var sc = net.zennin.mushoku.archer.ArcherSchool.values()[aff];
            out.add(Component.translatable("school.remushoku." + sc.key).withStyle(ChatFormatting.GOLD));
            out.add(Component.translatable("school.remushoku." + sc.key + ".desc").withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private static void split(List<Component> out, String key) {
        for (String s : Component.translatable(key).getString().split("\n")) out.add(Component.literal(s));
    }

    private static Component num(String key, double value, String fmt, boolean higherIsBetter) {
        ChatFormatting c = value == 0 ? ChatFormatting.GRAY : (value > 0) == higherIsBetter ? ChatFormatting.GREEN : ChatFormatting.RED;
        String v = (value > 0 ? "+" : "") + String.format(fmt, value);
        return Component.translatable(key).append(": ").append(Component.literal(v).withStyle(c));
    }

    private static Component mult(String key, double value) {
        ChatFormatting c = value == 1 ? ChatFormatting.GRAY : value > 1 ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.translatable(key).append(": ").append(Component.literal(String.format("×%.1f", value)).withStyle(c));
    }

    private static void raceStats(List<Component> out, Race r) {
        out.add(Component.translatable("gui.remushoku.cc.stat.health").append(": ")
                .append(Component.literal(String.format("%.0f ❤", r.health / 2)).withStyle(r.health > 20 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        out.add(num("gui.remushoku.cc.stat.speed", r.speed * 100, "%.0f%%", true));
        out.add(num("gui.remushoku.cc.stat.armor", r.armor, "%.0f", true));
        out.add(num("gui.remushoku.cc.stat.damage", r.damage, "%.0f", true));
        out.add(num("gui.remushoku.cc.stat.attack_speed", r.attackSpeed, "%.1f", true));
        out.add(mult("gui.remushoku.cc.stat.size", r.scale));
        out.add(Component.empty());
        out.add(Component.translatable("gui.remushoku.cc.stat.magic").withStyle(ChatFormatting.GOLD));
        out.add(mult("element.remushoku.fire", r.fire));
        out.add(mult("element.remushoku.water", r.water));
        out.add(mult("element.remushoku.earth", r.earth));
        out.add(mult("element.remushoku.wind", r.wind));
        out.add(Component.empty());
        out.add(Component.translatable("race.remushoku." + r.key + ".skill", "R").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

package net.zennin.mushoku.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.client.MedievalUi.MButton;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Libro Guia: indice de capitulos y texto del original (traducido) paginado en dos hojas. */
public class GuideScreen extends BookScreen {
    private static final int LINES = 17;
    private static String chapter = null;
    private static int spread = 0;
    private final Map<String, List<String>> chapters = new LinkedHashMap<>();
    private List<FormattedCharSequence> lines = List.of();

    public GuideScreen() {
        super(Component.translatable("guide.remushoku.title"));
        try (var reader = new InputStreamReader(Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(MushokuMod.id("guide.json")).open())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject ch = root.getAsJsonObject("chapters");
            for (JsonElement name : root.getAsJsonArray("order")) {
                List<String> keys = new ArrayList<>();
                for (JsonElement page : ch.getAsJsonArray(name.getAsString()))
                    for (JsonElement k : (JsonArray) page) keys.add(k.getAsString());
                chapters.put(name.getAsString(), keys);
            }
        } catch (Exception e) {
            MushokuMod.LOG.error("No se pudo leer guide.json", e);
        }
    }

    private String text(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String s = Component.translatable(k).getString().trim();
            boolean cont = s.endsWith("-");
            if (cont) s = s.substring(0, s.length() - 1).trim();
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append(' ');
            sb.append(s);
            if (k.startsWith("guide.")) sb.append('\n');
        }
        return sb.toString().replaceAll(" +", " ");
    }

    @Override
    protected void init() {
        layout();
        if (chapter == null) {
            int y = top + 30, i = 0;
            for (String name : chapters.keySet()) {
                int x = i < 3 ? leftPageX() + 10 : rightPageX() + 10;
                int yy = y + (i % 3) * 26;
                addRenderableWidget(new MButton(x, yy, PAGE_W - 20, 18, Component.translatable("guide.remushoku.chapter." + name), b -> {
                    chapter = name;
                    spread = 0;
                    rebuildWidgets();
                }));
                i++;
            }
            return;
        }
        List<FormattedCharSequence> all = new ArrayList<>();
        for (String para : text(chapters.getOrDefault(chapter, List.of())).split("\n")) {
            all.addAll(font.split(Component.literal(para), PAGE_W - 8));
            all.add(FormattedCharSequence.EMPTY);
        }
        lines = all;
        int spreads = Math.max(1, (lines.size() + LINES * 2 - 1) / (LINES * 2));
        spread = Math.min(spread, spreads - 1);
        int by = top + H - 20;
        MButton prev = new MButton(leftPageX() + 4, by, 22, 13, Component.literal("<"), b -> { spread--; rebuildWidgets(); });
        prev.active = spread > 0;
        MButton next = new MButton(rightPageX() + PAGE_W - 26, by, 22, 13, Component.literal(">"), b -> { spread++; rebuildWidgets(); });
        next.active = spread < spreads - 1;
        addRenderableWidget(prev);
        addRenderableWidget(next);
        addRenderableWidget(new MButton(width / 2 - 30, by, 60, 13, Component.translatable("guide.remushoku.index"), b -> { chapter = null; rebuildWidgets(); }));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        if (chapter == null) {
            ink(g, Component.translatable("guide.remushoku.index"), leftPageX() + 4, top + 16, INK_SOFT);
            return;
        }
        ink(g, Component.translatable("guide.remushoku.chapter." + chapter), leftPageX() + 4, top + 16, INK_RED);
        int start = spread * LINES * 2;
        for (int i = 0; i < LINES * 2; i++) {
            int idx = start + i;
            if (idx >= lines.size()) break;
            int x = i < LINES ? leftPageX() + 4 : rightPageX() + 2;
            int y = top + 28 + (i % LINES) * 9;
            g.drawString(font, lines.get(idx), x, y, INK, false);
        }
    }
}

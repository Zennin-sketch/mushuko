package net.zennin.mushoku.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de espadachin.
 * style: afinidad elegida ("" = ninguna). points: puntos de habilidad. touki: nivel de Touki. toukiOn: Touki activo.
 * learned: habilidades aprendidas. slots: las 3 habilidades asignadas a Z/X/C. toggles: habilidades encendidas.
 */
public record SwordData(String style, int points, int touki, boolean toukiOn,
                        List<String> learned, List<String> slots, List<String> toggles) {
    public static final SwordData DEFAULT = new SwordData("", 0, 0, false, List.of(), List.of("", "", ""), List.of());

    public static final Codec<SwordData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("style", "").forGetter(SwordData::style),
            Codec.INT.optionalFieldOf("points", 0).forGetter(SwordData::points),
            Codec.INT.optionalFieldOf("touki", 0).forGetter(SwordData::touki),
            Codec.BOOL.optionalFieldOf("touki_on", false).forGetter(SwordData::toukiOn),
            Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(SwordData::learned),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of("", "", "")).forGetter(SwordData::slots),
            Codec.STRING.listOf().optionalFieldOf("toggles", List.of()).forGetter(SwordData::toggles)
    ).apply(i, SwordData::new));

    public boolean knows(String id) { return learned.contains(id); }

    public boolean isOn(String id) { return toggles.contains(id); }

    public String slot(int i) { return i >= 0 && i < slots.size() ? slots.get(i) : ""; }

    public SwordData withStyle(String s) { return new SwordData(s, points, touki, toukiOn, learned, slots, toggles); }

    public SwordData withPoints(int p) { return new SwordData(style, Math.max(0, p), touki, toukiOn, learned, slots, toggles); }

    public SwordData withTouki(int t) { return new SwordData(style, points, t, toukiOn, learned, slots, toggles); }

    public SwordData withToukiOn(boolean on) { return new SwordData(style, points, touki, on, learned, slots, toggles); }

    public SwordData learn(String id) {
        if (knows(id)) return this;
        List<String> l = new ArrayList<>(learned);
        l.add(id);
        return new SwordData(style, points, touki, toukiOn, List.copyOf(l), slots, toggles);
    }

    public SwordData bind(int slot, String id) {
        List<String> s = new ArrayList<>(slots);
        while (s.size() < 3) s.add("");
        for (int i = 0; i < s.size(); i++) if (s.get(i).equals(id)) s.set(i, "");
        s.set(slot, id);
        return new SwordData(style, points, touki, toukiOn, learned, List.copyOf(s), toggles);
    }

    public SwordData toggle(String id) {
        List<String> t = new ArrayList<>(toggles);
        if (!t.remove(id)) t.add(id);
        return new SwordData(style, points, touki, toukiOn, learned, slots, List.copyOf(t));
    }
}

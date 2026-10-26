package net.zennin.mushoku.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Estado del arquero: escuela elegida, puntos, habilidades aprendidas y las 3 asignadas a Z/X/C. */
public record ArcherData(String school, int points, List<String> learned, List<String> slots) {
    public static final ArcherData DEFAULT = new ArcherData("", 0, List.of(), List.of("", "", ""));

    public static final Codec<ArcherData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("school", "").forGetter(ArcherData::school),
            Codec.INT.optionalFieldOf("points", 0).forGetter(ArcherData::points),
            Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(ArcherData::learned),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of("", "", "")).forGetter(ArcherData::slots)
    ).apply(i, ArcherData::new));

    public boolean knows(String id) { return learned.contains(id); }

    public String slot(int i) { return i >= 0 && i < slots.size() ? slots.get(i) : ""; }

    public ArcherData withSchool(String s) { return new ArcherData(s, points, learned, slots); }

    public ArcherData withPoints(int p) { return new ArcherData(school, Math.max(0, p), learned, slots); }

    public ArcherData learn(String id) {
        if (knows(id)) return this;
        List<String> l = new ArrayList<>(learned);
        l.add(id);
        return new ArcherData(school, points, List.copyOf(l), slots);
    }

    public ArcherData bind(int slot, String id) {
        List<String> s = new ArrayList<>(slots);
        while (s.size() < 3) s.add("");
        for (int i = 0; i < s.size(); i++) if (s.get(i).equals(id)) s.set(i, "");
        s.set(slot, id);
        return new ArcherData(school, points, learned, List.copyOf(s));
    }
}

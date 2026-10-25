package net.zennin.mushoku.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Datos de la Fase 6: ojos demoniacos (dos ranuras, cada uno encendido o no), conjuro silencioso
 * (3 hechizos asignados a Z/X/C), punto de regreso de la Clarividencia y centro del Vacio Absoluto.
 */
public record ExtraData(String eye1, String eye2, boolean on1, boolean on2, boolean silent, List<String> silentSlots,
                        Optional<Clair> clair, Optional<BlockPos> voidCenter) {
    public record Clair(double x, double y, double z, String dim, int mode) {
        public static final Codec<Clair> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Clair::x), Codec.DOUBLE.fieldOf("y").forGetter(Clair::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Clair::z), Codec.STRING.fieldOf("dim").forGetter(Clair::dim),
                Codec.INT.fieldOf("mode").forGetter(Clair::mode)).apply(i, Clair::new));
    }

    public static final ExtraData DEFAULT = new ExtraData("", "", false, false, false, List.of("", "", ""), Optional.empty(), Optional.empty());

    public static final Codec<ExtraData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("eye1", "").forGetter(ExtraData::eye1),
            Codec.STRING.optionalFieldOf("eye2", "").forGetter(ExtraData::eye2),
            Codec.BOOL.optionalFieldOf("on1", false).forGetter(ExtraData::on1),
            Codec.BOOL.optionalFieldOf("on2", false).forGetter(ExtraData::on2),
            Codec.BOOL.optionalFieldOf("silent", false).forGetter(ExtraData::silent),
            Codec.STRING.listOf().optionalFieldOf("silent_slots", List.of("", "", "")).forGetter(ExtraData::silentSlots),
            Clair.CODEC.optionalFieldOf("clair").forGetter(ExtraData::clair),
            BlockPos.CODEC.optionalFieldOf("void").forGetter(ExtraData::voidCenter)
    ).apply(i, ExtraData::new));

    public String eye(int slot) { return slot == 0 ? eye1 : eye2; }

    public boolean on(int slot) { return slot == 0 ? on1 : on2; }

    /** El ojo esta puesto y encendido en alguna ranura. */
    public boolean active(String eye) { return (eye1.equals(eye) && on1) || (eye2.equals(eye) && on2); }

    public boolean has(String eye) { return eye1.equals(eye) || eye2.equals(eye); }

    public ExtraData withEye(int slot, String eye) {
        return slot == 0 ? new ExtraData(eye, eye2, false, on2, silent, silentSlots, clair, voidCenter)
                : new ExtraData(eye1, eye, on1, false, silent, silentSlots, clair, voidCenter);
    }

    public ExtraData withOn(int slot, boolean on) {
        return slot == 0 ? new ExtraData(eye1, eye2, on, on2, silent, silentSlots, clair, voidCenter)
                : new ExtraData(eye1, eye2, on1, on, silent, silentSlots, clair, voidCenter);
    }

    public ExtraData withSilent(boolean s) { return new ExtraData(eye1, eye2, on1, on2, s, silentSlots, clair, voidCenter); }

    public ExtraData withSilentSlot(int slot, String spell) {
        List<String> l = new ArrayList<>(silentSlots);
        while (l.size() < 3) l.add("");
        l.set(slot, spell);
        return new ExtraData(eye1, eye2, on1, on2, silent, List.copyOf(l), clair, voidCenter);
    }

    public String silentSlot(int i) { return i < silentSlots.size() ? silentSlots.get(i) : ""; }

    public ExtraData withClair(Optional<Clair> c) { return new ExtraData(eye1, eye2, on1, on2, silent, silentSlots, c, voidCenter); }

    public ExtraData withVoid(Optional<BlockPos> v) { return new ExtraData(eye1, eye2, on1, on2, silent, silentSlots, clair, v); }
}

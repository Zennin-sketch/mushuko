package net.zennin.mushoku.race;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Rasgo visible de la raza que se dibuja sobre la cabeza.
 * kind: "" (nada), "elf_ears", "demon_horns", "beast_ears", "superd_gem".
 * variant: animal de las orejas de bestia (0 gato, 1 perro, 2 oso). height: 0 baja, 1 media, 2 alta.
 */
public record Appearance(String kind, int variant, int height) {
    public static final Appearance NONE = new Appearance("", 0, 1);
    public static final Codec<Appearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("kind", "").forGetter(Appearance::kind),
            Codec.INT.optionalFieldOf("variant", 0).forGetter(Appearance::variant),
            Codec.INT.optionalFieldOf("height", 1).forGetter(Appearance::height)
    ).apply(i, Appearance::new));

    public static final String[] BEAST_MODELS = {"beast_cat", "beast_dog", "beast_bear"};

    /** Rasgo que corresponde a cada raza. */
    public static Appearance forRace(Race r, int variant, int height) {
        if (r == null) return NONE;
        int v = Math.max(0, Math.min(2, variant)), h = Math.max(0, Math.min(2, height));
        return switch (r) {
            case ELF -> new Appearance("elf_ears", 0, h);
            case DEMON -> new Appearance("demon_horns", 0, h);
            case BEAST -> new Appearance("beast_ears", v, 0);
            case SUPERD -> new Appearance("superd_gem", 0, 0);
            default -> NONE;
        };
    }

    /** Nombre del modelo (assets/remushoku/cosmetics/<modelo>.json) o null si no tiene. */
    public String model() {
        return switch (kind) {
            case "elf_ears", "demon_horns", "superd_gem" -> kind;
            case "beast_ears" -> BEAST_MODELS[Math.max(0, Math.min(2, variant))];
            default -> null;
        };
    }
}

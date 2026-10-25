package net.zennin.mushoku.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Lo que el menu del personaje necesita mostrar para un jugador. line = ultima frase dicha (clave de idioma). */
public record CompanionState(int entityId, int hearts, int mood, int mode, boolean married, boolean marriedOther,
                             boolean romance, boolean hasHome, String kind, String line) {
    public static final Codec<CompanionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("e").forGetter(CompanionState::entityId),
            Codec.INT.fieldOf("h").forGetter(CompanionState::hearts),
            Codec.INT.fieldOf("m").forGetter(CompanionState::mood),
            Codec.INT.fieldOf("mo").forGetter(CompanionState::mode),
            Codec.BOOL.fieldOf("ma").forGetter(CompanionState::married),
            Codec.BOOL.fieldOf("mao").forGetter(CompanionState::marriedOther),
            Codec.BOOL.fieldOf("r").forGetter(CompanionState::romance),
            Codec.BOOL.fieldOf("ho").forGetter(CompanionState::hasHome),
            Codec.STRING.fieldOf("k").forGetter(CompanionState::kind),
            Codec.STRING.fieldOf("l").forGetter(CompanionState::line)
    ).apply(i, CompanionState::new));
}
